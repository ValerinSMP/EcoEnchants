import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
/** Additive ABI and linkage validation of the actual composed recipient-policy classes. */
public final class VerifyCombatTargets {
 static ClassNode read(byte[] bytes){var c=new ClassNode();new ClassReader(bytes).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c;}
 static boolean visible(int flags){return (flags&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0;}
 static void abi(String before,String after)throws Exception{
  try(var a=new JarFile(before);var b=new JarFile(after)){
   var entries=a.entries();int methods=0;
   while(entries.hasMoreElements()){
    var e=entries.nextElement();String name=e.getName();
    if(!name.startsWith("com/willfp/")||name.contains("/libs/")||!name.endsWith(".class")||name.contains("$"))continue;
    var next=b.getJarEntry(name);if(next==null)throw new AssertionError("Removed class "+name);
    var old=read(a.getInputStream(e).readAllBytes());var fresh=read(b.getInputStream(next).readAllBytes());
    for(var m:old.methods)if(visible(m.access)){
     boolean found=fresh.methods.stream().anyMatch(n->n.name.equals(m.name)&&n.desc.equals(m.desc)&&visible(n.access)&&((m.access^n.access)&Opcodes.ACC_STATIC)==0);
     if(!found)throw new AssertionError("Removed ABI method "+name+"."+m.name+m.desc);methods++;
    }
    for(var f:old.fields)if(visible(f.access)&&fresh.fields.stream().noneMatch(n->n.name.equals(f.name)&&n.desc.equals(f.desc)&&visible(n.access)&&((f.access^n.access)&Opcodes.ACC_STATIC)==0))throw new AssertionError("Removed ABI field "+name+"."+f.name);
   }
   System.out.println("Existing top-level public/protected ABI PASS: "+methods+" methods "+after);
  }
 }
 static void type(String name){
  if(name.startsWith("[")){desc(name);return;}
  if((name.startsWith("com/willfp/libreforge/")||name.startsWith("com/willfp/ecoenchants/"))&&!name.contains("/libs/")&&!VerifyBootstrapClosure.all.containsKey(name))throw new AssertionError("Missing owned class "+name);
 }
 static void check(Type t){if(t.getSort()==Type.ARRAY)check(t.getElementType());else if(t.getSort()==Type.OBJECT)type(t.getInternalName());else if(t.getSort()==Type.METHOD)desc(t.getDescriptor());}
 static void desc(String value){if(value.startsWith("(")){for(Type t:Type.getArgumentTypes(value))check(t);check(Type.getReturnType(value));}else check(Type.getType(value));}
 public static void main(String[] args)throws Exception{
  // eco, libreforge, skills, ecoenchants, oldLF, oldEE, changed entry list
  VerifyBootstrapClosure.main(Arrays.copyOfRange(args,0,4));abi(args[4],args[1]);abi(args[5],args[3]);
  int methods=0,refs=0;
  for(String entry:Files.readAllLines(Path.of(args[6]))){
   if(!entry.endsWith(".class"))continue;String name=entry.substring(0,entry.length()-6);var c=VerifyBootstrapClosure.all.get(name);if(c==null)throw new AssertionError("Missing reviewed class "+name);
   if(c.superName!=null)type(c.superName);for(String i:c.interfaces)type(i);for(var f:c.fields)desc(f.desc);
   for(var m:c.methods){methods++;desc(m.desc);for(var ins:m.instructions){
    if(ins instanceof MethodInsnNode v){type(v.owner);desc(v.desc);if(v.owner.startsWith("com/willfp/")&&!VerifyBootstrapClosure.member(v.owner,v.name,v.desc,false,new HashSet<>()))throw new AssertionError("Missing method "+name+" -> "+v.owner+"."+v.name+v.desc);refs++;}
    if(ins instanceof FieldInsnNode v){type(v.owner);desc(v.desc);if(v.owner.startsWith("com/willfp/")&&!VerifyBootstrapClosure.member(v.owner,v.name,v.desc,true,new HashSet<>()))throw new AssertionError("Missing field "+v.owner+"."+v.name);refs++;}
    if(ins instanceof TypeInsnNode v)type(v.desc);
    if(ins instanceof LdcInsnNode v&&v.cst instanceof Type t)check(t);
    if(ins instanceof InvokeDynamicInsnNode v){desc(v.desc);for(Object a:v.bsmArgs){if(a instanceof Type t)check(t);if(a instanceof Handle h){type(h.getOwner());desc(h.getDesc());VerifyBootstrapClosure.handle(h);}}}
   }}
  }
  System.out.println("Composed combat-policy closure PASS: "+methods+" methods, "+refs+" member references");
 }
}
