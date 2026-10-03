import {createHash,timingSafeEqual} from 'node:crypto';
export const APP_SOURCES={
 clinic:{name:'Clinic app',url:'https://app.rohitveterinary.com',key:'RVH_CLINIC_SYNC_TOKEN',metrics:['appointmentsBooked','completedVisits','revenueINR']},
 mart:{name:'Vet Mart',url:'https://mart.rohitveterinary.com',key:'RVH_MART_SYNC_TOKEN',metrics:['ordersPlaced','ordersCompleted','netSalesINR']},
 chat:{name:'Chatbot',url:'https://chat.rohitveterinary.com',key:'RVH_CHAT_SYNC_TOKEN',metrics:['enquiries','bookingRequests','humanHandoffs']},
};
const hash=s=>createHash('sha256').update(s).digest();
const istDay=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Kolkata',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export function createAppConnections({db,env,json,body,fail,audit}){
 db.exec(`CREATE TABLE IF NOT EXISTS app_connections(source TEXT PRIMARY KEY,enabled INTEGER NOT NULL DEFAULT 0);
 CREATE TABLE IF NOT EXISTS app_daily(source TEXT NOT NULL,day TEXT NOT NULL,observed_at TEXT NOT NULL,metrics TEXT NOT NULL,received_at TEXT NOT NULL,credential_hash TEXT NOT NULL,PRIMARY KEY(source,day));`);
 const enabled=source=>Boolean(db.prepare('SELECT enabled FROM app_connections WHERE source=?').get(source)?.enabled);
 const configured=source=>{const value=env[APP_SOURCES[source].key];return typeof value==='string'&&value.length>=32&&!Object.values(APP_SOURCES).some(x=>x.key!==APP_SOURCES[source].key&&env[x.key]===value)&&![env.ADMIN_PASSWORD,env.OPENAI_API_KEY,env.GEMINI_API_KEY,env.META_PAGE_ACCESS_TOKEN].includes(value);};
 const rows=source=>db.prepare('SELECT day,observed_at,metrics,received_at FROM app_daily WHERE source=? ORDER BY day DESC LIMIT 30').all(source).map(r=>({...r,metrics:JSON.parse(r.metrics)}));
 const state=()=>Object.entries(APP_SOURCES).map(([id,s])=>{
  const last=db.prepare('SELECT received_at,observed_at,credential_hash FROM app_daily WHERE source=? ORDER BY received_at DESC LIMIT 1').get(id);
  const verified=configured(id)&&last?.credential_hash===hash(env[s.key]).toString('hex');
  return {id,...s,enabled:enabled(id),configured:configured(id),status:!enabled(id)?'Paused':!configured(id)?'Setup needed':!verified?'Waiting for first sync':Date.now()-Math.min(Date.parse(last.received_at),Date.parse(last.observed_at))>48*3600000?'Sync overdue':'Receiving data',lastReceivedAt:last?.received_at||null,reports:rows(id)};
 });
 const limits=new Map();
 return {state,summary:()=>state().map(s=>({source:s.id,status:s.status,latest:s.reports[0]||null})),
 async machineRoute(req,res,path){
  const match=/^\/api\/integrations\/(clinic|mart|chat)\/(health|daily)$/.exec(path);if(!match)return false;
  const [,source,action]=match;
  const supplied=/^Bearer ([^\s]+)$/.exec(req.headers.authorization||'')?.[1]||'';
  if(!configured(source)||!timingSafeEqual(hash(supplied),hash(env[APP_SOURCES[source].key]||'')))throw fail(401,'Invalid integration credentials.');
  if(!enabled(source))throw fail(403,'This connection is paused. Enable it in Connected apps.');
  const window=Math.floor(Date.now()/3600000),limit=limits.get(source)||{window,count:0};if(limit.window!==window){limit.window=window;limit.count=0;}limits.set(source,limit);if(++limit.count>600)throw fail(429,'Integration request limit reached. Retry later.');
  if(action==='health'&&req.method==='GET'){json(res,200,{ok:true,source,schemaVersion:1,timezone:'Asia/Kolkata',mode:'daily-aggregate-receiver'});return true;}
  if(action!=='daily'||req.method!=='POST')throw fail(405,'Unsupported integration method.');
  const data=await body(req,8192);
  if(Object.keys(data).some(k=>!['schemaVersion','date','timezone','observedAt','metrics'].includes(k))||data.schemaVersion!==1||data.timezone!=='Asia/Kolkata')throw fail(400,'Send only schemaVersion, date, timezone, observedAt and metrics.');
  const day=data.date;
  if(typeof day!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(day)||!Number.isFinite(Date.parse(day))||new Date(day).toISOString().slice(0,10)!==day||day>istDay()||Date.parse(day)<Date.now()-366*86400000)throw fail(400,'Choose a valid date in the last year, using India time.');
  const observed=Date.parse(data.observedAt);
  if(typeof data.observedAt!=='string'||!/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(data.observedAt)||!Number.isFinite(observed)||observed>Date.now()+300000||observed<Date.parse(day)-19800000)throw fail(400,'observedAt must be a valid timestamp after the start of the reporting day.');
  const metrics=data.metrics,keys=APP_SOURCES[source].metrics;
  if(!metrics||typeof metrics!=='object'||Array.isArray(metrics)||Object.keys(metrics).length!==keys.length||Object.keys(metrics).some(k=>!keys.includes(k)))throw fail(400,'Supply exactly the supported aggregate metrics; no customer or patient data.');
  for(const k of keys)if(typeof metrics[k]!=='number'||!Number.isFinite(metrics[k])||metrics[k]<0||metrics[k]>1e10||(!k.endsWith('INR')&&!Number.isInteger(metrics[k]))||(k.endsWith('INR')&&Math.abs(metrics[k]*100-Math.round(metrics[k]*100))>0.0001))throw fail(400,'Metrics must be non-negative counts or INR amounts with up to two decimals.');
  const normalized=JSON.stringify(Object.fromEntries(keys.map(k=>[k,metrics[k]]))),observedAt=new Date(observed).toISOString();
  const old=db.prepare('SELECT * FROM app_daily WHERE source=? AND day=?').get(source,day);
  if(old&&(old.observed_at>observedAt||(old.observed_at===observedAt&&old.metrics!==normalized)))throw fail(409,'Newer or conflicting snapshot already exists. Recompute a fresh daily snapshot.');
  const duplicate=Boolean(old&&old.observed_at===observedAt&&old.metrics===normalized);
  // An identical retry is safe; replacement snapshots are never added together.
  db.prepare('INSERT INTO app_daily VALUES(?,?,?,?,?,?) ON CONFLICT(source,day) DO UPDATE SET observed_at=excluded.observed_at,metrics=excluded.metrics,received_at=excluded.received_at,credential_hash=excluded.credential_hash').run(source,day,observedAt,normalized,new Date().toISOString(),hash(env[APP_SOURCES[source].key]).toString('hex'));
  db.prepare('DELETE FROM app_daily WHERE day<?').run(new Date(Date.now()-400*86400000).toISOString().slice(0,10));
  if(!duplicate)audit('app_daily_received',source+':'+day);
  json(res,200,{ok:true,source,date:day,duplicate});return true;
 },
 async route(req,res,path,role){
  const match=/^\/api\/connected-apps\/(clinic|mart|chat)$/.exec(path);if(!match||req.method!=='PATCH')return false;
  if(role!=='owner')throw fail(403,'Only the owner can manage app connections.');
  const data=await body(req);if(typeof data.enabled!=='boolean')throw fail(400,'Choose enabled or paused.');
  db.prepare('INSERT INTO app_connections VALUES(?,?) ON CONFLICT(source) DO UPDATE SET enabled=excluded.enabled').run(match[1],Number(data.enabled));
  audit(data.enabled?'app_connection_enabled':'app_connection_paused',match[1]);json(res,200,state());return true;
 }};
}
