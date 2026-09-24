import { randomUUID } from 'node:crypto';

export const SEO_CHECKLIST = [
  ['titles', 'Unique, keyword-led title tags on every page'],
  ['meta', 'Meta descriptions written for click-through, not just keywords'],
  ['alt', 'Alt text on livestock and clinic photos'],
  ['schema', 'FAQ and LocalBusiness schema markup added'],
  ['speed', 'Mobile page speed under 3s on slow connections'],
  ['backlinks', 'Backlinks from agriculture and veterinary directories'],
  ['hindi', 'Hindi-language pages indexed and optimised'],
  ['sitemap', 'Sitemap submitted and fresh in Search Console'],
];

export const AIO_CHECKLIST = [
  ['qa', 'Key pages written in clear question-and-answer format'],
  ['structured', 'FAQ / HowTo schema added so answers are machine-readable'],
  ['citations', 'Claims backed by cited, authoritative veterinary sources'],
  ['llmstxt', 'llms.txt file published describing the site for AI crawlers'],
  ['freshness', 'Publish dates and facts kept current'],
  ['mentions', 'Consistent brand mentions across directories and forums'],
];

// Calls OpenAI's Responses API with the built-in web_search tool so keyword
// ranks and AI-citation checks reflect what's actually on the web right now.
// Injectable so tests never make a billable request.
export async function checkOnline({ key, model, prompt, fetchImpl = fetch }) {
  const response = await fetchImpl('https://api.openai.com/v1/responses', {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model,
      store: false,
      max_output_tokens: 400,
      tools: [{ type: 'web_search' }],
      input: prompt,
    }),
    signal: AbortSignal.timeout(60000),
  });
  if (!response.ok)
    throw new Error(
      response.status === 429
        ? 'AI quota or rate limit reached. Check your OpenAI account and try later.'
        : `AI provider returned HTTP ${response.status}. Check server configuration.`,
    );
  const data = await response.json();
  if (data.status !== 'completed') throw new Error('Check did not complete. Try again.');
  const output = (data.output || [])
    .filter((item) => item.type === 'message')
    .flatMap((item) => item.content || [])
    .filter((item) => item.type === 'output_text')
    .map((item) => item.text)
    .join(' ')
    .trim();
  if (!output) throw new Error('The check returned no result. Try again.');
  return output;
}

