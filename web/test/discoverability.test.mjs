import { test } from "node:test";
import assert from "node:assert/strict";
import { createApp } from "../server.mjs";
import { parseProductSitemap, classifyBulkScan, productSearchPriority, compareProductSitemapSnapshots, hasBulkSxoAudit, buildSupervisorSignals } from "../discoverability.mjs";
import { analysePage } from "../website-scan.mjs";
import { discoverPage } from "../public/discoverability-ui.js";

const password = "owner-test-password-long";

async function fixture(t, options = {}) {
  const env = {
    ADMIN_PASSWORD: password,
    STAFF_PASSWORD: "staff-test-password-long",
    APP_ORIGIN: "http://localhost:3000",
    DATA_FILE: ":memory:",
    ...options.env,
  };
  const server = createApp({ env, checkImpl: options.checkImpl, scanImpl:options.scanImpl, sitemapReadImpl:options.sitemapReadImpl||async()=>({status:200,text:'<urlset></urlset>'}), metaFetchImpl:options.metaFetchImpl, generateImpl:options.generateImpl });
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  t.after(() => new Promise((r) => server.close(r)));
  let cookie = "";
  const base = `http://127.0.0.1:${server.address().port}`;
  async function req(path, method = "GET", data) {
    const r = await fetch(base + "/api/" + path, {
      method,
      headers: {
        origin: env.APP_ORIGIN,
        cookie,
        ...(data ? { "content-type": "application/json" } : {}),
      },
      body: data ? JSON.stringify(data) : undefined,
    });
    if (r.headers.get("set-cookie")) cookie = r.headers.get("set-cookie").split(";")[0];
    return { status: r.status, data: await r.json() };
  }
  const login = (role = "owner") =>
    req("login", "POST", { role, password: role === "owner" ? password : env[role.toUpperCase() + "_PASSWORD"] });
  return { req, login, base };
}

test("bulk product sitemap parser keeps only canonical Vet Mart product URLs", () => {
  const xml=`<urlset>
    <url><loc>https://mart.rohitveterinary.com/product/anronil-bolus</loc></url>
    <url><loc>https://mart.rohitveterinary.com/product/mastina-ptm-bolus</loc></url>
    <url><loc>https://mart.rohitveterinary.com/product/anronil-bolus</loc></url>
    <url><loc>https://app.rohitveterinary.com/product/not-mart</loc></url>
    <url><loc>https://mart.rohitveterinary.com/catalog</loc></url>
  </urlset>`;
  assert.deepEqual(parseProductSitemap(xml),[
    "https://mart.rohitveterinary.com/product/anronil-bolus",
    "https://mart.rohitveterinary.com/product/mastina-ptm-bolus",
  ]);
});

test("catalogue drift compares the latest sitemap with the previous optimizer snapshot", () => {
  const previous=[
    {url:"https://mart.rohitveterinary.com/product/old-name"},
    {url:"https://mart.rohitveterinary.com/product/stable-product"},
  ];
  const next=[
    "https://mart.rohitveterinary.com/product/stable-product",
    "https://mart.rohitveterinary.com/product/new-name",
  ];
  const drift=compareProductSitemapSnapshots(previous,next,"2026-10-03T10:00:00.000Z","2026-10-02T10:00:00.000Z");
  assert.deepEqual(drift.added,["https://mart.rohitveterinary.com/product/new-name"]);
  assert.deepEqual(drift.removed,["https://mart.rohitveterinary.com/product/old-name"]);
  assert.equal(drift.detectedAt,"2026-10-03T10:00:00.000Z");
  const stable=compareProductSitemapSnapshots(next,next,"2026-10-04T10:00:00.000Z","2026-10-03T10:00:00.000Z");
  assert.equal(stable.detectedAt,null);
  assert.deepEqual(stable.added,[]);
  assert.deepEqual(stable.removed,[]);
});

