import { renderStudio, extraNames, extraIcons, studioInput, studioChange, studioClick, studioSubmit, profileSection, editorExtras, campaignAnalytics, libraryPage } from "/studio-ui.js";
const app = document.querySelector("#app");
let state,
  page = "dashboard",
  selected = "content",
  draftId = null,
  menu = false,
  busy = false,
  installPrompt = null,
  filter = "all";
let brief = "",
  language = "Hindi",
  loginError = "";
const esc = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const names = {
  dashboard: "Start here",
  manager: "Plan my week",
  approvals: "Review & approve",
  creator: "Write a post",
  video: "Make a video",
  images: "Photos & posters",
  whatsapp: "WhatsApp text",
  agents: "Specialist agents",
  chat: "AI Assistant",
  library: "Saved work",
  calendar: "Posting calendar",
  analytics: "Results",
  settings: "Settings",
};
const icons = {
  ...extraIcons(),
  dashboard: "▦",
  agents: "✧",
  library: "▤",
  calendar: "▣",
  analytics: "▥",
  settings: "⚙",
};
const statusLabel = {
  draft: "Draft",
  pending: "Needs approval",
  approved: "Approved",
  planned: "Planned",
  published: "Published",
  starting: "Starting",
  processing: "Generating",
  running: "Working",
  completed: "Completed",
  failed: "Failed",
};
const pill = (s) =>
  `<span class="pill ${esc(s)}">${esc(statusLabel[s] || s)}</span>`;
const date = (value) =>
  new Date(value).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kolkata",
  }) + " IST";
