import { createHash } from 'node:crypto';

// Read-only integration. No provider write calls, ad creation, or messages.
export function createMetaConnection({db,env,json,fail,audit,fetchImpl=fetch}) {
  db.exec("CREATE TABLE IF NOT EXISTS meta_connection(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL)");
  const version=env.META_API_VERSION||'v25.0';
  const configured=()=>Boolean(env.META_PAGE_ID&&env.META_PAGE_ACCESS_TOKEN);
  const fingerprint=()=>createHash('sha256').update([env.META_PAGE_ID,env.META_PAGE_ACCESS_TOKEN,env.META_INSTAGRAM_ACCOUNT_ID,version].join('|')).digest('hex');
  const read=()=>{const d=JSON.parse(db.prepare('SELECT data FROM meta_connection WHERE id=1').get()?.data||'{}');return d.fingerprint===fingerprint()?d:{};};
  const save=d=>db.prepare('INSERT OR REPLACE INTO meta_connection VALUES(1,?)').run(JSON.stringify({...d,fingerprint:fingerprint()}));
  const state=()=>{const {fingerprint:_,...data}=read();return {configured:configured(),apiVersion:version,status:'Not checked',...data};};
  async function get(path,fields) {
    const url=new URL(`https://graph.facebook.com/${version}/${path}`);url.searchParams.set('fields',fields);url.searchParams.set('limit','10');
    let r,data;
    try {r=await fetchImpl(url.href,{method:'GET',headers:{Authorization:`Bearer ${env.META_PAGE_ACCESS_TOKEN}`},redirect:'error',signal:AbortSignal.timeout(15000)});data=await r.json();}
    catch {throw Error('Meta could not be reached. Try checking the connection again later.');}
    if(!r.ok||data.error) {
      const code=Number(data.error?.code);
      // Do not expose provider messages/URLs; they can contain credentials.
      throw Error(code===190?'Meta token expired or is invalid. Replace the Page access token in Render.':code===10||code===200?'Meta permission is missing. Check Page access and app permissions.':code===4||code===17||code===32||code===613?'Meta rate limit reached. Check again later.':`Meta returned HTTP ${r.status}. Check the Page ID, token and permissions.`);
    }
    return data;
  }
  const id=x=>/^\d+$/.test(String(x||''));
  const safeLink=value=>{try{const u=new URL(value);return u.protocol==='https:'&&['www.facebook.com','facebook.com','www.instagram.com','instagram.com'].includes(u.hostname)?u.href:'';}catch{return '';}};
  let busy=false, lastAttempt=0;
  return {state, async route(req,res,path,role){
    if(path!=='/api/meta/check'||req.method!=='POST')return false;
    if(role!=='owner')throw fail(403,'Only the owner can verify Meta connections.');
    if(!configured())throw fail(503,'Add META_PAGE_ID and META_PAGE_ACCESS_TOKEN in Render Environment first.');
    if(!id(env.META_PAGE_ID)||!/^v\d+\.0$/.test(version)||(env.META_INSTAGRAM_ACCOUNT_ID&&!id(env.META_INSTAGRAM_ACCOUNT_ID)))throw fail(400,'Check the Meta Page ID, Instagram account ID and API version in Render.');
    if(busy||Date.now()-lastAttempt<10000)throw fail(429,'Wait a few seconds before checking again.');
    busy=true;lastAttempt=Date.now();
    try {
      const page=await get(env.META_PAGE_ID,'id,name');
      if(page.id!==env.META_PAGE_ID||!page.name)throw Error('Meta returned a different Page. Check META_PAGE_ID.');
      const snapshot={status:'Verified',checkedAt:new Date().toISOString(),page:{id:page.id,name:String(page.name).slice(0,300)},posts:[],instagram:null,notes:[]};
      try {const result=await get(`${page.id}/published_posts`,'id,message,created_time,permalink_url');snapshot.posts=(result.data||[]).slice(0,10).map(p=>({id:String(p.id),message:String(p.message||'').slice(0,5000),createdAt:p.created_time,url:safeLink(p.permalink_url)}));}
      catch(e){snapshot.notes.push('Recent Facebook posts: '+e.message);}
      if(env.META_INSTAGRAM_ACCOUNT_ID) {
        try {const ig=await get(env.META_INSTAGRAM_ACCOUNT_ID,'id,username,media_count');if(ig.id!==env.META_INSTAGRAM_ACCOUNT_ID)throw Error('Instagram account ID does not match.');snapshot.instagram={id:ig.id,username:String(ig.username||''),mediaCount:Number(ig.media_count)||0};}
        catch(e){snapshot.notes.push('Instagram: '+e.message);}
      } else snapshot.notes.push('Instagram is not configured yet.');
      save(snapshot);audit('meta_connection_checked');json(res,200,state());
    }catch(e){save({status:'Needs attention',checkedAt:new Date().toISOString(),error:e.message});throw fail(502,e.message);}
    finally{busy=false;}
    return true;
  }};
}
