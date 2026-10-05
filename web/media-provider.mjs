import {voices,styleInstructions} from './public/video-options.js';
const base = 'https://generativelanguage.googleapis.com/v1beta';
export class ProviderError extends Error { constructor(message, terminal=false){super(message);this.terminal=terminal;} }
async function checkedJson(response, provider) {
  if (!response.ok) {
    await response.body?.cancel();
    throw new ProviderError(`${provider} returned HTTP ${response.status}. Check API access, billing, model and quota in Settings.`);
  }
  return response.json();
}
export function mediaProvider(env, fetchImpl = fetch) {
  const googleHeaders = { 'x-goog-api-key': env.GEMINI_API_KEY || '', 'Content-Type': 'application/json' };
  return {
    async speech({text, language, voice='coral',style='warm'}) {
      if(!Object.hasOwn(voices,voice)||!Object.hasOwn(styleInstructions,style))throw new ProviderError('Choose a supported voice and delivery style.');
      const response = await fetchImpl('https://api.openai.com/v1/audio/speech', {
        method:'POST', headers:{Authorization:`Bearer ${env.OPENAI_API_KEY}`,'Content-Type':'application/json'},
        body:JSON.stringify({model:env.OPENAI_TTS_MODEL||'gpt-4o-mini-tts',voice,input:text,response_format:'pcm',instructions:`Read only the supplied text in ${language}. ${styleInstructions[style]} Clear Indian veterinary advertisement narration. Use the selected built-in voice; do not impersonate a real person. Concise natural delivery. Do not add words.`}),
        signal:AbortSignal.timeout(90000),
      });
      if(!response.ok) {await response.body?.cancel();throw new ProviderError(`Narration returned HTTP ${response.status}. Check OpenAI billing and speech model access.`);}
      const chunks=[];let size=0;
      for await(const chunk of response.body){size+=chunk.length;if(size>4*1024*1024)throw new ProviderError('Narration is too long. Shorten the scene script.');chunks.push(chunk);}
      if(size<4800||size%2)throw new ProviderError('Narration returned invalid audio.');
      return Buffer.concat(chunks);
    },
    async startVideo({ prompt, ratio, duration, image }) {
      const model = env.VEO_MODEL || 'veo-3.1-fast-generate-preview';
      if (!/^veo-[a-zA-Z0-9.-]+$/.test(model)) throw new ProviderError('Invalid Veo model configuration.');
      const instance = { prompt };
      if (image) instance.image = { bytesBase64Encoded: image.bytes.toString('base64'), mimeType: image.mime };
      const data = await checkedJson(await fetchImpl(`${base}/models/${model}:predictLongRunning`, {
        method: 'POST', headers: googleHeaders,
        body: JSON.stringify({ instances: [instance], parameters: { aspectRatio: ratio, durationSeconds: duration, resolution: '720p', sampleCount: 1 } }),
        signal: AbortSignal.timeout(60000),
      }), 'Veo');
      if (!data.name || !/^(?:models\/[\w.-]+\/)?operations\/[\w.-]+$/.test(data.name)) throw new ProviderError('Veo did not return a valid generation operation. No video is marked complete.');
      return data.name;
    },
    async pollVideo(operation) {
      if (!/^(?:models\/[\w.-]+\/)?operations\/[\w.-]+$/.test(operation)) throw new ProviderError('Invalid saved video operation.');
      const data = await checkedJson(await fetchImpl(`${base}/${operation}`, { headers: googleHeaders, signal: AbortSignal.timeout(30000) }), 'Veo');
      if (data.error) throw new ProviderError('Veo could not complete this video. Review your prompt, model access and Google quota.',true);
      if (!data.done) return null;
      const result = data.response?.generateVideoResponse;
      const uri = result?.generatedSamples?.[0]?.video?.uri;
      if (!uri) throw new ProviderError('Veo returned no video, possibly due to a content filter. Revise the prompt; no sample video was substituted.',true);
      return uri;
    },
    async downloadVideo(uri) {
      // Only the provider-supplied Google download URI is accepted. Never forward a key to a redirect host.
      let url = new URL(uri);
      if (url.protocol !== 'https:' || url.hostname !== 'generativelanguage.googleapis.com' || url.port || url.username || url.password) throw new ProviderError('Unexpected video download address.');
      let response;
      for (let redirects = 0; redirects <= 4; redirects++) {
        response = await fetchImpl(url, { redirect: 'manual', headers: url.hostname === 'generativelanguage.googleapis.com' ? { 'x-goog-api-key': env.GEMINI_API_KEY } : {}, signal: AbortSignal.timeout(60000) });
        if (![301,302,303,307,308].includes(response.status)) break;
        const location = response.headers.get('location');
        await response.body?.cancel();
        if (!location) throw new ProviderError('Video download redirect missing.');
        url = new URL(location, url);
        if (url.protocol !== 'https:' || url.port || url.username || url.password || !['generativelanguage.googleapis.com','storage.googleapis.com'].includes(url.hostname) && !url.hostname.endsWith('.googleusercontent.com') && !url.hostname.endsWith('.storage.googleapis.com')) throw new ProviderError('Unexpected video download redirect.');
      }
      if (!response.ok) { await response.body?.cancel(); throw new ProviderError('Video download failed. Check status again before the provider copy expires.'); }
      const parts = []; let length = 0;
      for await (const chunk of response.body) {
        length += chunk.length;
        if (length > 64 * 1024 * 1024) throw new ProviderError('Video exceeds the 64 MB workspace download limit.');
        parts.push(chunk);
      }
      const bytes = Buffer.concat(parts);
      if (bytes.length < 12 || bytes.toString('ascii',4,8) !== 'ftyp') throw new ProviderError('Provider returned an invalid MP4 file.');
      return bytes;
    },
    async image({ prompt, ratio }) {
      const size = ratio === '9:16' ? '1024x1536' : ratio === '16:9' ? '1536x1024' : '1024x1024';
      const result = await checkedJson(await fetchImpl('https://api.openai.com/v1/images/generations', {
        method: 'POST', headers: { Authorization: `Bearer ${env.OPENAI_API_KEY}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ model: env.OPENAI_IMAGE_MODEL || 'gpt-image-1', prompt, n: 1, size, quality: 'medium', output_format: 'png' }),
        signal: AbortSignal.timeout(180000),
      }), 'Image generation');
      const encoded = result.data?.[0]?.b64_json;
      if (typeof encoded !== 'string' || encoded.length > 28 * 1024 * 1024) throw new ProviderError('Image provider returned no usable image.');
      const bytes = Buffer.from(encoded, 'base64');
      if (bytes.subarray(0,8).toString('hex') !== '89504e470d0a1a0a') throw new ProviderError('Image provider returned an invalid PNG.');
      return bytes;
    },
  };
}
