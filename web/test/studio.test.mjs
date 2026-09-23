import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,rmSync,readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {createApp} from '../server.mjs';
import {mediaProvider} from '../media-provider.mjs';
const password='owner-test-password-long';
const png=readFileSync(new URL('../public/icon-192.png',import.meta.url));
async function fixture(t,options={}){
 const dir=mkdtempSync(join(tmpdir(),'rvh-studio-test-'));
 const env={VIDEO_BRANDING:'off',ADMIN_PASSWORD:password,STAFF_PASSWORD:'staff-test-password-long',CREATOR_PASSWORD:'creator-test-password-long',APP_ORIGIN:'http://localhost:3000',DATA_FILE:join(dir,'db.sqlite'),...options.env};
 const server=createApp({env,generateImpl:options.generateImpl,provider:options.provider,renderAdvertisementImpl:options.renderAdvertisementImpl});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 t.after(async()=>{await new Promise(r=>server.close(r));rmSync(dir,{recursive:true,force:true});});
 let cookie='';const base=`http://127.0.0.1:${server.address().port}`;
 async function req(path,method='GET',data,headers={}){const r=await fetch(base+'/api/'+path,{method,headers:{origin:env.APP_ORIGIN,cookie,...(data?{'content-type':'application/json'}:{}),...headers},body:data?JSON.stringify(data):undefined});if(r.headers.get('set-cookie'))cookie=r.headers.get('set-cookie').split(';')[0];return r;}
 const call=async(...a)=>{const r=await req(...a);return {status:r.status,data:await r.json()};};
 const login=async(role='owner')=>call('login','POST',{role,password:role==='owner'?password:env[role.toUpperCase()+'_PASSWORD']});
 return {call,req,login};
}
test('clinic profile persists and reaches content agents and assistant',async t=>{
 let received;const f=await fixture(t,{env:{OPENAI_API_KEY:'mock',OPENAI_MODEL:'mock'},generateImpl:async a=>{received=a;return 'Hindi campaign draft';}});await f.login();
 let s=(await f.call('state')).data;assert.equal(s.profile.hindiName,'रोहित भेटनरी हाउस');assert.equal(s.profile.phone,'9709095993');
 const profile={...s.profile,hours:'Monday 9am–5pm',services:'Consultations'};assert.equal((await f.call('profile','PUT',profile)).status,200);
 const d=await f.call('runs','POST',{agent:'content',language:'Hindi',brief:'Make a caption',meta:{platform:'Facebook'}});assert.equal(d.status,201);assert.equal(received.profile.hours,profile.hours);assert.equal(JSON.parse(d.data.meta_json).platform,'Facebook');
 assert.equal((await f.call('chat','POST',{text:'Remember our location',language:'Hindi'})).status,201);assert.equal(received.profile.address,profile.address);assert.equal((await f.call('state')).data.chat.length,2);
 assert.equal((await f.call('export')).data.profile.phone,'9709095993');
});
test('staff and creator permissions enforced on server, manual publishing and stale versions',async t=>{
 const f=await fixture(t);await f.login('creator');let d=(await f.call('studio/drafts','POST',{title:'Campaign',content:'Contact clinic',meta:{platform:'WhatsApp'}})).data;
 const change=async(action,extra={})=>f.call('drafts/'+d.id,'PATCH',{action,version:d.version,...extra});d=(await change('submit')).data;
 assert.equal((await change('approve')).status,403);assert.equal((await f.call('profile','PUT',{})).status,403);assert.equal((await f.call('export')).status,403);assert.equal((await change('results')).status,403);
 await f.login('staff');assert.equal((await change('approve')).status,403);await f.login();d=(await change('approve')).data;
 await f.login('staff');assert.equal((await change('publish',{confirmPublished:false,url:'https://example.com/post'})).status,400);assert.equal((await change('publish',{confirmPublished:true,url:'javascript:alert(1)'})).status,400);
 const oldVersion=d.version;d=(await change('publish',{confirmPublished:true,url:'https://example.com/post'})).data;assert.equal(d.status,'published');assert.ok(d.published_at);
 assert.equal((await f.call('drafts/'+d.id,'PATCH',{action:'edit',version:oldVersion,title:'old',content:'old'})).status,409);
 const metrics={period:'September',spend:100,clicks:20,leads:5,revenue:300,sent:10,failed:0,responses:4};d=(await change('results',{metrics})).data;assert.equal(JSON.parse(d.metrics_json).leads,5);
 d=(await change('edit',{title:'Changed',content:'Revised'})).data;assert.equal(d.status,'draft');assert.equal(d.published_at,null);
});
test('private media upload validates content, supports ranges, and owner-only deletion',async t=>{
 const f=await fixture(t);await f.login();assert.equal((await f.call('media','POST',{kind:'upload',title:'bad',base64:Buffer.from('<svg>not image</svg>').toString('base64')})).status,400);
 const r=await f.call('media','POST',{kind:'upload',title:'Clinic artwork',base64:png.toString('base64')});assert.equal(r.status,202);assert.equal(r.data.status,'completed');const id=r.data.id;
 const range=await f.req('media/'+id+'/file','GET',undefined,{range:'bytes=0-7'});assert.equal(range.status,206);assert.equal(Buffer.from(await range.arrayBuffer()).toString('hex'),'89504e470d0a1a0a');
 const denied=await f.req('media/'+id+'/file','GET',undefined,{cookie:''});assert.equal(denied.status,401);await denied.text();
 await f.login('creator');assert.equal((await f.call('media/'+id,'DELETE')).status,403);await f.login();assert.equal((await f.call('media/'+id,'DELETE')).status,200);assert.equal((await f.call('media/'+id+'/file')).status,404);
});
test('generation uses saved provider operations and never creates fake successful media',async t=>{
 let calls=0,prompt;const mp4=Buffer.from('00000018667479706d70343200000000','hex');
 const f=await fixture(t,{env:{GEMINI_API_KEY:'mock',OPENAI_API_KEY:'mock'},provider:{startVideo:async a=>{calls++;prompt=a.prompt;return 'models/veo-test/operations/one';},pollVideo:async()=> 'https://generativelanguage.googleapis.com/video',downloadVideo:async()=>mp4,image:async()=>{throw Error('secret-provider-error');}}});await f.login();
 assert.equal((await f.call('media','POST',{kind:'video',title:'Video',prompt:'Vet clinic',duration:8,ratio:'9:16'})).status,400);
 const r=await f.call('media','POST',{kind:'video',title:'Video',prompt:'Vet clinic',duration:8,ratio:'9:16',confirmCost:true});assert.equal(r.status,202);assert.match(prompt,/9709095993/);
 assert.equal((await f.call('media/'+r.data.id+'/check','POST',{})).data.status,'completed');await f.call('media/'+r.data.id+'/check','POST',{});assert.equal(calls,1);
 const file=await f.req('media/'+r.data.id+'/file');assert.deepEqual(Buffer.from(await file.arrayBuffer()),mp4);
 const im=await f.call('media','POST',{kind:'image',title:'Poster',prompt:'Vet clinic',confirmCost:true});const row=(await f.call('state')).data.media.find(m=>m.id===im.data.id);assert.equal(row.status,'failed');assert.ok(!row.error.includes('secret'));assert.equal(row.file,undefined);assert.equal(row.operation,undefined);
});
test('Veo REST contract, polling, image input and download key isolation',async()=>{
 const calls=[];const mp4=Buffer.from('00000018667479706d70343200000000','hex');
 const provider=mediaProvider({GEMINI_API_KEY:'secret'},async(url,opts)=>{calls.push({url:String(url),opts});if(String(url).endsWith('predictLongRunning'))return Response.json({name:'models/veo-test/operations/abc'});if(String(url).includes('/operations/'))return Response.json({done:true,response:{generateVideoResponse:{generatedSamples:[{video:{uri:'https://generativelanguage.googleapis.com/download'}}]}}});if(String(url).endsWith('/download'))return new Response(null,{status:302,headers:{location:'https://storage.googleapis.com/video.mp4'}});return new Response(mp4);});
 const op=await provider.startVideo({prompt:'Vet',ratio:'9:16',duration:8,image:{bytes:png,mime:'image/png'}});const body=JSON.parse(calls[0].opts.body);assert.equal(body.instances[0].image.mimeType,'image/png');assert.equal(body.parameters.durationSeconds,8);assert.ok(body.instances[0].image.bytesBase64Encoded);
 const uri=await provider.pollVideo(op);assert.deepEqual(await provider.downloadVideo(uri),mp4);assert.equal(calls.at(-1).opts.headers['x-goog-api-key'],undefined);await assert.rejects(provider.downloadVideo('https://evil.test/video'),/Unexpected/);
 const bad=mediaProvider({},async()=>Response.json({done:true,response:{}}));await assert.rejects(bad.pollVideo(op),/no video/);
});
test('weekly drafts and batch approval enforce permissions and stale-version atomicity',async t=>{
 const f=await fixture(t,{env:{OPENAI_API_KEY:'mock',OPENAI_MODEL:'mock'},generateImpl:async()=>JSON.stringify({items:Array.from({length:7},(_,day)=>({day,title:'Day '+day,content:'Clinic post for review',platform:'Facebook',agent:'content'}))})});await f.login();
 const plan={week:'2026-09-28',goal:'Preventive consultations',availability:'Call to confirm',budget:0};
 assert.equal((await f.call('weekly-plan','POST',plan)).status,201);
 const state=(await f.call('state')).data;assert.equal(state.drafts.length,7);assert.ok(state.drafts.every(d=>d.status==='pending'&&JSON.parse(d.meta_json).batch));
 const items=state.drafts.map(d=>({id:d.id,version:d.version}));
 await f.login('creator');assert.equal((await f.call('approval-batch','POST',{action:'approve',items})).status,403);assert.equal((await f.call('weekly-plan','POST',plan)).status,403);
 await f.login();assert.equal((await f.call('approval-batch','POST',{action:'approve',items:items.map((x,i)=>({...x,version:i===6?99:x.version}))})).status,409);
 assert.ok((await f.call('state')).data.drafts.every(d=>d.status==='pending'));
 assert.equal((await f.call('approval-batch','POST',{action:'approve',items})).status,200);
 assert.ok((await f.call('state')).data.drafts.every(d=>d.status==='approved'));
 assert.equal((await f.call('profile','PUT',{...state.profile,bookingUrl:'javascript:alert(1)'})).status,400);
});

