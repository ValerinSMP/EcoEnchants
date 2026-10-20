public final class VerifyReactiveLinkage {
 public static void main(String[] args)throws Exception {
  ClassLoader loader=VerifyReactiveLinkage.class.getClassLoader();
  for(String n:new String[]{"NativeReactiveProgression","NativeReactiveProgressionKt","NativeReactiveAction","EffectNativeReactive","ArmedProcLedger","NativeFirstHits","OriginalRuntime"})Class.forName("com.willfp.ecoenchants.progression."+n,false,loader).getDeclaredMethods();
  Class<?> families=Class.forName("com.willfp.ecoenchants.progression.OriginalProcEngine$Family");if(families.getEnumConstants().length!=20)throw new AssertionError("Missing native family");
  Class<?> guards=Class.forName("com.willfp.ecoenchants.progression.ProcGuards");
  for(Object f:families.getEnumConstants())if(!Boolean.TRUE.equals(guards.getMethod("arrivalReady",families,long.class).invoke(null,f,30000L)))throw new AssertionError("Missing arrival fence");
  System.out.println("Composed reactive runtime JVM smoke passed: native state/rejection boundaries, twenty proc families, arrival fences and shaded linkage; no server lifecycle/gameplay.");
 }
}
