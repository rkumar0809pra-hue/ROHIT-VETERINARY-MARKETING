import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {createOptimizer, canonicalSearchTaskKey, dedupeOptimizerTasks, selectMasterSearchConsoleReport, MASTER_SEARCH_CONSOLE_PROPERTY} from '../optimizer.mjs';

const googleEnv={
  APP_ORIGIN:'https://marketing.rohitveterinary.com',
  GOOGLE_CLIENT_ID:'client',
  GOOGLE_CLIENT_SECRET:'secret',
  GOOGLE_REDIRECT_URI:'https://marketing.rohitveterinary.com/api/auth/google/callback',
};

function fixture(env={},fetchImpl=async()=>({ok:false,json:async()=>({})})){
 const db=new DatabaseSync(':memory:');db.exec("CREATE TABLE seo(id INTEGER PRIMARY KEY,data TEXT)");
 db.prepare('INSERT INTO seo VALUES(1,?)').run(JSON.stringify({scans:[{url:'https://rohitveterinary.com/',checks:[{label:'Page title',passed:false,observed:'Missing',action:'Add title'}]}]}));
 const api=createOptimizer({db,env,fetchImpl,audit:()=>{},json:(res,status,data)=>Object.assign(res,{status,data}),body:async r=>r.data,fail:(status,message)=>Object.assign(Error(message),{status})});
 const call=async(path,{data={},role='owner',method='POST',url}={})=>{const requestUrl=url||'/api/'+path;const routePath=new URL(requestUrl,'https://test.local').pathname;const res={headers:{},writeHead(status,headers={}){this.status=status;this.headers=headers;},end(){this.ended=true;}};await api.route({method,data,url:requestUrl},res,routePath,role);return res;};
 return {api,db,call};
}

test('Search Console master property and cross-property task deduplication are stable',()=>{
 assert.equal(MASTER_SEARCH_CONSOLE_PROPERTY,'sc-domain:rohitveterinary.com');
 assert.equal(
   canonicalSearchTaskKey('  ANRONIL   BOLUS ','https://mart.rohitveterinary.com/product/anronil-bolus#buy'),
   canonicalSearchTaskKey('anronil bolus','https://mart.rohitveterinary.com/product/anronil-bolus')
 );
 const reports=[
   {id:'prefix-new',property:'https://www.rohitveterinary.com/',period:'28d',receivedAt:'2026-10-04T10:00:00Z'},
   {id:'master',property:'sc-domain:rohitveterinary.com',period:'28d',receivedAt:'2026-10-03T10:00:00Z'},
 ];
 assert.equal(selectMasterSearchConsoleReport(reports).id,'master');
 const tasks=dedupeOptimizerTasks([
   {id:'prefix',kind:'Search Console',url:'https://mart.rohitveterinary.com/product/anronil-bolus',status:'review',createdAt:'2026-10-04T10:00:00Z',searchConsole:{property:'https://www.rohitveterinary.com/',query:'ANRONIL BOLUS'}},
   {id:'master',kind:'Search Console',url:'https://mart.rohitveterinary.com/product/anronil-bolus#buy',status:'review',createdAt:'2026-10-03T10:00:00Z',searchConsole:{property:'sc-domain:rohitveterinary.com',query:'anronil bolus'}},
   {id:'other',kind:'SEO',url:'https://rohitveterinary.com/',status:'review'},
 ]);
 assert.equal(tasks.length,2);
 assert.equal(tasks.find(t=>t.kind==='Search Console').id,'master');
});

test('tasks deduplicate, enforce owner and approval, retain decision history',async()=>{
 const f=fixture();await assert.rejects(f.call('optimizer/prepare',{role:'staff'}),{status:403});
 assert.equal((await f.call('optimizer/prepare')).data.added,1);assert.equal((await f.call('optimizer/prepare')).data.added,0);
 const id=f.api.state().tasks[0].id;
 await assert.rejects(f.call('optimizer/tasks/'+id,{data:{status:'completed',note:'Done'},method:'PATCH'}),{status:400});
 await f.call('optimizer/tasks/'+id,{data:{status:'approved',note:''},method:'PATCH'});
 await assert.rejects(f.call('optimizer/tasks/'+id,{data:{status:'completed',note:''},method:'PATCH'}),{status:400});
 await f.call('optimizer/tasks/'+id,{data:{status:'completed',note:'Updated title and checked HTML'},method:'PATCH'});
 assert.equal(f.api.state().tasks[0].history.length,2);f.db.close();
});