export function createDiscoverability({ db, env, audit, json, body, requiredText, fail, quota, checkImpl = checkOnline }) {
  db.exec(`CREATE TABLE IF NOT EXISTS seo(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL DEFAULT '{}');
    CREATE TABLE IF NOT EXISTS aio(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL DEFAULT '{}');`);

  const defaultSeo = () => ({ domains: [], brand: '', checklist: {}, keywords: [] });
  const defaultAio = () => ({ checklist: {}, queries: [] });
  const readSeo = () => ({ ...defaultSeo(), ...JSON.parse(db.prepare('SELECT data FROM seo WHERE id=1').get()?.data || '{}') });
  const writeSeo = (d) => db.prepare('INSERT OR REPLACE INTO seo VALUES(1,?)').run(JSON.stringify(d));
  const readAio = () => ({ ...defaultAio(), ...JSON.parse(db.prepare('SELECT data FROM aio WHERE id=1').get()?.data || '{}') });
  const writeAio = (d) => db.prepare('INSERT OR REPLACE INTO aio VALUES(1,?)').run(JSON.stringify(d));

  async function check(prompt) {
    if (!env.OPENAI_API_KEY || !env.OPENAI_MODEL)
      throw fail(503, 'AI setup needed: configure OPENAI_API_KEY and OPENAI_MODEL on the server.');
    try {
      return await checkImpl({ key: env.OPENAI_API_KEY, model: env.OPENAI_MODEL, prompt });
    } catch (err) {
      throw fail(502, err.message);
    }
  }

  return {
    state: () => ({ seo: readSeo(), aio: readAio(), seoChecklist: SEO_CHECKLIST, aioChecklist: AIO_CHECKLIST }),
    async route(req, res, path, role) {
      const owner = () => { if (role !== 'owner') throw fail(403, 'Only the owner can change this setting.'); };

      if (path === '/api/seo/settings' && req.method === 'PUT') {
        owner();
        const data = await body(req);
        if (!Array.isArray(data.domains) || data.domains.length > 10 || data.domains.some((d) => typeof d !== 'string' || !d.trim() || d.length > 200))
          throw fail(400, 'Enter up to 10 valid domains.');
        const brand = requiredText(data.brand || 'Rohit Veterinary House', 200);
        const seo = readSeo();
        seo.domains = data.domains.map((d) => d.trim());
        seo.brand = brand;
        writeSeo(seo);
        audit('seo_settings_updated');
        json(res, 200, seo);
        return true;
      }
      if (path === '/api/seo/checklist' && req.method === 'PATCH') {
        const data = await body(req);
        if (!SEO_CHECKLIST.some(([k]) => k === data.key)) throw fail(400, 'Unknown checklist item.');
        const seo = readSeo();
        seo.checklist[data.key] = !seo.checklist[data.key];
        writeSeo(seo);
        json(res, 200, seo);
        return true;
      }
      if (path === '/api/seo/keywords' && req.method === 'POST') {
        const data = await body(req);
        const keyword = requiredText(data.keyword, 200);
        const page = requiredText(data.page || '\u2014', 200);
        const priority = ['High', 'Medium', 'Low'].includes(data.priority) ? data.priority : 'Medium';
        const seo = readSeo();
        if (seo.keywords.length >= 100) throw fail(400, 'Remove an old keyword before adding another.');
        seo.keywords.push({ id: randomUUID(), keyword, page, priority, rank: 'Not ranking', lastChecked: null, lastCheckedAt: null });
        writeSeo(seo);
        audit('seo_keyword_added');
        json(res, 201, seo);
        return true;
      }
      const kMatch = /^\/api\/seo\/keywords\/([a-f0-9-]+)(?:\/(check))?$/.exec(path);
      if (kMatch && !kMatch[2] && req.method === 'DELETE') {
        const seo = readSeo();
        seo.keywords = seo.keywords.filter((k) => k.id !== kMatch[1]);
        writeSeo(seo);
        audit('seo_keyword_deleted', kMatch[1]);
        json(res, 200, seo);
        return true;
      }
      if (kMatch && kMatch[2] === 'check' && req.method === 'POST') {
        const seo = readSeo();
        const k = seo.keywords.find((k) => k.id === kMatch[1]);
        if (!k) throw fail(404, 'Keyword not found.');
        if (!seo.domains.length) throw fail(400, 'Add a website domain in the SEO settings first.');
        quota('seo checks', 40);
        const text = await check(
          `Search the web for the keyword "${k.keyword}". In 2 short plain-text sentences, no markdown, say whether any of these sites \u2014 ${seo.domains.join(', ')} \u2014 rank on the first page of Google for it and roughly what position, and which pages currently rank in the top few results.`,
        );
        k.lastChecked = text;
        k.lastCheckedAt = new Date().toISOString();
        writeSeo(seo);
        audit('seo_keyword_checked', kMatch[1]);
        json(res, 200, seo);
        return true;
      }
      if (path === '/api/aio/checklist' && req.method === 'PATCH') {
        const data = await body(req);
        if (!AIO_CHECKLIST.some(([k]) => k === data.key)) throw fail(400, 'Unknown checklist item.');
        const aio = readAio();
        aio.checklist[data.key] = !aio.checklist[data.key];
        writeAio(aio);
        json(res, 200, aio);
        return true;
      }
      if (path === '/api/aio/queries' && req.method === 'POST') {
        const data = await body(req);
        const query = requiredText(data.query, 300);
        const aio = readAio();
        if (aio.queries.length >= 100) throw fail(400, 'Remove an old question before adding another.');
        aio.queries.push({ id: randomUUID(), query, status: 'Not started', lastChecked: null, lastCheckedAt: null });
        writeAio(aio);
        audit('aio_query_added');
        json(res, 201, aio);
        return true;
      }
      const qMatch = /^\/api\/aio\/queries\/([a-f0-9-]+)(?:\/(check))?$/.exec(path);
      if (qMatch && !qMatch[2] && req.method === 'DELETE') {
        const aio = readAio();
        aio.queries = aio.queries.filter((q) => q.id !== qMatch[1]);
        writeAio(aio);
        audit('aio_query_deleted', qMatch[1]);
        json(res, 200, aio);
        return true;
      }
      if (qMatch && !qMatch[2] && req.method === 'PATCH') {
        const data = await body(req);
        if (!['Not started', 'Drafted', 'Published', 'Cited'].includes(data.status)) throw fail(400, 'Invalid status.');
        const aio = readAio();
        const q = aio.queries.find((q) => q.id === qMatch[1]);
        if (!q) throw fail(404, 'Question not found.');
        q.status = data.status;
        writeAio(aio);
        json(res, 200, aio);
        return true;
      }
      if (qMatch && qMatch[2] === 'check' && req.method === 'POST') {
        const aio = readAio();
        const q = aio.queries.find((q) => q.id === qMatch[1]);
        if (!q) throw fail(404, 'Question not found.');
        const seo = readSeo();
        if (!seo.domains.length && !seo.brand) throw fail(400, 'Add a domain or brand name in SEO settings first.');
        quota('aio checks', 40);
        const raw = await check(
          `Search the web for "${q.query}". In 2 short plain-text sentences, no markdown, say whether "${seo.brand || 'the clinic'}" (sites: ${seo.domains.join(', ') || 'unknown'}) is mentioned, linked or cited in the top results or any AI-generated summary for this query. End with exactly one extra line containing only CITED or NOT_CITED.`,
        );
        const cited = /\bCITED\b/.test(raw) && !/NOT_CITED/.test(raw);
        q.lastChecked = raw.replace(/NOT_CITED/g, '').replace(/\bCITED\b/g, '').trim();
        q.lastCheckedAt = new Date().toISOString();
        if (cited) q.status = 'Cited';
        writeAio(aio);
        audit('aio_query_checked', qMatch[1]);
        json(res, 200, aio);
        return true;
      }
      return false;
    },
  };
}
