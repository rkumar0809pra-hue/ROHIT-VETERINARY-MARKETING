import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createApp} from '../server.mjs';
import {sendDailyReport} from '../integrations/send-daily.mjs';
const tokens={clinic:'c'.repeat(40),mart:'m'.repeat(40),chat:'h'.repeat(40)};
const date=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Kolkata',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const snapshot=(metrics={appointmentsBooked:2,completedVisits:1,revenueINR:500})=>({schemaVersion:1,date,timezone:'Asia/Kolkata',observedAt:new Date().toISOString(),metrics});
async function fixture(t,extra={}){
 const env={ADMIN_PASSWORD:'owner-integration-test-password',STAFF_PASSWORD:'staff-integration-test-password',APP_ORIGIN:'http://localhost:3000',DATA_FILE:':memory:',RVH_CLINIC_SYNC_TOKEN:tokens.clinic,RVH_MART_SYNC_TOKEN:tokens.mart,RVH_CHAT_SYNC_TOKEN:tokens.chat,...extra};
 const server=createApp({env});await new Promise(r=>server.listen(0,'127.0.0.1',r));t.after(()=>new Promise(r=>server.close(r)));
 const base='http://127.0.0.1:'+server.address().port;let cookie='';
 async function req(path,method='GET',data,token){const r=await fetch(base+'/api/'+path,{method,headers:{...(token?{Authorization:'Bearer '+token}:{origin:env.APP_ORIGIN,cookie}),...(data?{'Content-Type':'application/json'}:{})},body:data?JSON.stringify(data):undefined});if(r.headers.get('set-cookie'))cookie=r.headers.get('set-cookie').split(';')[0];return {status:r.status,data:await r.json()};}
 const login=role=>req('login','POST',{role:role||'owner',password:role==='staff'?env.STAFF_PASSWORD:env.ADMIN_PASSWORD});return {req,login,base};
}
test('connections start paused, require independent credentials and owner enablement',async t=>{
 const f=await fixture(t);assert.equal((await f.req('integrations/clinic/health','GET',null,tokens.clinic)).status,403);
 assert.equal((await f.req('integrations/clinic/health','GET',null,tokens.mart)).status,401);
 await f.login('staff');assert.equal((await f.req('connected-apps/clinic','PATCH',{enabled:true})).status,403);
 await f.login();assert.equal((await f.req('connected-apps/clinic','PATCH',{enabled:true})).status,200);
 assert.equal((await f.req('integrations/clinic/health','GET',null,tokens.clinic)).status,200);
 assert.equal((await f.req('integrations/clinic/health')).status,401); // browser session is not a machine credential
 assert.equal((await f.req('state')).data.connectedApps[0].status,'Waiting for first sync');
});
test('snapshots are idempotent, reject stale/conflicting values and keep secrets out of state/export',async t=>{
 const f=await fixture(t);await f.login();await f.req('connected-apps/clinic','PATCH',{enabled:true});
 const report=snapshot();const send=d=>f.req('integrations/clinic/daily','POST',d,tokens.clinic);
 assert.equal((await send(report)).status,200);assert.equal((await send(report)).data.duplicate,true);
 assert.equal((await send({...report,metrics:{...report.metrics,revenueINR:600}})).status,409);
 assert.equal((await send({...report,observedAt:new Date(Date.parse(report.observedAt)-1).toISOString()})).status,409);
 const newer={...report,observedAt:new Date(Date.parse(report.observedAt)+1).toISOString(),metrics:{...report.metrics,revenueINR:700}};assert.equal((await send(newer)).status,200);
 const state=(await f.req('state')).data;assert.equal(state.connectedApps[0].status,'Receiving data');assert.equal(state.connectedApps[0].reports.length,1);assert.equal(state.connectedApps[0].reports[0].metrics.revenueINR,700);
 const exported=(await f.req('export')).data;assert.equal(exported.connectedApps[0].reports.length,1);
 for(const value of Object.values(tokens)){assert.ok(!JSON.stringify(state).includes(value));assert.ok(!JSON.stringify(exported).includes(value));}
 assert.ok(!JSON.stringify(state).includes('credential_hash'));
 await f.req('connected-apps/clinic','PATCH',{enabled:false});assert.equal((await send(report)).status,403);
});
test('machine schema rejects PII, wrong source metrics, invalid dates and non-numeric totals',async t=>{
 const f=await fixture(t);await f.login();await f.req('connected-apps/clinic','PATCH',{enabled:true});
 const valid=snapshot();for(const data of [{...valid,phone:'9709095993'},{...valid,date:'2026-02-30'},{...valid,timezone:'UTC'},{...valid,metrics:{...valid.metrics,name:'Owner'}},{...valid,metrics:{...valid.metrics,appointmentsBooked:1.1}},{...valid,metrics:{...valid.metrics,revenueINR:-1}},{...valid,metrics:{...valid.metrics,revenueINR:1.001}},{...valid,metrics:{ordersPlaced:1,ordersCompleted:1,netSalesINR:10}}])assert.equal((await f.req('integrations/clinic/daily','POST',data,tokens.clinic)).status,400);
 assert.equal((await f.req('state')).data.connectedApps[0].reports.length,0);
});
test('duplicate tokens disable both sources; other sources store separate daily metrics',async t=>{
 const bad=await fixture(t,{RVH_MART_SYNC_TOKEN:tokens.clinic});await bad.login();assert.equal((await bad.req('state')).data.connectedApps[0].configured,false);
 const f=await fixture(t);await f.login();
 for(const [source,metrics] of [['mart',{ordersPlaced:5,ordersCompleted:4,netSalesINR:5000}],['chat',{enquiries:10,bookingRequests:3,humanHandoffs:2}]]){await f.req('connected-apps/'+source,'PATCH',{enabled:true});assert.equal((await f.req('integrations/'+source+'/daily','POST',snapshot(metrics),tokens[source])).status,200);}
 const s=(await f.req('state')).data.connectedApps;assert.equal(s[0].reports.length,0);assert.equal(s[1].reports[0].metrics.netSalesINR,5000);assert.equal(s[2].reports[0].metrics.bookingRequests,3);
 const r=await fetch(f.base+'/connections-ui.js');assert.equal(r.status,200);assert.match(await r.text(),/Connected apps/);
});
test('Node sender stays server-only, makes a single authenticated call and blocks redirects',async()=>{
 let calls=0;const env={RVH_MARKETING_SOURCE:'chat',RVH_MARKETING_SYNC_TOKEN:tokens.chat};
 await sendDailyReport({date,metrics:{enquiries:1,bookingRequests:0,humanHandoffs:0}},env,async(url,options)=>{calls++;assert.equal(new URL(url).pathname,'/api/integrations/chat/daily');assert.equal(options.redirect,'error');assert.ok(!String(url).includes(tokens.chat));assert.equal(options.headers.Authorization,'Bearer '+tokens.chat);return Response.json({ok:true});});assert.equal(calls,1);
 await assert.rejects(sendDailyReport({date,metrics:{}},{...env,RVH_MARKETING_URL:'http://example.com'}));
});
