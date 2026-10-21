"""Extend only the pinned bootstrap-fixed runtime; foreign work and SDK stay untouched."""
from pathlib import Path
import hashlib,json,sys,zipfile
r=Path('/root/development/projects/eco-suite/EcoEnchants');w=Path(sys.argv[1]);w.mkdir(parents=True,exist_ok=True)
base=Path('/root/development/artifacts/eco-suite/native-expansion-20261020/bootstrap-fixed/EcoEnchants-2026.27-native-bootstrap-fixed-all.jar')
assert hashlib.sha256(base.read_bytes()).hexdigest()=='1d7a0ca87822f38be5dbb9e2aae528cdb4e7895a301d179394e25ba34e64152c'
def read(p):
 with zipfile.ZipFile(p) as z:return {n:z.read(n) for n in z.namelist()}
before=read(base);fresh=read(r/'eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar')
roots={'OriginalRuntime','OriginalProcEngine','NativeBurstProgression','NativeProcAdapters','VfxProfiles'}
def selected(n):return n.startswith('com/willfp/ecoenchants/progression/') and n.endswith('.class') and n.rsplit('/',1)[-1].split('$')[0].removesuffix('.class') in roots
changes={n:b for n,b in fresh.items() if selected(n)}
removed={n for n in before if selected(n) and n not in changes}
assert 'com/willfp/ecoenchants/progression/VfxProfiles.class' in changes
# Preserve every existing config byte and append only the new defaults. Active YAML is merged by the owner.
fragment=(r/'delivery/native-vfx-20261021/visuals.yml').read_text()
block='    max-scenes:'+fragment.split('    max-scenes:',1)[1]
changes['config.yml']=before['config.yml']+block.encode()
after={n:b for n,b in before.items() if n not in removed}|changes
assert all(after[n]==b for n,b in before.items() if n not in changes and n not in removed)
assert all(after[n]==b for n,b in before.items() if n.startswith('com/willfp/ecoenchants/api/'))
assert all(after[n]==b for n,b in before.items() if n.startswith('enchants/'))
assert 'com/willfp/ecoenchants/progression/NativeUtilityProgression.class' in after
out=w/'EcoEnchants-2026.27-vfx-pilot-all.jar'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for n,b in sorted(after.items()):
  info=zipfile.ZipInfo(n,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
report={'file':out.name,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'baselineSha256':hashlib.sha256(base.read_bytes()).hexdigest(),'changedEntries':sorted(n for n,b in changes.items() if before.get(n)!=b),'removedObsoleteOwnedClasses':sorted(removed),'profiles':['eco_resonante','brasa_errante'],'sdkByteIdentical':True,'enchantDefinitionsByteIdentical':True,'installed':False,'active':False,'gameplayVerified':False}
(w/'runtime-manifest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
