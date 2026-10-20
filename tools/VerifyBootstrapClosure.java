import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.jar.*;
/** Artifact-only bootstrap audit. Never treats an absent EcoEnchants class as external. */
public final class VerifyBootstrapClosure {
 static final Map<String,ClassNode> all=new HashMap<>();
 static boolean owned(String s){return s.startsWith("com/willfp/ecoenchants/");}
 static void type(String s){if(s.startsWith("[")){descriptor(s);return;}if(owned(s)&&!all.containsKey(s))throw new AssertionError("Missing bootstrap class: "+s);}
 static void descriptor(String s){for(Type t: s.startsWith("(")?Type.getArgumentTypes(s):new Type[]{Type.getType(s)})checkType(t);if(s.startsWith("("))checkType(Type.getReturnType(s));}
 static void checkType(Type t){if(t.getSort()==Type.ARRAY)checkType(t.getElementType());else if(t.getSort()==Type.OBJECT)type(t.getInternalName());else if(t.getSort()==Type.METHOD)descriptor(t.getDescriptor());}
 static boolean member(String owner,String name,String desc,boolean field,Set<String> seen){
  type(owner);if(!seen.add(owner))return false;ClassNode c=all.get(owner);
  if(c==null&&owner.startsWith("java/"))try(var in=ClassLoader.getSystemResourceAsStream(owner+".class")){if(in!=null){c=new ClassNode();new ClassReader(in).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e); }
  if(c==null)return true; // external server member, outside this static audit
  if(field?c.fields.stream().anyMatch(f->f.name.equals(name)&&f.desc.equals(desc)):c.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(desc)))return true;
  if(name.equals("<init>"))return false;
  if(c.superName!=null&&member(c.superName,name,desc,field,seen))return true;
  for(String i:c.interfaces)if(member(i,name,desc,field,seen))return true;return false;
 }
 static void handle(Handle h){if(!member(h.getOwner(),h.getName(),h.getDesc(),h.getTag()<=Opcodes.H_PUTSTATIC,new HashSet<>()))throw new AssertionError("Missing handle "+h);}
 public static void main(String[] args)throws Exception{
  for(String path:args)try(JarFile jar=new JarFile(path)){var entries=jar.entries();while(entries.hasMoreElements()){var e=entries.nextElement();if(!e.getName().endsWith(".class")||!e.getName().startsWith("com/willfp/")||e.getName().contains("/libs/"))continue;ClassNode c=new ClassNode();new ClassReader(jar.getInputStream(e)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);all.put(c.name,c);}}
  int methods=0,refs=0;
  for(var c:all.values())if(c.name.startsWith("com/willfp/ecoenchants/progression/")||c.name.equals("com/willfp/ecoenchants/EcoEnchantsPlugin")){
   if(c.superName!=null)type(c.superName);c.interfaces.forEach(VerifyBootstrapClosure::type);
   for(var f:c.fields)descriptor(f.desc);
   for(var m:c.methods){if(!c.name.contains("/progression/")&&!m.name.equals("handleLoad"))continue;methods++;descriptor(m.desc);
    for(var i:m.instructions){
     if(i instanceof MethodInsnNode v){type(v.owner);descriptor(v.desc);if(v.owner.startsWith("com/willfp/")&&!member(v.owner,v.name,v.desc,false,new HashSet<>()))throw new AssertionError("Missing method: "+c.name+" -> "+v.owner+"."+v.name+v.desc);refs++;}
     if(i instanceof FieldInsnNode v){type(v.owner);descriptor(v.desc);if(v.owner.startsWith("com/willfp/")&&!member(v.owner,v.name,v.desc,true,new HashSet<>()))throw new AssertionError("Missing field: "+v.owner+"."+v.name);refs++;}
     if(i instanceof TypeInsnNode v)type(v.desc);
     if(i instanceof LdcInsnNode v&&v.cst instanceof Type t)checkType(t);
     if(i instanceof InvokeDynamicInsnNode v){descriptor(v.desc);for(Object a:v.bsmArgs){if(a instanceof Handle h)handle(h);if(a instanceof Type t)checkType(t);}}
    }
   }
  }
  System.out.println("Bootstrap closure PASS: "+methods+" methods, "+refs+" member references; missing owned classes are fatal.");
 }
}
