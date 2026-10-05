import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';

test('specialist agent submit passes through Studio without referencing a click element',async()=>{
  const source=readFileSync(new URL('../public/studio-ui.js',import.meta.url),'utf8');
  // Execute the real module with browser imports stubbed; no paid request is made.
  const context=vm.createContext({
    discoverabilitySubmit:async()=>false, advertisementSubmit:async()=>false,
    document:{getElementById(){throw Error('Unexpected form field access');}},
  });
  vm.runInContext(source.replace(/^import .*;\n/gm,'').replace(/export /g,''),context);
  const result=await context.studioSubmit({id:'agent-form',dataset:{}},null,{state:{}});
  assert.equal(result,false);
});