test("bulk readiness separates SEO, AEO and GEO and preserves failures", () => {
  const scan={checks:[
    {label:"Page response",passed:true},{label:"Page title",passed:true},{label:"Search description",passed:true},
    {label:"Canonical link",passed:true},{label:"Indexing directive",passed:true},{label:"Image alt attributes",passed:true},
    {label:"Main heading",passed:true},{label:"Readable page content",passed:false,observed:"120 characters",action:"Add useful crawlable text"},
    {label:"Structured data",passed:true},{label:"Phone in page text",passed:true},
  ]};
  const result=classifyBulkScan(scan);
  assert.equal(result.readiness.seo.ready,true);
  assert.equal(result.readiness.aeo.ready,false);
  assert.equal(result.readiness.geo.ready,false);
  assert.equal(result.readiness.sxo.ready,false);
  assert.deepEqual(result.failures.map(x=>x.label),["Readable page content"]);
});

test("legacy bulk products without SXO are not treated as fully audited", () => {
  assert.equal(hasBulkSxoAudit({readiness:{seo:{total:6},aeo:{total:4},geo:{total:4}}}),false);
  assert.equal(hasBulkSxoAudit({readiness:{sxo:{passed:10,total:10,ready:true}}}),true);
  assert.equal(hasBulkSxoAudit({readiness:{sxo:{passed:0,total:0,ready:false}}}),false);
});

test("automation supervisor identifies stale search data and unresolved bulk work", () => {
  const now=Date.parse("2026-10-04T12:00:00.000Z");
  const result=buildSupervisorSignals({
    nowMs:now,
    bulk:{
      drift:{detectedAt:"2026-10-04T08:00:00.000Z"},
      summary:{driftAdded:2,driftRemoved:1,driftUnresolved:1,pending:3,failed:1,needsSxo:4,needsReview:2}
    },
    optimizerState:{
      searchConsole:{connected:true},
      reports:[
        {id:"prefix-fresh",property:"https://www.rohitveterinary.com/",period:"28d",receivedAt:"2026-10-04T10:00:00.000Z"},
        {id:"r1",property:"sc-domain:rohitveterinary.com",period:"28d",receivedAt:"2026-09-30T12:00:00.000Z"},
      ],
      tasks:[{status:"review"},{status:"completed"}],
    }
  });
  assert.equal(result.openTasks,1);
  assert.equal(result.latestReport.id,"r1");
  assert.ok(result.reportAgeHours>72);
  const keys=result.signals.map(x=>x.key);
  assert.ok(keys.includes("gsc-stale|r1"));
  assert.ok(keys.includes("catalogue-drift|2026-10-04T08:00:00.000Z"));
  assert.ok(keys.includes("bulk-pending"));
  assert.ok(keys.includes("bulk-failed"));
  assert.ok(keys.includes("bulk-sxo"));
  assert.ok(keys.includes("bulk-review"));
});

test("Search Console product priority distinguishes exact and query-name matches", () => {
  const report={queryPages:[
    {query:"mastina ptm bolus price",page:"https://mart.rohitveterinary.com/catalog?problem=mastitis",impressions:96,clicks:1,ctr:.0104,position:7.1},
    {query:"anronil bolus",page:"https://mart.rohitveterinary.com/product/anronil-bolus",impressions:12,clicks:1,ctr:.08,position:6},
  ]};
  const mastina=productSearchPriority("https://mart.rohitveterinary.com/product/mastina-ptm-bolus",report);
  assert.equal(mastina.priority,"High");
  assert.equal(mastina.gsc.matchType,"query-slug");
  assert.equal(mastina.gsc.impressions,96);
  const anronil=productSearchPriority("https://mart.rohitveterinary.com/product/anronil-bolus",report);
  assert.equal(anronil.gsc.matchType,"exact-page");
  assert.equal(anronil.priority,"High");
  const unknown=productSearchPriority("https://mart.rohitveterinary.com/product/unknown-product",report);
  assert.equal(unknown.priority,"Standard");
  assert.equal(unknown.gsc,null);
});