function notice(message) {
  document.querySelector("#notice").textContent = message;
  setTimeout(() => (document.querySelector("#notice").textContent = ""), 6500);
}
async function api(path, method = "GET", data) {
  let response;
  try {
    response = await fetch("/api/" + path, {
      method,
      headers: data ? { "Content-Type": "application/json" } : {},
      body: data ? JSON.stringify(data) : undefined,
    });
  } catch {
    throw Error("Connection unavailable. Reconnect and try again.");
  }
  const result = await response.json();
  if (!response.ok) {
    if (response.status === 401 && path !== "login") {
      state = null;
      render();
    }
    throw Error(result.error || "Request failed.");
  }
  return result;
}
async function refresh() {
  state = await api("state");
}
function context() { return { state, page, filter, draftId, esc, date, pill, list, statusLabel, api, refresh, render, notice, go: (next,id) => {page=next; if(id)draftId=id;menu=false;render();} }; }
function agentName(id) {
  return state.agents.find((a) => a.id === id)?.name || "Manual draft";
}
function unsavedDraft() {
  if (page !== "editor") return false;
  const draft = state?.drafts.find((d) => d.id === draftId);
  return (
    draft &&
    (document.querySelector("#title")?.value !== draft.title ||
      document.querySelector("#content")?.value !== draft.content)
  );
}
function cards() {
  return `<div class="grid">${state.agents.map((a) => `<article class="card agent-card"><div class="agent-icon">${a.icon}</div><h3>${esc(a.name)}</h3><p>${esc(a.description)}</p><button data-agent="${a.id}">Open agent ↗</button></article>`).join("")}</div>`;
}
function list(items) {
  return items.length
    ? `<div class="list">${items.map((d) => `<button class="draft-row" data-draft="${d.id}"><span><strong>${esc(d.title)}</strong><small>${esc(agentName(d.agent))} · ${esc(d.language)}${d.planned_at ? " · " + date(d.planned_at) : ""}</small></span>${pill(d.status)}</button>`).join("")}</div>`
    : '<div class="card empty"><strong>Your next campaign starts here</strong>Create a draft yourself or give an agent a brief.</div>';
}
function dashboard() {
 const tasks=[['creator','Write a post','पोस्ट बनाएं','Facebook or Instagram text'],['video','Make a video','वीडियो बनाएं','Guided scenes, clinic photos and narration'],['images','Photos & posters','फोटो और पोस्टर','Upload clinic photos or make a poster'],['whatsapp','WhatsApp text','संदेश बनाएं','Prepare a message to copy and share'],['manager','Plan my week','सप्ताह की योजना','Seven daily drafts for your review'],['chat','Ask for help','AI से पूछें','Ideas, captions and marketing questions']];
 const pending=state.drafts.filter(d=>d.status==='pending').length;
 return `<div class="heading"><div><h1>What would you like to do?</h1><p>Choose one task. Your AI helper is already selected.</p></div></div><div class="grid">${tasks.map(([key,title,hindi,help])=>`<button class="card task-card" data-page="${key}"><h2>${title}</h2><strong>${hindi}</strong><p>${help}</p><span>Start →</span></button>`).join('')}</div><section class="card next-step"><h2>Your next step</h2><p>${pending?`${pending} drafts are waiting for your review.`:'Create something above, then review it in Saved work.'}</p><button class="primary" data-page="${pending?'approvals':'library'}">${pending?'Review drafts':'Open saved work'}</button><p class="note">Posts and WhatsApp messages are shared manually after approval.</p></section><div class="section-title"><h2>Recent work</h2><button data-page="library">View all</button></div>${list(state.drafts.slice(0,4))}`;
}
function navigation(){
 const link=key=>`<button data-page="${key}" class="${key===page?'active':''}" ${key===page?'aria-current="page"':''}><span>${icons[key]||'•'}</span>${names[key]}</button>`;
 const primary=['dashboard','creator','video','images','whatsapp','approvals','library'];
 const more=['manager','chat','calendar','analytics','agents','settings'];
 return primary.map(link).join('')+`<details ${more.includes(page)?'open':''}><summary>More tools</summary>${more.map(link).join('')}</details>`;
}
const agentExamples={strategy:'Plan a week of awareness posts for pet owners in Lohardaga. Ask readers to call for appointment availability.',content:'Write a short Hindi Facebook post introducing our clinic and inviting owners to call.',video:'Write a 30-second Hindi script introducing our clinic, with a phone number at the end.',whatsapp:'Write a short Hindi consultation invitation for opted-in customers, including an opt-out line.',analytics:'Explain the saved campaign results. Identify missing information and suggest three improvements.',review:'Paste the draft to check here. Check the clinic name, phone, clarity and unsupported claims.'};
function agentPage() {
  const a = state.agents.find((a) => a.id === selected);
  return `<div class="heading"><div><div class="eyebrow">Create with purpose</div><h1>Specialist agents</h1><p>For everyday work, use Start here. Here you can give a specialist a custom request.</p></div></div><div class="split"><form id="agent-form" class="card"><label for="agent">Your agent</label><select id="agent" ${busy ? "disabled" : ""}>${state.agents.map((a) => `<option value="${a.id}" ${a.id === selected ? "selected" : ""}>${esc(a.name)}</option>`).join("")}</select><p>${esc(a.description)}</p><label for="language">Output language</label><select id="language">${["Hindi", "English", "Hinglish"].map((l) => `<option ${l === language ? "selected" : ""}>${l}</option>`).join("")}</select><button type="button" data-agent-example>Use an example</button><label for="brief">What do you want this helper to do?</label><textarea id="brief" maxlength="6000" required placeholder="Example: Create a vaccination awareness post for dog owners in Lohardaga. Invite them to book a consultation.">${esc(brief)}</textarea><p class="note">Include the audience, goal and confirmed offer details. Use aggregate information without customer names or phone numbers.</p><button class="primary" ${busy || !state.aiConfigured ? "disabled" : ""}>${busy ? "Agent is working…" : "Generate draft ↗"}</button></form><section class="card"><div class="section-title"><h2>Your previous results</h2><span class="pill">OpenAI</span></div>${busy ? '<div class="empty"><div class="spinner"></div><p>Preparing your draft. This can take a minute.</p></div>' : ""}${state.runs.length ? state.runs.map((r) => `<div class="run"><div><strong>${esc(agentName(r.agent))}</strong><small>${date(r.created_at)}</small>${r.error ? `<p>${esc(r.error)}</p>` : ""}${r.status === "completed" ? `<button data-draft="${r.id}">Open draft</button>` : ""}</div><div>${pill(r.status)}</div></div>`).join("") : '<div class="empty"><strong>A brief becomes a draft</strong>Completed work will be saved in your content library.</div>'}</section></div>`;
}
function editor() {
  const d = state.drafts.find((d) => d.id === draftId);
  if (!d) return "<p>Draft not found. Reload the library.</p>";
  return `<div class="heading"><div><button data-page="library">← Saved work</button><h1>Review your draft</h1></div>${pill(d.status)}</div><div class="split"><form id="edit-form" class="card"><label for="title">Title</label><input id="title" maxlength="160" value="${esc(d.title)}" required><label for="content">Content</label><textarea class="editor" id="content" maxlength="20000" required>${esc(d.content)}</textarea><p class="note">Saving an edit returns the item to Draft and clears any publishing plan. Submit it again for approval.</p><button class="primary">Save changes</button></form><section class="card"><div class="eyebrow">Review & handoff</div><h2>${esc(agentName(d.agent))}</h2><p>${esc(d.language)} · ${date(d.created_at)}</p><div class="actions">${d.status === "draft" ? '<button class="primary" data-action="submit">Submit for approval</button>' : ""}${d.status === "pending" && state.role === "owner" ? '<button class="primary" data-action="approve">Approve draft</button><button data-action="reject">Return to draft</button>' : ""}${["approved", "planned", "published"].includes(d.status) ? "<button data-copy>Copy approved content</button>" : ""}<button data-review>Send to quality reviewer</button></div>${["approved", "planned"].includes(d.status) && state.role !== "creator" ? `<form id="plan-form"><label for="planned">Plan publication (India Standard Time)</label><input id="planned" type="datetime-local" required><button class="primary">Save publishing plan</button></form>${d.planned_at ? `<p>Planned: ${date(d.planned_at)}</p><button data-action="unplan">Remove from plan</button>` : ""}` : ""}<p class="note">Posting calendars do not send posts automatically. Copy approved content to your chosen platform. Your edits must be saved before using review actions.</p><details><summary>Original brief</summary><p class="output">${esc(d.brief || "Written manually.")}</p></details></section></div>`;
}
function newDraft() {
  return '<div class="heading"><h1>Create a draft</h1></div><form id="new-form" class="card"><label for="title">Title</label><input id="title" maxlength="160" required><label for="content">Content</label><textarea id="content" class="editor" maxlength="20000" required></textarea><button class="primary">Save draft</button></form>';
}
function analytics() {
  const m = state.metrics;
  const val = (k) => (m ? m[k] : "");
  return `<div class="heading"><div><div class="eyebrow">Recorded results</div><h1>Know what works.</h1><p>Enter aggregate campaign totals for a consistent reporting period.</p></div></div><div class="stats">${[
    ["Spend", m ? "₹" + m.spend.toLocaleString("en-IN") : "—"],
    ["Leads", m ? m.leads : "—"],
    [
      "Cost per lead",
      m && m.leads > 0 ? "₹" + (m.spend / m.leads).toFixed(2) : "—",
    ],
    [
      "Revenue / ad spend",
      m && m.spend > 0 ? (m.revenue / m.spend).toFixed(2) + "×" : "—",
    ],
  ]
    .map(
      ([l, v]) =>
        `<div class="stat"><span>${l}</span><strong>${v}</strong></div>`,
    )
    .join(
      "",
    )}</div><div class="split"><form id="metrics-form" class="card"><h2>Update totals</h2><p class="note">These totals are manually entered, not imported from Meta. Saving replaces the previous totals.</p><div class="metrics-grid">${[
    ["spend", "Ad spend (₹)"],
    ["clicks", "Clicks"],
    ["leads", "Leads"],
    ["revenue", "Attributed revenue (₹)"],
  ]
    .map(
      ([k, l]) =>
        `<div><label for="${k}">${l}</label><input id="${k}" type="number" min="0" max="10000000000" step="${["clicks", "leads"].includes(k) ? "1" : "0.01"}" value="${val(k)}" required></div>`,
    )
    .join(
      "",
    )}</div><div class="actions"><button class="primary">Save totals</button></div></form><div class="card"><h2>Turn results into a next step</h2><p>The performance analyst uses these saved totals and the reporting context you put in the brief. Missing metrics are never filled with sample results.</p><button data-agent="analytics">Open performance analyst ↗</button><p class="note">Revenue / ad spend is ROAS, not profit or ROI. A dash means the inputs needed for that calculation are unavailable.</p></div></div>`;
}
function settings() {
  return `<div class="heading"><h1>Your workspace</h1></div><div class="split"><div class="card"><h2>Desktop & Android</h2><p>Open this same app address on each device to access the shared workspace.</p><button data-install>Install app</button><p class="install-guide">On desktop, use Chrome or Edge’s install app option.<br>On Android, open in Chrome and choose Install app or Add to Home screen.</p><p class="note">Installation requires HTTPS or localhost. The interface opens offline; signing in, drafts and agents require a connection.</p></div><div class="card"><h2>Connections</h2><p>OpenAI agents: <strong>${state.aiConfigured ? "Configured" : "Setup required"}</strong></p><p>Model: ${esc(state.model || "Not configured")}</p><p>Facebook / Instagram publishing: Not connected<br>WhatsApp sending: Not connected</p><p class="note">Server configuration controls API credentials. Never put keys into a campaign brief. Team access is configured below.</p><button data-logout>Sign out</button></div></div>`;
}
function render() {
  if (!state) {
    app.innerHTML = `<form id="login-form" class="card login"><img src="/icon.svg" alt="RVH"><div class="eyebrow">Rohit Veterinary House</div><h1>Marketing Studio</h1><p>Your clinic. Your voice.<br>One workspace for every campaign.</p><label for="login-role">Workspace role</label><select id="login-role"><option value="owner">Owner</option><option value="staff">Marketing staff</option><option value="creator">Content creator</option></select><label for="password">Workspace password</label><input type="password" id="password" autocomplete="current-password" required><p role="alert">${esc(loginError)}</p><button class="primary">Sign in →</button><p class="note">Use the password configured for your role.</p></form>`;
    return;
  }
  const content = renderStudio(page, context()) ?? (
    page === "dashboard"
      ? dashboard()
      : page === "agents"
        ? agentPage()
        : page === "editor"
          ? editor() + `<div class="split studio-extra">${editorExtras(context(),state.drafts.find(d=>d.id===draftId))}</div>`
          : page === "new"
            ? newDraft()
            : page === "analytics"
              ? analytics() + campaignAnalytics(context())
              : page === "settings"
                ? settings() + `<div class="split studio-extra">${profileSection(context())}</div>`
                : libraryPage(context()));
  app.innerHTML = `<aside class="${menu ? "open" : ""}"><div class="brand"><img src="/icon.svg" alt=""><div><strong>RVH Studio</strong><small>MARKETING WORKSPACE</small></div></div><nav class="nav" aria-label="Main navigation">${navigation()}</nav><div class="side-bottom">Rohit Veterinary House<br>Lohardaga, Jharkhand<br><button data-logout>Sign out</button></div></aside><div class="shell"><header><button class="mobile-menu" data-menu aria-label="Toggle navigation" aria-expanded="${menu}">☰</button><span class="header-name">${esc(names[page]||"Review your work")}</span><span class="pill">Dr. Rohit · ${esc(state.role)}</span></header><main>${!navigator.onLine ? '<div class="banner offline">You are offline. Reconnect to load or save your work.</div>' : ""}${!state.aiConfigured ? '<div class="banner">AI agents need setup: add your OpenAI key and model to the server configuration. You can create and review manual drafts now.</div>' : ""}${content}</main></div>`;
}
app.addEventListener("input", (e) => {
  if(state) studioInput(e,context());
  if (e.target.id === "brief") brief = e.target.value;
  if (e.target.id === "language") language = e.target.value;
});
app.addEventListener("change", (e) => {
  if(state) studioChange(e,context());
  if (e.target.id === "agent") {
    selected = e.target.value;
    render();
  }
  if (e.target.id === "filter") {
    filter = e.target.value;
    render();
  }
});
app.addEventListener("click", async (e) => {
  const el = e.target.closest("button");
  if (!el) return;
  if (
    unsavedDraft() &&
    (el.dataset.remix || el.dataset.mediaBrief || el.dataset.shareApproved || el.dataset.downloadDraft || el.dataset.action ||
      el.hasAttribute("data-copy") ||
      el.hasAttribute("data-review"))
  ) {
    notice("Save your edits before reviewing or copying this draft.");
    return;
  }
  if (
    unsavedDraft() &&
    (el.dataset.page ||
      el.dataset.agent ||
      el.hasAttribute("data-new") ||
      el.hasAttribute("data-logout")) &&
    !confirm("Leave this draft and discard unsaved edits?")
  )
    return;
  try {
    if(state && await studioClick(el,context())) return;
    if(el.hasAttribute("data-agent-example")){brief=agentExamples[selected]||"";render();}
    if (el.dataset.page) {
      page = el.dataset.page;
      menu = false;
      render();
    }
    if (el.dataset.agent) {
      selected = el.dataset.agent;
      page = "agents";
      render();
    }
    if (el.hasAttribute("data-new")) {
      page = "new";
      render();
    }
    if (el.dataset.draft) {
      draftId = el.dataset.draft;
      page = "editor";
      render();
    }
    if (el.hasAttribute("data-menu")) {
      menu = !menu;
      render();
    }
    if (el.hasAttribute("data-logout")) {
      await api("logout", "POST", {});
      state = null;
      brief = "";
      draftId = null;
      render();
    }
    if (el.dataset.action) {
      el.disabled = true;
      const d = state.drafts.find((d) => d.id === draftId);
      await api("drafts/" + d.id, "PATCH", {
        action: el.dataset.action,
        version: d.version,
      });
      await refresh();
      render();
    }
    if (el.hasAttribute("data-copy")) {
      await navigator.clipboard.writeText(
        state.drafts.find((d) => d.id === draftId).content,
      );
      notice("Approved content copied.");
    }
    if (el.hasAttribute("data-review")) {
      brief = state.drafts.find((d) => d.id === draftId).content.slice(0, 6000);
      selected = "review";
      page = "agents";
      render();
    }
    if (el.hasAttribute("data-install")) {
      if (installPrompt) {
        await installPrompt.prompt();
        installPrompt = null;
      } else
        notice("Use your browser menu → Install app / Add to Home screen.");
    }
  } catch (err) {
    el.disabled = false;
    notice(err.message);
  }
});
app.addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;
  const submit = e.submitter || form.querySelector("button");
  if (submit) submit.disabled = true;
  try {
    if(state && ["publish-form","results-form"].includes(form.id) && unsavedDraft()) throw Error("Save your draft edits first.");
    if(state && await studioSubmit(form,e.submitter,context())) return;
    if (form.id === "login-form") {
      await api("login", "POST", {
        password: document.querySelector("#password").value,
        role: document.querySelector("#login-role").value,
      });
      loginError = "";
      await refresh();
      render();
      return;
    }
    if (form.id === "agent-form") {
      brief = document.querySelector("#brief").value;
      language = document.querySelector("#language").value;
      busy = true;
      render();
      try {
        const draft = await api("runs", "POST", {
          agent: selected,
          language,
          brief,
        });
        await refresh();
        draftId = draft.id;
        page = "editor";
        notice("Draft saved. Review it before approval.");
      } finally {
        busy = false;
        render();
      }
      return;
    }
    if (form.id === "new-form") {
      const d = await api("drafts", "POST", {
        title: document.querySelector("#title").value,
        content: document.querySelector("#content").value,
      });
      draftId = d.id;
      page = "editor";
    }
    if (form.id === "edit-form") {
      const d = state.drafts.find((d) => d.id === draftId);
      await api("drafts/" + d.id, "PATCH", {
        action: "edit",
        version: d.version,
        title: document.querySelector("#title").value,
        content: document.querySelector("#content").value,
      });
      notice("Saved as draft.");
    }
    if (form.id === "plan-form") {
      if (unsavedDraft())
        throw Error("Save your edits before planning publication.");
      const d = state.drafts.find((d) => d.id === draftId);
      await api("drafts/" + d.id, "PATCH", {
        action: "plan",
        version: d.version,
        planned_at: new Date(
          document.querySelector("#planned").value + ":00+05:30",
        ).toISOString(),
      });
      notice("Posting calendar saved.");
    }
    if (form.id === "metrics-form") {
      await api(
        "metrics",
        "PUT",
        Object.fromEntries(
          ["spend", "clicks", "leads", "revenue"].map((k) => [
            k,
            Number(document.getElementById(k).value),
          ]),
        ),
      );
      notice("Metrics saved.");
    }
    await refresh();
    render();
  } catch (err) {
    if (form.id === "login-form") {
      loginError = err.message;
      render();
    } else notice(err.message);
  } finally {
    if (submit) submit.disabled = false;
  }
});
window.addEventListener("beforeinstallprompt", (e) => {
  e.preventDefault();
  installPrompt = e;
});
// Avoid rerendering editors on network changes: unsaved input must stay intact.
window.addEventListener("offline", () =>
  notice("Offline. Keep this page open to preserve unsaved work."),
);
window.addEventListener("online", () =>
  notice("Connection restored. You can save your work."),
);
if ("serviceWorker" in navigator)
  navigator.serviceWorker.register("/sw.js").catch(() => {});
try {
  await refresh();
} catch (err) {
  if (!/sign in/i.test(err.message)) loginError = err.message;
}
render();
