import java.lang.reflect.*;
import java.util.UUID;

/** Executes only pure runtime boundaries from composed JARs; never starts a Bukkit server. */
public final class VerifyAdditiveLinkage {
 public static void main(String[] args)throws Exception {
  Class<?> bypass=Class.forName("com.willfp.libreforge.effects.impl.DamageMultiplierBypass");
  Object bridge=bypass.getField("INSTANCE").get(null);Method current=bypass.getMethod("getCurrent");
  if(current.invoke(bridge)!=null)throw new AssertionError("Dirty provenance");
  Class<?> scope=Class.forName("com.willfp.libreforge.effects.impl.RepeatedDamage");
  Object runtime=scope.getField("INSTANCE").get(null);
  Class<?> fn=Class.forName("com.willfp.eco.libs.kotlin.jvm.functions.Function0");
  Method run=scope.getMethod("run",UUID.class,UUID.class,UUID.class,fn);
  Object callback=Proxy.newProxyInstance(fn.getClassLoader(),new Class<?>[]{fn},(proxy,method,arguments)->{
   if(!method.getName().equals("invoke"))throw new AssertionError(method);
   Object provenance=current.invoke(bridge);
   if(provenance==null||!provenance.getClass().getMethod("getOrigin").invoke(provenance).toString().equals("PASSIVE"))throw new AssertionError("Missing native scope");
   return "runtime-ok";
  });
  if(!"runtime-ok".equals(run.invoke(runtime,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),callback)))throw new AssertionError("Wrong callback result");
  if(current.invoke(bridge)!=null)throw new AssertionError("Provenance leaked");
  Object failing=Proxy.newProxyInstance(fn.getClassLoader(),new Class<?>[]{fn},(proxy,method,arguments)->{throw new IllegalStateException("expected-boundary");});
  try{run.invoke(runtime,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),failing);throw new AssertionError("Exception swallowed");}
  catch(InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalStateException))throw expected;}
  if(current.invoke(bridge)!=null)throw new AssertionError("Exception scope leaked");
  Class<?> sum=Class.forName("com.willfp.libreforge.effects.impl.DamageContributionSum");
  Object calculation=sum.getConstructor().newInstance();Method offer=sum.getMethod("offer",Object.class,double.class);
  if(!Boolean.TRUE.equals(offer.invoke(calculation,"a",1.5))||!Boolean.TRUE.equals(offer.invoke(calculation,"b",1.4)))throw new AssertionError("Runtime calculator failed");
  Class.forName("com.willfp.libreforge.effects.impl.AdditiveDamageGroups");
  System.out.println("Composed runtime JVM smoke: shaded Kotlin linkage, native provenance callback, exception restoration and calculator passed; no server lifecycle/gameplay.");
 }
}