test("SXO scan signals detect mobile, action, navigation, price and availability on a product page", () => {
  assert.equal(typeof discoverPage,"function");
  const html=`<!doctype html><html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Veterinary product">
    <title>Test Product — RVH Vet Mart</title>
    <link rel="canonical" href="https://mart.rohitveterinary.com/product/test-product">
    <script type="application/ld+json">{"@type":"Product"}</script>
  </head><body>
    <h1>Test Product</h1>
    <p>Useful veterinary product information with enough readable content for a customer to understand the product before ordering. Contact Rohit Veterinary House at 9709095993 for support. Current price ₹100 and 5 in stock.</p>
    <img src="/test.jpg" alt="Test Product">
    <a href="/catalog">Related products</a>
    <button>Add to cart</button>
  </body></html>`;
  const scan=analysePage({url:"https://mart.rohitveterinary.com/product/test-product",status:200,headers:{},text:html},{phone:"9709095993"});
  const byLabel=new Map(scan.checks.map(x=>[x.label,x]));
  for(const label of ["Mobile viewport","Primary action","Internal navigation","Product price clarity","Product availability clarity"])assert.equal(byLabel.get(label)?.passed,true,label);
  const bulk=classifyBulkScan(scan);
  assert.equal(bulk.readiness.sxo.ready,true);
});

test("automation supervisor is enabled by default and can run or pause without publishing", async (t) => {
  const f=await fixture(t);
  assert.equal((await f.login()).status,200);
  let s=(await f.req("state")).data;
  assert.equal(s.supervisor.enabled,true);
  assert.equal(s.supervisor.intervalHours,6);
  let r=await f.req("discover/supervisor/run","POST",{});
  assert.equal(r.status,200);
  assert.ok(r.data.lastRunAt);
  assert.ok(r.data.nextRunAt);
  r=await f.req("discover/supervisor","PATCH",{enabled:false});
  assert.equal(r.status,200);
  assert.equal(r.data.enabled,false);
  s=(await f.req("state")).data;
  assert.equal(s.supervisor.enabled,false);
});

test("automation supervisor refreshes sitemap, audits new products and creates only genuine review tasks", async (t) => {
  const goodChecks=[
    {label:"Page response",passed:true},{label:"Page title",passed:true},{label:"Search description",passed:true},
    {label:"Canonical link",passed:true},{label:"Indexing directive",passed:true},{label:"Image alt attributes",passed:true},
    {label:"Main heading",passed:true},{label:"Readable page content",passed:true},{label:"Structured data",passed:true},
    {label:"Phone in page text",passed:true},{label:"Mobile viewport",passed:true},{label:"Primary action",passed:true},
    {label:"Internal navigation",passed:true},{label:"Product price clarity",passed:true},{label:"Product availability clarity",passed:true},
  ];
  const urls=[
    "https://mart.rohitveterinary.com/product/new-good",
    "https://mart.rohitveterinary.com/product/new-review",
  ];
  const f=await fixture(t,{
    sitemapReadImpl:async()=>({status:200,text:`<urlset>${urls.map(url=>`<url><loc>${url}</loc></url>`).join("")}</urlset>`}),
    scanImpl:async({url})=>({url,checks:url.endsWith("new-review")?goodChecks.map(x=>x.label==="Main heading"?{...x,passed:false,observed:"Missing",action:"Use one clear main heading."}:x):goodChecks}),
  });
  assert.equal((await f.login()).status,200);
  const r=await f.req("discover/supervisor/run","POST",{});
  assert.equal(r.status,200);
  assert.equal(r.data.lastSummary.automatic.sitemapRefreshed,true);
  assert.equal(r.data.lastSummary.automatic.productsAudited,2);
  assert.equal(r.data.lastSummary.automatic.productTasksCreated,1);
  const state=(await f.req("state")).data;
  assert.equal(state.bulkProductAudit.summary.total,2);
  assert.equal(state.bulkProductAudit.summary.ready,1);
  assert.equal(state.bulkProductAudit.summary.needsReview,1);
  const productTasks=state.optimizer.tasks.filter(x=>x.kind==="Bulk SEO/AEO/GEO/SXO");
  assert.equal(productTasks.length,1);
  assert.match(productTasks[0].title,/new review/i);
  const again=await f.req("discover/supervisor/run","POST",{});
  assert.equal(again.data.lastSummary.automatic.productsAudited,0);
  assert.equal((await f.req("state")).data.optimizer.tasks.filter(x=>x.kind==="Bulk SEO/AEO/GEO/SXO").length,1);
});

