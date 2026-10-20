"""Compose reviewed classes only; preserve runtime assets, loader, adapters, SDK and foreign work."""
from pathlib import Path
import ast,hashlib,json,subprocess,sys,zipfile
r=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1]);asm=sys.argv[2];tmp=w/'composition-consolidated';tmp.mkdir(exist_ok=True)
tree=ast.parse((r/'eco/tools/compose-requirement-views.py').read_text())
helper=next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='java' for t in n.targets))
(tmp/'GraftViews.java').write_text(helper);subprocess.run(['javac','--release','21','-cp',asm,'-d',str(tmp),str(tmp/'GraftViews.java')],check=True)
(tmp/'EmptyGraft.java').write_text('final class EmptyGraft { private EmptyGraft(){} }')
subprocess.run(['javac','--release','21','-d',str(tmp),str(tmp/'EmptyGraft.java')],check=True)
empty=(tmp/'EmptyGraft.class').read_bytes()
def read(p):
 with zipfile.ZipFile(p) as z:return {n:z.read(n) for n in z.namelist()}
def transform(before,fresh):
 (tmp/'before.class').write_bytes(before);(tmp/'fresh.class').write_bytes(fresh)
 subprocess.run(['java','-Xmx128m','-cp',str(tmp)+':'+asm,'GraftViews','copy',str(tmp/'before.class'),str(tmp/'fresh.class'),str(tmp/'after.class'),'-','-'],check=True)
 return (tmp/'after.class').read_bytes()
plans=[('LibreForge',w/'LibreForge-2026.27-native-bursts-all.jar','6616d2de528d906f7ce13a8709dfe42892afb86b9f893bd67e6f6543329204e2','LibreForge-2026.27-native-consolidated-all.jar'),('EcoEnchants',w/'EcoEnchants-2026.27-native-indexed-all.jar','07b96ff165da2e957902e1e9cd7086a423f1605b70217808a1717a6c5601651a','EcoEnchants-2026.27-native-consolidated-all.jar')]
reports=[]
for repo,path,sha,name in plans:
 assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
 before=read(path);changes={}
 if repo=='LibreForge':
  classes=r/repo/'core/common/build/classes/kotlin/main'
  for token in ['EffectRapidBows','EffectDamageVictim','TriggerShieldBlock','TriggerKill']:
   for f in classes.rglob(token+'*.class'):
    n=str(f.relative_to(classes));changes[n]=transform(before.get(n,empty),f.read_bytes())
 else:
  fresh=read(r/repo/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
  tokens=['OriginalProgression','OriginalProcEffect','OriginalRuntime','OriginalProcEngine','ProcGuards','NativeBurstProgression','EffectNativeBurst','NativeReactiveProgression','NativeCombatProgression','FilterEnchantCombatMode','FilterGenuineKill','NativeUtilityProgression','NativeUtilityProgressionKt','EffectDoubleMobXp','NativeFirstHits','FirstHitsLedger','EffectThirdHitMultiplier','EffectRecentKillsMultiplier','NativeSwarm','EffectSwarmMultiplier','EnchantmentReplenish']
  changes={n:b for n,b in fresh.items() if n.endswith('.class') and n.startswith('com/willfp/ecoenchants/') and n.split('/')[-1].split('$')[0].removesuffix('.class') in tokens}
 after=before|changes
 assert all(after[n]==b for n,b in before.items() if n not in changes)
 assert all(after[n]==b for n,b in before.items() if n.startswith(('com/willfp/ecoskills/api/','com/willfp/ecoenchants/api/')))
 out=w/name
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in sorted(after.items()):
   info=zipfile.ZipInfo(n,(2026,10,20,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
 reports.append(dict(repo=repo,file=name,sha256=hashlib.sha256(out.read_bytes()).hexdigest(),baseline=sha,changes=sorted(changes),installed=False,active=False,gameplayVerified=False))
(w/'consolidated-runtime-manifest.json').write_text(json.dumps(dict(artifacts=reports,requiresUnchanged={'eco':'5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f','EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'},runtimeTest=False),indent=2)+'\n');print(json.dumps(reports))
