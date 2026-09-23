# RVH Marketing Studio — desktop & Android PWA

A shared veterinary marketing workspace with owner, marketing staff and content creator roles. It runs in a desktop browser or installs as a PWA on desktop and Android. Both devices use the same server database.

## Included

- Responsive sidebar / mobile menu, dashboard and installable app manifest with icons.
- Six on-demand specialist agents: strategy, content, video scripts, WhatsApp drafts, performance analysis and brand review. Each has its own instructions and work log. OpenAI Responses API calls happen only on the server.
- Hindi, English and Hinglish output; editable drafts; approval queue; manual publishing plan.
- Persisted SQLite drafts, runs, metrics and audit events. Optimistic version checking prevents a stale device from overwriting a saved change.
- Manual aggregate metrics and calculated cost per lead / revenue-to-ad-spend ratio. No fabricated demo performance.
- Role passwords, expiring HttpOnly sessions, same-origin mutation checks, server-side approval transitions, login throttling, bounded AI concurrency and hourly run limit.
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

Do **not** deploy the current SQLite store on an ephemeral or multi-instance serverless filesystem, including default Cloud Run instances. For Cloud Run, first replace SQLite with a managed shared database and replace in-memory sessions and rate limits with shared storage. Restarting expires sessions and resets text-agent hourly limits. Media and chat daily quotas are persisted.

After HTTPS deployment:

- **Desktop:** open the URL in Chrome / Edge and use Install app.
- **Android:** open the same URL in Chrome and use Install app / Add to Home screen.
- Sign in with the same owner password. Drafts are shared; reload to see changes made on another device.

This produces an installed PWA, not a signed APK/AAB or Play Store listing. The native Android project remains a separate client with its own data and AI service.

## Daily workflow

1. Select a specialist, language and brief. Agents run on demand, with a maximum of two simultaneous runs and 30 runs per server hour.
2. Review the saved result in the library and edit it.
3. Submit it for approval, then approve as the owner.
4. Copy approved content to the destination platform, or add a future publishing plan. Calendar times are displayed in IST; date input also uses IST.
5. Enter campaign totals for one consistent reporting period in Analytics. The analyst can use the saved totals plus the period/context supplied in its brief.

Editing approved or planned content returns it to Draft and clears the plan. The quality agent gives advisory feedback; it cannot approve content. A failed or interrupted AI call is recorded as failed, without pretending a draft was generated.

## Studio tools and connections

Content Creator, WhatsApp campaigns, Video Maker, Poster & media, AI Assistant, searchable library, an IST calendar and campaign analytics are now available. See [the source-port feature map](IMPORT_NOTES.md) for exact capabilities and limits.

- Save clinic name, phone, address and brand rules in Settings. Every agent uses this profile.
- For real Veo video clips, set `GEMINI_API_KEY` in Render Environment. `VEO_MODEL` defaults to `veo-3.1-fast-generate-preview`. Paid Google API access is required.
- For images, the existing `OPENAI_API_KEY` is used with `OPENAI_IMAGE_MODEL` (default `gpt-image-1`); model access is required. A free local branded-poster template is also included.
- Media generation is asynchronous, with saved jobs and private downloads. Keep media storage on the persistent disk. Back up both database and media files.
- Optional `STAFF_PASSWORD` and `CREATOR_PASSWORD` must be distinct, at least 16 characters. Only the owner approves content or changes settings. Staff can plan approved content and record results; creators draft and submit. These are shared role logins, not individual employee accounts.
- Facebook, Instagram and WhatsApp delivery remain unconnected. Plans do not automatically send posts. Copy/share approved content and record publication manually.
- Analytics are manually entered. No fabricated sample results or automatic CRM imports.
- Native Android data is not in the source export and is not imported. The existing Android client remains separate.
- Provider generation must be checked using your own account; automated tests do not spend API credits.

## Verify

```sh
cd web
npm test
node --check public/app.js
```

