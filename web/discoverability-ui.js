let discoverTab = 'seo';
let seoDomainsText = null, seoBrandText = null;
let checkingId = null;

function heading(title, subtitle) {
  return `<div class="heading"><div><div class="eyebrow">RVH Marketing Studio</div><h1>${title}</h1><p>${subtitle}</p></div></div>`;
}

function checklistBlock(title, items, checked, dataAttr) {
  const done = items.filter(([k]) => checked[k]).length;
  const pct = Math.round((done / items.length) * 100);
  return `<section class="card">
    <div class="section-title"><h2>${title}</h2><span>${pct}% complete</span></div>
    <div class="progress-track"><div class="progress-fill" style="width:${pct}%"></div></div>
    <ul class="checklist">${items.map(([k, label]) => `<li><label><input type="checkbox" data-${dataAttr}-check="${k}" ${checked[k] ? 'checked' : ''}> ${label}</label></li>`).join('')}</ul>
  </section>`;
}

function seoView(c) {
  const { state, esc } = c;
  const seo = state.seo;
  return `
    <section class="card">
      <h2>Live check settings</h2>
      <p class="note">List every site tied to the business, comma-separated. Shared by both SEO and AIO checks below.</p>
      <form id="seo-settings-form">
        <label for="seo-domains">Website domains</label>
        <input id="seo-domains" maxlength="2000" value="${esc(seoDomainsText ?? seo.domains.join(', '))}" placeholder="rohitveterinary.com, app.rohitveterinary.com">
        <label for="seo-brand">Brand name</label>
        <input id="seo-brand" maxlength="200" value="${esc(seoBrandText ?? seo.brand)}" placeholder="Rohit Veterinary House">
        <button class="primary" ${state.role !== 'owner' ? 'disabled' : ''}>Save</button>
        ${state.role !== 'owner' ? '<p class="note">Only the owner can change these settings.</p>' : ''}
      </form>
      ${seo.domains.length ? `<div class="actions">${seo.domains.map((d) => `<span class="pill">${esc(d)}</span>`).join('')}</div>` : '<p class="note">Add at least one domain to enable live checks.</p>'}
    </section>
    ${checklistBlock('On-page checklist', state.seoChecklist, seo.checklist, 'seo')}
    <section class="card">
      <h2>Keyword tracker</h2>
      ${!state.aiConfigured ? '<div class="banner">AI setup needed: add OPENAI_API_KEY and OPENAI_MODEL in Render to enable live rank checks.</div>' : ''}
      <form id="seo-keyword-form">
        <div class="metrics-grid">
          <label>Keyword<input id="kw-keyword" maxlength="200" required placeholder="cattle foot rot treatment"></label>
          <label>Target page<input id="kw-page" maxlength="200" placeholder="Blog: Foot rot in cattle"></label>
        </div>
        <label>Priority<select id="kw-priority"><option>High</option><option selected>Medium</option><option>Low</option></select></label>
        <button class="primary">Add keyword</button>
      </form>
      <div class="table-wrap"><table><thead><tr><th>Keyword</th><th>Page</th><th>Rank</th><th>Priority</th><th>Live check</th><th></th></tr></thead><tbody>
        ${seo.keywords.length ? seo.keywords.map((k) => `<tr>
          <td>${esc(k.keyword)}</td><td>${esc(k.page)}</td><td>${esc(k.rank)}</td><td><span class="pill">${esc(k.priority)}</span></td>
          <td><button type="button" data-seo-check-keyword="${k.id}" ${!seo.domains.length || !state.aiConfigured || checkingId === k.id ? 'disabled' : ''}>${checkingId === k.id ? 'Checking\u2026' : 'Check now'}</button>${k.lastChecked ? `<p class="note">${esc(k.lastChecked)}</p>` : ''}</td>
          <td><button type="button" data-seo-delete-keyword="${k.id}">Remove</button></td>
        </tr>`).join('') : '<tr><td colspan="6">No keywords tracked yet.</td></tr>'}
      </tbody></table></div>
    </section>`;
}

