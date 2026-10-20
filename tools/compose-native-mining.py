"""Compose executable mining increment on the verified requirement-view delivery. No lifecycle."""
from pathlib import Path
import ast,sys,subprocess,zipfile,json,hashlib
root=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1]);asm=sys.argv[2]
work=w/'composition-mining';work.mkdir(exist_ok=True)
source=ast.parse((root/'eco/tools/compose-requirement-views.py').read_text())
helper=next(ast.literal_eval(n.value) for n in source.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='java' for t in n.targets))
(work/'GraftViews.java').write_text(helper)
subprocess.run(['javac','--release','21','-cp',asm,'-d',str(work),str(work/'GraftViews.java')],check=True)
def read(path):
 with zipfile.ZipFile(path) as z:return {n:z.read(n) for n in z.namelist()}
def patch(before,fresh):
 (work/'before.class').write_bytes(before);(work/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(work)+':'+asm,'GraftViews','methods:onEnable',str(work/'before.class'),str(work/'fresh.class'),str(work/'after.class'),'-','-'],check=True)
 return (work/'after.class').read_bytes()
base=root.parent.parent/'artifacts/eco-suite/requirement-lore-20261020'
reports=[]
for repo,filename,sha in [('LibreForge','libreforge-2026.27-requirement-views.jar','eef18801e17ef51bc72564c8aa9692b91870fcae6430465e8e8bf24ace51ee06'),('EcoEnchants','EcoEnchants-2026.27-all-requirement-views.jar','88d48c9717559a23170afcb14ae48ca4e7ff93707881a33a9e9ac7bc3dc6a383')]:
 original=base/filename;assert hashlib.sha256(original.read_bytes()).hexdigest()==sha
 before=read(original)
 if repo=='LibreForge':
  name='com/willfp/libreforge/effects/templates/AttributeEffect.class'
  fresh=(root/repo/'core/common/build/classes/kotlin/main'/name).read_bytes()
  changes={name:patch(before[name],fresh)}
 else:
  fresh=read(root/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
  tokens=['NativeMiningProgression','ManualCropGuard','EffectGuardImmature','ConditionMiningRhythm','MiningRhythmState','OriginalProgression']
  changes={n:b for n,b in fresh.items() if n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and any(n.split('/')[-1].startswith(t) for t in tokens)}
  for id in ['cuidado','ritmo_minero']:changes['enchants/'+id+'.yml']=fresh['enchants/'+id+'.yml']
 after=before|changes;out=w/(repo+'-2026.27-native-mining-all.jar')
 assert all(after[n]==b for n,b in before.items() if n not in changes)
 assert all(after[n]==b for n,b in before.items() if n.startswith('com/willfp/ecoenchants/api/'))
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in after.items():z.writestr(n,b)
 reports.append({'file':out.name,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'baseline':sha,'changes':sorted(changes),'installed':False,'gameplayVerified':False})
(w/'mining-runtime-manifest.json').write_text(json.dumps({'increment':1,'families':['cuidado','ritmo_minero'],'artifacts':reports,'requiresUnchanged':{'eco':'7565c0bcac30521dc6f2ba0a4ab3c448d1d7109196782767950c73753a99c0dc','EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'}},indent=2)+'\n')
print(json.dumps(reports))
