import https from 'node:https';
import { lookup } from 'node:dns';
import { isIP } from 'node:net';

// Deliberately limited to the clinic's public websites. Never fetch arbitrary
// URLs, authenticated pages, or a URL supplied by an AI response.
export const SCAN_HOSTS = ['rohitveterinary.com', 'www.rohitveterinary.com', 'app.rohitveterinary.com', 'mart.rohitveterinary.com'];
export function scanUrl(value) {
  const u = new URL(value.includes('://') ? value : `https://${value}`);
  if (u.protocol !== 'https:' || u.username || u.password || u.port || !SCAN_HOSTS.includes(u.hostname) || u.search || u.hash)
    throw Error('Choose a public HTTPS page on rohitveterinary.com, app.rohitveterinary.com or mart.rohitveterinary.com, without query parameters.');
  return u;
}
export function publicIPv4(address) {
  if (isIP(address) !== 4) return false;
  const [a,b] = address.split('.').map(Number);
  return !(a===0 || a===10 || a===127 || a>=224 || (a===169&&b===254) || (a===172&&b>=16&&b<=31) || (a===192&&(b===168||b===0)) || (a===100&&b>=64&&b<=127) || (a===198&&(b===18||b===19)));
}
export function readPublicPage(value, redirects=0) {
  const url=scanUrl(value);
  return new Promise((resolve,reject)=>{
    const request=https.get(url,{headers:{'User-Agent':'RVH-Website-Check/1.0','Accept':'text/html,text/plain,application/xml','Accept-Encoding':'identity'},lookup(host,options,cb){
      lookup(host,{family:4},(err,address,family)=>{
        if(err)return cb(err);
        if(!publicIPv4(address))return cb(Error('Website resolves to a private or unsupported address.'));
        // Pin the validated address to this connection; no second DNS lookup.
        cb(null, options.all ? [{address,family}] : address, family);
      });
    }},res=>{
      if([301,302,303,307,308].includes(res.statusCode)) {
        res.resume(); clearTimeout(timer);
        if(redirects>=3)return reject(Error('Too many website redirects.'));
        try { const next=scanUrl(new URL(res.headers.location,url).href); resolve(readPublicPage(next.href,redirects+1)); } catch(e){reject(e);} return;
      }
      let length=0; const chunks=[];
      res.on('data',chunk=>{length+=chunk.length;if(length>1024*1024){request.destroy(Error('Page is larger than the 1 MB scan limit.'));return;}chunks.push(chunk);});
      res.on('end',()=>{clearTimeout(timer);resolve({url:url.href,status:res.statusCode,headers:res.headers,text:Buffer.concat(chunks).toString('utf8')});});
      res.on('error',reject);
    });
    const timer=setTimeout(()=>request.destroy(Error('Website check timed out.')),12000);
    request.on('error',err=>{clearTimeout(timer);reject(err);});
  });
}
const clean=s=>(s||'').replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim();
function attr(tag,name){return new RegExp(`\\b${name}\\s*=\\s*(?:"([^"]*)"|'([^']*)'|([^\\s>]+))`,'i').exec(tag)?.slice(1).find(x=>x!==undefined)||'';}
export function analysePage(page,profile) {
  const html=page.text, tags=html.match(/<meta\b[^>]*>/gi)||[];
  const meta=name=>attr(tags.find(t=>attr(t,'name').toLowerCase()===name)||'','content');
  const title=clean(/<title\b[^>]*>([\s\S]*?)<\/title>/i.exec(html)?.[1]);
  const description=meta('description'), h1=(html.match(/<h1\b[^>]*>[\s\S]*?<\/h1>/gi)||[]).map(clean);
  const canonical=attr((html.match(/<link\b[^>]*>/gi)||[]).find(t=>attr(t,'rel').toLowerCase()==='canonical')||'','href');
  const robots=[meta('robots'),page.headers?.['x-robots-tag']||''].join(' ');
  const viewport=meta('viewport');
  const text=clean(html.replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi,''));
  const phone=String(profile.phone||'').replace(/\D/g,'');
  const images=html.match(/<img\b[^>]*>/gi)||[], missingAlt=images.filter(t=>! /\balt\s*=/i.test(t)).length;
  const interactive=html.match(/<(?:a|button)\b[^>]*>[\s\S]*?<\/(?:a|button)>/gi)||[];
  const actionText=interactive.map(clean).filter(Boolean).join(' · ');
  const primaryAction=/\b(add to cart|buy now|book|appointment|call|whatsapp|contact|share|copy link|notify me|directions?)\b/i.test(actionText);
  let current;try{current=new URL(page.url);}catch{current=null;}
  const links=(html.match(/<a\b[^>]*>/gi)||[]).map(t=>attr(t,'href')).filter(Boolean);
  const internalLinks=links.filter(href=>{try{const u=new URL(href,page.url);return current&&u.protocol==='https:'&&u.hostname===current.hostname;}catch{return false;}}).length;
  const productPage=Boolean(current&&current.hostname==='mart.rohitveterinary.com'&&/^\/product\/[^/]+\/?$/.test(current.pathname));
  const rows=[
    ['Page response',page.status===200,`HTTP ${page.status}`,'Make this public page return HTTP 200.'],
    ['Page title',Boolean(title),title||'Missing','Add a clear page title including the service and location.'],
    ['Search description',Boolean(description),description||'Missing','Write a helpful description of this page.'],
    ['Main heading',h1.length===1,h1.join(' · ')||'Missing','Use one clear main heading.'],
    ['Canonical link',Boolean(canonical),canonical||'Missing','Set the preferred public URL for this page.'],
    ['Indexing directive',! /\bnoindex\b/i.test(robots),robots.trim()||'No noindex directive found','Review noindex only if this page should be public in search. Keep private portal pages private.'],
    ['Phone in page text',Boolean(phone)&&text.replace(/\D/g,'').includes(phone),'Clinic contact in the initial HTML','Add the clinic phone to the visible public contact section.'],
    ['Image alt attributes',missingAlt===0,`${missingAlt} of ${images.length} images missing alt`,'Add descriptive alt text to meaningful images; leave decorative alt text empty.'],
    ['Structured data',/<script\b[^>]*type\s*=\s*["']application\/ld\+json["']/i.test(html),'JSON-LD presence only; validity not checked','Consider accurate structured data matching the visible page.'],
    ['Readable page content',text.length>250,`${text.length} characters in initial HTML`,'Provide helpful public service and contact information in crawlable text.'],
    ['Mobile viewport',/width\s*=\s*device-width/i.test(viewport),viewport||'Missing','Add a responsive viewport meta tag for mobile visitors.'],
    ['Primary action',primaryAction,actionText.slice(0,300)||'No clear action found','Make the main visitor action clear, such as Add to cart, Book, Call or WhatsApp.'],
    ['Internal navigation',internalLinks>0,`${internalLinks} same-site links found`,'Add a clear same-site path to related products, services, categories or the next step.'],
  ];
  if(productPage){
    rows.push(
      ['Product price clarity',/(?:₹\s*\d|\bINR\s*\d|\bprice on request\b)/i.test(text),'Visible price or price-on-request text in initial HTML','Show the current selling price, or clearly state Price on request, in crawlable visible text.'],
      ['Product availability clarity',/\b(?:in stock|out of stock|available|availability|notify me when back in stock|stock)\b/i.test(text),'Visible availability/stock text in initial HTML','Show whether the product is in stock, out of stock or otherwise available.'],
    );
  }
  const checks=rows.map(([label,passed,observed,action])=>({label,passed,observed,action}));
  return {url:page.url,checkedAt:new Date().toISOString(),checks,limitations:'Single-page initial HTML check, not a rendered-browser audit. SXO signals here cover crawlable page experience basics only; they do not measure rendered layout, Core Web Vitals, checkout completion, accessibility testing or Google ranking/indexing. Login pages may intentionally be private.'};
}
export async function scanWebsite({url,profile,readImpl=readPublicPage}) { return analysePage(await readImpl(scanUrl(url).href),profile); }