test('OAuth connect validates one-time state, stores encrypted refresh token and discovers properties',async()=>{
 const requests=[];
 const f=fixture(googleEnv,async(url,options={})=>{
   requests.push({url:String(url),options});
   if(String(url).includes('oauth2.googleapis.com/token'))return {ok:true,json:async()=>({access_token:'access',refresh_token:'refresh-secret'})};
   if(String(url).endsWith('/webmasters/v3/sites'))return {ok:true,json:async()=>({siteEntry:[{siteUrl:'sc-domain:rohitveterinary.com',permissionLevel:'siteOwner'},{siteUrl:'https://mart.rohitveterinary.com/',permissionLevel:'siteFullUser'}]})};
   return {ok:false,json:async()=>({})};
 });
 const start=await f.call('auth/google',{method:'GET'});
 assert.equal(start.status,302);const authUrl=new URL(start.headers.Location);assert.equal(authUrl.origin,'https://accounts.google.com');
 assert.equal(authUrl.searchParams.get('scope'),'https://www.googleapis.com/auth/webmasters.readonly');
 const state=authUrl.searchParams.get('state');assert.ok(state);
 await assert.rejects(f.call('auth/google/callback',{method:'GET',url:'/api/auth/google/callback?state=bad&code=12345678901'}),{status:400});
 const callback=await f.call('auth/google/callback',{method:'GET',url:'/api/auth/google/callback?state='+encodeURIComponent(state)+'&code=valid-code-12345'});
 assert.equal(callback.status,303);assert.equal(callback.headers.Location,'https://marketing.rohitveterinary.com/?gsc=connected');
 const stateView=f.api.state().searchConsole;assert.equal(stateView.connected,true);assert.deepEqual(stateView.properties,['sc-domain:rohitveterinary.com','https://mart.rohitveterinary.com/']);assert.equal(stateView.masterProperty,'sc-domain:rohitveterinary.com');
 assert.ok(!JSON.stringify(f.api.state()).includes('refresh-secret'));
 const stored=f.db.prepare('SELECT data FROM optimizer_google WHERE id=1').get().data;assert.ok(!stored.includes('refresh-secret'));assert.match(stored,/v1\./);
 assert.equal(requests.length,2);f.db.close();
});

