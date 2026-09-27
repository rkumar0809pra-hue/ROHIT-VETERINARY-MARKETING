export function metaPage({state,esc,date}) {
 const m=state.metaConnection||{},owner=state.role==='owner';
 return `<div class="heading"><div><h1>Facebook & Instagram</h1><p>Connect your clinic accounts and check recent activity.</p></div></div>
 <section class="card"><h2>1. Check your connection</h2><p><strong>${esc(m.status||'Not checked')}</strong>${m.checkedAt?' · Last checked '+date(m.checkedAt):''}</p>${m.error?`<div class="banner">${esc(m.error)}</div>`:''}
 ${m.page?`<p>Facebook Page: <strong>${esc(m.page.name)}</strong> · ${esc(m.page.id)}</p>`:''}
 ${m.instagram?`<p>Instagram: <strong>@${esc(m.instagram.username)}</strong> · ${m.instagram.mediaCount} media items</p>`:''}
 <button class="primary" data-meta-check ${!owner||!m.configured?'disabled':''}>Check connection & refresh posts</button>
 ${!m.configured?'<p>Add the Page connection details below first.</p>':''}${!owner?'<p>Ask the owner to check the connection.</p>':''}
 ${(m.notes||[]).map(x=>`<p class="note">${esc(x)}</p>`).join('')}
 <details ${!m.configured?'open':''}><summary>Connection setup</summary><ol><li>Use your Meta developer app and a Page access token for the clinic’s Facebook Page.</li><li>In Render → Environment, set <code>META_PAGE_ID</code> and <code>META_PAGE_ACCESS_TOKEN</code>.</li><li>For Instagram, add <code>META_INSTAGRAM_ACCOUNT_ID</code> for your linked professional account using Facebook Login.</li><li>Save and redeploy, then press <strong>Check connection</strong> above.</li></ol><p class="note">Page reading requires pages_read_engagement; Instagram reading requires instagram_basic and the appropriate account access. App review or advanced access may be required for accounts outside your app roles. Keep access tokens in Render, never in a draft.</p><a href="https://developers.facebook.com/docs/pages-api/getting-started/" target="_blank" rel="noopener noreferrer">Meta setup documentation ↗</a></details></section>
 <section class="card"><h2>2. Recent Facebook posts</h2><p class="note">Up to 10 posts fetched from Meta, not a complete analytics report. ${m.checkedAt?'Snapshot from the last check.':''}</p>${(m.posts||[]).map(p=>`<article class="draft-row"><div><p class="output">${esc(p.message||'Media post without a caption')}</p>${p.createdAt?`<small>${date(p.createdAt)}</small>`:''}${p.url?`<p><a href="${esc(p.url)}" target="_blank" rel="noopener noreferrer">View live post ↗</a></p>`:''}</div></article>`).join('')||'<p>No posts loaded. Verify the connection above.</p>'}</section>
 <section class="card"><h2>3. Prepare your next post</h2><button data-page="creator">Write a post</button><p class="note">This connection reads account information. Automatic publishing, ads, leads and WhatsApp sending are not enabled in this release. Continue sharing approved content manually.</p></section>`;
}
export async function metaClick(el,c){
 if(!el.hasAttribute('data-meta-check'))return false;
 el.disabled=true;el.textContent='Checking…';
 try{await c.api('meta/check','POST',{});await c.refresh();c.render();c.notice('Connection checked.');}
 catch(e){await c.refresh();c.render();c.notice(e.message);}
 return true;
}