test('advertisement workflow guards spend, preserves clips, pauses failures, and assembles once',async t=>{
 let starts=0,speech=0,renders=0,failSpeech=true;
 const mp4=Buffer.from('00000018667479706d70343200000000','hex');
 const f=await fixture(t,{env:{OPENAI_API_KEY:'mock',OPENAI_MODEL:'mock',GEMINI_API_KEY:'mock'},
  generateImpl:async()=>JSON.stringify({scenes:Array.from({length:4},()=>({prompt:'A calm dog at the clinic',narration:'अपने पशु की देखभाल करें।'}))}),
  provider:{startVideo:async a=>{assert.match(a.prompt,/male presenter/);return `operations/scene-${++starts}`;},pollVideo:async()=> 'https://generativelanguage.googleapis.com/video',downloadVideo:async()=>mp4,speech:async a=>{assert.equal(a.voice,'echo');assert.equal(a.style,'cinematic');speech++;if(failSpeech){failSpeech=false;throw Error('quota');}return Buffer.alloc(48000);}},
  renderAdvertisementImpl:async args=>{renders++;assert.equal(args.scenes.length,4);assert.equal(args.profile.phone,'9709095993');return mp4;}});
 await f.login('creator');const request={title:'Clinic ad',topic:'Clinic care',duration:30,ratio:'9:16',language:'Hindi',presenterType:'male',voice:'echo',voiceStyle:'cinematic'};
 assert.equal((await f.call('advertisements','POST',request)).status,403);
 await f.login();let ad=(await f.call('advertisements','POST',request)).data;assert.equal(ad.status,'draft');assert.equal(starts,0);
 const state=async()=>{await new Promise(r=>setTimeout(r,5));return (await f.call('state')).data;};
 const current=async()=> (await state()).advertisements.find(a=>a.id===ad.id);
 assert.equal((await f.call(`advertisements/${ad.id}/start`,'POST',{version:ad.version})).status,400);
 assert.equal((await f.call(`advertisements/${ad.id}`,'PUT',{...ad,version:99})).status,409);
 assert.equal((await f.call(`advertisements/${ad.id}`,'PUT',{...ad,scenes:ad.scenes.map(s=>({...s,mode:'still'}))})).status,400);
 let started=await f.call(`advertisements/${ad.id}/start`,'POST',{version:ad.version,confirmCost:true});assert.equal(started.status,202);
 assert.equal((await f.call(`advertisements/${ad.id}/start`,'POST',{version:ad.version,confirmCost:true})).status,409);
 for(let i=0;i<12;i++){
  const s=await state();for(const m of s.media.filter(m=>m.status==='processing'))await f.call('media/'+m.id+'/check','POST',{});
  await f.call(`advertisements/${ad.id}/check`,'POST',{});ad=await current();if(ad.status==='paused')break;
 }
 assert.equal(ad.status,'paused');assert.equal(starts,4);assert.equal(speech,1);assert.equal(renders,0);
 assert.equal((await f.call('media/'+ad.scenes[0].mediaId,'DELETE')).status,409);
 assert.equal((await f.call(`advertisements/${ad.id}`,'PUT',ad)).status,409);
 assert.equal((await f.call(`advertisements/${ad.id}/start`,'POST',{version:ad.version,confirmCost:true})).status,202);
 for(let i=0;i<20;i++){ad=await current();if(ad.status==='completed')break;}
 assert.equal(ad.status,'completed');assert.equal(starts,4);assert.equal(speech,5);assert.equal(renders,1);
 await f.call(`advertisements/${ad.id}/check`,'POST',{});assert.equal(renders,1);
 const file=await f.req('media/'+ad.resultId+'/file');assert.deepEqual(Buffer.from(await file.arrayBuffer()),mp4);
 const finished=ad.resultId;assert.equal((await f.call(`advertisements/${ad.id}`,'DELETE')).status,200);assert.equal((await f.req('media/'+finished+'/file')).status,200);
});