test('Search Console sync uses discovered properties, date ranges and returns dashboard data without secrets',async()=>{
 let phase='connect';
 const f=fixture(googleEnv,async(url,options={})=>{
   const u=String(url);
   if(u.includes('oauth2.googleapis.com/token'))return {ok:true,json:async()=>phase==='connect'?{access_token:'access1',refresh_token:'refresh1'}:{access_token:'access2'}};
   if(u.endsWith('/webmasters/v3/sites'))return {ok:true,json:async()=>({siteEntry:[{siteUrl:'sc-domain:rohitveterinary.com',permissionLevel:'siteOwner'}]})};
   if(u.includes('/searchAnalytics/query')){
     const requestBody=JSON.parse(options.body),dims=requestBody.dimensions||[];
     if(!dims.length)return {ok:true,json:async()=>({rows:[{clicks:30,impressions:500,ctr:.06,position:8.2}]})};
     if(dims[0]==='date')return {ok:true,json:async()=>({rows:[{keys:['2026-09-25'],clicks:3,impressions:50,ctr:.06,position:7.5}]})};
     if(dims[0]==='query'&&dims[1]==='page')return {ok:true,json:async()=>({rows:[
       {keys:['anronil bolus','https://mart.rohitveterinary.com/'],clicks:0,impressions:102,ctr:0,position:8.2},
       {keys:['vet clinic lohardaga','https://rohitveterinary.com/'],clicks:1,impressions:20,ctr:.05,position:3}
     ]})};
     if(dims[0]==='query')return {ok:true,json:async()=>({rows:[{keys:['vet clinic lohardaga'],clicks:1,impressions:100,ctr:.01,position:9}]})};
     if(dims[0]==='page'){
       const previous=requestBody.endDate<'2026-09-01';
       return {ok:true,json:async()=>({rows:[{keys:['https://rohitveterinary.com/'],clicks:previous?9:4,impressions:80,ctr:.05,position:6}]})};
     }
   }
   if(u.includes('oauth2.googleapis.com/revoke'))return {ok:true,json:async()=>({})};
   return {ok:false,json:async()=>({})};
 });
 const start=await f.call('auth/google',{method:'GET'});const state=new URL(start.headers.Location).searchParams.get('state');
 await f.call('auth/google/callback',{method:'GET',url:'/api/auth/google/callback?state='+state+'&code=valid-code-12345'});phase='sync';
 await assert.rejects(f.call('search-console/sync',{data:{property:'https://evil.test/',period:'28d'}}),{status:400});
 const result=await f.call('search-console/sync',{data:{property:'sc-domain:rohitveterinary.com',period:'28d'}});
 assert.equal(result.status,200);assert.equal(result.data.metrics.clicks,30);assert.equal(result.data.queries[0].query,'vet clinic lohardaga');assert.equal(result.data.opportunities.highImpressionLowCtr.length,1);assert.equal(result.data.opportunities.positionOpportunities.length,1);
 assert.equal(result.data.queryPages[0].query,'anronil bolus');assert.equal(result.data.queryPages[0].subdomain,'RVH Vet Mart');assert.equal(result.data.opportunities.queryLandingOpportunities[0].page,'https://mart.rohitveterinary.com/');assert.match(result.data.opportunities.queryLandingOpportunities[0].recommendation,/landing on a broad page/i);
 const task=await f.call('optimizer/search-console-task',{data:{reportId:result.data.id,index:0}});assert.equal(task.status,201);assert.equal(task.data.task.url,'https://mart.rohitveterinary.com/');assert.equal(task.data.task.kind,'Search Console');
 const duplicate=await f.call('optimizer/search-console-task',{data:{reportId:result.data.id,index:0}});assert.equal(duplicate.status,200);assert.equal(duplicate.data.created,false);
 assert.ok(!JSON.stringify(result.data).includes('refresh1'));assert.equal(f.api.state().reports.length,1);
 const performance=await f.call('search-console/performance?property=sc-domain%3Arohitveterinary.com&period=28d',{method:'GET',url:'/api/search-console/performance?property=sc-domain%3Arohitveterinary.com&period=28d'});
 assert.equal(performance.data.report.metrics.impressions,500);
 const disconnect=await f.call('search-console/disconnect');assert.equal(disconnect.data.connected,false);assert.equal(f.api.state().searchConsole.connected,false);f.db.close();
});

test('Google errors are redacted and invalid server OAuth config is rejected',async()=>{
 const invalid=fixture({APP_ORIGIN:'https://marketing.rohitveterinary.com',GOOGLE_CLIENT_ID:'c',GOOGLE_CLIENT_SECRET:'s',GOOGLE_REDIRECT_URI:'https://evil.test/api/auth/google/callback'});
 await assert.rejects(invalid.call('auth/google',{method:'GET'}),{status:503});invalid.db.close();
 const failed=fixture(googleEnv,async()=>{throw Error('secret provider details');});
 const start=await failed.call('auth/google',{method:'GET'}),state=new URL(start.headers.Location).searchParams.get('state');
 await assert.rejects(failed.call('auth/google/callback',{method:'GET',url:'/api/auth/google/callback?state='+state+'&code=valid-code-12345'}),e=>e.status===502&&!e.message.includes('secret provider details'));failed.db.close();
});
