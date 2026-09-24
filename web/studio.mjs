import {photoCategories} from './public/video-options.js';
import { createAdvertisements } from './advertisements.mjs';
import { brandVideo } from './video-branding.mjs';
import { randomUUID } from 'node:crypto';
import { mkdirSync, writeFileSync, readFileSync, unlinkSync, statSync, createReadStream, renameSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { defaultProfile, brandInstructions } from './brand.mjs';
import { mediaProvider, ProviderError } from './media-provider.mjs';

export function createStudio({ db, env, dbPath, audit, json, body, requiredText, fail, generateImpl, provider, renderAdvertisementImpl }) {
  provider ||= mediaProvider(env);
  db.exec(`CREATE TABLE IF NOT EXISTS profile(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS chat(id TEXT PRIMARY KEY,role TEXT NOT NULL,text TEXT NOT NULL,created_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS media(id TEXT PRIMARY KEY,kind TEXT NOT NULL,title TEXT NOT NULL,prompt TEXT NOT NULL,ratio TEXT NOT NULL,duration INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,operation TEXT,file TEXT,mime TEXT,bytes INTEGER NOT NULL DEFAULT 0,error TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS quota(id INTEGER PRIMARY KEY,kind TEXT NOT NULL,created_at TEXT NOT NULL);`);
  const mediaColumns=db.prepare('PRAGMA table_info(media)').all().map(c=>c.name);
  for(const name of ['category','description'])if(!mediaColumns.includes(name))db.exec(`ALTER TABLE media ADD COLUMN ${name} TEXT NOT NULL DEFAULT ''`);
  function photoDetails(data){const category=data.category||'Unlabelled',description=data.description||'';if(!photoCategories.includes(category)||typeof description!=='string'||description.length>600)throw fail(400,'Choose a photo category and a description up to 600 characters.');return {category,description:description.trim()};}
  const columns = db.prepare('PRAGMA table_info(drafts)').all().map(c => c.name);
  for (const [name, spec] of Object.entries({meta_json:"TEXT NOT NULL DEFAULT '{}'", published_at:'TEXT', published_url:'TEXT', metrics_json:"TEXT NOT NULL DEFAULT '{}'"})) {
    if (!columns.includes(name)) db.exec(`ALTER TABLE drafts ADD COLUMN ${name} ${spec}`);
  }
  db.prepare("UPDATE media SET status='failed',error='Server restarted before the generation request finished. Check provider usage before retrying.' WHERE status='starting'").run();
  const mediaDir = env.MEDIA_DIR || join(dbPath === ':memory:' ? '/tmp/rvh-studio-' + randomUUID() : dirname(dbPath), 'media');
  mkdirSync(mediaDir, { recursive: true });
  const now = () => new Date().toISOString();
  let stopped = false, chatBusy = false;
  let managerBusy=false;
  const running = new Set(), tasks = new Set();
  const track = (promise) => { tasks.add(promise); promise.finally(() => tasks.delete(promise)).catch(() => {}); return promise; };
  const profile = () => ({ ...defaultProfile, ...JSON.parse(db.prepare('SELECT data FROM profile WHERE id=1').get()?.data || '{}') });
  const media = () => db.prepare('SELECT id,kind,title,prompt,ratio,duration,status,mime,bytes,error,created_at,updated_at,category,description FROM media ORDER BY created_at DESC LIMIT 200').all();
  const getMedia = id => { const row=db.prepare('SELECT * FROM media WHERE id=?').get(id); if(!row) throw fail(404,'Media not found.'); return row; };
  const publicMedia = row => { const {file, operation,...rest}=row; return rest; };
  function quota(kind, max) {
    const since=new Date(Date.now()-86400000).toISOString();
    if(db.prepare('SELECT COUNT(*) AS n FROM quota WHERE kind=? AND created_at>?').get(kind,since).n >= max) throw fail(429,`Daily ${kind} limit reached. Try again tomorrow.`);
    db.prepare('INSERT INTO quota(kind,created_at) VALUES(?,?)').run(kind,now());
  }
  function room(extra) {
    const used = db.prepare('SELECT COALESCE(SUM(bytes),0) AS n FROM media').get().n;
    if(used+extra > 600*1024*1024) throw fail(413,'Media storage is full (600 MB). Download and remove old media first.');
  }
  function finish(id, bytes, mime) {
    room(bytes.length);
    const file=id+(mime==='video/mp4'?'.mp4':mime==='audio/pcm'?'.pcm':mime==='image/jpeg'?'.jpg':'.png');
    writeFileSync(join(mediaDir,file+'.tmp'),bytes);
    renameSync(join(mediaDir,file+'.tmp'),join(mediaDir,file));
    db.prepare("UPDATE media SET status='completed',file=?,mime=?,bytes=?,error=NULL,updated_at=? WHERE id=?").run(file,mime,bytes.length,now(),id);
    audit('media_completed',id);
  }
  const message = err => err instanceof ProviderError || err.status ? err.message : 'Generation could not complete. Check provider configuration and try again.';
  async function poll(row) {
    if(stopped || running.has(row.id)) return;
    running.add(row.id);
    try {
      const uri=await provider.pollVideo(row.operation);
      if(uri) {
        const original=await provider.downloadVideo(uri);
        // Preserve the provider result even if local branding is unavailable.
        let branded;
        try {if(row.kind!=='scene'&&env.VIDEO_BRANDING!=='off') branded=await brandVideo(original,profile(),row.ratio,row.duration);}catch{}
        finish(row.id,branded||original,'video/mp4');
        if(branded) db.prepare('UPDATE media SET duration=? WHERE id=?').run(row.duration+4,row.id);
        else if(row.kind!=='scene'&&env.VIDEO_BRANDING!=='off') db.prepare('UPDATE media SET error=? WHERE id=?').run('Original video saved. Automatic closing card could not be added; check server video dependencies.',row.id);
      }
      else db.prepare('UPDATE media SET error=NULL,updated_at=? WHERE id=?').run(now(),row.id);
    } catch(err) {
      // Keep the saved operation for retries; never start a second billable generation automatically.
      if(err.terminal) db.prepare("UPDATE media SET status='failed',error=?,updated_at=? WHERE id=?").run(message(err),now(),row.id);
      else db.prepare('UPDATE media SET error=?,updated_at=? WHERE id=?').run(message(err),now(),row.id);
    } finally { running.delete(row.id); }
  }
  const ads=createAdvertisements({db,env,profile,provider,generateImpl,body,json,fail,requiredText,audit,quota,room,finish,getMedia,mediaDir,track,isStopped:()=>stopped,renderImpl:renderAdvertisementImpl});
  const timer=setInterval(() => {
    if(stopped) return;
    for(const row of db.prepare("SELECT * FROM media WHERE status='processing'").all()) {
      if(Date.now()-Date.parse(row.created_at)>24*3600000) db.prepare("UPDATE media SET status='failed',error='Video did not finish within 24 hours. Check your provider before starting another request.' WHERE id=?").run(row.id);
      else track(poll(row));
    }
    track(ads.tick());
  },15000);
  timer.unref();
  function metadata(data) {
    const out={};
    for(const key of ['platform','category','audience','service','tone','cta','headline','hashtags','imagePrompt','videoPrompt','batch','week','day']) {
      const val=data?.[key];
      if(val !== undefined) { if(typeof val!=='string'||val.length>3000) throw fail(400,'Invalid content details.'); out[key]=val.trim(); }
    }
    return out;
  }
  function draft(data) {
    const id=randomUUID();
    const meta=metadata(data.meta);
    const lang=['Hindi','English','Hinglish'].includes(data.language)?data.language:'Hindi';
    db.prepare('INSERT INTO drafts(id,title,agent,language,brief,content,created_at,meta_json) VALUES(?,?,?,?,?,?,?,?)').run(id,requiredText(data.title,160),data.agent||'manual',lang,data.brief||'',requiredText(data.content,20000),now(),JSON.stringify(meta));
    audit('created',id); return db.prepare('SELECT * FROM drafts WHERE id=?').get(id);
  }
  return {
    profile, metadata,
    state: () => ({ profile: profile(), media: media(), advertisements:ads.list(), chat:db.prepare('SELECT * FROM chat ORDER BY created_at DESC LIMIT 100').all().reverse(), mediaConfigured:{video:Boolean(env.GEMINI_API_KEY),image:Boolean(env.OPENAI_API_KEY)}, mediaModels:{video:env.VEO_MODEL||'veo-3.1-fast-generate-preview',image:env.OPENAI_IMAGE_MODEL||'gpt-image-1'}, mediaBytes:db.prepare('SELECT COALESCE(SUM(bytes),0) AS n FROM media').get().n, release:'studio-3-ads' }),
    close: async () => { stopped=true; clearInterval(timer); await Promise.allSettled([...tasks]); },
    async route(req,res,path,role) {
      if(await ads.route(req,res,path,role))return true;
      const owner=()=>{if(role!=='owner') throw fail(403,'Only the owner can change this setting.');};
      if(path==='/api/weekly-plan' && req.method==='POST') {
        owner(); const data=await body(req);
        const goal=requiredText(data.goal,1500), availability=requiredText(data.availability,1000);
        if(!/^\d{4}-\d{2}-\d{2}$/.test(data.week)||!Number.isFinite(Date.parse(data.week))||new Date(data.week).toISOString().slice(0,10)!==data.week)throw fail(400,'Choose a valid week start date.');
        if(typeof data.budget!=='number'||!Number.isFinite(data.budget)||data.budget<0||data.budget>1000000)throw fail(400,'Enter a valid weekly budget.');
        if(!env.OPENAI_API_KEY||!env.OPENAI_MODEL)throw fail(503,'Configure OpenAI before creating a weekly plan.');
        if(managerBusy)throw fail(409,'A weekly plan is already being prepared.');
        quota('weekly plans',4);managerBusy=true;
        try {
          const answer=await generateImpl({key:env.OPENAI_API_KEY,model:env.OPENAI_MODEL,language:'Hindi',profile:profile(),agent:{id:'manager',name:'Weekly marketing manager',instruction:'Return ONLY a JSON object with an items array of exactly seven entries. Each entry has title, content (ready to review Hindi caption or video storyboard), platform (Facebook, Instagram, WhatsApp or Video), agent (content, whatsapp or video), and day (integer 0 through 6, unique). Respect the supplied availability and budget; no invented offers. These are assigned drafts, not executed tasks. Include clinic contact details. Override the plain text formatting instruction with JSON for this structured task.'},brief:JSON.stringify({goal,availability,weeklyBudgetINR:data.budget,week:data.week})});
          let plan;try{plan=JSON.parse(answer.replace(/^```(?:json)?\s*|\s*```$/g,''));}catch{throw fail(502,'The manager returned an invalid plan. No drafts were saved.');}
          if(!Array.isArray(plan.items)||plan.items.length!==7||new Set(plan.items.map(x=>x.day)).size!==7)throw fail(502,'The manager must return seven unique daily drafts.');
          for(const x of plan.items){requiredText(x.title,160);requiredText(x.content,20000);if(!Number.isInteger(x.day)||x.day<0||x.day>6||!['content','video','whatsapp'].includes(x.agent)||!['Facebook','Instagram','WhatsApp','Video'].includes(x.platform))throw fail(502,'Invalid task in the generated plan.');}
          const batch=randomUUID();db.exec('BEGIN');
          try {for(const x of plan.items){const d=draft({title:x.title,content:x.content,agent:x.agent,language:'Hindi',brief:JSON.stringify({goal,availability,budget:data.budget,week:data.week,day:x.day}),meta:{platform:x.platform,category:'Weekly plan',batch,week:data.week,day:String(x.day)}});db.prepare("UPDATE drafts SET status='pending' WHERE id=?").run(d.id);}audit('weekly_plan_created',batch);db.exec('COMMIT');}catch(err){db.exec('ROLLBACK');throw err;}
          json(res,201,{batch,count:7});
        } finally {managerBusy=false;}
        return true;
      }
      if(path==='/api/approval-batch' && req.method==='POST') {
        owner();const data=await body(req);
        if(!['approve','reject'].includes(data.action)||!Array.isArray(data.items)||!data.items.length||data.items.length>100)throw fail(400,'Select up to 100 pending drafts.');
        if(new Set(data.items.map(x=>x.id)).size!==data.items.length)throw fail(400,'Duplicate selections.');
        db.exec('BEGIN');try{for(const item of data.items){const d=db.prepare('SELECT * FROM drafts WHERE id=?').get(item.id);if(!d||d.status!=='pending'||d.version!==item.version)throw fail(409,'A selected draft changed. Refresh and review again.');db.prepare('UPDATE drafts SET status=?,version=version+1 WHERE id=?').run(data.action==='approve'?'approved':'draft',item.id);audit(data.action,item.id);}db.exec('COMMIT');}catch(err){db.exec('ROLLBACK');throw err;}
        json(res,200,{count:data.items.length});return true;
      }
      if(path==='/api/profile' && req.method==='PUT') {
        owner(); const data=await body(req), old=profile();
        for(const [k, max] of Object.entries({bookingUrl:1000,name:120,hindiName:120,phone:30,address:300,services:1000,hours:200,guidelines:2000})) {
          if(k==='bookingUrl' && data[k]===undefined) continue;
          if(typeof data[k]!=='string'||data[k].length>max) throw fail(400,`Invalid ${k}.`);
          if(['name','hindiName','phone','address'].includes(k) && !data[k].trim()) throw fail(400,`${k} is required.`);
          old[k]=data[k].trim();
        }
        if(old.bookingUrl) {let u;try{u=new URL(old.bookingUrl);}catch{throw fail(400,'Enter a valid booking URL.');}if(u.protocol!=='https:'||u.username||u.password)throw fail(400,'Booking link must use HTTPS.');}
        if(!/^\+?[\d ()-]{7,25}$/.test(old.phone)) throw fail(400,'Enter a valid clinic phone number.');
        db.prepare('INSERT OR REPLACE INTO profile VALUES(1,?)').run(JSON.stringify(old)); audit('profile_updated'); json(res,200,old); return true;
      }
      if(path==='/api/studio/drafts' && req.method==='POST') {json(res,201,draft(await body(req)));return true;}
      if(path==='/api/chat' && req.method==='POST') {
        const data=await body(req); const text=requiredText(data.text,4000);
        if(!env.OPENAI_API_KEY||!env.OPENAI_MODEL) throw fail(503,'Configure OpenAI in Render to use the assistant.');
        if(chatBusy) throw fail(429,'The assistant is already replying.');
        quota('chat',60);chatBusy=true;
        const history=db.prepare('SELECT role,text FROM chat ORDER BY created_at DESC LIMIT 12').all().reverse();
        try {
          const answer=await generateImpl({key:env.OPENAI_API_KEY,model:env.OPENAI_MODEL,agent:{name:'Marketing assistant',id:'assistant',instruction:'Answer the marketing question using the conversation. Offer practical next steps, captions, campaign ideas and media prompts. You cannot browse the web, send messages, or modify the app; do not claim those actions.'},language:['English','Hindi','Hinglish'].includes(data.language)?data.language:'Hindi',brief:JSON.stringify({history,message:text}),profile:profile()});
          const reply=requiredText(answer,20000);
          db.prepare('INSERT INTO chat VALUES(?,?,?,?)').run(randomUUID(),'user',text,now());
          const id=randomUUID();db.prepare('INSERT INTO chat VALUES(?,?,?,?)').run(id,'assistant',reply,now());
          audit('chat_reply',id);json(res,201,{id,text:reply});
        } finally {chatBusy=false;}
        return true;
      }
      if(path==='/api/chat'&&req.method==='DELETE') {owner();db.prepare('DELETE FROM chat').run();audit('chat_cleared');json(res,200,{ok:true});return true;}
      if(path==='/api/export'&&req.method==='GET') {
        owner();res.setHeader('Content-Disposition','attachment; filename="rvh-workspace.json"');
        json(res,200,{exportedAt:now(),profile:profile(),drafts:db.prepare('SELECT * FROM drafts').all(),metrics:db.prepare('SELECT * FROM metrics').all(),media:media(),advertisements:ads.list(),chat:db.prepare('SELECT * FROM chat').all(),audit:db.prepare('SELECT * FROM audit ORDER BY id DESC LIMIT 1000').all()});return true;
      }
      if(path==='/api/media' && req.method==='POST') {
        const data=await body(req,9*1024*1024);
        const kind=data.kind;
        if(!['image','video','upload'].includes(kind)) throw fail(400,'Choose image, video or upload.');
        if(kind==='video'&&!env.GEMINI_API_KEY) throw fail(503,'Add GEMINI_API_KEY in Render Environment to enable Veo.');
        if(kind==='image'&&!env.OPENAI_API_KEY) throw fail(503,'Add OPENAI_API_KEY to enable image creation.');
        if(kind!=='upload'&&data.confirmCost!==true) throw fail(400,'Confirm provider usage charges before generation.');
        const id=randomUUID(),title=requiredText(data.title,160),prompt=kind==='upload'?'':requiredText(data.prompt,6000);
        const labels=kind==='upload'?photoDetails(data):null;
        const ratio=['9:16','16:9','1:1'].includes(data.ratio)?data.ratio:'9:16';
        if(kind==='video'&&(ratio==='1:1'||![4,6,8].includes(data.duration))) throw fail(400,'Veo supports 9:16 or 16:9 and 4, 6 or 8 seconds here.');
        let image=null,bytes=null,mime=null;
        if(kind==='upload') {
          if(typeof data.base64!=='string'||data.base64.length>8*1024*1024||!/^[A-Za-z0-9+/]*={0,2}$/.test(data.base64)) throw fail(400,'Invalid image upload.');
          bytes=Buffer.from(data.base64,'base64');
          mime=bytes.subarray(0,8).toString('hex')==='89504e470d0a1a0a'?'image/png':bytes.subarray(0,3).toString('hex')==='ffd8ff'?'image/jpeg':null;
          if(!mime||bytes.length>6*1024*1024||bytes.length<16) throw fail(400,'Upload a PNG or JPEG image, up to 6 MB.');
        }
        if(kind==='video'&&data.imageId) {
          const source=getMedia(data.imageId);
          if(source.status!=='completed'||!['image/png','image/jpeg'].includes(source.mime)||source.bytes>6*1024*1024) throw fail(400,'Choose a completed PNG/JPEG image up to 6 MB.');
          image={bytes:readFileSync(join(mediaDir,source.file)),mime:source.mime};
        }
        if(kind!=='upload' && db.prepare("SELECT COUNT(*) n FROM media WHERE status IN ('starting','processing')").get().n>=2) throw fail(429,'Two media jobs are already active.');
        room(kind==='video'?64*1024*1024:bytes?.length||20*1024*1024);
        quota(kind==='upload'?'uploads':'media',kind==='upload'?100:12);
        db.prepare('INSERT INTO media(id,kind,title,prompt,ratio,duration,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)').run(id,kind,title,prompt,ratio,kind==='video'?data.duration:0,'starting',now(),now());
        audit('media_requested',id);
        if(kind==='upload') {db.prepare('UPDATE media SET category=?,description=? WHERE id=?').run(labels.category,labels.description,id);finish(id,bytes,mime);}
        else {
          const brand=brandInstructions(profile());
          const work=async()=>{
            try {
              if(kind==='image') finish(id,await provider.image({prompt:`${brand}\nCreate a veterinary marketing image. ${prompt}\nUse only the supplied brand identity for any contact text. Do not invent offers or promise cures.`,ratio}),'image/png');
              else {
                const operation=await provider.startVideo({prompt:`${brand}\n${prompt}\nRespectful veterinary marketing scene. No guaranteed cure claims.`,ratio,duration:data.duration,image});
                db.prepare("UPDATE media SET status='processing',operation=?,updated_at=? WHERE id=?").run(operation,now(),id);
              }
            } catch(err) { db.prepare("UPDATE media SET status='failed',error=?,updated_at=? WHERE id=?").run(message(err),now(),id); }
          };
          track(work());
        }
        json(res,202,publicMedia(getMedia(id)));return true;
      }
      const match=/^\/api\/media\/([a-f0-9-]+)(?:\/(file|check))?$/.exec(path);
      if(match) {
        const row=getMedia(match[1]);
        if(req.method==='PATCH'&&!match[2]){owner();if(row.kind!=='upload')throw fail(400,'Only uploaded clinic photos can be labelled.');const data=await body(req),labels=photoDetails(data),title=requiredText(data.title,160);db.prepare('UPDATE media SET title=?,category=?,description=?,updated_at=? WHERE id=?').run(title,labels.category,labels.description,now(),row.id);audit('photo_labelled',row.id);json(res,200,publicMedia(getMedia(row.id)));return true;}
        if(req.method==='POST'&&match[2]==='check') {
          if(row.status==='processing') await track(poll(row));
          json(res,200,publicMedia(getMedia(row.id)));return true;
        }
        if(req.method==='DELETE'&&!match[2]) {
          owner();if(ads.used(row.id))throw fail(409,'This media belongs to an advertisement. Remove the project first.');if(['starting','processing'].includes(row.status)||running.has(row.id)) throw fail(409,'Wait for this job to finish before deleting it.');
          if(row.file) {try{unlinkSync(join(mediaDir,row.file));}catch(err){if(err.code!=='ENOENT')throw err;}}
          db.prepare('DELETE FROM media WHERE id=?').run(row.id);audit('media_deleted',row.id);json(res,200,{ok:true});return true;
        }
        if(req.method==='GET'&&match[2]==='file') {
          if(row.status!=='completed'||!row.file) throw fail(409,'Media is not ready.');
          const file=join(mediaDir,row.file);let size;
          try{size=statSync(file).size;}catch{throw fail(404,'Media file is missing. Check the persistent disk configuration.');}
          const download=new URL(req.url,'http://local').searchParams.has('download');
          res.setHeader('Content-Type',row.mime);res.setHeader('Accept-Ranges','bytes');
          if(download)res.setHeader('Content-Disposition',`attachment; filename="rvh-${row.id}${row.mime==='video/mp4'?'.mp4':row.mime==='image/png'?'.png':'.jpg'}"`);
          let start=0,end=size-1,code=200;
          if(req.headers.range) {
            const r=/^bytes=(\d*)-(\d*)$/.exec(req.headers.range);
            if(!r||!r[1]&&!r[2])throw fail(416,'Invalid byte range.');
            if(!r[1]){start=Math.max(0,size-Number(r[2]));}else {start=Number(r[1]);if(r[2])end=Math.min(Number(r[2]),end);}
            if(start>end||start>=size) {res.setHeader('Content-Range',`bytes */${size}`);throw fail(416,'Invalid byte range.');}
            code=206;res.setHeader('Content-Range',`bytes ${start}-${end}/${size}`);
          }
          res.setHeader('Content-Length',end-start+1);res.writeHead(code);
          const stream=createReadStream(file,{start,end});stream.on('error',()=>res.destroy());res.on('close',()=>stream.destroy());stream.pipe(res);return true;
        }
      }
      return false;
    },
  };
}