function aioView(c) {
  const { state, esc } = c;
  const aio = state.aio, seo = state.seo;
  return `
    ${checklistBlock('AI-optimisation checklist', state.aioChecklist, aio.checklist, 'aio')}
    <section class="card">
      <h2>Target questions</h2>
      <p class="note">Questions you want an AI assistant such as ChatGPT or an AI Overview to answer using your content.</p>
      ${!state.aiConfigured ? '<div class="banner">AI setup needed: add OPENAI_API_KEY and OPENAI_MODEL in Render to enable live citation checks.</div>' : ''}
      <form id="aio-query-form">
        <label for="aio-query-input">Question</label>
        <input id="aio-query-input" maxlength="300" required placeholder="how to spot early signs of bloat in cattle">
        <button class="primary">Add</button>
      </form>
      <div class="list">${aio.queries.length ? aio.queries.map((q) => `<div class="draft-row"><span><strong>${esc(q.query)}</strong>${q.lastChecked ? `<small>${esc(q.lastChecked)}</small>` : ''}</span><span class="pill">${esc(q.status)}</span><button type="button" data-aio-check-query="${q.id}" ${(!seo.domains.length && !seo.brand) || !state.aiConfigured || checkingId === q.id ? 'disabled' : ''}>${checkingId === q.id ? 'Checking\u2026' : 'Check now'}</button><button type="button" data-aio-delete-query="${q.id}">Remove</button></div>`).join('') : '<div class="card empty">No target questions yet.</div>'}</div>
    </section>`;
}

export function discoverPage(c) {
  return heading('Search & AI discoverability', 'Track how findable the clinic is on Google and in AI assistants like ChatGPT.') +
    `<div class="actions">
      <button type="button" class="${discoverTab === 'seo' ? 'primary' : ''}" data-discover-tab="seo">SEO</button>
      <button type="button" class="${discoverTab === 'aio' ? 'primary' : ''}" data-discover-tab="aio">AIO organiser</button>
    </div>` +
    (discoverTab === 'seo' ? seoView(c) : aioView(c));
}

export function discoverabilityChange(e, c) {
  if (e.target.id === 'seo-domains') { seoDomainsText = e.target.value; return true; }
  if (e.target.id === 'seo-brand') { seoBrandText = e.target.value; return true; }
  return false;
}

export async function discoverabilityClick(el, c) {
  const { api, refresh, render, notice } = c;
  if (el.dataset.discoverTab) { discoverTab = el.dataset.discoverTab; render(); return true; }
  if (el.dataset.seoCheck) {
    await api('seo/checklist', 'PATCH', { key: el.dataset.seoCheck });
    await refresh(); render(); return true;
  }
  if (el.dataset.aioCheck) {
    await api('aio/checklist', 'PATCH', { key: el.dataset.aioCheck });
    await refresh(); render(); return true;
  }
  if (el.dataset.seoDeleteKeyword) {
    await api('seo/keywords/' + el.dataset.seoDeleteKeyword, 'DELETE');
    await refresh(); render(); return true;
  }
  if (el.dataset.seoCheckKeyword) {
    checkingId = el.dataset.seoCheckKeyword; render();
    try { await api('seo/keywords/' + el.dataset.seoCheckKeyword + '/check', 'POST', {}); await refresh(); }
    catch (err) { notice(err.message); }
    finally { checkingId = null; render(); }
    return true;
  }
  if (el.dataset.aioDeleteQuery) {
    await api('aio/queries/' + el.dataset.aioDeleteQuery, 'DELETE');
    await refresh(); render(); return true;
  }
  if (el.dataset.aioCheckQuery) {
    checkingId = el.dataset.aioCheckQuery; render();
    try { await api('aio/queries/' + el.dataset.aioCheckQuery + '/check', 'POST', {}); await refresh(); }
    catch (err) { notice(err.message); }
    finally { checkingId = null; render(); }
    return true;
  }
  return false;
}

export async function discoverabilitySubmit(form, c) {
  const { api, refresh, render, notice } = c;
  if (form.id === 'seo-settings-form') {
    const domains = document.getElementById('seo-domains').value.split(',').map((d) => d.trim()).filter(Boolean);
    const brand = document.getElementById('seo-brand').value;
    await api('seo/settings', 'PUT', { domains, brand });
    seoDomainsText = null; seoBrandText = null;
    await refresh(); render(); notice('Live check settings saved.');
    return true;
  }
  if (form.id === 'seo-keyword-form') {
    const keyword = document.getElementById('kw-keyword').value;
    const page = document.getElementById('kw-page').value;
    const priority = document.getElementById('kw-priority').value;
    if (!keyword.trim()) throw Error('Enter a keyword.');
    await api('seo/keywords', 'POST', { keyword, page, priority });
    document.getElementById('kw-keyword').value = '';
    document.getElementById('kw-page').value = '';
    await refresh(); render();
    return true;
  }
  if (form.id === 'aio-query-form') {
    const query = document.getElementById('aio-query-input').value;
    if (!query.trim()) throw Error('Enter a question.');
    await api('aio/queries', 'POST', { query });
    await refresh(); render();
    return true;
  }
  return false;
}
