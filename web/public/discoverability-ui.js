let tab='seo', working=false;
const values={url:'https://rohitveterinary.com/',topic:'',domains:null,brand:null,keyword:'',question:''};
const links=(sources,esc)=>(sources||[]).filter(s=>{try{return new URL(s.url).protocol==='https:';}catch{return false;}}).map(s=>`<a href="${esc(s.url)}" target="_blank" rel="noopener noreferrer">${esc(s.title||s.url)} ↗</a>`).join(' · ');
export function discoverPage(c){
 const {state:s,esc,date}=c,seo=s.seo,aio=s.aio,disabled=working?'disabled':'';
 const research=(item,kind)=>`<article class="card"><h3>${esc(item.keyword||item.query)}</h3><button data-research="${kind}" data-id="${item.id}" ${disabled} ${!s.aiConfigured?'disabled':''}>Research on the web</button><button data-discover-remove="${kind}" data-id="${item.id}" ${disabled}>Remove</button>${item.lastChecked?`<p class="output">${esc(item.lastChecked)}</p><p class="note">${date(item.lastCheckedAt)}</p><p>${links(item.sources,esc)}</p>`:'<p class="note">Not researched yet.</p>'}</article>`;
 return `<div class="heading"><div><h1>SEO & AI Visibility</h1><p>Help people find रोहित भेटनरी हाउस. Check a page, then prepare improvements.</p></div></div>
 <div class="actions"><button data-discover-tab="seo" class="${tab==='seo'?'primary':''}">Google / SEO</button><button data-discover-tab="aio" class="${tab==='aio'?'primary':''}">AI answers / AIO</button><button data-page="meta">Facebook & Instagram</button></div>
 ${tab==='seo'?`<section class="card"><h2>1. Check a website page</h2><p>Choose the clinic, app or mart. A public information page is more useful than a login screen.</p><form id="discover-scan"><label for="discover-url">Public page URL</label><input id="discover-url" name="url" type="url" required maxlength="1000" value="${esc(values.url)}"><div class="actions">${['rohitveterinary.com','app.rohitveterinary.com','mart.rohitveterinary.com'].map(host=>`<button type="button" data-scan-host="${host}">${host.split('.')[0]==='rohitveterinary'?'Clinic website':host.startsWith('app.')?'Clinic app':'Vet mart'}</button>`).join('')}</div><button class="primary" ${disabled} ${s.role!=='owner'?'disabled':''}>${working?'Working…':'Scan this page'}</button></form><p class="note">No AI charge for this HTML scan. It does not change your website.</p></section>
 ${(seo.scans||[]).map(scan=>`<section class="card"><h3>${esc(scan.url)}</h3><p class="note">Checked ${date(scan.checkedAt)}</p>${scan.checks.map(x=>`<details><summary>${x.passed?'✓ Found':'Review'} · ${esc(x.label)}</summary><p>${esc(x.observed)}</p>${!x.passed?`<p>${esc(x.action)}</p>`:''}</details>`).join('')}<p class="note">${esc(scan.limitations)}</p></section>`).join('')}`:
 `<section class="card"><h2>1. Answer real customer questions</h2><p>Create clear answers about your services, location and appointment process. Use confirmed facts from your clinic profile.</p><p class="note">AIO means making useful information easy to understand and find. It does not guarantee mentions in ChatGPT, Gemini or Google AI answers.</p></section>`}
 <section class="card"><h2>2. Draft an improvement</h2><form id="discover-draft"><label for="discover-topic">${tab==='seo'?'Which service or page should we write?':'What question should your website answer?'}</label><textarea id="discover-topic" name="topic" maxlength="1000" required placeholder="Example: How can pet owners book a consultation in Lohardaga?">${esc(values.topic)}</textarea><button class="primary" ${disabled} ${!s.aiConfigured||s.role!=='owner'?'disabled':''}>Create Hindi website draft</button></form><p class="note">Uses your OpenAI account. Saves to your review workflow; applying it to the clinic or mart website is a separate step.</p></section>
 <details class="card"><summary>Optional: research and progress checklist</summary><p class="note">Web research includes sources when available. It does not measure Google positions or prove citations in other AI products.</p>
 <form id="discover-settings"><label>Website domains<input name="domains" id="discover-domains" value="${esc(values.domains??seo.domains.join(', '))}" placeholder="rohitveterinary.com, app.rohitveterinary.com, mart.rohitveterinary.com" maxlength="2000"></label><label>Business name<input name="brand" id="discover-brand" value="${esc(values.brand??(seo.brand||s.profile.name))}" maxlength="200"></label><button ${disabled} ${s.role!=='owner'?'disabled':''}>Save research settings</button></form>
 <form id="discover-research-add"><label>${tab==='seo'?'Search phrase':'Customer question'}<input id="discover-${tab==='seo'?'keyword':'question'}" name="query" required maxlength="200" value="${esc(tab==='seo'?values.keyword:values.question)}"></label><button ${disabled}>Add for research</button></form>
 ${(tab==='seo'?seo.keywords:aio.queries).map(x=>research(x,tab)).join('')}
 <h3>Your checklist (marked by you)</h3>${(tab==='seo'?s.seoChecklist:s.aioChecklist).map(([key,label])=>`<p><label><input type="checkbox" data-discover-check="${key}" data-kind="${tab}" ${(tab==='seo'?seo:aio).checklist[key]?'checked':''} ${disabled}> ${esc(label)}</label></p>`).join('')}
 <p><a href="https://search.google.com/search-console" target="_blank" rel="noopener noreferrer">Open Google Search Console ↗</a></p></details>`;
}
export function discoverabilityChange(e){if(e.target.id.startsWith('discover-')){const k=e.target.id.slice(9);if(k in values)values[k]=e.target.value;}}
async function work(c,fn){if(working)return;working=true;c.render();try{await fn();await c.refresh();}finally{working=false;c.render();}}
export async function discoverabilityClick(el,c){
 if(el.dataset.discoverTab){tab=el.dataset.discoverTab;c.render();return true;}
 if(el.dataset.scanHost){values.url='https://'+el.dataset.scanHost+'/';c.render();return true;}
 if(el.dataset.discoverCheck){await work(c,()=>c.api(el.dataset.kind+'/checklist','PATCH',{key:el.dataset.discoverCheck}));return true;}
 if(el.dataset.research){await work(c,()=>c.api(`${el.dataset.research}/${el.dataset.research==='seo'?'keywords':'queries'}/${el.dataset.id}/check`,'POST',{}));return true;}
 if(el.dataset.discoverRemove){await work(c,()=>c.api(`${el.dataset.discoverRemove}/${el.dataset.discoverRemove==='seo'?'keywords':'queries'}/${el.dataset.id}`,'DELETE'));return true;}
 return false;
}
export async function discoverabilitySubmit(form,c){
 if(!form.id.startsWith('discover-'))return false;
 const f=Object.fromEntries(new FormData(form));
 if(form.id==='discover-scan')await work(c,async()=>{await c.api('discover/scan','POST',f);c.notice('Page check saved. Review the findings below.');});
 if(form.id==='discover-settings')await work(c,async()=>{await c.api('seo/settings','PUT',{domains:f.domains.split(',').map(s=>s.trim()).filter(Boolean),brand:f.brand});values.domains=values.brand=null;});
 if(form.id==='discover-draft'){let id;await work(c,async()=>{const d=await c.api('discover/draft','POST',{topic:f.topic,kind:tab});id=d.id;});if(id)c.go('editor',id);}
 if(form.id==='discover-research-add')await work(c,async()=>{await c.api(tab+'/'+(tab==='seo'?'keywords':'queries'),'POST',tab==='seo'?{keyword:f.query}:{query:f.query});values.keyword=values.question='';});
 return true;
}
