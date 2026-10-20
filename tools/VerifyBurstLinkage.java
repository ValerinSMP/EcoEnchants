public final class VerifyBurstLinkage {
 public static void main(String[] args)throws Exception {
  ClassLoader loader=VerifyBurstLinkage.class.getClassLoader();
  for(String n:new String[]{"NativeBurstProgression","NativeBurstProgressionKt","ConfirmedNativeBurst","EffectNativeBurst","ConditionHuntersTrail","OriginalRuntime","OriginalProcEngine","ProcGuards"})Class.forName("com.willfp.ecoenchants.progression."+n,false,loader).getDeclaredMethods();
  Class<?> families=Class.forName("com.willfp.ecoenchants.progression.OriginalProcEngine$Family");
  if(families.getEnumConstants().length!=12)throw new AssertionError("Missing native families");
  Class<?> guards=Class.forName("com.willfp.ecoenchants.progression.ProcGuards");
  for(Object family:families.getEnumConstants())if(!Boolean.TRUE.equals(guards.getMethod("arrivalReady",families,long.class).invoke(null,family,30000L)))throw new AssertionError("Missing family gate");
  System.out.println("Composed burst runtime JVM smoke passed: eight runtime boundaries, twelve native engine families, arrival fences and real shaded linkage. No Paper lifecycle/gameplay.");
 }
}
