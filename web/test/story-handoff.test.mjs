import { test } from 'node:test';
import assert from 'node:assert/strict';
import { storyHandoff } from '../public/story-handoff.js';
const content = 'Internal note: confirm price.\n[VIDEO_BRIEF]Clinic introduction[/VIDEO_BRIEF]\n[POSTER_HEADLINE]RVH[/POSTER_HEADLINE]\n[POSTER_MESSAGE]Call for appointments[/POSTER_MESSAGE]\n[SOCIAL_CAPTION]Approved public caption[/SOCIAL_CAPTION]\n[WHATSAPP_MESSAGE]Hello. Reply STOP to opt out.[/WHATSAPP_MESSAGE]\nSource check — not public copy';
test('handoff extracts only public blocks from an approved story', () => {
  const result = storyHandoff({agent:'story',status:'approved',content});
  assert.deepEqual(result, {video:'Clinic introduction',poster:{headline:'RVH',message:'Call for appointments'},social:'Approved public caption',whatsapp:'Hello. Reply STOP to opt out.'});
});
test('unapproved, edited and non-story drafts cannot be handed off', () => {
  for (const status of ['draft','pending','failed']) assert.deepEqual(storyHandoff({agent:'story',status,content}),{});
  assert.deepEqual(storyHandoff({agent:'content',status:'approved',content}),{});
});
test('malformed, duplicate and oversized blocks are not silently copied or truncated', () => {
  for (const content of ['[SOCIAL_CAPTION]missing end','[/SOCIAL_CAPTION]reversed[SOCIAL_CAPTION]','[SOCIAL_CAPTION]one[/SOCIAL_CAPTION][SOCIAL_CAPTION]two[/SOCIAL_CAPTION]', '[SOCIAL_CAPTION]'+'x'.repeat(3001)+'[/SOCIAL_CAPTION]']) {
    assert.deepEqual(storyHandoff({agent:'story',status:'approved',content}),{});
  }
});
