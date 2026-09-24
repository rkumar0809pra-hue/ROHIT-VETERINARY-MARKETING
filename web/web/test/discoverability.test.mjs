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
  const server = createApp({ env, checkImpl: options.checkImpl });
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
  return { req, login };
}

test("SEO/AIO state defaults and checklist item validation", async (t) => {
  const f = await fixture(t);
  await f.login();
  const s = (await f.req("state")).data;
  assert.deepEqual(s.seo, { domains: [], brand: "", checklist: {}, keywords: [] });
  assert.deepEqual(s.aio, { checklist: {}, queries: [] });
  assert.equal(s.seoChecklist.length, 8);
  assert.equal(s.aioChecklist.length, 6);
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
  const s = (await f.req("state")).data;
  assert.equal(s.seo.checklist.titles, false);
  assert.equal(s.aio.checklist.qa, true);
});

test("keyword add/delete and check requires a configured domain and AI setup", async (t) => {
  const f = await fixture(t);
  await f.login();
  const added = await f.req("seo/keywords", "POST", { keyword: "cattle foot rot treatment", page: "Blog", priority: "High" });
  assert.equal(added.status, 201);
  assert.equal(added.data.keywords.length, 1);
  const id = added.data.keywords[0].id;
  assert.equal(added.data.keywords[0].rank, "Not ranking");

  // No domain configured yet takes priority over the AI-not-configured case.
  assert.equal((await f.req(`seo/keywords/${id}/check`, "POST", {})).status, 400);
  await f.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "RVH" });
  assert.equal((await f.req(`seo/keywords/${id}/check`, "POST", {})).status, 503);

  const del = await f.req(`seo/keywords/${id}`, "DELETE");
  assert.equal(del.status, 200);
  assert.equal(del.data.keywords.length, 0);
});

test("a stubbed live rank check updates the keyword without calling the real API", async (t) => {
  let received;
  const f = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async (args) => { received = args; return "rohitveterinary.com ranks around position 6 for this term."; },
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
  assert.match(kw.lastChecked, /position 6/);
  assert.ok(kw.lastCheckedAt);
});

test("AIO query check auto-marks Cited only when the model reports it, and status can be set manually", async (t) => {
  const cited = await fixture(t, {
    env: { OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" },
    checkImpl: async () => "Rohit Veterinary House is cited in the top AI Overview answer.\nCITED",
  });
  await cited.login();
  await cited.req("seo/settings", "PUT", { domains: ["rohitveterinary.com"], brand: "Rohit Veterinary House" });
  const q1 = await cited.req("aio/queries", "POST", { query: "how to treat mastitis in cattle" });
  const id1 = q1.data.queries[0].id;
  const r1 = await cited.req(`aio/queries/${id1}/check`, "POST", {});
  assert.equal(r1.data.queries[0].status, "Cited");
  assert.doesNotMatch(r1.data.queries[0].lastChecked, /CITED/);

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
