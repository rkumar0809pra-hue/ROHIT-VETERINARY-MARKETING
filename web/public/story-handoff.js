// Fail closed on absent, duplicate or oversized blocks; never truncate public copy.
export function storyHandoff(draft) {
  if (draft?.agent !== 'story' || !['approved','planned','published'].includes(draft.status)) return {};
  const content = String(draft.content || '');
  const block = (name, max) => {
    const open = `[${name}]`, close = `[/${name}]`;
    if (content.split(open).length !== 2 || content.split(close).length !== 2) return '';
    const start = content.indexOf(open) + open.length, end = content.indexOf(close);
    if (end < start) return '';
    const value = content.slice(start, end).trim();
    return value && value.length <= max && !/\[\/?[A-Z_]+\]/.test(value) ? value : '';
  };
  const result = {};
  const video = block('VIDEO_BRIEF', 1900), headline = block('POSTER_HEADLINE', 100), message = block('POSTER_MESSAGE', 350);
  const social = block('SOCIAL_CAPTION', 3000), whatsapp = block('WHATSAPP_MESSAGE', 3000);
  if (video) result.video = video;
  if (headline && message) result.poster = { headline, message };
  if (social) result.social = social;
  if (whatsapp) result.whatsapp = whatsapp;
  return result;
}
