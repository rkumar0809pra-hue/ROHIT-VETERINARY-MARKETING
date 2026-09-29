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

One Google OAuth authorization can cover both properties if that Google identity has access:
- https://www.rohitveterinary.com/
- https://app.rohitveterinary.com/

Enable the Search Console API in the Google project and obtain an OAuth refresh token with https://www.googleapis.com/auth/webmasters.readonly scope through Google's authorization flow. Configure these server secrets in Render:
- GSC_CLIENT_ID
- GSC_CLIENT_SECRET
- GSC_REFRESH_TOKEN

Do not put credentials in browser code or paste them into app forms. A built-in Google consent/callback screen is not included in this revision; an administrator must provision the OAuth credentials first. Merely having property links does not grant access. Redeploy after configuration, then use Reports → Import last 28 available days for each property. The import verifies access; “credentials configured” does not claim a successful connection.

Imports request final Web Search data for a 28-day period ending three days before the request. Dates retain Search Console's reporting semantics. No rows is shown as no data, not invented zero metrics. Google totals are property-level, not attributed to individual improvements. Up to 30 reports are retained. No background scheduler or paid AI request runs during imports.

## Validation

Run node --test web/test/*.test.mjs. Tests cover authorization, task state transitions, required completion evidence, deduplication, credential redaction, property restrictions and no-data responses with mocked Google requests.
