import {presenters,voices,voiceStyles} from './video-options.js';
let selected='',busy=false;
let brief={title:'',topic:'',presenter:'',presenterType:'female',voice:'coral',voiceStyle:'warm',duration:'30',ratio:'9:16',language:'Hindi',photoIds:[]};
const opts=(values,current,esc)=>values.map(v=>`<option value="${esc(v)}" ${v===current?'selected':''}>${esc(v)}</option>`).join('');
const selectOptions=(values,current,esc)=>Object.entries(values).map(([value,label])=>`<option value="${esc(value)}" ${value===current?'selected':''}>${esc(label)}</option>`).join('');
const readBrief=form=>{const f=new FormData(form);return {...Object.fromEntries(f),photoIds:f.getAll('photoIds')};};
const casting=(data,esc)=>`<div class="metrics-grid"><label>Presenter<select name="presenterType">${selectOptions(presenters,data.presenterType||'custom',esc)}</select></label><label>AI narration voice<select name="voice">${selectOptions(voices,data.voice||'coral',esc)}</select></label><label>Voice delivery<select name="voiceStyle">${selectOptions(voiceStyles,data.voiceStyle||'warm',esc)}</select></label></div><p class="note">Built-in AI voices with original delivery styles. These are not celebrity voices. Presenter appearance and narration voice are selected separately.</p>`;
export function advertisementSection(c){
 const {state,esc}=c,rows=state.advertisements||[],ad=rows.find(a=>a.id===selected);
 const images=state.media.filter(m=>m.status==='completed'&&['image/png','image/jpeg'].includes(m.mime)&&m.bytes<=6*1024*1024);
 const photos=images.filter(m=>m.kind==='upload');
 const owner=state.role==='owner';
 let content='';
 if(ad){
  const editable=ad.status==='draft',active=['generating','assembling'].includes(ad.status);
  const missing=ad.scenes.filter(s=>s.mode==='veo'&&(!s.mediaId||state.media.find(m=>m.id===s.mediaId)?.status==='failed'));
  const seconds=missing.reduce((n,s)=>n+s.duration,0),model=state.mediaModels.video;
  const rate={'veo-3.1-fast-generate-preview':0.10,'veo-3.1-generate-preview':0.40,'veo-3.1-lite-generate-preview':0.05}[model];
  const result=state.media.find(m=>m.id===ad.resultId);
  content=`<div class="section-title"><h3>${esc(ad.title)}</h3><span class="pill">${esc(ad.status)}</span></div>${ad.error?`<p class="banner">${esc(ad.error)}</p>`:''}
  <p>${ad.duration} seconds · ${esc(ad.language)} · ${esc(ad.ratio)} · includes a four-second clinic ending</p><p class="note">${esc(ad.brand.hindiName)} · ${esc(ad.brand.phone)} · ${esc(ad.brand.address)}</p>
  ${result?.status==='completed'?`<video controls preload="metadata" class="asset-preview" src="/api/media/${result.id}/file"></video><p><a class="button primary" href="/api/media/${result.id}/file?download">Download complete advertisement</a></p><p>Review the visuals, pronunciation and clinic details before publishing. Narration is AI-generated. No post has been published.</p>`:''}
  <h3>4. Review scenes and photos</h3><form id="ad-edit" data-id="${ad.id}" data-version="${ad.version}"><fieldset ${!editable||!owner?'disabled':''}>
  <label>Advertisement title<input name="title" value="${esc(ad.title)}" maxlength="160" required></label>
  ${casting(ad,esc)}<label>Presenter appearance / continuity notes<textarea name="presenter" maxlength="600">${esc(ad.presenter)}</textarea></label>
  <div class="metrics-grid"><label class="check"><input type="checkbox" name="narration" ${ad.narration?'checked':''}> Add AI narration (OpenAI usage)</label></div>
  <p class="note">Scene captions use the narration text. Narration replaces clip audio; without narration the finished advertisement is silent. Presenter consistency and lip synchronisation are not guaranteed. A photo reference is used as the scene’s starting image.</p>
  ${ad.scenes.map((s,i)=>{const media=state.media.find(m=>m.id===s.mediaId);return `<section class="card"><h3>Scene ${i+1} · ${s.duration} seconds</h3><label>Visual source<select name="mode-${i}"><option value="veo" ${s.mode==='veo'?'selected':''}>Generate Veo clip</option><option value="still" ${s.mode==='still'?'selected':''}>Use photo / screenshot unchanged</option></select></label>
  <label>Photo or screenshot<select name="image-${i}"><option value="">No reference</option>${images.map(m=>`<option value="${m.id}" ${m.id===s.imageId?'selected':''}>${esc(m.title)}</option>`).join('')}</select></label>
  ${s.imageId?`<img class="asset-preview" style="max-height:180px;object-fit:contain" src="/api/media/${s.imageId}/file" alt="Selected scene photo">`:""}<label>Scene direction<textarea name="prompt-${i}" maxlength="1800" required>${esc(s.prompt)}</textarea></label>
  <label>Narration and caption (up to ${s.duration*3} words)<textarea name="narration-${i}" maxlength="240" required>${esc(s.narration)}</textarea></label>
  ${media?`<p>Clip: ${esc(media.status)}</p>${media.error?`<p class="banner">${esc(media.error)}</p>`:''}`:''}</section>`;}).join('')}
  ${editable?`<button class="primary" ${busy?'disabled':''}>Save reviewed scenes</button>`:''}</fieldset></form>
  ${ad.scenes.some(s=>s.mediaId)?`<details><summary>Preview saved clips</summary><div class="media-grid">${ad.scenes.map((s,i)=>{const m=state.media.find(m=>m.id===s.mediaId);return m?.status==='completed'?`<div><p>Scene ${i+1}</p><video controls preload="none" class="asset-preview" src="/api/media/${m.id}/file"></video></div>`:'';}).join('')}</div></details>`:''}
  ${['draft','paused'].includes(ad.status)?`<h3>5. Generate your advertisement</h3><form id="ad-start" data-id="${ad.id}" data-version="${ad.version}"><p><strong>Remaining generation:</strong> ${missing.length} new Veo clips (${seconds} seconds)${rate!==undefined?` · estimated video cost US$${(seconds*rate).toFixed(2)}`:''}. ${ad.narration?'Narration adds OpenAI usage charges.':''}</p><p class="note">Video estimate uses listed 720p pricing checked 23 September 2026; excludes narration, script generation, taxes and earlier attempts. <a href="https://ai.google.dev/gemini-api/docs/pricing" target="_blank" rel="noopener">Check Google pricing</a>. Completed clips and narration are reused. A failed or interrupted request may already have incurred usage.</p>${ad.status==='paused'&&ad.narration?'<label class="check"><input type="checkbox" name="silent"> Continue without narration (silent video with captions)</label>':''}<label class="check"><input name="confirm" type="checkbox" required> ${missing.length||ad.narration?'I reviewed and saved these scenes and authorise the remaining paid requests.':'I reviewed these scenes and want to assemble the video.'}</label><button class="primary" ${busy||!owner?'disabled':''}>${ad.status==='draft'?'Generate complete advertisement':'Continue remaining work'}</button></form>`:''}
  <div class="actions">${active?`<button data-ad-pause="${ad.id}" ${!owner?'disabled':''}>Pause after current request</button>`:''}<button data-ad-check="${ad.id}">Refresh progress</button>${!active&&owner?`<button data-ad-delete="${ad.id}">Remove project</button>`:''}</div>`;
 }else content=`<form id="ad-create"><h3>1. Your idea</h3><div class="metrics-grid"><label>Title<input name="title" value="${esc(brief.title)}" maxlength="160" required></label><label>Length<select name="duration">${opts(['30','60'],brief.duration,esc)}</select></label><label>Language<select name="language">${opts(['Hindi','English','Hinglish'],brief.language,esc)}</select></label><label>Format<select name="ratio">${opts(['9:16','16:9'],brief.ratio,esc)}</select></label></div><label>Topic and confirmed details<textarea name="topic" maxlength="2000" placeholder="Introduce our clinic, pet consultations and booking phone" required>${esc(brief.topic)}</textarea></label><h3>2. Choose establishment photos</h3><p>Tick up to seven photos. AI can place matching photos into scenes as unchanged images with narration. Review its choices before generation.</p><div class="media-grid">${photos.map(m=>`<label class="card"><img loading="lazy" class="asset-preview" style="height:120px;object-fit:contain" src="/api/media/${m.id}/file" alt="${esc(m.title)}"><span><input type="checkbox" name="photoIds" value="${m.id}" ${(brief.photoIds||[]).includes(m.id)?'checked':''}> ${esc(m.title)}</span><small>${esc(m.category||'Unlabelled')} · ${esc(m.description||'')}</small></label>`).join('')||'<p>No clinic photos yet. Upload them in Poster & media, then return here.</p>'}</div><h3>3. Presenter and narration</h3>${casting(brief,esc)}<label>Presenter description (optional)<textarea name="presenter" maxlength="600" placeholder="Same adult presenter, navy blazer, light blue shirt in every scene">${esc(brief.presenter)}</textarea></label><button type="button" data-ad-prompt ${busy||!owner||!state.aiConfigured?'disabled':''}>Improve idea with AI</button><p class="note">Enter your idea above. AI adds scene, camera and lighting directions to your editable brief using one OpenAI request.</p><p>First prepare a script using one OpenAI request. Review scenes and costs before starting video generation. Upload clinic photos or app screenshots in Poster & media to use them here.</p><button class="primary" ${busy||!owner||!state.aiConfigured?'disabled':''}>${busy?'Preparing scenes…':'Prepare advertisement script'}</button></form>`;
 return `<section class="card"><div class="section-title"><div><div class="eyebrow">Complete advertisements</div><h2>One idea. A finished clinic video.</h2></div><button data-ad-new>New advertisement</button></div><p>Plan scenes, add your photos, then combine clips, narration, scene captions and clinic branding into one 720p video.</p><label>Saved projects<select id="ad-project"><option value="">Create a new advertisement</option>${rows.map(a=>`<option value="${a.id}" ${a.id===selected?'selected':''}>${esc(a.title)} — ${esc(a.status)}</option>`).join('')}</select></label>${content}</section>`;
}
export function advertisementChange(e,c){if(e.target.closest('#ad-create'))brief=readBrief(e.target.closest('form'));if(e.target.id==='ad-project'){selected=e.target.value;c.render();}}
export async function advertisementClick(el,c){
 if(el.hasAttribute('data-ad-prompt')){
  if(busy)return true;
  brief=readBrief(document.getElementById('ad-create'));
  if(!brief.topic.trim())throw Error('Enter a topic or idea first.');
  busy=true;el.disabled=true;el.textContent='Improving your idea…';
  try{const result=await c.api('advertisements/prompt','POST',{...brief,duration:Number(brief.duration)});brief={...brief,...result};c.notice('AI brief ready. Review the details, then prepare your script.');}
  finally{busy=false;c.render();}return true;
 }
 if(el.hasAttribute('data-ad-new')){selected='';c.render();return true;}
 const id=el.dataset.adCheck||el.dataset.adPause||el.dataset.adDelete;if(!id)return false;
 const ad=c.state.advertisements.find(a=>a.id===id);el.disabled=true;
 try{
  if(el.dataset.adDelete){if(!confirm('Remove this project and its intermediate clips? The completed advertisement and uploaded photos will stay.'))return true;await c.api('advertisements/'+id,'DELETE');selected='';}
  else await c.api('advertisements/'+id+(el.dataset.adPause?'/pause':'/check'),'POST',{version:ad.version});
  await c.refresh();c.render();
 }finally{el.disabled=false;}return true;
}
export async function advertisementSubmit(form,c){
 if(!['ad-create','ad-edit','ad-start'].includes(form.id))return false;
 if(busy)return true;const f=new FormData(form),id=form.dataset.id;busy=true;
 const button=form.querySelector('button');if(button)button.disabled=true;
 try{
  if(form.id==='ad-create'){
   brief=readBrief(form);const ad=await c.api('advertisements','POST',{...brief,duration:Number(brief.duration)});selected=ad.id;
  }else if(form.id==='ad-edit'){
   const ad=c.state.advertisements.find(a=>a.id===id);
   await c.api('advertisements/'+id,'PUT',{version:Number(form.dataset.version),title:f.get('title'),presenter:f.get('presenter'),narration:f.has('narration'),voice:f.get('voice'),presenterType:f.get('presenterType'),voiceStyle:f.get('voiceStyle'),scenes:ad.scenes.map((s,i)=>({mode:f.get('mode-'+i),imageId:f.get('image-'+i),prompt:f.get('prompt-'+i),narration:f.get('narration-'+i)}))});
   c.notice('Reviewed scenes saved. You can now authorise generation below.');
  }else{
   // Require the edit form to match the saved project before spending.
   const edit=document.getElementById('ad-edit'),ad=c.state.advertisements.find(a=>a.id===id);
   if(ad.status==='draft'&&edit){const fields=new FormData(edit);if(fields.get('title')!==ad.title||fields.get('presenter')!==ad.presenter||fields.has('narration')!==ad.narration||fields.get('voice')!==ad.voice||fields.get('presenterType')!==(ad.presenterType||'custom')||fields.get('voiceStyle')!==(ad.voiceStyle||'warm')||ad.scenes.some((s,i)=>fields.get('mode-'+i)!==s.mode||fields.get('image-'+i)!==s.imageId||fields.get('prompt-'+i)!==s.prompt||fields.get('narration-'+i)!==s.narration))throw Error('Save your edited scenes before starting generation.');}
   await c.api('advertisements/'+id+'/start','POST',{version:Number(form.dataset.version),confirmCost:f.has('confirm'),disableNarration:f.has('silent')});c.notice('Advertisement started. Progress is saved; you can leave this page.');
  }
  await c.refresh();busy=false;c.render();
 }finally{busy=false;if(button)button.disabled=false;}return true;
}