test("SEO/AIO state defaults and checklist item validation", async (t) => {
  const f = await fixture(t);
  await f.login();
  const s = (await f.req("state")).data;
  assert.deepEqual(s.seo, { domains: [], brand: "", checklist: {}, keywords: [], scans: [] });
  assert.deepEqual(s.aio, { checklist: {}, queries: [] });
  assert.deepEqual(s.geo, { checklist: {} });
  assert.deepEqual(s.sxo, { checklist: {} });
  assert.equal(s.bulkProductAudit.summary.total, 0);
  assert.equal(s.bulkProductAudit.sitemapUrl, "https://app.rohitveterinary.com/api/store/sitemap.xml");
  assert.equal(s.bulkProductAudit.summary.driftAdded, 0);
  assert.equal(s.bulkProductAudit.summary.driftRemoved, 0);
  assert.equal(s.bulkProductAudit.summary.driftUnresolved, 0);
  assert.equal(s.bulkProductAudit.summary.needsSxo, 0);
  assert.equal(s.seoChecklist.length, 8);
  assert.equal(s.aioChecklist.length, 6);
  assert.equal(s.geoChecklist.length, 7);
  assert.equal(s.sxoChecklist.length, 7);
  assert.equal(s.visibility.manual.geo.total, 7);
  assert.equal(s.visibility.manual.sxo.total, 7);
  assert.match(s.visibility.note, /not Google/i);
  assert.equal((await f.req("seo/checklist", "PATCH", { key: "not-a-real-key" })).status, 400);
});

test("SEO settings are owner-only and validated", async (t) => {
  const f = await fixture(t);
  await f.login("staff");
  assert.equal((await f.req("seo/settings", "PUT", { domains: ["a.com"], brand: "RVH" })).status, 403);
  await f.login();
  assert.equal((await f.req("seo/settings", "PUT", { domains: "not-an-array", brand: "RVH" })).status, 400);
  assert.equal((await f.req("seo/settings", "PUT", { domains: Array(11).fill("a.com"), brand: "RVH" })).status, 400);
  const r = await f.req("seo/settings", "PUT", { domains: ["rohitveterinary.com", " app.rohitveterinary.com "], brand: "RVH" });
  assert.equal(r.status, 200);
  assert.deepEqual(r.data.domains, ["rohitveterinary.com", "app.rohitveterinary.com"]);
  assert.equal(r.data.brand, "RVH");
});

test("checklist toggles persist and compute independently for SEO and AIO", async (t) => {
  const f = await fixture(t);
  await f.login();
  let r = await f.req("seo/checklist", "PATCH", { key: "titles" });
  assert.equal(r.data.checklist.titles, true);
  r = await f.req("seo/checklist", "PATCH", { key: "titles" });
  assert.equal(r.data.checklist.titles, false);
  r = await f.req("aio/checklist", "PATCH", { key: "qa" });
  assert.equal(r.data.checklist.qa, true);
  r = await f.req("geo/checklist", "PATCH", { key: "entity" });
  assert.equal(r.data.checklist.entity, true);
  r = await f.req("sxo/checklist", "PATCH", { key: "cta" });
  assert.equal(r.data.checklist.cta, true);
  const s = (await f.req("state")).data;
  assert.equal(s.seo.checklist.titles, false);
  assert.equal(s.aio.checklist.qa, true);
  assert.equal(s.geo.checklist.entity, true);
  assert.equal(s.visibility.manual.geo.passed, 1);
  assert.equal(s.sxo.checklist.cta, true);
  assert.equal(s.visibility.manual.sxo.passed, 1);
});

test("keyword add/delete and check requires a configured domain and AI setup", async (t) => {
  const f = await fixture(t);
  await f.login();
  const added = await f.req("seo/keywords", "POST", { keyword: "cattle foot rot treatment", page: "Blog", priority: "High" });
  assert.equal(added.status, 201);
  assert.equal(added.data.keywords.length, 1);
  const id = added.data.keywords[0].id;
  assert.equal(added.data.keywords[0].rank, "Not measured");

  // No domain configured yet takes priority over the AI-not-configured case.
  assert.equal((await f.req(`seo/keywords/${id}/check`, "POST", {})).status, 400);
  await f.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "RVH" });
  assert.equal((await f.req(`seo/keywords/${id}/check`, "POST", {})).status, 503);

  const del = await f.req(`seo/keywords/${id}`, "DELETE");
  assert.equal(del.status, 200);
  assert.equal(del.data.keywords.length, 0);
});