Tests cover authentication and CSRF-origin protection, approval and planning transitions, stale edits, missing AI configuration, agent failure handling, metrics validation, Responses parsing and persistence across restart. Browser verification should cover desktop and Android-sized layouts, generating a draft, approval, planning, offline shell loading and installability on the final HTTPS domain.

## Weekly planning upgrade

Render Node build command: `npm ci && npm test`. Start: `npm start`.

Settings includes an optional HTTPS booking URL. Marketing manager creates seven daily Hindi drafts from confirmed goals, availability and budget (four plans per day). Approval inbox provides owner-only batch approval with stale-version protection. Tasks are assigned drafts, not independently executed social jobs. No automatic sending or booking is enabled.

New Veo clips receive a four-second branded contact ending; existing videos are unchanged. Install dependencies with npm ci. If branding fails, the original clip is saved with a warning. Set VIDEO_BRANDING=off to disable. Speech transcription, subtitles and custom logo uploads are not included.

## Complete advertisements (Video Maker)

Owners can prepare an editable 30- or 60-second advertisement, review it, and authorise its remaining paid requests. Thirty seconds uses four scenes (8/6/6/6 seconds) plus a four-second clinic card; sixty seconds uses seven eight-second scenes plus the card. Each scene can use a Veo clip, optionally starting from a saved photo, or a saved photo/screenshot unchanged. This preserves real signage and app screens. Reference photos do not guarantee presenter identity or lip sync.

The worker processes one advertisement at a time, records Veo operation IDs, polls existing jobs, and never automatically repeats failed paid requests. Restarted advertisements pause for owner review. Continue reuses saved clips and narration; failed/ambiguous requests require renewed authorisation. Pause stops future requests; a request already sent may complete and be billed. Completed projects remain private until downloaded and manually published. Remove a project to release its intermediate clips/audio; its finished video and uploaded photos stay.

OpenAI speech uses `OPENAI_API_KEY` and optional `OPENAI_TTS_MODEL` (default `gpt-4o-mini-tts`). Built-in coral/onyx voices are available. Per-scene PCM audio is saved, fitted to its scene without clipping speech, and replaces the Veo audio. Long speech pauses assembly; the owner can continue silently. Scene-level captions are burned into a dedicated panel using the bundled Devanagari font. An AI-voice disclosure is included. No background music or word-by-word karaoke timing is added. Review pronunciation and visuals before use.

Output is 720p MP4, with a snapshot of the saved clinic identity. Render needs the existing ffmpeg-static/sharp dependencies; keep build `npm ci && npm test` and start `npm start`. Intermediate and finished media count against the 600 MB app storage limit. Generation caps remain 12 video/image requests per rolling day; narration has a 30-request daily cap. A 60-second project can exceed the remaining quota and pause until it resets. Leave storage headroom for seven clips plus the finished render.

The displayed video-only USD estimate uses the configured supported Veo model and published 720p rates checked 2026-09-23. It excludes OpenAI script/speech charges, taxes, and previous attempts; it is not a billing cap. Unknown models show requested seconds without an invented monetary estimate.

API references: https://ai.google.dev/gemini-api/docs/pricing and https://developers.openai.com/api/docs/guides/text-to-speech . Tests use fake provider responses; no paid generation is performed by the test suite.

### Presenter and prompt controls
Complete advertisements supports female, male, no presenter, or custom presenter directions. Choose Coral, Nova, Shimmer, Onyx, Echo, or Sage narration separately, with warm, professional, cinematic, energetic, or storyteller delivery. These are built-in AI voices, not celebrity impersonations. Appearance consistency and lip synchronisation remain best-effort.

“Improve idea with AI” expands the topic into an editable video brief using one OpenAI request. It does not generate media. Review the brief, prepare a storyboard, save reviewed scenes, then authorise video generation. Existing projects retain their previous voice and use warm delivery by default.
