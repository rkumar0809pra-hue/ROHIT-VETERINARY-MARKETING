import {test} from 'node:test';
import assert from 'node:assert/strict';
import {scanUrl,publicIPv4,analysePage,scanWebsite} from '../website-scan.mjs';
import {checkOnline} from '../discoverability.mjs';

test('scanner rejects off-site, credentials, ports, queries and internal addresses before network access',async()=>{
 for(const url of ['http://rohitveterinary.com/','https://rohitveterinary.com:8443','https://rohitveterinary.com.evil.com','https://user:pass@rohitveterinary.com','https://127.0.0.1','https://rohitveterinary.com/?token=secret','https://[::1]'])assert.throws(()=>scanUrl(url));
 for(const address of ['127.0.0.1','10.0.0.1','172.16.1.1','192.168.1.1','169.254.169.254','100.64.1.1','::1','::ffff:127.0.0.1'])assert.equal(publicIPv4(address),false,address);
 assert.equal(publicIPv4('8.8.8.8'),true);
 let calls=0;await assert.rejects(scanWebsite({url:'https://evil.com',profile:{},readImpl:async()=>{calls++;}}));assert.equal(calls,0);
});

test('initial HTML audit reports missing fields and respects noindex header',()=>{
 const scan=analysePage({url:'https://rohitveterinary.com/',status:200,headers:{'x-robots-tag':'noindex'},text:'<html><title>Clinic</title><meta name="description" content="Our clinic"><h1>Our clinic</h1><p>9709095993</p><img src="dog.jpg"></html>'},{phone:'9709095993'});
 assert.equal(scan.checks.find(x=>x.label==='Page title').passed,true);
 assert.equal(scan.checks.find(x=>x.label==='Search description').observed,'Our clinic');
 assert.equal(scan.checks.find(x=>x.label==='Indexing directive').passed,false);
 assert.equal(scan.checks.find(x=>x.label==='Phone in page text').passed,true);
 assert.equal(scan.checks.find(x=>x.label==='Image alt attributes').passed,false);
 assert.match(scan.limitations,/not a rendered-browser audit/);
});

test('web research retains source evidence and never renders unsafe annotation URLs',async()=>{
 const r=await checkOnline({key:'mock',model:'mock',prompt:'Research clinic',fetchImpl:async()=>Response.json({status:'completed',output:[{type:'message',content:[{type:'output_text',text:'Found a page',annotations:[{type:'url_citation',url:'https://rohitveterinary.com',title:'Clinic'},{type:'url_citation',url:'javascript:alert(1)',title:'Bad'}]}]}]})});
 assert.equal(r.sources.length,1);assert.equal(r.text,'Found a page');
});
