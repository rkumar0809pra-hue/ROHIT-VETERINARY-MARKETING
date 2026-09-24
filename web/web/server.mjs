import { createServer } from "node:http";
import {
  createHash,
  randomBytes,
  randomUUID,
  timingSafeEqual,
} from "node:crypto";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, mkdirSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { agents, generate } from "./agents.mjs";
import { createStudio } from "./studio.mjs";
const root = dirname(fileURLToPath(import.meta.url));
const fail = (status, message) => Object.assign(new Error(message), { status });
const hash = (value) => createHash("sha256").update(value).digest();
const requiredText = (value, max) => {
  if (typeof value !== "string" || !value.trim() || value.length > max)
    throw fail(400, `Text must contain 1–${max} characters.`);
  return value.trim();
};
async function body(req, limit = 160000) {
  if (!req.headers["content-type"]?.startsWith("application/json"))
    throw fail(415, "JSON required.");
  const chunks=[]; let bytes=0;
  for await (const chunk of req) {
    bytes += chunk.length;
    if (bytes > limit) throw fail(413, "Request too large.");
    chunks.push(chunk);
  }
  const raw=Buffer.concat(chunks).toString("utf8");
  try {
    const value = JSON.parse(raw);
    if (!value || Array.isArray(value) || typeof value !== "object")
      throw Error();
    return value;
  } catch {
    throw fail(400, "Invalid JSON.");
  }
}
export function createApp({ env = process.env, generateImpl = generate, provider, renderAdvertisementImpl, checkImpl } = {}) {
  if (
    !env.ADMIN_PASSWORD ||
    env.ADMIN_PASSWORD.length < 16 ||
    env.ADMIN_PASSWORD === "replace-with-a-unique-long-password"
  )
    throw Error("Set ADMIN_PASSWORD to at least 16 characters.");
  const credentials = { owner: env.ADMIN_PASSWORD, staff: env.STAFF_PASSWORD, creator: env.CREATOR_PASSWORD };
  const configured = Object.values(credentials).filter(Boolean);
  if(configured.some(p => p.length < 16) || new Set(configured).size !== configured.length) throw Error("Role passwords must be distinct and at least 16 characters.");
  const origin = new URL(env.APP_ORIGIN || "http://localhost:3000").origin;
  if (
    !origin.startsWith("https:") &&
    !["localhost", "127.0.0.1"].includes(new URL(origin).hostname)
  )
    throw Error("APP_ORIGIN must use HTTPS outside localhost.");
  const dbPath = env.DATA_FILE || join(root, "data", "marketing.sqlite");
  if (dbPath !== ":memory:")
    mkdirSync(dirname(resolve(dbPath)), { recursive: true });
  const db = new DatabaseSync(dbPath);
  db.exec(`PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;
    CREATE TABLE IF NOT EXISTS drafts(id TEXT PRIMARY KEY,title TEXT NOT NULL,agent TEXT NOT NULL,language TEXT NOT NULL,brief TEXT NOT NULL,content TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'draft',planned_at TEXT,created_at TEXT NOT NULL,version INTEGER NOT NULL DEFAULT 1);
    CREATE TABLE IF NOT EXISTS runs(id TEXT PRIMARY KEY,agent TEXT NOT NULL,status TEXT NOT NULL,error TEXT,created_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS metrics(id INTEGER PRIMARY KEY CHECK(id=1),spend REAL NOT NULL,clicks INTEGER NOT NULL,leads INTEGER NOT NULL,revenue REAL NOT NULL);
    CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY,action TEXT NOT NULL,draft_id TEXT,created_at TEXT NOT NULL);`);
  // An interrupted run is never represented as successfully completed.
  db.prepare(
    "UPDATE runs SET status='failed',error='Server restarted before completion.' WHERE status='running'",
  ).run();
  const sessions = new Map();
  let activeRuns = 0;
  let loginWindow = { start: Date.now(), count: 0 };
  let aiWindow = { start: Date.now(), count: 0 };
  const audit = (action, id = null) =>
    db
      .prepare("INSERT INTO audit(action,draft_id,created_at) VALUES(?,?,?)")
      .run(action, id, new Date().toISOString());
  const getDraft = (id) => {
    const d = db.prepare("SELECT * FROM drafts WHERE id=?").get(id);
    if (!d) throw fail(404, "Draft not found.");
    return d;
  };
  const metrics = () =>
    db
      .prepare("SELECT spend,clicks,leads,revenue FROM metrics WHERE id=1")
      .get() || null;
  const json = (res, status, data) => {
    res.writeHead(status, {
      "Content-Type": "application/json; charset=utf-8",
    });
    res.end(JSON.stringify(data));
  };
  const studio=createStudio({db,env,dbPath,audit,json,body,requiredText,fail,generateImpl,provider,renderAdvertisementImpl,checkImpl});
  const files = {
    "/video-options.js": ["video-options.js", "text/javascript"],
    "/devanagari.ttf": ["../assets/NotoSansDevanagari.ttf", "font/ttf"],
    "/advertisement-ui.js": ["advertisement-ui.js", "text/javascript"],
    "/studio-ui.js": ["studio-ui.js", "text/javascript"],
    "/": ["index.html", "text/html"],
    "/app.js": ["app.js", "text/javascript"],
    "/styles.css": ["styles.css", "text/css"],
    "/sw.js": ["sw.js", "text/javascript"],
    "/manifest.webmanifest": [
      "manifest.webmanifest",
      "application/manifest+json",
    ],
    "/icon.svg": ["icon.svg", "image/svg+xml"],
    "/icon-192.png": ["icon-192.png", "image/png"],
    "/icon-512.png": ["icon-512.png", "image/png"],
  };
  const server = createServer(async (req, res) => {
    res.setHeader("Cache-Control", "no-store");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Referrer-Policy", "no-referrer");
    res.setHeader(
      "Content-Security-Policy",
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob: data:; media-src 'self' blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'",
    );
    try {
      const path = new URL(req.url, origin).pathname;
      if (req.method === "GET" && files[path]) {
        const [file, type] = files[path];
        res.writeHead(200, {
          "Content-Type": `${type}${type.startsWith("text/") ? "; charset=utf-8" : ""}`,
        });
        return res.end(readFileSync(join(root, "public", file)));
      }
      if (!path.startsWith("/api/")) throw fail(404, "Not found.");
      if (req.method !== "GET" && req.headers.origin !== origin)
        throw fail(403, "Request origin not allowed.");
      if (path === "/api/login" && req.method === "POST") {
        if (Date.now() - loginWindow.start > 60000)
          loginWindow = { start: Date.now(), count: 0 };
        if (++loginWindow.count > 15)
          throw fail(429, "Too many login attempts. Wait a minute.");
        const data = await body(req);
        const role = data.role || "owner";
        if (
          !credentials[role] ||
          typeof data.password !== "string" ||
          !timingSafeEqual(hash(data.password), hash(credentials[role]))
        )
          throw fail(401, "Incorrect password.");
        for (const [key, session] of sessions)
          if (session.expiry < Date.now()) sessions.delete(key);
        if (sessions.size >= 100) sessions.delete(sessions.keys().next().value);
        const token = randomBytes(32).toString("hex");
        sessions.set(hash(token).toString("hex"), {expiry: Date.now() + 8 * 3600000, role});
        res.setHeader(
          "Set-Cookie",
          `rvh_session=${token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800${origin.startsWith("https:") ? "; Secure" : ""}`,
        );
        return json(res, 200, { ok: true });
      }
      const token =
        /(?:^|;\s*)rvh_session=([a-f0-9]{64})(?:;|$)/.exec(
          req.headers.cookie || "",
        )?.[1] || "";
      const sessionKey = hash(token).toString("hex");
      if ((sessions.get(sessionKey)?.expiry || 0) <= Date.now())
        throw fail(401, "Please sign in.");
      const role=sessions.get(sessionKey).role;
      if(await studio.route(req,res,path,role)) return;
      if (path === "/api/logout" && req.method === "POST") {
        sessions.delete(sessionKey);
        res.setHeader(
          "Set-Cookie",
          "rvh_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0",
        );
        return json(res, 200, { ok: true });
      }
      if (path === "/api/state" && req.method === "GET")
        return json(res, 200, {
          ...studio.state(),
          role,
          rolesConfigured: { staff: Boolean(env.STAFF_PASSWORD), creator: Boolean(env.CREATOR_PASSWORD) },
          agents: agents.map(({ instruction, ...agent }) => agent),
          aiConfigured: Boolean(env.OPENAI_API_KEY && env.OPENAI_MODEL),
          model: env.OPENAI_MODEL || null,
          drafts: db
            .prepare("SELECT * FROM drafts ORDER BY created_at DESC")
            .all(),
          runs: db
            .prepare("SELECT * FROM runs ORDER BY created_at DESC LIMIT 30")
            .all(),
          metrics: metrics(),
        });
      if (path === "/api/metrics" && req.method === "PUT") {
        if(role === "creator") throw fail(403,"Only the owner or marketing staff can update metrics.");
        const data = await body(req);
        for (const k of ["spend", "clicks", "leads", "revenue"])
          if (
            typeof data[k] !== "number" ||
            !Number.isFinite(data[k]) ||
            data[k] < 0 ||
            data[k] > 1e10 ||
            (["clicks", "leads"].includes(k) && !Number.isInteger(data[k]))
          )
            throw fail(
              400,
              "Use valid nonnegative metrics; clicks and leads must be whole numbers.",
            );
        db.prepare("INSERT OR REPLACE INTO metrics VALUES(1,?,?,?,?)").run(
          data.spend,
          data.clicks,
          data.leads,
          data.revenue,
        );
        audit("metrics_updated");
        return json(res, 200, { ok: true });
      }
      if (path === "/api/drafts" && req.method === "POST") {
        const data = await body(req);
        const id = randomUUID();
        db.prepare(
          "INSERT INTO drafts(id,title,agent,language,brief,content,created_at) VALUES(?,?,'manual','English','',?,?)",
        ).run(
          id,
          requiredText(data.title, 160),
          requiredText(data.content, 20000),
          new Date().toISOString(),
        );
        audit("created", id);
        return json(res, 201, getDraft(id));
      }
      if (path === "/api/runs" && req.method === "POST") {
        const data = await body(req);
        const agent = agents.find((a) => a.id === data.agent);
        if (!agent || !["English", "Hindi", "Hinglish"].includes(data.language))
          throw fail(400, "Choose a valid agent and language.");
        const brief = requiredText(data.brief, 6000);
        const draftMeta = JSON.stringify(studio.metadata(data.meta));
        if (!env.OPENAI_API_KEY || !env.OPENAI_MODEL)
          throw fail(
            503,
            "AI setup needed: configure OPENAI_API_KEY and OPENAI_MODEL on the server.",
          );
        if (activeRuns >= 2)
          throw fail(429, "Two agents are already working. Try again shortly.");
        if (Date.now() - aiWindow.start > 3600000)
          aiWindow = { start: Date.now(), count: 0 };
        if (aiWindow.count >= 30)
          throw fail(429, "Hourly limit of 30 agent runs reached.");
        aiWindow.count++;
        activeRuns++;
        const id = randomUUID(),
          now = new Date().toISOString();
        db.prepare("INSERT INTO runs VALUES(?,?,'running',NULL,?)").run(
          id,
          agent.id,
          now,
        );
        try {
          const content = requiredText(
            await generateImpl({
              key: env.OPENAI_API_KEY,
              model: env.OPENAI_MODEL,
              agent,
              language: data.language,
              brief,
              metrics: metrics(),
              profile: studio.profile(),
            }),
            20000,
          );
          db.exec("BEGIN");
          try {
            db.prepare(
              "INSERT INTO drafts(id,title,agent,language,brief,content,created_at) VALUES(?,?,?,?,?,?,?)",
            ).run(
              id,
              brief.slice(0, 100),
              agent.id,
              data.language,
              brief,
              content,
              now,
            );
            db.prepare("UPDATE drafts SET meta_json=? WHERE id=?").run(draftMeta,id);
            db.prepare("UPDATE runs SET status='completed' WHERE id=?").run(id);
            audit("agent_draft_created", id);
            db.exec("COMMIT");
          } catch (err) {
            db.exec("ROLLBACK");
            throw err;
          }
          return json(res, 201, getDraft(id));
        } catch (err) {
          const message =
            err.name === "TimeoutError"
              ? "Agent timed out. Please try again."
              : err.message.startsWith("AI ") ||
                  err.message.startsWith("The AI")
                ? err.message
                : "Agent failed. Please try again.";
          db.prepare("UPDATE runs SET status='failed',error=? WHERE id=?").run(
            message,
            id,
          );
          throw fail(502, message);
        } finally {
          activeRuns--;
        }
      }
      const match = /^\/api\/drafts\/([a-f0-9-]+)$/.exec(path);
      if (match && req.method === "PATCH") {
        const old = getDraft(match[1]),
          data = await body(req);
        if (data.version !== old.version)
          throw fail(
            409,
            "This draft changed on another device. Reload and try again.",
          );
        if (["approve","reject"].includes(data.action) && role !== "owner") throw fail(403,"Only the owner can approve or reject content.");
        if (["plan","unplan","publish","results"].includes(data.action) && role === "creator") throw fail(403,"Only the owner or marketing staff can manage publishing and results.");
        let { title, content, status, planned_at, meta_json, published_at, published_url, metrics_json } = old;
        if (data.action === "edit") {
          title = requiredText(data.title, 160);
          content = requiredText(data.content, 20000);
          status = "draft";
          planned_at = null;
          published_at = null; published_url = null;
          if(data.meta !== undefined) meta_json=JSON.stringify(studio.metadata(data.meta));
        } else if (data.action === "submit" && status === "draft")
          status = "pending";
        else if (data.action === "approve" && status === "pending")
          status = "approved";
        else if (data.action === "reject" && status === "pending")
          status = "draft";
        else if (
          data.action === "plan" &&
          ["approved", "planned"].includes(status)
        ) {
          if (
            typeof data.planned_at !== "string" ||
            !Number.isFinite(Date.parse(data.planned_at)) ||
            Date.parse(data.planned_at) <= Date.now()
          )
            throw fail(400, "Choose a future date and time.");
          planned_at = new Date(data.planned_at).toISOString();
          status = "planned";
        } else if (data.action === "unplan" && status === "planned") {
          status = "approved";
          planned_at = null;
        } else if(data.action === "publish" && ["approved","planned"].includes(status)) {
          if(data.confirmPublished !== true) throw fail(400,"Confirm you published this content yourself.");
          const url=requiredText(data.url,1000); let parsed;
          try{parsed=new URL(url);}catch{throw fail(400,"Enter the published post's HTTPS link.");}
          if(parsed.protocol!=="https:" || parsed.username || parsed.password) throw fail(400,"Use an HTTPS link.");
          published_url=url; published_at=new Date().toISOString(); status="published"; planned_at=null;
        } else if(data.action === "results") {
          const result={};
          for(const key of ["spend","clicks","leads","revenue","sent","failed","responses"]) {
            const value=data.metrics?.[key];
            if(typeof value!=="number" || !Number.isFinite(value) || value<0 || value>1e10 || (!["spend","revenue"].includes(key)&&!Number.isInteger(value))) throw fail(400,"Enter valid nonnegative campaign results.");
            result[key]=value;
          }
          result.period=requiredText(data.metrics.period,160);result.updated_at=new Date().toISOString();metrics_json=JSON.stringify(result);
        } else throw fail(409, "That action is not available for this draft.");
        db.prepare(
          "UPDATE drafts SET title=?,content=?,status=?,planned_at=?,meta_json=?,published_at=?,published_url=?,metrics_json=?,version=version+1 WHERE id=?",
        ).run(title, content, status, planned_at, meta_json, published_at, published_url, metrics_json, old.id);
        audit(data.action, old.id);
        return json(res, 200, getDraft(old.id));
      }
      throw fail(404, "Not found.");
    } catch (err) {
      if (!res.headersSent)
        json(res, err.status || 500, {
          error: err.status ? err.message : "Server error. Please try again.",
        });
      else res.end();
    }
  });
  server.on("close", () => { studio.close().then(() => db.close()); });
  server.requestTimeout = 100000;
  return server;
}
if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const server = createApp();
  server.listen(
    Number(process.env.PORT || 3000),
    process.env.HOST || "127.0.0.1",
    () => console.log("RVH Marketing listening on configured port."),
  );
}
