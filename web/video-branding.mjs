import {mkdtemp,writeFile,readFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
const escape=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
export async function brandVideo(bytes,profile,ratio,duration){
 const {default:sharp}=await import('sharp');
 const {default:ffmpeg}=await import('ffmpeg-static');
 const dir=await mkdtemp(join(tmpdir(),'rvh-brand-'));
 try{
  const width=ratio==='16:9'?1280:720,height=ratio==='16:9'?720:1280;
  const lines=[profile.hindiName,profile.name,profile.phone,profile.address,profile.bookingUrl||'पशु स्वास्थ्य परामर्श के लिए कॉल करें'];
  const markup=lines.map((s,i)=>`<span foreground="${i===2?'#efc474':'#ffffff'}" size="${i===2?36000:i===0?30000:18000}">${escape(s)}</span>`).join('\n\n');
  const text=await sharp({text:{text:markup,font:'Noto Sans Devanagari',fontfile:fileURLToPath(new URL('./assets/NotoSansDevanagari.ttf',import.meta.url)),width:width-100,height:height-160,align:'centre',rgba:true}}).png().toBuffer();
  await sharp({create:{width,height,channels:4,background:'#123c36'}}).composite([{input:text,gravity:'centre'}]).png().toFile(join(dir,'card.png'));
  await writeFile(join(dir,'source.mp4'),bytes);
  await new Promise((resolve,reject)=>{
   const child=spawn(ffmpeg,['-y','-i',join(dir,'source.mp4'),'-loop','1','-framerate','24','-t','4','-i',join(dir,'card.png'),'-filter_complex',`[0:v]scale=${width}:${height}:force_original_aspect_ratio=decrease,pad=${width}:${height}:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=24,format=yuv420p[v0];[1:v]setsar=1,format=yuv420p[v1];[v0][v1]concat=n=2:v=1:a=0[v]`,'-map','[v]','-map','0:a?','-af','apad','-t',String(duration+4),'-c:v','libx264','-threads','1','-preset','veryfast','-crf','22','-c:a','aac','-movflags','+faststart',join(dir,'branded.mp4')],{stdio:'ignore'});
   const timeout=setTimeout(()=>child.kill('SIGKILL'),180000);
   child.on('error',e=>{clearTimeout(timeout);reject(e);});child.on('close',code=>{clearTimeout(timeout);code===0?resolve():reject(Error('Video branding failed'));});
  });
  return await readFile(join(dir,'branded.mp4'));
 }finally{await rm(dir,{recursive:true,force:true});}
}