test('speech provider uses bounded PCM, server credentials, and approved voice input',async()=>{
 let body;const p=mediaProvider({OPENAI_API_KEY:'test'},async(url,opts)=>{assert.equal(url,'https://api.openai.com/v1/audio/speech');assert.equal(opts.headers.Authorization,'Bearer test');body=JSON.parse(opts.body);return new Response(Buffer.alloc(48000));});
 assert.equal((await p.speech({text:'नमस्ते',language:'Hindi',voice:'coral',style:'energetic'})).length,48000);assert.equal(body.response_format,'pcm');assert.equal(body.input,'नमस्ते');assert.match(body.instructions,/energetic advertising/);await assert.rejects(p.speech({text:'Test',voice:'celebrity'}),/supported voice/);
});

test('advertisement restart pauses paid work and retains a known operation',async()=>{
 const {DatabaseSync}=await import('node:sqlite');const {createAdvertisements}=await import('../advertisements.mjs');const db=new DatabaseSync(':memory:');
 const args={db,env:{GEMINI_API_KEY:'mock'},isStopped:()=>false,profile:()=>({}),fail:(status,message)=>Object.assign(Error(message),{status}),getMedia:()=>({status:'processing',operation:'operations/saved'}),track:p=>p,audit:()=>{},room:()=>{},quota:()=>{}};
 let starts=0;args.provider={startVideo:async()=>{starts++;}};
 createAdvertisements(args);
 db.prepare('INSERT INTO advertisements VALUES(?,?,?,?,?,?,?,?)').run('test','Ad','generating',JSON.stringify({scenes:[{mode:'veo',mediaId:'known'}]}),1,null,'2026-09-23','2026-09-23');
 const restarted=createAdvertisements(args);assert.equal(restarted.list()[0].status,'paused');await restarted.tick();assert.equal(starts,0);
 db.prepare("UPDATE advertisements SET status='generating'").run();await restarted.tick();assert.equal(starts,0);assert.equal(restarted.list()[0].scenes[0].mediaId,'known');db.close();
});

 test('AI video prompt creator preserves selection and only creates an editable brief',async t=>{
 let received,calls=0;const f=await fixture(t,{env:{OPENAI_API_KEY:'mock',OPENAI_MODEL:'mock'},generateImpl:async args=>{received=args;calls++;return JSON.stringify({title:'Clinic introduction',topic:'A warm clinic introduction with a closing call to action.'});}});
 const input={topic:'Introduce our clinic',duration:30,language:'Hindi',presenterType:'female',voice:'nova',voiceStyle:'warm'};
 await f.login('creator');assert.equal((await f.call('advertisements/prompt','POST',input)).status,403);
 await f.login();assert.equal((await f.call('advertisements/prompt','POST',{...input,voice:'unknown'})).status,400);assert.equal(calls,0);
 const result=await f.call('advertisements/prompt','POST',input);assert.equal(result.status,200);assert.equal(result.data.title,'Clinic introduction');assert.match(JSON.parse(received.brief).presenter,/female presenter/);assert.equal(received.profile.phone,'9709095993');
 const state=(await f.call('state')).data;assert.equal(state.advertisements.length,0);assert.equal(state.media.length,0);
 });