test("web research is saved with sources without treating it as rank measurement", async (t) => {
  let received;
  const f = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async (args) => { received = args; return {text:"A relevant clinic page was found.",sources:[{title:"Clinic",url:"https://rohitveterinary.com/"}]}; },
  });
  await f.login();
  await f.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "RVH" });
  const added = await f.req("seo/keywords", "POST", { keyword: "poultry vaccination schedule", page: "Blog" });
  const id = added.data.keywords[0].id;
  const checked = await f.req(`seo/keywords/${id}/check`, "POST", {});
  assert.equal(checked.status, 200);
  assert.match(received.prompt, /poultry vaccination schedule/);
  assert.match(received.prompt, /rohitveterinary\.com/);
  const kw = checked.data.keywords[0];
  assert.match(kw.lastChecked, /clinic page/);
  assert.match(received.prompt,/not a Google rank tracker/);
  assert.equal(kw.sources[0].url,"https://rohitveterinary.com/");
  assert.ok(kw.lastCheckedAt);
});

test("AIO research never automatically claims another AI product cited the clinic", async (t) => {
  const cited = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async () => "Rohit Veterinary House is cited in the top AI Overview answer.\nCITED",
  });
  await cited.login();
  await cited.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "Rohit Veterinary House" });
  const q1 = await cited.req("aio/queries", "POST", { query: "how to treat mastitis in cattle" });
  const id1 = q1.data.queries[0].id;
  const r1 = await cited.req(`aio/queries/${id1}/check`, "POST", {});
  assert.equal(r1.data.queries[0].status, "Not started");


  const notCited = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async () => "No mention of this clinic was found in the results.\nNOT_CITED",
  });
  await notCited.login();
  await notCited.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "Rohit Veterinary House" });
  const q2 = await notCited.req("aio/queries", "POST", { query: "best telemedicine vet app for farmers India" });
  const id2 = q2.data.queries[0].id;
  const r2 = await notCited.req(`aio/queries/${id2}/check`, "POST", {});
  assert.equal(r2.data.queries[0].status, "Not started");

  const manual = await notCited.req(`aio/queries/${id2}`, "PATCH", { status: "Drafted" });
  assert.equal(manual.data.queries[0].status, "Drafted");
  assert.equal((await notCited.req(`aio/queries/${id2}`, "PATCH", { status: "not-a-status" })).status, 400);

  const del = await notCited.req(`aio/queries/${id2}`, "DELETE");
  assert.equal(del.data.queries.length, 0);
});

test("a failed provider call surfaces as an error, not a silently saved success", async (t) => {
  const f = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async () => { throw new Error("AI provider returned HTTP 500. Check server configuration."); },
  });
  await f.login();
  await f.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "RVH" });
  const added = await f.req("seo/keywords", "POST", { keyword: "test keyword", page: "" });
  const id = added.data.keywords[0].id;
  const r = await f.req(`seo/keywords/${id}/check`, "POST", {});
  assert.equal(r.status, 502);
  const s = (await f.req("state")).data;
  assert.equal(s.seo.keywords[0].lastChecked, null);
});

test("every client module the app statically imports is actually servable (no 404 in the import chain)", async (t) => {
  // studio-ui.js does `import ... from '/discoverability-ui.js'` at module
  // top level. If server.mjs's static-file whitelist doesn't include a file,
  // the request 404s and the whole ES module chain fails to load in a real
  // browser — this caught exactly that bug once already.
  const f = await fixture(t);
  for (const [path, contentType] of [
    ["/app.js", "text/javascript"],
    ["/studio-ui.js", "text/javascript"],
    ["/advertisement-ui.js", "text/javascript"],
    ["/discoverability-ui.js", "text/javascript"],
    ["/meta-ui.js", "text/javascript"],
    ["/video-options.js", "text/javascript"],
    ["/styles.css", "text/css"],
    ["/", "text/html"],
  ]) {
    const r = await fetch(f.base + path);
    assert.equal(r.status, 200, `${path} should be servable, got ${r.status}`);
    assert.ok(r.headers.get("content-type").startsWith(contentType), `${path} content-type was ${r.headers.get("content-type")}`);
  }
});


