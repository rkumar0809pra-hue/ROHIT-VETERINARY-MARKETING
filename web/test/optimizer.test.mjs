import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {createOptimizer} from '../optimizer.mjs';
function fixture(env={},fetchImpl){
 const db=new DatabaseSync(':memory:');db.exec("CREATE TABLE seo(id INTEGER PRIMARY KEY,data TEXT)");
 db.prepare('INSERT INTO seo VALUES(1,?)').run(JSON.stringify({scans:[{url:'https://rohitveterinary.com/',checks:[{label:'Page title',passed:false,observed:'Missing',action:'Add title'}]}]}));
 const api=createOptimizer({db,env,fetchImpl,audit:()=>{},json:(res,status,data)=>Object.assign(res,{status,data}),body:async r=>r.data,fail:(status,message)=>Object.assign(Error(message),{status})});
 return {api,db,call:async(path,data={},role='owner',method='POST')=>{const res={};await api.route({method,data},res,'/api/optimizer/'+path,role);return res;}};
}
test('tasks deduplicate, enforce owner and approval, retain decision history',async()=>{
 const f=fixture();await assert.rejects(f.call('prepare',{},'staff'),{status:403});
 assert.equal((await f.call('prepare')).data.added,1);assert.equal((await f.call('prepare')).data.added,0);
 const id=f.api.state().tasks[0].id;
 await assert.rejects(f.call('tasks/'+id,{status:'completed',note:'Done'},'owner','PATCH'),{status:400});
 await f.call('tasks/'+id,{status:'approved',note:''},'owner','PATCH');
 await assert.rejects(f.call('tasks/'+id,{status:'completed',note:''},'owner','PATCH'),{status:400});
 await f.call('tasks/'+id,{status:'completed',note:'Updated title and checked HTML'},'owner','PATCH');
 assert.equal(f.api.state().tasks[0].history.length,2);f.db.close();
});
test('Search Console needs credentials, scopes property and retains real totals without secrets',async()=>{
 const missing=fixture();await assert.rejects(missing.call('search-console'),{status:503});missing.db.close();
 const requests=[];const f=fixture({GSC_CLIENT_ID:'client',GSC_CLIENT_SECRET:'secret',GSC_REFRESH_TOKEN:'refresh'},async(url,options)=>{requests.push({url,options});return {ok:true,json:async()=>url.includes('oauth2')?{access_token:'access'}:{rows:[{clicks:3,impressions:40,ctr:0.075,position:5}]}}});
 await assert.rejects(f.call('search-console',{property:'https://evil.com/'}),{status:400});
 await f.call('search-console',{property:'https://www.rohitveterinary.com/'});
 assert.equal(requests.length,2);assert.equal(requests[1].options.headers.Authorization,'Bearer access');
 assert.equal(f.api.state().reports[0].metrics.clicks,3);assert.ok(!JSON.stringify(f.api.state()).includes('refresh'));f.db.close();
});
test('Google error is redacted and no-data is distinct from zero',async()=>{
 const env={GSC_CLIENT_ID:'c',GSC_CLIENT_SECRET:'s',GSC_REFRESH_TOKEN:'r'};
 const f=fixture(env,async url=>({ok:true,json:async()=>url.includes('oauth2')?{access_token:'a'}:{}}));
 await f.call('search-console',{property:'https://www.rohitveterinary.com/'});assert.equal(f.api.state().reports[0].metrics,null);f.db.close();
 const failed=fixture(env,async()=>{throw Error('secret token');});await assert.rejects(failed.call('search-console',{property:'https://www.rohitveterinary.com/'}),e=>e.status===502&&!e.message.includes('secret token'));failed.db.close();
});
