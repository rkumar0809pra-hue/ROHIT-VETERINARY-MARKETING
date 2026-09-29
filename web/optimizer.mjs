import { randomUUID } from 'node:crypto';
const properties = ['https://www.rohitveterinary.com/', 'https://app.rohitveterinary.com/'];
export function createOptimizer({db,env,audit,json,body,fail,fetchImpl=fetch}) {
 db.exec("CREATE TABLE IF NOT EXISTS optimizer_tasks(id TEXT PRIMARY KEY,data TEXT NOT NULL); CREATE TABLE IF NOT EXISTS optimizer_reports(id TEXT PRIMARY KEY,data TEXT NOT NULL)");
 const tasks=()=>db.prepare('SELECT data FROM optimizer_tasks ORDER BY rowid DESC LIMIT 300').all().map(x=>JSON.parse(x.data));
 const reports=()=>db.prepare('SELECT data FROM optimizer_reports ORDER BY rowid DESC LIMIT 30').all().map(x=>JSON.parse(x.data));
 const configured=()=>Boolean(env.GSC_CLIENT_ID&&env.GSC_CLIENT_SECRET&&env.GSC_REFRESH_TOKEN);
 const save=t=>db.prepare('INSERT OR REPLACE INTO optimizer_tasks VALUES(?,?)').run(t.id,JSON.stringify(t));
 return {state:()=>({tasks:tasks(),reports:reports(),searchConsole:{configured:configured(),properties}}),
 async route(req,res,path,role){
 if(!path.startsWith('/api/optimizer/'))return false;
 if(role!=='owner')throw fail(403,'Only the owner can manage optimizer tasks and reports.');
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
 if(path==='/api/optimizer/search-console'&&req.method==='POST'){
 if(!configured())throw fail(503,'Search Console setup needed. Configure server OAuth credentials with read-only Search Console access.');
 const d=await body(req);if(!properties.includes(d.property))throw fail(400,'Choose a supported Search Console property.');
 const end=new Date();end.setUTCDate(end.getUTCDate()-3);const start=new Date(end);start.setUTCDate(start.getUTCDate()-27);
 const startDate=start.toISOString().slice(0,10),endDate=end.toISOString().slice(0,10);
 try{
 const auth=await fetchImpl('https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({client_id:env.GSC_CLIENT_ID,client_secret:env.GSC_CLIENT_SECRET,refresh_token:env.GSC_REFRESH_TOKEN,grant_type:'refresh_token'}),signal:AbortSignal.timeout(15000),redirect:'error'});
 if(!auth.ok)throw Error();const token=await auth.json();if(!token.access_token)throw Error();
 const r=await fetchImpl('https://www.googleapis.com/webmasters/v3/sites/'+encodeURIComponent(d.property)+'/searchAnalytics/query',{method:'POST',headers:{Authorization:'Bearer '+token.access_token,'Content-Type':'application/json'},body:JSON.stringify({startDate,endDate,dataState:'final',type:'web'}),signal:AbortSignal.timeout(15000),redirect:'error'});
 if(!r.ok)throw Error();const data=await r.json();
 const row=data.rows?.[0];if(row&&['clicks','impressions','ctr','position'].some(k=>typeof row[k]!=='number'||!Number.isFinite(row[k])||row[k]<0))throw Error();
 const report={id:randomUUID(),property:d.property,startDate,endDate,receivedAt:new Date().toISOString(),metrics:row?Object.fromEntries(['clicks','impressions','ctr','position'].map(k=>[k,row[k]])):null};
 db.prepare('INSERT INTO optimizer_reports VALUES(?,?)').run(report.id,JSON.stringify(report));db.exec('DELETE FROM optimizer_reports WHERE rowid NOT IN (SELECT rowid FROM optimizer_reports ORDER BY rowid DESC LIMIT 30)');audit('search_console_report_imported');json(res,200,report);return true;
 }catch{throw fail(502,'Search Console import failed. Check OAuth credentials, API access and permission for this exact property.');}
 }
 return false;
 }};
}
