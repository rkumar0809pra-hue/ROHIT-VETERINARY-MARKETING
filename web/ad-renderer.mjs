import {mkdtemp,writeFile,readFile,rm,stat} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
import {brandVideo} from './video-branding.mjs';
const escape=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
async function run(ffmpeg,args){
 await new Promise((resolve,reject)=>{
  const child=spawn(ffmpeg,['-y','-filter_complex_threads','1',...args],{stdio:'ignore'});
  const timer=setTimeout(()=>child.kill('SIGKILL'),240000);
  child.on('error',e=>{clearTimeout(timer);reject(e);});child.on('close',code=>{clearTimeout(timer);code===0?resolve():reject(Error('Advertisement rendering failed. Check available disk space and video dependencies.'));});
 });
}
// Render one scene at a time to keep memory bounded on small Render instances.
export async function renderAdvertisement({scenes,profile,ratio,narration}){
 const {default:sharp}=await import('sharp'),{default:ffmpeg}=await import('ffmpeg-static');
 const dir=await mkdtemp(join(tmpdir(),'rvh-ad-'));
 const width=ratio==='16:9'?1280:720,height=ratio==='16:9'?720:1280;
 try{
  const outputs=[];
  for(const [i,s] of scenes.entries()){
   const overlay=join(dir,`caption-${i}.png`),output=join(dir,`scene-${i}.mp4`);
   const caption=await sharp({text:{text:`<span foreground="white">${escape(s.narration)}</span>`,font:'Noto Sans Devanagari',fontfile:fileURLToPath(new URL('./assets/NotoSansDevanagari.ttf',import.meta.url)),width:width-100,height:150,align:'centre',rgba:true}}).png().toBuffer();
   const captionMeta=await sharp(caption).metadata();
   const footer=await sharp({text:{text:`<span foreground="white">${escape(profile.phone)} · ${narration?'AI voice':'AI-assisted advertisement'}</span>`,font:'sans',width:width-100,height:30,align:'centre',rgba:true}}).png().toBuffer();
   await sharp({create:{width,height:240,channels:4,background:'#123c36'}}).composite([{input:caption,top:20,left:Math.round((width-captionMeta.width)/2)},{input:footer,top:190,left:Math.round((width-(await sharp(footer).metadata()).width)/2)}]).png().toFile(overlay);
   const args=[...(s.mode==='still'?['-loop','1','-framerate','24']:[]),'-i',s.file,'-loop','1','-framerate','24','-i',overlay];
   let speed=1;
   if(narration){
    const duration=(await stat(s.audioFile)).size/48000;
    speed=Math.max(1,duration/(s.duration-0.15));
    if(speed>1.8)throw Error(`Scene ${i+1} narration is too long. Create a new project with a shorter script, or assemble without narration.`);
    args.push('-f','s16le','-ar','24000','-ac','1','-i',s.audioFile);
   }else args.push('-f','lavfi','-i','anullsrc=r=24000:cl=mono');
   const filter=`[0:v]scale=${width}:${height-240}:force_original_aspect_ratio=decrease,pad=${width}:${height-240}:(ow-iw)/2:(oh-ih)/2:color=0x123c36,setsar=1,fps=24,tpad=stop_mode=clone:stop_duration=60[scene];[scene][1:v]vstack=inputs=2,format=yuv420p[v];[2:a]atempo=${speed.toFixed(5)},apad,atrim=duration=${s.duration},asetpts=PTS-STARTPTS[a]`;
   args.push('-filter_complex',filter,'-map','[v]','-map','[a]','-t',String(s.duration),'-c:v','libx264','-threads','1','-preset','veryfast','-crf','23','-c:a','aac','-ar','24000',output);
   await run(ffmpeg,args);outputs.push(output);
  }
  await writeFile(join(dir,'list.txt'),outputs.map(x=>`file '${x}'`).join('\n'));
  const joined=join(dir,'joined.mp4');
  await run(ffmpeg,['-f','concat','-safe','0','-i',join(dir,'list.txt'),'-c','copy','-movflags','+faststart',joined]);
  return await brandVideo(await readFile(joined),profile,ratio,scenes.reduce((n,s)=>n+s.duration,0));
 }finally{await rm(dir,{recursive:true,force:true});}
}
