"""Compose only the utility increment over the pinned mining/five/requirement-view artifacts."""
from pathlib import Path
import ast,hashlib,json,subprocess,sys,zipfile
r=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1]);asm=sys.argv[2];tmp=w/'composition-utility';tmp.mkdir(exist_ok=True)
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
plans=[('eco',w/'eco-2026.28-native-additive-all.jar','7c3f30330924719e89ff247f0d59c4063361f51f41d8830e9010aaafe8f2892e','eco-2026.28-native-utility-all.jar'),('LibreForge',w/'LibreForge-2026.27-native-additive-all.jar','ef6931f7bcc24df2a7f072e039a5f497f1c8e1d4c9999f4a4f2251f4ce7fb6bb','LibreForge-2026.27-native-utility-all.jar'),('EcoEnchants',w/'EcoEnchants-2026.27-native-additive-all.jar','78c8612c4cb1538ce9d8a909a12620a9fd98ac5d33380b5047cc8df3e0718501','EcoEnchants-2026.27-native-utility-all.jar')]
def remap(mode,before,fresh,source,target):
 (tmp/'before.class').write_bytes(before);(tmp/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(tmp)+':'+asm,'GraftViews',mode,str(tmp/'before.class'),str(tmp/'fresh.class'),str(tmp/'after.class'),source,target],check=True)
 return (tmp/'after.class').read_bytes()
reports=[]
for repo,path,sha,name in plans:
 assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
 before=read(path);changes={}
 if repo=='eco':
  api=r/repo/'eco-api/build/classes/java/main'
  for f in (api/'com/willfp/eco/core/entities').glob('BoundedLivingQueries*.class'):
   changes[str(f.relative_to(api))]=f.read_bytes()
  source='com/willfp/eco/internal/spigot/proxy/common/'
  common=r/repo/'eco-core/core-nms/common/build/classes/kotlin/main'
  setter='CommonsProvider$Companion.class'
  targets=[n[:-len(setter)] for n in before if n.endswith('/'+setter)]
  assert len(targets)==5,targets
  for target in targets:
   n=target+setter
   changes[n]=remap('methods:setIfNeeded',before[n],(common/(source+setter)).read_bytes(),source,target)
   for f in (common/source).glob('BoundedLivingQueriesBackend*.class'):
    n=target+f.name;changes[n]=remap('copy',before.get(n,empty),f.read_bytes(),source,target)
 elif repo=='LibreForge':
  n='com/willfp/libreforge/triggers/event/EditableXpDropEvent.class'
  changes[n]=transform('copy',before[n],(r/repo/'core/common/build/classes/kotlin/main'/n).read_bytes())
 else:
  fresh=read(r/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
  tokens=['NativeCombatProgression','OriginalProgression','OriginalRuntime','NativeUtilityProgressionKt','EffectDoubleMobXp','NativeMagicRecovery','NativeMagicRecoveryKt','ManaCredit','MagicWriteResult','FirstHitsLedger','FirstHitResolution','NativeFirstHits','EffectFirstHitsMultiplier','NativeSwarm','EffectSwarmMultiplier','SwarmQueryBudget']
  changes={n:b for n,b in fresh.items() if n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and n.split('/')[-1].split('$')[0].removesuffix('.class') in tokens}
 after=before|changes
 assert all(after[n]==b for n,b in before.items() if n not in changes)
 assert all(after[n]==b for n,b in before.items() if n.startswith(('com/willfp/ecoskills/api/','com/willfp/ecoenchants/api/')))
 out=w/name
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in sorted(after.items()):
   info=zipfile.ZipInfo(n,(2026,10,20,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
 reports.append(dict(repo=repo,file=name,sha256=hashlib.sha256(out.read_bytes()).hexdigest(),baseline=sha,changes=sorted(changes),installed=False,active=False,gameplayVerified=False))
(w/'utility-runtime-manifest.json').write_text(json.dumps(dict(increment=5,artifacts=reports,requiresUnchanged={'EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'},runtimeTest=False),indent=2)+'\n');print(json.dumps(reports))
