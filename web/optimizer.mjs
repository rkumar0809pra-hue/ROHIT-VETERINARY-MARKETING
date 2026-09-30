import { createCipheriv, createDecipheriv, createHash, randomBytes, randomUUID } from 'node:crypto';

const SCOPE = 'https://www.googleapis.com/auth/webmasters.readonly';
const STATE_TTL_MS = 10 * 60 * 1000;
const PERIODS = { '7d': 7, '28d': 28, '3m': 90 };
const TOKEN_AAD = Buffer.from('rvh-google-search-console-v1');

export function createOptimizer({db,env,audit,json,body,fail,fetchImpl=fetch}) {
  db.exec(`CREATE TABLE IF NOT EXISTS optimizer_tasks(id TEXT PRIMARY KEY,data TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS optimizer_reports(id TEXT PRIMARY KEY,data TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS optimizer_google(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS optimizer_oauth_states(hash TEXT PRIMARY KEY,created_at INTEGER NOT NULL);`);

  const tasks=()=>db.prepare('SELECT data FROM optimizer_tasks ORDER BY rowid DESC LIMIT 300').all().map(x=>JSON.parse(x.data));
  const reports=()=>db.prepare('SELECT data FROM optimizer_reports ORDER BY rowid DESC LIMIT 30').all().map(x=>JSON.parse(x.data));
  const save=t=>db.prepare('INSERT OR REPLACE INTO optimizer_tasks VALUES(?,?)').run(t.id,JSON.stringify(t));
  const readGoogle=()=>{try{return JSON.parse(db.prepare('SELECT data FROM optimizer_google WHERE id=1').get()?.data||'null');}catch{return null;}};
  const writeGoogle=data=>db.prepare('INSERT OR REPLACE INTO optimizer_google VALUES(1,?)').run(JSON.stringify(data));
  const appOrigin=()=>{try{return new URL(env.APP_ORIGIN||'http://localhost:3000').origin;}catch{return '';}};
  const googleConfig=()=>{
    const clientId=env.GOOGLE_CLIENT_ID||env.GSC_CLIENT_ID||'';
    const clientSecret=env.GOOGLE_CLIENT_SECRET||env.GSC_CLIENT_SECRET||'';
    const redirectUri=env.GOOGLE_REDIRECT_URI||`${appOrigin()}/api/auth/google/callback`;
    let valid=false;
    try{const u=new URL(redirectUri);valid=Boolean(clientId&&clientSecret&&u.origin===appOrigin()&&u.pathname==='/api/auth/google/callback');}catch{}
    return {clientId,clientSecret,redirectUri,valid};
  };
  const key=()=>createHash('sha256').update(`rvh-gsc\0${googleConfig().clientSecret}\0${appOrigin()}`).digest();
  const seal=value=>{
    const iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',key(),iv);cipher.setAAD(TOKEN_AAD);
    const encrypted=Buffer.concat([cipher.update(value,'utf8'),cipher.final()]),tag=cipher.getAuthTag();
    return `v1.${iv.toString('base64url')}.${tag.toString('base64url')}.${encrypted.toString('base64url')}`;
  };
  const open=value=>{
    const [version,iv,tag,data]=String(value||'').split('.');if(version!=='v1'||!iv||!tag||!data)throw Error('invalid token');
    const decipher=createDecipheriv('aes-256-gcm',key(),Buffer.from(iv,'base64url'));decipher.setAAD(TOKEN_AAD);decipher.setAuthTag(Buffer.from(tag,'base64url'));
    return Buffer.concat([decipher.update(Buffer.from(data,'base64url')),decipher.final()]).toString('utf8');
  };
  const publicSearchConsole=()=>{
    const g=readGoogle();
    return {configured:googleConfig().valid,connected:Boolean(g?.refreshToken),properties:(g?.properties||[]).map(x=>x.siteUrl),connectedAt:g?.connectedAt||null,lastSyncAt:g?.lastSyncAt||null};
  };
  const redirect=(res,location,status=303)=>{res.writeHead(status,{Location:location,'Cache-Control':'no-store'});res.end();};
  const googleError=(message='Google Search Console request failed. Reconnect Google and try again.')=>fail(502,message);
  const parseJson=async response=>{try{return await response.json();}catch{return {};}};
  async function accessToken() {
    const g=readGoogle();if(!g?.refreshToken)throw fail(409,'Connect Google Search Console first.');
    let refreshToken;try{refreshToken=open(g.refreshToken);}catch{throw fail(409,'Stored Google authorization can no longer be read. Reconnect Google Search Console.');}
    const cfg=googleConfig();if(!cfg.valid)throw fail(503,'Google OAuth setup is incomplete on the server.');
    let response;try{response=await fetchImpl('https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({client_id:cfg.clientId,client_secret:cfg.clientSecret,refresh_token:refreshToken,grant_type:'refresh_token'}),signal:AbortSignal.timeout(15000),redirect:'error'});}catch{throw googleError();}
    const data=await parseJson(response);if(!response.ok||typeof data.access_token!=='string'||!data.access_token)throw googleError('Google authorization expired or was revoked. Reconnect Google Search Console.');
    return data.access_token;
  }
  async function listSites(token) {
    let response;try{response=await fetchImpl('https://www.googleapis.com/webmasters/v3/sites',{headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(15000),redirect:'error'});}catch{throw googleError();}
    const data=await parseJson(response);if(!response.ok)throw googleError();
    return (Array.isArray(data.siteEntry)?data.siteEntry:[]).filter(x=>typeof x?.siteUrl==='string'&&x.siteUrl.length<=1000).slice(0,500).map(x=>({siteUrl:x.siteUrl,permissionLevel:typeof x.permissionLevel==='string'?x.permissionLevel:''}));
  }
  async function query(token,property,payload) {
    let response;try{response=await fetchImpl(`https://www.googleapis.com/webmasters/v3/sites/${encodeURIComponent(property)}/searchAnalytics/query`,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify(payload),signal:AbortSignal.timeout(20000),redirect:'error'});}catch{throw googleError();}
    const data=await parseJson(response);if(!response.ok)throw googleError('Search Console could not read this property. Check that the connected Google account has access.');
    return Array.isArray(data.rows)?data.rows:[];
  }
  const metric=row=>({clicks:Number(row?.clicks||0),impressions:Number(row?.impressions||0),ctr:Number(row?.ctr||0),position:Number(row?.position||0)});
  const validMetric=m=>Object.values(m).every(v=>Number.isFinite(v)&&v>=0);
  const dateRange=period=>{
    const days=PERIODS[period];if(!days)throw fail(400,'Choose 7 days, 28 days or 3 months.');
    const end=new Date();end.setUTCHours(0,0,0,0);end.setUTCDate(end.getUTCDate()-3);
    const start=new Date(end);start.setUTCDate(start.getUTCDate()-(days-1));
    const previousEnd=new Date(start);previousEnd.setUTCDate(previousEnd.getUTCDate()-1);
    const previousStart=new Date(previousEnd);previousStart.setUTCDate(previousStart.getUTCDate()-(days-1));
    const iso=d=>d.toISOString().slice(0,10);
    return {days,startDate:iso(start),endDate:iso(end),previousStartDate:iso(previousStart),previousEndDate:iso(previousEnd)};
  };
  const normalizeRows=(rows,label)=>rows.map(row=>{const m=metric(row);if(!validMetric(m))throw googleError('Google returned invalid Search Console metrics.');const key=Array.isArray(row.keys)?String(row.keys[0]||''):'';return {[label]:key,...m};});
  async function refreshProperties() {
    const token=await accessToken(),sites=await listSites(token),g=readGoogle();
    if(!g)throw fail(409,'Connect Google Search Console first.');
    writeGoogle({...g,properties:sites,updatedAt:new Date().toISOString()});
    return sites;
  }

  return {
    state:()=>({tasks:tasks(),reports:reports(),searchConsole:publicSearchConsole()}),
    async route(req,res,path,role){
      const optimizerPath=path.startsWith('/api/optimizer/');
      const oauthPath=path==='/api/auth/google'||path==='/api/auth/google/callback';
      const searchConsolePath=path.startsWith('/api/search-console/');
      if(!optimizerPath&&!oauthPath&&!searchConsolePath)return false;
      if(role!=='owner')throw fail(403,'Only the owner can manage optimizer tasks and reports.');

      if(path==='/api/auth/google'&&req.method==='GET'){
        const cfg=googleConfig();if(!cfg.valid)throw fail(503,'Google OAuth setup is incomplete. Check GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI and APP_ORIGIN.');
        db.prepare('DELETE FROM optimizer_oauth_states WHERE created_at<?').run(Date.now()-STATE_TTL_MS);
        const state=randomBytes(32).toString('base64url'),hash=createHash('sha256').update(state).digest('hex');
        db.prepare('INSERT INTO optimizer_oauth_states VALUES(?,?)').run(hash,Date.now());
        const u=new URL('https://accounts.google.com/o/oauth2/v2/auth');
        for(const [k,v] of Object.entries({client_id:cfg.clientId,redirect_uri:cfg.redirectUri,response_type:'code',scope:SCOPE,access_type:'offline',prompt:'consent',state}))u.searchParams.set(k,v);
        audit('search_console_oauth_started');redirect(res,u.toString(),302);return true;
      }

      if(path==='/api/auth/google/callback'&&req.method==='GET'){
        const cfg=googleConfig();if(!cfg.valid)throw fail(503,'Google OAuth setup is incomplete on the server.');
        const u=new URL(req.url,appOrigin()),state=u.searchParams.get('state')||'',code=u.searchParams.get('code')||'',oauthError=u.searchParams.get('error');
        if(oauthError)throw fail(400,'Google authorization was cancelled or denied.');
        if(!/^[A-Za-z0-9_-]{20,200}$/.test(state)||code.length<10||code.length>5000)throw fail(400,'Invalid Google OAuth callback.');
        const stateHash=createHash('sha256').update(state).digest('hex'),saved=db.prepare('SELECT created_at FROM optimizer_oauth_states WHERE hash=?').get(stateHash);
        db.prepare('DELETE FROM optimizer_oauth_states WHERE hash=?').run(stateHash);
        if(!saved||Date.now()-saved.created_at>STATE_TTL_MS)throw fail(400,'Google OAuth session expired. Start the connection again.');
        let response;try{response=await fetchImpl('https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({client_id:cfg.clientId,client_secret:cfg.clientSecret,code,grant_type:'authorization_code',redirect_uri:cfg.redirectUri}),signal:AbortSignal.timeout(15000),redirect:'error'});}catch{throw googleError('Google authorization could not be completed. Try connecting again.');}
        const data=await parseJson(response);if(!response.ok||typeof data.access_token!=='string')throw googleError('Google authorization could not be completed. Try connecting again.');
        const previous=readGoogle();let refreshToken=typeof data.refresh_token==='string'&&data.refresh_token?data.refresh_token:null;
        if(!refreshToken&&previous?.refreshToken){try{refreshToken=open(previous.refreshToken);}catch{}}
        if(!refreshToken)throw googleError('Google did not provide offline access. Reconnect and approve Search Console access again.');
        const sites=await listSites(data.access_token),now=new Date().toISOString();
        writeGoogle({refreshToken:seal(refreshToken),properties:sites,connectedAt:previous?.connectedAt||now,updatedAt:now,lastSyncAt:previous?.lastSyncAt||null});
        audit('search_console_connected');redirect(res,`${appOrigin()}/?gsc=connected`);return true;
      }

      if(path==='/api/search-console/status'&&req.method==='GET'){
        const current=publicSearchConsole();if(!current.connected){json(res,200,current);return true;}
        await refreshProperties();json(res,200,publicSearchConsole());return true;
      }

      if(path==='/api/search-console/disconnect'&&req.method==='POST'){
        const g=readGoogle();
        if(g?.refreshToken){try{const token=open(g.refreshToken);await fetchImpl('https://oauth2.googleapis.com/revoke',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({token}),signal:AbortSignal.timeout(10000),redirect:'error'});}catch{}}
        db.prepare('DELETE FROM optimizer_google WHERE id=1').run();audit('search_console_disconnected');json(res,200,{connected:false});return true;
      }

      if((path==='/api/search-console/sync'||path==='/api/optimizer/search-console')&&req.method==='POST'){
        if(!googleConfig().valid)throw fail(503,'Google OAuth setup is incomplete on the server.');
        if(!readGoogle()?.refreshToken)throw fail(409,'Connect Google Search Console first.');
        const d=await body(req),period=typeof d.period==='string'?d.period:'28d',range=dateRange(period),token=await accessToken(),sites=await listSites(token);
        if(typeof d.property!=='string'||!sites.some(x=>x.siteUrl===d.property))throw fail(400,'Choose a Search Console property available to the connected Google account.');
        const base={startDate:range.startDate,endDate:range.endDate,dataState:'final',type:'web'};
        const [totalsRows,trendRows,queryRows,pageRows,previousPageRows]=await Promise.all([
          query(token,d.property,{...base,rowLimit:1}),
          query(token,d.property,{...base,dimensions:['date'],rowLimit:25000}),
          query(token,d.property,{...base,dimensions:['query'],rowLimit:100}),
          query(token,d.property,{...base,dimensions:['page'],rowLimit:100}),
          query(token,d.property,{...base,startDate:range.previousStartDate,endDate:range.previousEndDate,dimensions:['page'],rowLimit:100}),
        ]);
        const totals=totalsRows[0]?metric(totalsRows[0]):null;if(totals&&!validMetric(totals))throw googleError('Google returned invalid Search Console metrics.');
        const trend=normalizeRows(trendRows,'date'),queries=normalizeRows(queryRows,'query'),pages=normalizeRows(pageRows,'page'),previousPages=normalizeRows(previousPageRows,'page');
        const previousMap=new Map(previousPages.map(x=>[x.page,x.clicks]));
        const decliningPages=pages.map(x=>({...x,previousClicks:previousMap.get(x.page)||0,clickChange:x.clicks-(previousMap.get(x.page)||0)})).filter(x=>x.previousClicks>0&&x.clickChange<0).sort((a,b)=>a.clickChange-b.clickChange).slice(0,10);
        const highImpressionLowCtr=queries.filter(x=>x.impressions>=10&&x.ctr<0.03).sort((a,b)=>b.impressions-a.impressions).slice(0,10);
        const positionOpportunities=queries.filter(x=>x.position>=4&&x.position<=20).sort((a,b)=>b.impressions-a.impressions).slice(0,10);
        const report={id:randomUUID(),property:d.property,period,startDate:range.startDate,endDate:range.endDate,receivedAt:new Date().toISOString(),metrics:totals,trend,queries,pages,opportunities:{highImpressionLowCtr,decliningPages,positionOpportunities}};
        db.prepare('INSERT INTO optimizer_reports VALUES(?,?)').run(report.id,JSON.stringify(report));db.exec('DELETE FROM optimizer_reports WHERE rowid NOT IN (SELECT rowid FROM optimizer_reports ORDER BY rowid DESC LIMIT 30)');
        const g=readGoogle();writeGoogle({...g,properties:sites,lastSyncAt:report.receivedAt,updatedAt:report.receivedAt});audit('search_console_report_imported');json(res,200,report);return true;
      }

      if(path==='/api/search-console/performance'&&req.method==='GET'){
        const u=new URL(req.url,appOrigin()),property=u.searchParams.get('property'),period=u.searchParams.get('period');
        const report=reports().find(r=>(!property||r.property===property)&&(!period||r.period===period))||null;json(res,200,{report});return true;
      }

      if(path==='/api/optimizer/prepare'&&req.method==='POST'){
        const seo=JSON.parse(db.prepare('SELECT data FROM seo WHERE id=1').get()?.data||'{}');
        const existing=tasks();let added=0;
        for(const scan of seo.scans||[])for(const check of scan.checks.filter(c=>!c.passed)){
          const key=scan.url+'|'+check.label;if(existing.some(t=>t.key===key))continue;
          if(existing.length+added>=300)break;
          const t={id:randomUUID(),key,kind:'SEO',title:check.label,url:scan.url,evidence:check.observed,recommendation:check.action,priority:['Page response','Indexing directive','Page title'].includes(check.label)?'High':'Medium',status:'review',createdAt:new Date().toISOString(),history:[]};save(t);added++;
        }
        audit('optimizer_tasks_prepared');json(res,200,{added});return true;
      }
      const match=/^\/api\/optimizer\/tasks\/([a-f0-9-]+)$/.exec(path);
      if(match&&req.method==='PATCH'){
        const d=await body(req),t=tasks().find(t=>t.id===match[1]);if(!t)throw fail(404,'Task not found.');
        const allowed={review:['approved','dismissed'],approved:['completed','review'],completed:['review'],dismissed:['review']};
        if(!allowed[t.status].includes(d.status))throw fail(400,'Invalid task transition. Review and approve the task first.');
        if(typeof d.note!=='string'||d.note.length>1000||(d.status==='completed'&&!d.note.trim()))throw fail(400,'Add a completion note describing the change and verification.');
        t.history.push({from:t.status,to:d.status,note:d.note,at:new Date().toISOString()});t.status=d.status;save(t);audit('optimizer_task_'+d.status,t.id);json(res,200,t);return true;
      }
      return false;
    }
  };
}
