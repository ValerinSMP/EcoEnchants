"""Append the three combat families to the verified mining increment, preserving unrelated runtime entries."""
from pathlib import Path
import zipfile,hashlib,json,sys
root=Path('/root/development/projects/eco-suite');w=Path(sys.argv[1])
def read(p):
 with zipfile.ZipFile(p) as z:return {n:z.read(n) for n in z.namelist()}
base=w/'EcoEnchants-2026.27-native-mining-all.jar'
sha='2125248d8ba1b186fe5e9204e7a059d9515d687ccdd453de0f009783e269f715'
assert hashlib.sha256(base.read_bytes()).hexdigest()==sha
before=read(base);fresh=read(root/'EcoEnchants/eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
tokens=['OriginalRuntime','OriginalProgression','NativeProcAdapters','NativeCombatProgression','NativeCombatRules','ArmorStack','LastResortGate','CommandDamageGuard','ConfirmedNativeCombat','NativeCombatEffect','ConditionRested']
changes={n:b for n,b in fresh.items() if n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and any(n.split('/')[-1].split('$')[0]==t+'.class' or n.split('/')[-1].split('$')[0]==t for t in tokens)}
for id in ['letalidad','reposo','ultimo_recurso']:changes['enchants/'+id+'.yml']=fresh['enchants/'+id+'.yml']
after=before|changes
assert all(after[n]==b for n,b in before.items() if n not in changes)
assert all(after[n]==b for n,b in before.items() if n.startswith('com/willfp/ecoenchants/api/'))
assert not any(n.startswith('com/willfp/ecoenchants/target/') for n in changes)
out=w/'EcoEnchants-2026.27-native-five-all.jar'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for n,b in after.items():z.writestr(n,b)
report={'increment':2,'families':['cuidado','ritmo_minero','letalidad','reposo','ultimo_recurso'],'file':out.name,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'baseline':sha,'changes':sorted(changes),'requiresUnchanged':{'eco':'7565c0bcac30521dc6f2ba0a4ab3c448d1d7109196782767950c73753a99c0dc','LibreForge':'b0a0ab399620a3d37b37c16a0ae33f5d20a3a533378a9bb68984b56435971d77','EcoSkills':'0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca'},'installed':False,'active':False,'gameplayVerified':False}
(w/'five-runtime-manifest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
