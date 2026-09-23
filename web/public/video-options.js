export const presenters={female:'Female presenter',male:'Male presenter',none:'No presenter — animals / clinic only',custom:'Custom presenter description'};
export const voices={coral:'Coral',nova:'Nova',shimmer:'Shimmer',onyx:'Onyx',echo:'Echo',sage:'Sage'};
export const voiceStyles={warm:'Warm and caring',professional:'Clear and professional',cinematic:'Cinematic narrator',energetic:'Energetic commercial',storyteller:'Gentle storyteller'};
export const styleInstructions={warm:'Warm, reassuring and caring delivery.',professional:'Clear, composed professional delivery.',cinematic:'Rich cinematic narration with measured emphasis. Keep pauses short.',energetic:'Bright, energetic advertising delivery, clear and not rushed.',storyteller:'Conversational storytelling with gentle expression.'};
export function presenterDirection(type='custom',notes=''){
 if(type==='none')return 'No human presenter. Focus on animals, products and clinic visuals. Do not include a human host.';
 const base={female:'An original adult Indian female presenter.',male:'An original adult Indian male presenter.',none:'No human presenter. Focus on animals, products and clinic visuals.',custom:'An original adult presenter if requested in the appearance notes.'}[type]||'';
 return `${base} ${notes} Keep appearance and clothing consistent between scenes. Use an original fictional presenter; do not imply celebrity endorsement.`;
}
