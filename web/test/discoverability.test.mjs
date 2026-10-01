import { test } from "node:test";
import assert from "node:assert/strict";
import { createApp } from "../server.mjs";

const password = "owner-test-password-long";

async function fixture(t, options = {}) {
  const env = {
    ADMIN_PASSWORD: password,
    STAFF_PASSWORD: "staff-test-password-long",
    APP_ORIGIN: "http://localhost:3000",
    DATA_FILE: ":memory:",
    ...options.env,
  };
  const server = createApp({ env, checkImpl: options.checkImpl, scanImpl:options.scanImpl, metaFetchImpl:options.metaFetchImpl, generateImpl:options.generateImpl });
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

test("SEO/AIO state defaults and checklist item validation", async (t) => {
  const f = await fixture(t);
  await f.login();
  const s = (await f.req("state")).data;
  assert.deepEqual(s.seo, { domains: [], brand: "", checklist: {}, keywords: [], scans: [] });
  assert.deepEqual(s.aio, { checklist: {}, queries: [] });
  assert.deepEqual(s.geo, { checklist: {} });
  assert.equal(s.seoChecklist.length, 8);
  assert.equal(s.aioChecklist.length, 6);
  assert.equal(s.geoChecklist.length, 7);
  assert.equal(s.visibility.manual.geo.total, 7);
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
  const s = (await f.req("state")).data;
  assert.equal(s.seo.checklist.titles, false);
  assert.equal(s.aio.checklist.qa, true);
  assert.equal(s.geo.checklist.entity, true);
  assert.equal(s.visibility.manual.geo.passed, 1);
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
