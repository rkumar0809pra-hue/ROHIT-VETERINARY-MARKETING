# Marketing Optimizer

The existing SEO & AI Visibility screen now provides three main actions and SEO, AIO, Meta, Tasks and Reports navigation. Existing AI drafts continue into the existing draft review workflow. Meta opens the existing Facebook/Instagram workspace.

## Workflow

1. Check a public website page. Scans remain bounded to the configured clinic/app/mart hosts.
2. Prepare improvements creates deduplicated SEO tasks from failed saved scan checks.
3. Review the evidence and approve or dismiss each task.
4. Apply approved changes separately on the relevant website, then record completion with a verification note. Approval never publishes, modifies a website or spends advertising money.
5. Reports retains task progress and imported Search Console summaries. No AI-citation measurement is implied. Scan scores are internal readiness checks only.

Task decisions and report history use the existing SQLite database. Ensure DATA_FILE is on the existing persistent disk and covered by backups. Supabase is not configured by this change.

## Search Console read-only authentication

The Marketing Optimizer now includes its own Google OAuth flow. In Render configure these server-only variables:

- `APP_ORIGIN=https://marketing.rohitveterinary.com`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI=https://marketing.rohitveterinary.com/api/auth/google/callback`

In the same Google OAuth client, authorize:

- JavaScript origin: `https://marketing.rohitveterinary.com`
- Redirect URI: `https://marketing.rohitveterinary.com/api/auth/google/callback`
- Scope: `https://www.googleapis.com/auth/webmasters.readonly`

The owner signs in to Marketing Studio, opens Reports, and chooses **Connect Google Search Console**. The server uses a short-lived one-time OAuth state, stores the returned refresh token encrypted in the existing SQLite database, and never returns it to the browser. Search Console properties are discovered from the connected Google account rather than hard-coded.

The Reports screen supports 7-day, 28-day and 3-month imports with clicks, impressions, CTR, average position, top queries, top pages and factual opportunity lists. Disconnect attempts to revoke the Google token and removes the local credential.

Keep `DATA_FILE` on the Render persistent disk so the encrypted Google authorization survives deploys/restarts. Changing `GOOGLE_CLIENT_SECRET` or `APP_ORIGIN` makes the stored encrypted token unreadable; reconnect Google after such a change.

## Validation

Run node --test web/test/*.test.mjs. Tests cover authorization, task state transitions, required completion evidence, deduplication, credential redaction, property restrictions and no-data responses with mocked Google requests.