test('website scan persists, website draft enters review workflow with clinic profile, and owner gate holds',async t=>{
 let received;
 const f=await fixture(t,{env:{OPENAI_API_KEY:'mock',OPENAI_MODEL:'mock'},scanImpl:async({url,profile})=>({url,phone:profile.phone,checks:[],checkedAt:new Date().toISOString()}),generateImpl:async args=>{received=args;return 'रोहित भेटनरी हाउस — 9709095993';}});
 await f.login('staff');
 assert.equal((await f.req('discover/scan','POST',{url:'https://rohitveterinary.com/'})).status,403);
 assert.equal((await f.req('discover/draft','POST',{topic:'Booking',kind:'aio'})).status,403);
 await f.login();
 assert.equal((await f.req('discover/scan','POST',{url:'https://rohitveterinary.com/'})).status,200);
 const r=await f.req('discover/draft','POST',{topic:'Booking a consultation',kind:'aio'});
 assert.equal(r.status,201);
 const s=(await f.req('state')).data;
 assert.equal(s.seo.scans.length,1);
 assert.equal(s.drafts.find(d=>d.id===r.data.id).status,'draft');
 assert.equal(received.profile.phone,'9709095993');
 assert.match(received.brief,/Do not promise search rankings/);
 const exported=(await f.req('export')).data;assert.equal(exported.seo.scans.length,1);
});

test('Meta verification makes GET requests only, keeps credentials private and tolerates optional Instagram failure',async t=>{
 const requests=[];
 const f=await fixture(t,{env:{META_PAGE_ID:'1234',META_PAGE_ACCESS_TOKEN:'private-test-token',META_INSTAGRAM_ACCOUNT_ID:'5678'},metaFetchImpl:async(url,options)=>{
  requests.push({url,options});
  if(url.includes('/5678?'))return new Response(JSON.stringify({error:{code:190,message:'private-test-token'}}),{status:400});
  if(url.includes('/published_posts?'))return Response.json({data:[{id:'1234_99',message:'Hello',permalink_url:'https://www.facebook.com/1234/posts/99',created_time:'2026-09-27T10:00:00Z'}],paging:{next:'https://example.com/?access_token=private-test-token'}});
  return Response.json({id:'1234',name:'Clinic Page'});
 }});
 assert.equal((await f.req('meta/check','POST',{})).status,401);
 await f.login('staff');assert.equal((await f.req('meta/check','POST',{})).status,403);
 await f.login();const checked=await f.req('meta/check','POST',{});
 assert.equal(checked.status,200);assert.equal(checked.data.status,'Verified');assert.equal(checked.data.posts.length,1);
 assert.match(checked.data.notes[0],/expired/);
 assert.equal(requests.length,3);assert.ok(requests.every(x=>x.options.method==='GET'&&!x.url.includes('private-test-token')));
 assert.doesNotMatch(JSON.stringify((await f.req('state')).data),/private-test-token|fingerprint|paging/);
 assert.doesNotMatch(JSON.stringify((await f.req('export')).data),/private-test-token|fingerprint/);
 assert.equal((await f.req('meta/check','POST',{})).status,429);
});

test('Meta rejects missing configuration and safely reports provider errors',async t=>{
 const empty=await fixture(t);await empty.login();assert.equal((await empty.req('meta/check','POST',{})).status,503);
 const failed=await fixture(t,{env:{META_PAGE_ID:'1234',META_PAGE_ACCESS_TOKEN:'secret-sentinel'},metaFetchImpl:async()=>new Response(JSON.stringify({error:{code:190,message:'secret-sentinel'}}),{status:400})});
 await failed.login();const r=await failed.req('meta/check','POST',{});assert.equal(r.status,502);assert.doesNotMatch(JSON.stringify(r),/secret-sentinel/);
 assert.equal((await failed.req('state')).data.metaConnection.status,'Needs attention');
});
