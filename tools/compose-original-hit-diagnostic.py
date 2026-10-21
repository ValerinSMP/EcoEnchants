"""Deliver observation only over the hash-pinned installed EcoEnchants; no installation."""
from pathlib import Path
import hashlib,json,sys,zipfile
root=Path(__file__).resolve().parents[1]
out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
base=Path(sys.argv[2]);fix=len(sys.argv)>3 and sys.argv[3]=="policy-fix"
expected="193174602f9f66ec361a591ac9a2e140380cc653f0bbe2e016259979fb4e7a52" if fix else "5c56242290c5683c9baf2853908fa90accde19f1418fa117f007130819629ca5"
assert hashlib.sha256(base.read_bytes()).hexdigest()==expected, "Runtime baseline changed; review before composing"
def read(path):
 with zipfile.ZipFile(path) as z:return {n:z.read(n) for n in z.namelist()}
before=read(base);fresh=read(root/"eco-core/core-plugin/build/libs/core-plugin-2026.27-all.jar")
roots={"OriginalRuntime","EcoEnchantLevel"} if fix else {"OriginalRuntime","OriginalHitDiagnostic","OriginalProcEffect","CommandEcoEnchants","CommandOriginalDiagnostic"}
def selected(n):return n.startswith("com/willfp/ecoenchants/") and n.endswith(".class") and n.rsplit("/",1)[-1].split("$")[0].removesuffix(".class") in roots
changes={n:b for n,b in fresh.items() if selected(n)}
assert roots <= {n.rsplit("/",1)[-1].split("$")[0].removesuffix(".class") for n in changes}
removed={n for n in before if selected(n) and n not in changes}
after={n:b for n,b in before.items() if n not in removed}|changes
assert all(after[n]==b for n,b in before.items() if not selected(n))
assert all(after[n]==b for n,b in before.items() if n.startswith(("enchants/","com/willfp/ecoenchants/api/")) or not n.endswith(".class"))
jar=out/("EcoEnchants-2026.27-policy-wrapper-fix-all.jar" if fix else "EcoEnchants-2026.27-original-hit-diagnostic-all.jar")
with zipfile.ZipFile(jar,"w",zipfile.ZIP_DEFLATED) as z:
 for n,b in sorted(after.items()):
  info=zipfile.ZipInfo(n,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
report=dict(file=jar.name,sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),baseline=expected,changedEntries=sorted(n for n in changes if before.get(n)!=changes[n]),removed=sorted(removed),apiAndYamlByteIdentical=True,functionalFixImplemented=fix,rootCauseDemonstrated=fix,installed=False,active=False,gameplayVerified=False)
(out/"runtime-manifest.json").write_text(json.dumps(report,indent=2)+"\n")
print(json.dumps(report))
