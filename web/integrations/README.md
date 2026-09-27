# Connect the RVH clinic, Mart and chatbot

Status: the marketing receiver and UI are implemented. The sender helpers here are ready to install in the source backends; they are **not yet wired to the production apps or a scheduler**. No customer data has been imported. The clinic/Mart FastAPI and chatbot Express repositories were inspected, but their production branch/domain mapping and metric semantics still need verification before installing extraction jobs.

## Connection map

| Service | Source identifier | Token variable in marketing Render | Sender runtime |
| --- | --- | --- | --- |
| app.rohitveterinary.com | clinic | RVH_CLINIC_SYNC_TOKEN | Python/FastAPI: send_daily.py |
| mart.rohitveterinary.com | mart | RVH_MART_SYNC_TOKEN | Python/FastAPI: send_daily.py |
| chat.rohitveterinary.com | chat | RVH_CHAT_SYNC_TOKEN | Node/Express: send-daily.mjs |

The repositories found are `rkumar0809pra-hue/ROHIT-VETERINARY-HOUSE`, `ROHIT-VETERINARY-HOUSE-MART`, and `CHATBOT`. Clinic and Mart contain similar backend code; confirm which deployments and databases feed each domain. Do not sum their revenue if the same orders appear in both systems.

## One-time setup

1. Generate three independent random tokens locally (at least 32 characters; use a password manager or `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`). Never commit them or send them in a chat.
2. Set the three mapped variables above in the marketing app's Render Environment and redeploy. They must be distinct and must not reuse owner passwords or provider keys.
3. Copy the appropriate sender helper into each backend, outside frontend/static folders. Set backend environment variables:
   - `RVH_MARKETING_URL=https://rohit-veterinary-marketing.onrender.com`
   - `RVH_MARKETING_SOURCE=clinic` (or `mart` / `chat`)
   - `RVH_MARKETING_SYNC_TOKEN=` the corresponding token from step 1.
   Never use a `VITE_` or `REACT_APP_` variable for this secret.
4. Sign in as owner in marketing → More tools → Connected apps. Enable the service. It remains **Waiting for first sync** until an authenticated report is received.
5. Verify `GET /api/integrations/{source}/health` with the Bearer token. This checks credentials and enablement but deliberately does not mark data as received.
6. In the source backend, compute the daily aggregates below from actual records using the IST day boundaries. Call the helper from a background job after the database is available; keep this outside booking/payment request transactions. Use `asyncio.to_thread(send_daily_report, ...)` with FastAPI.
7. After reconciling one real day's totals with the source dashboard, schedule the job in the source hosting platform. No scheduler is created by this kit. Start with one completed-day snapshot each day; re-send corrected snapshots with a newer observedAt.

## Payload contract, version 1

POST `/api/integrations/clinic/daily`, `/mart/daily` or `/chat/daily` with `Authorization: Bearer <source-token>` and `Content-Type: application/json`. The body contains exactly:

```
{
  "schemaVersion": 1,
  "date": "YYYY-MM-DD",
  "timezone": "Asia/Kolkata",
  "observedAt": "ISO timestamp with Z or timezone offset",
  "metrics": { "all three metrics for the source": "numeric values" }
}
```

Above is a structural illustration, not a valid report to submit. Use real numeric totals and a real report date. Counts are non-negative integers; INR amounts are non-negative with at most two decimals. If a metric is unknown, do not substitute zero: delay the snapshot until it can be computed. Dates must be within the last year, not future IST dates. `observedAt` is when the snapshot was computed. The receiver stores the newest snapshot for each source/day rather than adding repeated snapshots.

| Source | Required metrics | Definition to implement and reconcile |
| --- | --- | --- |
| clinic | appointmentsBooked, completedVisits, revenueINR | New bookings created during the day; visits completed during the day; net collected clinic receipts during the day (exclude Mart receipts) |
| mart | ordersPlaced, ordersCompleted, netSalesINR | Orders created during the day; orders completed during the day; net recognised Mart sales after refunds during the day |
| chat | enquiries, bookingRequests, humanHandoffs | New enquiry sessions during the day; explicit booking requests; explicit handoffs to staff. Requests are not confirmed bookings. |

Refunds cannot be represented as negative totals in v1. If the day's net amount is negative, do not send that report: extend the contract first. Do not clamp negative amounts to zero. Unique session/order counts must come from source records, not message counts or inferred AI classifications.

The receiver rejects extra top-level fields or metric keys. Never send names, phones, emails, patient IDs, clinical records, chat text or order line items. Product catalogues, reminders, consent records, unified login and booking/order writes are later integrations, not part of this aggregate contract.

## Reliability and access

- Each token can write only its own source. Browser sessions do not authorize machine requests. Missing, short or reused tokens are disabled.
- Owner can pause reception immediately in Connected apps. Existing reports remain available as history.
- Limit: 600 authenticated requests per source/hour. Payload limit 8 KB; retained reports up to 400 days, latest 30 days displayed/exported.
- Equal timestamp + equal totals is an idempotent retry. An older timestamp, or equal timestamp with changed totals, returns 409. Recompute a new snapshot for corrections.
- Persist failed snapshots in a source-side outbox. Retry timeouts/5xx/429 with bounded backoff and the same snapshot; do not change observedAt for a retry. Stop and fix 400/401/403/409 instead of looping. Helpers make one attempt; they do not install an outbox.
- Token rotation: replace both sides, redeploy, then send a fresh report. Connection verification reflects the current token. No token is returned through browser state or workspace exports.
- These totals remain separate from manually entered campaign attribution metrics. The weekly planner receives labelled latest reports and must not infer causal marketing ROI, live appointment capacity or automatic bookings.

## Call the helpers

Python backend:

```python
from send_daily import send_daily_report
# aggregate_metrics is calculated from your database; no record-level data.
result = send_daily_report(report_date, aggregate_metrics, snapshot_timestamp)
```

Node backend:

```js
import {sendDailyReport} from './send-daily.mjs';
const result = await sendDailyReport({date: reportDate, metrics: aggregateMetrics, observedAt: snapshotTimestamp});
```

Keep health checks and metrics extraction server-side. The default deployment URL is the existing marketing service; there is no dependency on Supabase, a new database or an AI provider for sync itself.
