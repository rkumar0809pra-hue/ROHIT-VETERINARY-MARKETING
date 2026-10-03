"""Server-only FastAPI adapter. Calls require database-derived aggregate totals.
No network activity on import. Use asyncio.to_thread from async handlers.
"""
import json
import os
from datetime import datetime, timezone
from urllib.parse import urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def send_daily_report(day, metrics, observed_at=None):
    source = os.environ.get('RVH_MARKETING_SOURCE')
    token = os.environ.get('RVH_MARKETING_SYNC_TOKEN', '')
    if source not in ('clinic', 'mart', 'chat') or len(token) < 32:
        raise ValueError('Configure a source and unique server sync token.')
    base = os.environ.get('RVH_MARKETING_URL', 'https://rohit-veterinary-marketing.onrender.com')
    url = urlparse(base)
    if url.scheme != 'https' or not url.netloc or url.username or url.password or url.query or url.fragment or url.path not in ('', '/'):
        raise ValueError('RVH_MARKETING_URL must be an HTTPS origin.')
    payload = {'schemaVersion': 1, 'date': day, 'timezone': 'Asia/Kolkata',
               'observedAt': observed_at or datetime.now(timezone.utc).isoformat(), 'metrics': metrics}
    request = Request(base.rstrip('/') + '/api/integrations/' + source + '/daily',
                      data=json.dumps(payload).encode(), method='POST',
                      headers={'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'})
    try:
        with build_opener(NoRedirect).open(request, timeout=15) as response:
            return json.load(response)
    except HTTPError as exc:
        raise RuntimeError('Marketing sync HTTP ' + str(exc.code) + '. Check server configuration.') from None
