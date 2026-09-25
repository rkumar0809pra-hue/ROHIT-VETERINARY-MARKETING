import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createApp } from "../server.mjs";
import { generate, agents } from "../agents.mjs";
const env = {
  ADMIN_PASSWORD: "test-password-long-enough",
  APP_ORIGIN: "http://localhost:3000",
  DATA_FILE: ":memory:",
};
async function fixture(t, options = {}) {
  const server = createApp({
    env: { ...env, ...options.env },
    generateImpl: options.generateImpl,
  });
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  t.after(() => new Promise((r) => server.close(r)));
  let cookie = "";
  const request = async (path, method = "GET", data, extra = {}) => {
    const r = await fetch(
      `http://127.0.0.1:${server.address().port}/api/${path}`,
      {
        method,
        headers: {
          origin: env.APP_ORIGIN,
          cookie,
          ...(data ? { "content-type": "application/json" } : {}),
          ...extra,
        },
        body: data ? JSON.stringify(data) : undefined,
      },
    );
    if (r.headers.get("set-cookie"))
      cookie = r.headers.get("set-cookie").split(";")[0];
    return { status: r.status, data: await r.json(), headers: r.headers };
  };
  return {
    request,
    login: () => request("login", "POST", { password: env.ADMIN_PASSWORD }),
  };
}
test("authentication, origin checking and logout", async (t) => {
  const f = await fixture(t);
  assert.equal((await f.request("state")).status, 401);
  assert.equal(
    (await f.request("login", "POST", { password: "bad" })).status,
    401,
  );
  assert.equal(
    (
      await f.request(
        "login",
        "POST",
        { password: env.ADMIN_PASSWORD },
        { origin: "https://evil.test" },
      )
    ).status,
    403,
  );
  assert.equal((await f.login()).status, 200);
  assert.match(
    (await f.request("state")).headers.get("cache-control"),
    /no-store/,
  );
  await f.request("logout", "POST", {});
  assert.equal((await f.request("state")).status, 401);
});
test("approval gates, date validation, stale updates and edits invalidate approval", async (t) => {
  const f = await fixture(t);
  await f.login();
  let d = (
    await f.request("drafts", "POST", {
      title: "Vaccination",
      content: "Book a consultation.",
    })
  ).data;
  const change = (action, more = {}) =>
    f.request("drafts/" + d.id, "PATCH", {
      action,
      version: d.version,
      ...more,
    });
  assert.equal((await change("approve")).status, 409);
  assert.equal(
    (await change("plan", { planned_at: "2030-01-01T00:00:00Z" })).status,
    409,
  );
  d = (await change("submit")).data;
  d = (await change("approve")).data;
  assert.equal(d.status, "approved");
  assert.equal((await change("plan", { planned_at: "invalid" })).status, 400);
  assert.equal(
    (await change("plan", { planned_at: "2000-01-01T00:00:00Z" })).status,
    400,
  );
  d = (await change("plan", { planned_at: "2030-01-01T00:00:00Z" })).data;
  assert.equal(d.status, "planned");
  const oldVersion = d.version;
  d = (await change("edit", { title: "Revised", content: "New content" })).data;
  assert.equal(d.status, "draft");
  assert.equal(d.planned_at, null);
  assert.equal(
    (
      await f.request("drafts/" + d.id, "PATCH", {
        action: "submit",
        version: oldVersion,
      })
    ).status,
    409,
  );
  assert.equal((await change("publish")).status, 409);
});
test("missing AI config and invalid agent produce no fake drafts", async (t) => {
  const f = await fixture(t);
  await f.login();
  assert.equal(
    (
      await f.request("runs", "POST", {
        agent: "content",
        language: "Hindi",
        brief: "Test",
      })
    ).status,
    503,
  );
  assert.equal(
    (
      await f.request("runs", "POST", {
        agent: "unknown",
        language: "Hindi",
        brief: "Test",
      })
    ).status,
    400,
  );
  assert.deepEqual((await f.request("state")).data.drafts, []);
});
test("agent output, aggregate analytics and failure records", async (t) => {
  let received;
  const f = await fixture(t, {
    env: { OPENAI_API_KEY: "test", OPENAI_MODEL: "test-model" },
    generateImpl: async (args) => {
      received = args;
      if (args.brief === "fail") throw Error("provider failed with secret");
      return "Draft in requested language";
    },
  });
  await f.login();
  assert.equal(
    (
      await f.request("metrics", "PUT", {
        spend: 200,
        clicks: 20,
        leads: 4,
        revenue: 700,
      })
    ).status,
    200,
  );
  assert.equal(
    (
      await f.request("metrics", "PUT", {
        spend: -1,
        clicks: 20,
        leads: 4,
        revenue: 700,
      })
    ).status,
    400,
  );
  const r = await f.request("runs", "POST", {
    agent: "analytics",
    language: "Hindi",
    brief: "Analyze last week",
  });
  assert.equal(r.status, 201);
  assert.equal(r.data.status, "draft");
  assert.equal(received.metrics.leads, 4);
  const failed = await f.request("runs", "POST", {
    agent: "content",
    language: "English",
    brief: "fail",
  });
  assert.equal(failed.status, 502);
  assert.ok(!failed.data.error.includes("secret"));
  const s = (await f.request("state")).data;
  assert.equal(s.drafts.length, 1);
  assert.equal(s.runs.filter((r) => r.status === "failed").length, 1);
});
test("Responses parser handles reasoning and multiple messages; rejects incomplete and quota errors", async () => {
  const args = {
    key: "secret",
    model: "configured-model",
    agent: agents[1],
    language: "Hindi",
    brief: "Test",
  };
  let request;
  const output = await generate({
    ...args,
    fetchImpl: async (url, options) => {
      request = JSON.parse(options.body);
      return {
        ok: true,
        json: async () => ({
          status: "completed",
          output: [
            { type: "reasoning" },
            {
              type: "message",
              content: [{ type: "output_text", text: "First" }],
            },
            {
              type: "message",
              content: [{ type: "output_text", text: "Second" }],
            },
          ],
        }),
      };
    },
  });
  assert.equal(output, "First\nSecond");
  assert.equal(request.store, false);
  assert.equal(request.model, "configured-model");
  assert.match(request.instructions, /Hindi/);
  await assert.rejects(
    generate({
      ...args,
      fetchImpl: async () => ({
        ok: true,
        json: async () => ({ status: "incomplete", output: [] }),
      }),
    }),
    /incomplete/,
  );
  await assert.rejects(
    generate({ ...args, fetchImpl: async () => ({ ok: false, status: 429 }) }),
    /quota/,
  );
});
test("database survives restart but sessions expire", async () => {
  const dir = mkdtempSync(join(tmpdir(), "rvh-test-"));
  let s;
  const start = async () => {
    s = createApp({ env: { ...env, DATA_FILE: join(dir, "test.sqlite") } });
    await new Promise((r) => s.listen(0, "127.0.0.1", r));
    return `http://127.0.0.1:${s.address().port}`;
  };
  const login = async (base) => {
    const r = await fetch(base + "/api/login", {
      method: "POST",
      headers: { origin: env.APP_ORIGIN, "content-type": "application/json" },
      body: JSON.stringify({ password: env.ADMIN_PASSWORD }),
    });
    await r.text();
    return r.headers.get("set-cookie").split(";")[0];
  };
  try {
    let base = await start();
    const cookie = await login(base);
    const r = await fetch(base + "/api/drafts", {
      method: "POST",
      headers: {
        origin: env.APP_ORIGIN,
        "content-type": "application/json",
        cookie,
      },
      body: JSON.stringify({ title: "Persistent", content: "Saved on disk" }),
    });
    assert.equal(r.status, 201);
    await r.text();
    await new Promise((r) => s.close(r));
    base = await start();
    const denied = await fetch(base + "/api/state", { headers: { cookie } });
    assert.equal(denied.status, 401);
    await denied.text();
    const response = await fetch(base + "/api/state", {
      headers: { cookie: await login(base) },
    });
    assert.equal((await response.json()).drafts[0].title, "Persistent");
  } finally {
    if (s?.listening) await new Promise((r) => s.close(r));
    rmSync(dir, { recursive: true, force: true });
  }
});

test("PWA resources exist and private server files are not served", async (t) => {
  const f = await fixture(t);
  // A separate server exposes its address for public-resource checks.
  const server = createApp({ env });
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  t.after(() => new Promise((r) => server.close(r)));
  const base = `http://127.0.0.1:${server.address().port}`;
  for (const path of [
    "/",
    "/app.js",
    "/styles.css",
    "/sw.js",
    "/manifest.webmanifest",
    "/icon-192.png",
    "/icon-512.png",
  ]) {
    const r = await fetch(base + path);
    assert.equal(r.status, 200, path);
    await r.arrayBuffer();
  }
  const hidden = await fetch(base + "/.env");
  assert.equal(hidden.status, 404);
  await hidden.text();
  const api = await f.request("state");
  assert.equal(api.status, 401);
});
