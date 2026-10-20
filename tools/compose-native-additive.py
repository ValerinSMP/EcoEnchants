"""Compose only the native damage/death increment over the pinned mining/five/requirement-view artifacts."""
from pathlib import Path
import ast,hashlib,json,subprocess,sys,zipfile
r=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1]);asm=sys.argv[2];tmp=w/'composition-additive';tmp.mkdir(exist_ok=True)
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
plans=[('eco',r.parent.parent/'artifacts/eco-suite/requirement-lore-20261020/eco-2026.28-all-requirement-views.jar','7565c0bcac30521dc6f2ba0a4ab3c448d1d7109196782767950c73753a99c0dc','eco-2026.28-native-additive-all.jar'),('LibreForge',w/'LibreForge-2026.27-native-mining-all.jar','b0a0ab399620a3d37b37c16a0ae33f5d20a3a533378a9bb68984b56435971d77','LibreForge-2026.27-native-additive-all.jar'),('EcoEnchants',w/'EcoEnchants-2026.27-native-five-all.jar','180e9bd974dd676602a6203895b07cd73d49b4a8249dd3b6715203692f26ec21','EcoEnchants-2026.27-native-additive-all.jar')]
reports=[]
for repo,path,sha,name in plans:
 assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
 before=read(path);changes={}
 if repo=='eco':
  classes=r/repo/'eco-core/core-plugin/build/classes/kotlin/main'
  prefix='com/willfp/eco/internal/spigot/eventlisteners/'
  n=prefix+'EntityDeathByEntityBuilder.class';fresh=(classes/n).read_bytes();changes[n]=transform('copy',before[n],fresh)
  n=prefix+'EntityDeathByEntityListeners.class';changes[n]=transform('methods:onEntityDamage,onEntityDeath',before[n],(classes/n).read_bytes())
 elif repo=='LibreForge':
  classes=r/repo/'core/common/build/classes/kotlin/main';prefix='com/willfp/libreforge/'
  selections={'effects/Effect.class':'trigger,onRegister$lambda$0','triggers/Trigger.class':'dispatchOnEffectsMeasured,enable$lambda$0','effects/impl/EffectDamageMultiplier.class':'onTrigger,arguments$lambda$0'}
  for relative,methods in selections.items():
   n=prefix+relative;changes[n]=transform('methods:'+methods,before[n],(classes/n).read_bytes())
  for token in ['AdditiveDamageGroups','DamageContributionSum','RepeatedDamage','EffectDamageTwice']:
   for f in (classes/(prefix+'effects/impl')).glob(token+'*.class'):
    n=str(f.relative_to(classes));fresh=f.read_bytes();changes[n]=transform('copy',before.get(n,empty),fresh)
 else:
  fresh=read(r/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
  tokens=['NativeNormalDamage','NativeCombatProgression','OriginalProgression']
  changes={n:b for n,b in fresh.items() if n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and any(n.split('/')[-1].split('$')[0] in [t,t+'.class'] for t in tokens)}
 after=before|changes
 assert all(after[n]==b for n,b in before.items() if n not in changes)
 if repo=='EcoEnchants':assert all(after[n]==b for n,b in before.items() if n.startswith('com/willfp/ecoenchants/api/'))
 out=w/name
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in sorted(after.items()):
   info=zipfile.ZipInfo(n,(2026,10,20,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
 reports.append(dict(repo=repo,file=name,sha256=hashlib.sha256(out.read_bytes()).hexdigest(),baseline=sha,changes=sorted(changes),installed=False,active=False,gameplayVerified=False))
(w/'additive-runtime-manifest.json').write_text(json.dumps(dict(increment=4,artifacts=reports,requiresUnchanged={'EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'},configOptIn='args.damage_group: ecoenchants_normal',runtimeTest=False),indent=2)+'\n');print(json.dumps(reports))
