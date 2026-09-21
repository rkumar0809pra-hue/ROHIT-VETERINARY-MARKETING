# RVH Marketing Studio — desktop & Android PWA

A first working, single-owner marketing workspace. It runs in a desktop browser or installs as a PWA on desktop and Android. Both devices use the same server database.

## Included

- Responsive sidebar / mobile menu, dashboard and installable app manifest with icons.
- Six on-demand specialist agents: strategy, content, video scripts, WhatsApp drafts, performance analysis and brand review. Each has its own instructions and work log. OpenAI Responses API calls happen only on the server.
- Hindi, English and Hinglish output; editable drafts; approval queue; manual publishing plan.
- Persisted SQLite drafts, runs, metrics and audit events. Optimistic version checking prevents a stale device from overwriting a saved change.
- Manual aggregate metrics and calculated cost per lead / revenue-to-ad-spend ratio. No fabricated demo performance.
- One owner password, expiring HttpOnly sessions, same-origin mutation checks, server-side approval transitions, login throttling, bounded AI concurrency and hourly run limit.
- Offline application shell. Customer content and API responses are not cached by the service worker. Saving, reading drafts, login and AI require the server.

## Run locally

Install Node.js 24 or later. No npm dependencies or frontend build are needed.

```sh
cd web
cp .env.example .env
```

Edit `web/.env`:

1. Replace `ADMIN_PASSWORD` with a unique password of at least 16 characters.
2. Set `OPENAI_API_KEY` to your own OpenAI project API key.
3. Set `OPENAI_MODEL` to a Responses API text model available in your project. There is no silent model fallback.
4. Keep `APP_ORIGIN=http://localhost:3000` for local use.

```sh
npm start
```

Open `http://localhost:3000`. Without AI credentials you can still create, edit, review and plan manual drafts. Credentials stay in the server environment; they are never delivered to the browser. Do not put the OpenAI key in the Android project's `.env`, since native app secrets can be extracted from the APK.

[OpenAI text generation documentation](https://developers.openai.com/api/docs/guides/text) describes the Responses API used here. API usage requires a funded/eligible OpenAI project; a ChatGPT subscription does not configure this server.

## Deploy and install

Run one Node server instance behind an HTTPS reverse proxy with a **persistent local disk**. Set `APP_ORIGIN` to its exact public origin (scheme and host, no path), `HOST=0.0.0.0`, and a persistent `DATA_FILE`. The proxy must allow agent requests to run for at least 100 seconds. Forward requests and cookies without changing the browser Origin header. Apply additional login rate limiting at the proxy if exposed publicly.

Container build, from the repository root:

```sh
docker build -t rvh-marketing ./web
docker volume create rvh-marketing-data
docker run --env-file web/.env -e HOST=0.0.0.0 \
  -e DATA_FILE=/app/data/marketing.sqlite \
  -p 127.0.0.1:3000:3000 \
  -v rvh-marketing-data:/app/data rvh-marketing
```

Set the production HTTPS `APP_ORIGIN` in `web/.env` before starting this container. The container does not itself terminate TLS. Keep `.env` and database backups private. Back up SQLite using its online backup API, or stop the server before copying the database and WAL files together. Test restoration before relying on backups.

Do **not** deploy the current SQLite store on an ephemeral or multi-instance serverless filesystem, including default Cloud Run instances. For Cloud Run, first replace SQLite with a managed shared database and replace in-memory sessions and rate limits with shared storage. Restarting this version expires owner sessions and resets rate-limit windows.

After HTTPS deployment:

- **Desktop:** open the URL in Chrome / Edge and use Install app.
- **Android:** open the same URL in Chrome and use Install app / Add to Home screen.
- Sign in with the same owner password. Drafts are shared; reload to see changes made on another device.

This produces an installed PWA, not a signed APK/AAB or Play Store listing. The native Android project remains a separate client with its own data and AI service.

## Daily workflow

1. Select a specialist, language and brief. Agents run on demand, with a maximum of two simultaneous runs and 30 runs per server hour.
2. Review the saved result in the library and edit it.
3. Submit it for approval, then approve as the owner.
4. Copy approved content to the destination platform, or add a future publishing plan. Calendar times are displayed in IST; date input uses your device timezone.
5. Enter campaign totals for one consistent reporting period in Analytics. The analyst can use the saved totals plus the period/context supplied in its brief.

Editing approved or planned content returns it to Draft and clears the plan. The quality agent gives advisory feedback; it cannot approve content. A failed or interrupted AI call is recorded as failed, without pretending a draft was generated.

## Explicit limits / next integration work

- Agents generate text and scripts; they do not render images or videos.
- No autonomous background agent runner or social publishing worker is present.
- Facebook, Instagram and WhatsApp delivery are not connected. The calendar is a publishing plan, not an automatic scheduler. No send/publish API is exposed.
- Add Meta OAuth / WhatsApp Business provider credentials, consent records, approved templates, delivery receipts, retries and an approval-checked job queue before implementing actual sending.
- There is one owner account; staff identity and role-based access are not implemented. Do not share the owner password with staff if they should not approve drafts.
- Analytics totals are manually supplied, not automatically imported or broken down by campaign. No CRM customer data integration is included.
- Existing Android Room data and Gemini workflows are not migrated. The new web app uses OpenAI only; Gemini/Claude fallback is future integration work.
- Live provider generation must be smoke-tested with the deployment's configured key/model. Automated tests use a stub and cannot verify account billing or model access.

## Verify

```sh
cd web
npm test
node --check public/app.js
```

Tests cover authentication and CSRF-origin protection, approval and planning transitions, stale edits, missing AI configuration, agent failure handling, metrics validation, Responses parsing and persistence across restart. Browser verification should cover desktop and Android-sized layouts, generating a draft, approval, planning, offline shell loading and installability on the final HTTPS domain.
