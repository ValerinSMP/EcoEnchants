"""Personal-only packaging: preserve installed runtime, add the reviewed first increment."""
import sys,hashlib,zipfile,subprocess,json
from pathlib import Path
base,fresh,out,asm=Path(sys.argv[1]),Path(sys.argv[2]),Path(sys.argv[3]),sys.argv[4]
assert hashlib.sha256(base.read_bytes()).hexdigest()=="7dd5c987b3d2fcdcb3069b35d20a2609a8fbea8be9c503971ebe0ddb87281a0e"
work=out.parent/"composition";work.mkdir(exist_ok=True)
java="""import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
public class PatchProgression {
 public static void main(String[]args)throws Exception{
  var reader=new ClassReader(Files.readAllBytes(Path.of(args[0])));
  var node=new ClassNode();reader.accept(node,0);
  var method=node.methods.stream().filter(m->m.name.equals("handleEnable")&&m.desc.equals("()V")).findFirst().orElseThrow();
  var hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,0));
  hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/willfp/ecoenchants/progression/OriginalProgression","diagnose","(Lcom/willfp/ecoenchants/EcoEnchantsPlugin;)V",false));
  method.instructions.insert(hook);
  var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);node.accept(writer);
  Files.write(Path.of(args[1]),writer.toByteArray());
 }
}
"""
(work/"PatchProgression.java").write_text(java)
subprocess.run(["javac","--release","21","-cp",asm,"-d",str(work),str(work/"PatchProgression.java")],check=True)
plugin="com/willfp/ecoenchants/EcoEnchantsPlugin.class"
assets={"enchants/ecoskills/angler.yml","enchants/infernal_touch.yml","enchants/_eco_resonante.yml","enchants/_sello_glacial.yml"}
with zipfile.ZipFile(base)as z:before={n:z.read(n)for n in z.namelist()}
with zipfile.ZipFile(fresh)as z:
 additions={n:z.read(n)for n in z.namelist()if n.startswith("com/willfp/ecoenchants/progression/")and n.endswith(".class")}
 for n in assets:additions[n]=z.read(n)
 assert "enchants/eco_resonante.yml" not in z.namelist()
 assert "enchants/sello_glacial.yml" not in z.namelist()
(work/"before.class").write_bytes(before[plugin])
subprocess.run(["java","-Xmx128m","-cp",str(work)+":"+asm,"PatchProgression",str(work/"before.class"),str(work/"after.class")],check=True)
additions[plugin]=(work/"after.class").read_bytes()
with zipfile.ZipFile(base)as old,zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED)as new:
 for info in old.infolist():
  if info.filename not in additions:new.writestr(info,before[info.filename])
 for n,b in additions.items():new.writestr(n,b)
with zipfile.ZipFile(out)as z:
 after={n:z.read(n)for n in z.namelist()}
 assert all(after[n]==b for n,b in before.items()if n not in additions)
 assert all(after[n]==b for n,b in before.items()if n.startswith("com/willfp/ecoenchants/api/"))
 report={"runtime":out.name,"sha256":hashlib.sha256(out.read_bytes()).hexdigest(),"baseline":hashlib.sha256(base.read_bytes()).hexdigest(),"changedEntries":[n for n in after if before.get(n)!=after[n]],"unchangedEntries":sum(after.get(n)==b for n,b in before.items()),"apiByteIdentical":True,"installed":False,"active":False,"originalFamiliesExecutable":False}
(out.parent/"runtime-manifest.json").write_text(json.dumps(report,indent=2)+"\n")
print(json.dumps(report))
