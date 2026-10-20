"""Compose only the reactive increment over the pinned mining/five/requirement-view artifacts."""
from pathlib import Path
import ast,hashlib,json,subprocess,sys,zipfile
r=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1]);asm=sys.argv[2];tmp=w/'composition-reactive';tmp.mkdir(exist_ok=True)
tree=ast.parse((r/'eco/tools/compose-requirement-views.py').read_text())
helper=next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='java' for t in n.targets))
(tmp/'GraftViews.java').write_text(helper);subprocess.run(['javac','--release','21','-cp',asm,'-d',str(tmp),str(tmp/'GraftViews.java')],check=True)
(tmp/'EmptyGraft.java').write_text('final class EmptyGraft { private EmptyGraft(){} }')
subprocess.run(['javac','--release','21','-d',str(tmp),str(tmp/'EmptyGraft.java')],check=True)
empty=(tmp/'EmptyGraft.class').read_bytes()
def read(p):
 with zipfile.ZipFile(p) as z:return {n:z.read(n) for n in z.namelist()}
def transform(mode,before,fresh):
 (tmp/'before.class').write_bytes(before);(tmp/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(tmp)+':'+asm,'GraftViews',mode,str(tmp/'before.class'),str(tmp/'fresh.class'),str(tmp/'after.class'),'-','-'],check=True)
 return (tmp/'after.class').read_bytes()
plans=[('EcoEnchants',w/'EcoEnchants-2026.27-native-bursts-all.jar','b98f62cf5266680c37d3fa493954d2e2711145f87fdfc59a415d061117f001f0','EcoEnchants-2026.27-native-reactive-all.jar')]
def remap(mode,before,fresh,source,target):
 (tmp/'before.class').write_bytes(before);(tmp/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(tmp)+':'+asm,'GraftViews',mode,str(tmp/'before.class'),str(tmp/'fresh.class'),str(tmp/'after.class'),source,target],check=True)
 return (tmp/'after.class').read_bytes()
reports=[]
for repo,path,sha,name in plans:
 assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
 before=read(path);changes={}
 fresh=read(r/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
 tokens=['OriginalProgression','OriginalRuntime','OriginalProcEngine','ProcGuards','NativeBurstProgression','NativeBurstProgressionKt','NativeReactiveProgression','NativeReactiveProgressionKt','NativeReactiveAction','EffectNativeReactive','ArmedProcLedger','NativeFirstHits']
 changes={n:b for n,b in fresh.items() if n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and n.split('/')[-1].split('$')[0].removesuffix('.class') in tokens}
 after=before|changes
 assert all(after[n]==b for n,b in before.items() if n not in changes)
 assert all(after[n]==b for n,b in before.items() if n.startswith(('com/willfp/ecoskills/api/','com/willfp/ecoenchants/api/')))
 out=w/name
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in sorted(after.items()):
   info=zipfile.ZipInfo(n,(2026,10,20,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
 reports.append(dict(repo=repo,file=name,sha256=hashlib.sha256(out.read_bytes()).hexdigest(),baseline=sha,changes=sorted(changes),installed=False,active=False,gameplayVerified=False))
(w/'reactive-runtime-manifest.json').write_text(json.dumps(dict(increment=9,artifacts=reports,requiresUnchanged={'LibreForge':'6616d2de528d906f7ce13a8709dfe42892afb86b9f893bd67e6f6543329204e2','eco':'5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f','EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'},runtimeTest=False),indent=2)+'\n');print(json.dumps(reports))
