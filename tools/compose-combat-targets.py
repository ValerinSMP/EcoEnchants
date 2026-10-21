"""Only reviewed owned class roots over hash-pinned Dev artifacts. No deployment."""
from pathlib import Path
import ast,hashlib,json,subprocess,sys,zipfile
root=Path('/root/development/projects/eco-suite');out=Path(sys.argv[1]);asm=sys.argv[2]
out.mkdir(parents=True,exist_ok=True);tmp=out/'composition';tmp.mkdir(exist_ok=True)
tree=ast.parse((root/'eco/tools/compose-requirement-views.py').read_text())
helper=next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='java' for t in n.targets))
(tmp/'GraftViews.java').write_text(helper)
subprocess.run(['javac','--release','21','-cp',asm,'-d',str(tmp),str(tmp/'GraftViews.java')],check=True)
(tmp/'EmptyGraft.java').write_text('final class EmptyGraft { private EmptyGraft(){} }')
subprocess.run(['javac','--release','21','-d',str(tmp),str(tmp/'EmptyGraft.java')],check=True)
empty=(tmp/'EmptyGraft.class').read_bytes()
def read(p):
 with zipfile.ZipFile(p) as z:return {n:z.read(n) for n in z.namelist()}
def transform(before,fresh):
 (tmp/'before.class').write_bytes(before);(tmp/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(tmp)+':'+asm,'GraftViews','copy',str(tmp/'before.class'),str(tmp/'fresh.class'),str(tmp/'after.class'),'dev/romainguy/kotlin/math/','com/willfp/libreforge/libs/math/'],check=True,stdout=subprocess.DEVNULL)
 return (tmp/'after.class').read_bytes()
lfroots={'CombatTargets','CombatTargetHolder','DamageUtilsKt','Effect','ElementLike','TriggerData','EffectAOE','EffectBleed','EffectChainLightning','EffectCreateExplosion','EffectDamageNearbyEntities','EffectFrostNova','EffectSetVelocity','EffectSetVictimVelocity','EffectShockwave','EffectSoulRip','EffectSpawnPotionCloud','EffectStrikeLightning','EffectStun','EffectSwarm','EffectTimeBomb','EffectVortex'}
eeroots={'CombatTargetPolicy','EcoEnchantBase','EcoEnchantLevel','FilterEnchantCombatMode','OriginalRuntime'}
base=Path('/var/lib/pterodactyl/volumes/48d37562-51f7-48c7-812b-b295df2e8aeb/plugins')
plans=[('LibreForge',base/'libreforge-2026.27.jar','cc3a4c063e8f286aba82cf7469756d079c18b3ac245bd50e912840516bb25426','LibreForge-2026.27-combat-targets.jar',lfroots),('EcoEnchants',base/'EcoEnchants-2026.27-all.jar','78eff168d845d1bf6ab2fb1709f9da33da2564833388ca82da6b187e9c9d3e50','EcoEnchants-2026.27-combat-targets-all.jar',eeroots)]
reports=[]
for repo,path,sha,name,roots in plans:
 assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
 before=read(path);changes={}
 if repo=='LibreForge':
  classes=root/repo/'core/common/build/classes/kotlin/main'
  for f in (classes/'com/willfp/libreforge').rglob('*.class'):
   if f.name.split('$')[0].removesuffix('.class') in roots:
    n=str(f.relative_to(classes));changes[n]=transform(before.get(n,empty),f.read_bytes())
 else:
  fresh=read(root/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
  changes={n:b for n,b in fresh.items() if n.endswith('.class') and n.startswith('com/willfp/ecoenchants/') and n.rsplit('/',1)[-1].split('$')[0].removesuffix('.class') in roots}
 assert roots <= {n.rsplit('/',1)[-1].split('$')[0].removesuffix('.class') for n in changes}, (repo,'missing root')
 selected=lambda n:n.endswith('.class') and n.startswith('com/willfp/') and n.rsplit('/',1)[-1].split('$')[0].removesuffix('.class') in roots
 removed={n for n in before if selected(n) and n not in changes}
 after={n:b for n,b in before.items() if n not in removed}|changes
 assert all(after[n]==b for n,b in before.items() if n not in changes and n not in removed)
 assert all(after[n]==b for n,b in before.items() if n.startswith(('com/willfp/ecoenchants/api/','com/willfp/libreforge/api/','enchants/')) or not n.endswith('.class'))
 assert all(after.get(n)==b for n,b in before.items() if 'ContextualAccumulator' in n or '/counters/bind/BoundCounter' in n or n.startswith('com/willfp/ecoenchants/target/'))
 artifact=out/name
 with zipfile.ZipFile(artifact,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in sorted(after.items()):
   info=zipfile.ZipInfo(n,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
 reports.append(dict(repo=repo,file=name,sha256=hashlib.sha256(artifact.read_bytes()).hexdigest(),baseline=sha,changes=sorted(n for n,b in changes.items() if before.get(n)!=b),removedObsoleteNestedClasses=sorted(removed),installed=False,active=False,gameplayVerified=False))
(out/'runtime-manifest.json').write_text(json.dumps(dict(artifacts=reports,requiresUnchanged={'eco':'5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f','EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'}),indent=2)+'\n')
print(json.dumps(reports))
