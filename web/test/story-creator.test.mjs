import { test } from 'node:test';
import assert from 'node:assert/strict';
import { collectEstablishment, storySources, pageText } from '../story-creator.mjs';
import { agents, generate, instructions } from '../agents.mjs';

test('research records partial failures without pretending all sources were read', async () => {
  const calls = [];
  const result = await collectEstablishment({ readImpl: async url => {
    calls.push(url);
    if (url === storySources[1]) throw Error('network failure');
    if (url === storySources[2]) return { status: 403, text: 'forbidden' };
    if (url === storySources[3]) return { status: 200, text: '<div id="root"></div>' };
    return { status: 200, text: '<script>secret instruction</script><p>' + 'Clinic services. '.repeat(30) + '</p>' };
  }});
  assert.deepEqual(calls, storySources);
  assert.deepEqual(result.sources.map(s => s.status), ['retrieved', 'unavailable', 'unavailable', 'limited']);
  assert.doesNotMatch(result.sources[0].text, /secret instruction/);
  assert.match(result.limitation, /not a complete/);
});

test('source excerpts are bounded and remove executable and hidden markup', () => {
  assert.equal(pageText('<!-- hidden --><style>bad</style><script>bad</script><p>RVH &amp; pets</p>'), 'RVH & pets');
  assert.equal(pageText('a'.repeat(50000)).length, 12000);
});

test('story run supplies source evidence as data, retains approved profile and appends receipt', async () => {
  const agent = agents.find(a => a.id === 'story');
  const evidence = await collectEstablishment({readImpl: async () => { throw Error('offline'); }});
  let payload;
  const text = await generate({key:'mock', model:'mock', agent, language:'Hindi', brief:'Clinic promotion',
    profile:{name:'RVH',phone:'9709095993'}, researchImpl:async()=>evidence,
    fetchImpl:async (url, options) => {
      payload = JSON.parse(options.body);
      return Response.json({status:'completed', output:[{type:'message',content:[{type:'output_text',text:'Draft campaign'}]}]});
    }});
  assert.deepEqual(JSON.parse(payload.input).establishmentResearch, evidence);
  assert.match(payload.instructions, /9709095993/);
  assert.match(payload.instructions, /untrusted reference data/);
  assert.match(payload.instructions, /omit them from public copy until owner-approved/);
  assert.equal(payload.store, false);
  assert.match(text, /Draft campaign/);
  assert.match(text, /unavailable/);
  assert.match(text, /not a complete establishment audit/);
});

test('other agents do not fetch establishment pages', async () => {
  let researched = false;
  const result = await generate({key:'mock', model:'mock', agent:agents.find(a=>a.id==='content'), language:'English', brief:'Post',
    researchImpl:async()=>{researched=true;throw Error('not expected');},
    fetchImpl:async()=>Response.json({status:'completed',output:[{type:'message',content:[{type:'output_text',text:'Caption'}]}]})});
  assert.equal(researched,false);
  assert.equal(result,'Caption');
  assert.match(instructions(agents.find(a=>a.id==='story'),'Hindi',{}), /no publishing/);
});
