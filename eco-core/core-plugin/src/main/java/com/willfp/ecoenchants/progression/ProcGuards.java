package com.willfp.ecoenchants.progression;

/** Exact predicates shared by live Paper adapter and deterministic boundary tests. */
public final class ProcGuards {
 private ProcGuards(){}
 public static boolean mayAct(boolean online,boolean dead){return online&&!dead;}
 public static boolean arrivalReady(OriginalProcEngine.Family family,long ageMillis){return ageMillis>=(family==OriginalProcEngine.Family.ECO_RESONANTE?8000:10000);}
 public static double confirmedDamage(boolean cancelled,boolean sameLastCause,double finalDamage,double healthBefore,double healthAfter){
  if(cancelled||!sameLastCause||!Double.isFinite(finalDamage)||!Double.isFinite(healthBefore)||!Double.isFinite(healthAfter)||finalDamage<=0)return 0;
  return Math.max(0,Math.min(finalDamage,healthBefore-healthAfter));
 }
 public static boolean eligible(boolean monster,String spawnReason,boolean boss,boolean mythic,boolean marked,boolean allied,boolean invulnerable,boolean protectedTarget){
  return monster&&("NATURAL".equals(spawnReason)||"COMMAND".equals(spawnReason)||"SPAWNER_EGG".equals(spawnReason)||"DISPENSE_EGG".equals(spawnReason))&&!boss&&!mythic&&!marked&&!allied&&!invulnerable&&!protectedTarget;
 }
}
