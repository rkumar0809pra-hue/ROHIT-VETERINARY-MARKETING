import {randomUUID} from 'node:crypto';
import {readFileSync,unlinkSync} from 'node:fs';
import {join} from 'node:path';
import {brandInstructions} from './brand.mjs';
import {renderAdvertisement} from './ad-renderer.mjs';

export function createAdvertisements({db,env,profile,provider,generateImpl,body,json,fail,requiredText,audit,quota,room,finish,getMedia,mediaDir,track,isStopped,renderImpl=renderAdvertisement}){
 db.exec(`CREATE TABLE IF NOT EXISTS advertisements(id TEXT PRIMARY KEY,title TEXT NOT NULL,status TEXT NOT NULL,data TEXT NOT NULL,version INTEGER NOT NULL DEFAULT 1,error TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)`);
 // A process may die after a paid request is accepted but before its ID is saved.
 // Never silently repeat that request. Known Veo operations remain pollable in media.
 db.prepare("UPDATE advertisements SET status='paused',error='Server restarted. Review saved scenes and provider usage before continuing.' WHERE status IN ('generating','assembling')").run();
 const now=()=>new Date().toISOString();let busy=false,planning=false;
 const unpack=row=>({...row,...JSON.parse(row.data),data:undefined});
 const get=id=>{const row=db.prepare('SELECT * FROM advertisements WHERE id=?').get(id);if(!row)throw fail(404,'Advertisement not found.');return unpack(row);};
 function save(ad){const {id,title,status,version,error,created_at,updated_at,...data}=ad;db.prepare('UPDATE advertisements SET title=?,status=?,data=?,error=?,version=version+1,updated_at=? WHERE id=?').run(title,status,JSON.stringify(data),error||null,now(),id);}
 const image=id=>{const m=getMedia(id);if(m.status!=='completed'||!['image/png','image/jpeg'].includes(m.mime)||m.bytes>6*1024*1024)throw fail(400,'Choose a saved PNG or JPEG up to 6 MB.');return m;};
 function validate(data){
  if(![30,60].includes(data.duration)||!['9:16','16:9'].includes(data.ratio)||!['Hindi','English','Hinglish'].includes(data.language))throw fail(400,'Choose 30 or 60 seconds, a supported format and language.');
  const durations=data.duration===30?[8,6,6,6]:[8,8,8,8,8,8,8];
  if(!Array.isArray(data.scenes)||data.scenes.length!==durations.length)throw fail(400,`This advertisement needs ${durations.length} scenes.`);
  return data.scenes.map((s,i)=>{
   if(!s||!['veo','still'].includes(s.mode))throw fail(400,'Choose Veo or a still photo for each scene.');
   const narration=requiredText(s.narration,240);
   if(narration.split(/\s+/).length>durations[i]*3)throw fail(400,`Shorten scene ${i+1} narration to ${durations[i]*3} words or fewer.`);
   const imageId=s.imageId||'';if(imageId)image(imageId);if(s.mode==='still'&&!imageId)throw fail(400,'Choose an image for every still-photo scene.');
   return {duration:durations[i],mode:s.mode,prompt:requiredText(s.prompt,1800),narration,imageId,mediaId:null,audioId:null};
  });
 }
 function insertMedia(kind,title,prompt,ratio,duration){const id=randomUUID();db.prepare('INSERT INTO media(id,kind,title,prompt,ratio,duration,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)').run(id,kind,title,prompt,ratio,duration,'starting',now(),now());return id;}
 function used(id){return db.prepare('SELECT * FROM advertisements').all().some(row=>{const a=unpack(row);return a.resultId===id||a.scenes.some(s=>s.mediaId===id||s.imageId===id||s.audioId===id);});}
 async function tick(){
  if(busy||isStopped())return;busy=true;
  let ad;
  try{
   const row=db.prepare("SELECT * FROM advertisements WHERE status IN ('generating','assembling') ORDER BY created_at LIMIT 1").get();if(!row)return;
   ad=unpack(row);
   for(const [index,s] of ad.scenes.entries()){
    if(isStopped())return;
    if(s.mode==='still'){image(s.imageId);continue;}
    if(s.mediaId){const m=getMedia(s.mediaId);if(m.status==='failed')throw fail(409,`Scene ${index+1} failed. Review provider usage, then continue to request a replacement.`);if(m.status!=='completed')return;continue;}
    if(db.prepare("SELECT COUNT(*) n FROM media WHERE status IN ('starting','processing')").get().n>=2)return;
    room(64*1024*1024);quota('media',12);
    s.mediaId=insertMedia('scene',`${ad.title} · Scene ${index+1}`,s.prompt,ad.ratio,s.duration);save(ad);
    try{
     const src=s.imageId?image(s.imageId):null;
     const operation=await provider.startVideo({prompt:`${brandInstructions(ad.brand)}\n${ad.presenter}\n${s.prompt}\nSilent visual scene for a narrated veterinary advertisement. No dialogue, lettering or invented logos. No guaranteed cures. Keep the same presenter appearance where possible.`,ratio:ad.ratio,duration:s.duration,image:src?{bytes:readFileSync(join(mediaDir,src.file)),mime:src.mime}:null});
     db.prepare("UPDATE media SET status='processing',operation=?,updated_at=? WHERE id=?").run(operation,now(),s.mediaId);
    }catch(err){db.prepare("UPDATE media SET status='failed',error=?,updated_at=? WHERE id=?").run(err.status||err.name==='ProviderError'?err.message:'Scene request failed. Check provider usage before retrying.',now(),s.mediaId);throw fail(502,`Scene ${index+1} request failed. Check provider usage before continuing.`);}
    return; // Poll the saved operation; never resubmit it automatically.
   }
   if(get(ad.id).status==='paused'||isStopped())return;
   ad.status='assembling';save(ad);
   if(ad.narration){
    for(const [index,s] of ad.scenes.entries()){
     if(isStopped()||get(ad.id).status==='paused')return;
     if(s.audioId){if(getMedia(s.audioId).status==='completed')continue;throw fail(409,`Scene ${index+1} narration was interrupted. Check OpenAI usage before continuing.`);}
     quota('narration',30);s.audioId=insertMedia('audio',`${ad.title} · Voice ${index+1}`,s.narration,ad.ratio,s.duration);save(ad);
     try{finish(s.audioId,await provider.speech({text:s.narration,language:ad.language,voice:ad.voice}),'audio/pcm');}
     catch(err){db.prepare("UPDATE media SET status='failed',error=? WHERE id=?").run('Narration request failed.',s.audioId);throw fail(502,`Scene ${index+1} narration failed. Check OpenAI billing and model access, then continue.`);}
    }
   }
   if(isStopped()||get(ad.id).status==='paused')return;
   const rendered=await renderImpl({profile:ad.brand,ratio:ad.ratio,narration:ad.narration,scenes:ad.scenes.map(s=>({...s,file:join(mediaDir,getMedia(s.mode==='still'?s.imageId:s.mediaId).file),audioFile:ad.narration?join(mediaDir,getMedia(s.audioId).file):null}))});
   // Rendering is local; it can be retried without regenerating completed assets.
   const resultId=insertMedia('video',ad.title,'Complete advertisement: '+ad.scenes.map(s=>s.prompt).join('\n'),ad.ratio,ad.duration);
   finish(resultId,rendered,'video/mp4');ad.resultId=resultId;ad.status='completed';ad.error=null;save(ad);audit('advertisement_completed',ad.id);
  }catch(err){if(ad){const current=get(ad.id);current.status='paused';current.error=err.status?err.message:(err.message?.startsWith('Scene ')?err.message:'Assembly paused. Check server video dependencies, storage, and provider configuration. Completed scenes are preserved.');save(current);}}
  finally{busy=false;}
 }
 return {
  list:()=>db.prepare('SELECT * FROM advertisements ORDER BY created_at DESC LIMIT 50').all().map(unpack),
  used,tick,
  async route(req,res,path,role){
   if(!path.startsWith('/api/advertisements'))return false;
   if(role!=='owner')throw fail(403,'Only the owner can manage advertisement projects.');
   if(path==='/api/advertisements'&&req.method==='POST'){
    const data=await body(req),title=requiredText(data.title,160),topic=requiredText(data.topic,2000);
    if(![30,60].includes(data.duration)||!['9:16','16:9'].includes(data.ratio)||!['Hindi','English','Hinglish'].includes(data.language))throw fail(400,'Choose a supported duration, format and language.');
    const presenter=typeof data.presenter==='string'?data.presenter.trim():'';if(presenter.length>600)throw fail(400,'Presenter description is too long.');
    if(!env.OPENAI_API_KEY||!env.OPENAI_MODEL)throw fail(503,'Configure OpenAI to prepare a storyboard.');
    if(planning)throw fail(409,'A storyboard is already being prepared.');
    if(db.prepare('SELECT COUNT(*) n FROM advertisements').get().n>=50)throw fail(409,'Remove an old advertisement project first.');
    quota('advertisement plans',6);planning=true;
    try{
     const durations=data.duration===30?[8,6,6,6]:[8,8,8,8,8,8,8];
     const answer=await generateImpl({key:env.OPENAI_API_KEY,model:env.OPENAI_MODEL,language:data.language,profile:profile(),agent:{id:'manager',name:'Advertisement director',instruction:`Return ONLY JSON with a scenes array of exactly ${durations.length} items. Each item has prompt (visual scene direction in English, no spoken dialogue), narration (ready to speak in ${data.language}, maximum 12 words per scene). Durations in order: ${durations.join(',')} seconds. Build an engaging opening, useful service information, then a call to action. The app adds a four-second contact card. Do not invent prices, clinic appearance, offers or treatment outcomes. Keep presenter clothing and appearance consistent. Do not include contact address in every scene.`},brief:JSON.stringify({topic,presenter})});
     let parsed;try{parsed=JSON.parse(answer.replace(/^```(?:json)?\s*|\s*```$/g,''));}catch{throw fail(502,'Storyboard was not valid. No project was saved.');}
     if(!Array.isArray(parsed.scenes))throw fail(502,'No scenes returned.');
     const scenes=validate({...data,scenes:parsed.scenes.map(s=>({...s,mode:'veo',imageId:''}))});
     const id=randomUUID(),brand=profile();
     db.prepare('INSERT INTO advertisements(id,title,status,data,created_at,updated_at) VALUES(?,?,?,?,?,?)').run(id,title,'draft',JSON.stringify({topic,presenter,duration:data.duration,ratio:data.ratio,language:data.language,scenes,narration:true,voice:'coral',brand,resultId:null}),now(),now());
     audit('advertisement_planned',id);json(res,201,get(id));
    }finally{planning=false;}return true;
   }
   const match=/^\/api\/advertisements\/([a-f0-9-]+)(?:\/(start|pause|check))?$/.exec(path);if(!match)return false;
   const ad=get(match[1]);
   if(req.method==='POST'&&match[2]==='check'){track(tick());json(res,200,get(ad.id));return true;}
   if(req.method==='DELETE'&&!match[2]){
    if(busy||['generating','assembling'].includes(ad.status)||ad.scenes.some(s=>s.mediaId&&['starting','processing'].includes(getMedia(s.mediaId).status)))throw fail(409,'Wait for active scene generation to finish before removing the project.');
    db.prepare('DELETE FROM advertisements WHERE id=?').run(ad.id);
    for(const s of ad.scenes)for(const id of [s.mediaId,s.audioId].filter(Boolean)){const m=getMedia(id);if(m.file)try{unlinkSync(join(mediaDir,m.file));}catch{}db.prepare('DELETE FROM media WHERE id=?').run(id);}
    audit('advertisement_deleted',ad.id);json(res,200,{ok:true});return true;
   }
   const data=await body(req);
   if(data.version!==ad.version)throw fail(409,'This project changed. Refresh and review it again.');
   if(req.method==='PUT'&&!match[2]){
    if(ad.status!=='draft')throw fail(409,'Started projects are locked so paid scenes keep their reviewed script.');
    ad.title=requiredText(data.title,160);ad.scenes=validate({...ad,scenes:data.scenes});
    ad.presenter=typeof data.presenter==='string'?data.presenter.trim():'';if(ad.presenter.length>600)throw fail(400,'Presenter description is too long.');
    ad.narration=data.narration===true;if(!['coral','onyx'].includes(data.voice))throw fail(400,'Choose a supported voice.');ad.voice=data.voice;ad.brand=profile();save(ad);json(res,200,get(ad.id));return true;
   }
   if(req.method==='POST'&&match[2]==='pause'){
    if(!['generating','assembling'].includes(ad.status))throw fail(409,'Project is not running.');
    ad.status='paused';ad.error='Paused by owner. A request already in progress can still complete and be charged.';save(ad);json(res,200,get(ad.id));return true;
   }
   if(req.method==='POST'&&match[2]==='start'){
    if(!['draft','paused'].includes(ad.status))throw fail(409,'This project has already started or finished.');
    if(data.confirmCost!==true)throw fail(400,'Confirm paid video and narration usage.');
    if(busy||db.prepare("SELECT COUNT(*) n FROM advertisements WHERE status IN ('generating','assembling')").get().n)throw fail(409,'One advertisement is already active.');
    if(ad.scenes.some(s=>s.mode==='veo')&&!env.GEMINI_API_KEY)throw fail(503,'Configure GEMINI_API_KEY for Veo scenes.');
    if(ad.status==='paused'&&data.disableNarration===true)ad.narration=false;
    if(ad.narration&&!env.OPENAI_API_KEY)throw fail(503,'Configure OpenAI for narration.');
    for(const s of ad.scenes){if(s.imageId)image(s.imageId);for(const k of ['mediaId','audioId'])if(s[k]&&getMedia(s[k]).status==='failed')s[k]=null;}
    // Reserve enough headroom before authorizing a batch (actual files count on completion).
    const missing=ad.scenes.filter(s=>s.mode==='veo'&&!s.mediaId).length;
    room((missing*64+64+ad.scenes.length*4)*1024*1024);
    ad.status='generating';ad.error=null;save(ad);audit('advertisement_authorized',ad.id);track(tick());json(res,202,get(ad.id));return true;
   }
   return false;
  }
 };
}
