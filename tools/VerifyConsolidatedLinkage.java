public final class VerifyConsolidatedLinkage {
 public static void main(String[] args)throws Exception {
  ClassLoader loader=VerifyConsolidatedLinkage.class.getClassLoader();
  for(String n:new String[]{"NativeReactiveProgression","NativeReactiveAction","NativeBurstProgression","EffectNativeBurst","NativeCombatProgression","NativeFirstHits","FirstHitsLedger","EffectThirdHitMultiplier","EffectRecentKillsMultiplier","FilterEnchantCombatMode","FilterGenuineKill","OriginalRuntime","NativeSwarm"})Class.forName("com.willfp.ecoenchants.progression."+n,false,loader).getDeclaredMethods();
  Class.forName("com.willfp.ecoenchants.enchant.impl.hardcoded.EnchantmentReplenish",false,loader).getDeclaredMethods();
  for(String n:new String[]{"effects.impl.EffectRapidBows","effects.impl.EffectDamageVictim","triggers.impl.TriggerShieldBlock","triggers.impl.TriggerKill"})Class.forName("com.willfp.libreforge."+n,true,loader).getDeclaredMethods();
  Class<?> families=Class.forName("com.willfp.ecoenchants.progression.OriginalProcEngine$Family");if(families.getEnumConstants().length!=27)throw new AssertionError("Missing native family");
  Class<?> guards=Class.forName("com.willfp.ecoenchants.progression.ProcGuards");
  if(!Boolean.TRUE.equals(guards.getMethod("combatModeAllows",String.class,boolean.class).invoke(null,"both",true)))throw new AssertionError("PvP not permitted");
  if(!Boolean.FALSE.equals(guards.getMethod("combatModeAllows",String.class,boolean.class).invoke(null,"pve",true)))throw new AssertionError("PvE bypass");
  for(Object f:families.getEnumConstants())if(!Boolean.TRUE.equals(guards.getMethod("arrivalReady",families,long.class).invoke(null,f,30000L)))throw new AssertionError("Missing arrival fence");
  System.out.println("Composed runtime JVM smoke passed: 27 families, PvP/PvE, native/shared classes and shaded linkage; no server lifecycle/gameplay.");
 }
}
