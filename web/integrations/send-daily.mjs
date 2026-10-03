// Server-only Node/Express adapter. Call with database-derived daily totals.
// Deliberately sends nothing on import. Do not bundle into Vite/React.
export async function sendDailyReport({date,metrics,observedAt=new Date().toISOString()},env=process.env,fetchImpl=fetch){
 const source=env.RVH_MARKETING_SOURCE,token=env.RVH_MARKETING_SYNC_TOKEN;
 if(!['clinic','mart','chat'].includes(source)||!token||token.length<32)throw Error('Set RVH_MARKETING_SOURCE and a unique RVH_MARKETING_SYNC_TOKEN on the server.');
 const base=env.RVH_MARKETING_URL||'https://rohit-veterinary-marketing.onrender.com';
 const url=new URL(base);if(url.protocol!=='https:'||url.username||url.password||url.search||url.hash||url.pathname!=='/')throw Error('RVH_MARKETING_URL must be an HTTPS origin.');
 const r=await fetchImpl(new URL(`/api/integrations/${source}/daily`,url),{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({schemaVersion:1,date,timezone:'Asia/Kolkata',observedAt,metrics}),redirect:'error',signal:AbortSignal.timeout(15000)});
 if(!r.ok)throw Error(`Marketing sync HTTP ${r.status}. Check connection configuration; do not log tokens or retry a stale report.`);
 return r.json();
}
