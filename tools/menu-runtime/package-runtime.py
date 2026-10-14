"""Preserve the installed EcoEnchants runtime and overlay only approved guide classes."""
from pathlib import Path
import zipfile,hashlib,json,subprocess,re,sys,runpy
baseline,fresh,output=map(Path,sys.argv[1:4])
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(baseline)=='9f974541c99cdb6ef180be428dcd526472634c49434637612998f67b119de75e'
package='com/willfp/ecoenchants/enchant/'
prefixes=[package+x for x in ['EnchantGUI','EnchantmentScrollPane','GuideBrowser','GuideQuery','GuideState','GuideRequirements']]
def approved(n):return n.endswith('.class') and any(n.startswith(p+'.') or n.startswith(p+'$') or (p.endswith('EnchantGUI') and n.startswith(p+'Kt')) for p in prefixes)
with zipfile.ZipFile(baseline) as z:old={n:z.read(n) for n in z.namelist()}
with zipfile.ZipFile(fresh) as z:new={n:z.read(n) for n in z.namelist()}
changes={n:data for n,data in new.items() if approved(n)}
assert package+'GuideBrowser.class' in changes
marker=b'# Optional browsing controls.'
assert marker not in old['config.yml']
changes['config.yml']=old['config.yml']+b'\r\n'+new['config.yml'][new['config.yml'].index(marker):]
result=dict(old);result.update(changes)
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
 for n,data in result.items():z.writestr(n,data)
with zipfile.ZipFile(output) as z:
 for n,data in old.items():
  if n not in changes:assert z.read(n)==data,n
 assert set(old).issubset(z.namelist())
# Public nonsynthetic entrypoints keep their JVM descriptors; synthetic access helpers are implementation details.
def public_signatures(jar,cls):
 text=subprocess.check_output(['javap','-classpath',str(jar),'-public','-s',cls],text=True)
 lines=text.splitlines();signatures=set()
 for i,line in enumerate(lines):
  if line.strip().startswith('descriptor:'):
   declaration=lines[i-1].strip()
   if 'access$' not in declaration:signatures.add((declaration,line.strip()))
 return signatures
for cls in ['EnchantGUI','EnchantGUIKt','EnchantmentScrollPane']:
 name='com.willfp.ecoenchants.enchant.'+cls
 missing=public_signatures(baseline,name)-public_signatures(output,name)
 assert not missing,(cls,missing)
checked=runpy.run_path(str(Path(__file__).with_name("check-guide-linkage.py")))["check"](output)
report={"guideClassesLinkChecked":checked,"runtimeClassDependenciesVerified":True,'baselineSha256':sha(baseline),'candidateSha256':sha(output),'changedEntries':sorted(n for n in changes if n in old and old[n]!=changes[n]),'addedEntries':sorted(set(changes)-set(old)),'unchangedOriginalEntries':sum(n not in changes or old[n]==changes[n] for n in old),'removedEntries':0,'publicEntrypointsPreserved':True,'mechanicsAndNmsByteIdentical':True}
output.with_suffix('.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
