import java.lang.reflect.*;
public final class VerifyUtilityLinkage {
 public static void main(String[] args)throws Exception {
  Class<?> math=Class.forName("com.willfp.ecoenchants.progression.NativeMagicRecoveryKt");
  Method credit=math.getMethod("manaCredit",int.class,int.class,int.class);
  int carry=0,whole=0;
  for(int i=0;i<4;i++){
   Object result=credit.invoke(null,100,1,carry);whole+=(int)result.getClass().getMethod("getWhole").invoke(result);carry=(int)result.getClass().getMethod("getRemainder").invoke(result);
  }
  if(whole!=1||carry!=0)throw new AssertionError("Fractional mana lost");
  Class<?> xp=Class.forName("com.willfp.libreforge.triggers.event.EditableXpDropEvent");xp.getMethod("claimAdjustment",String.class);
  Class<?> api=Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI");
  Class<?> player=Class.forName("org.bukkit.entity.Player"),type=Class.forName("com.willfp.ecoskills.magic.MagicType");
  api.getMethod("getMaxMagic",player,type);api.getMethod("setMagic",player,type,int.class);
  Class.forName("com.willfp.ecoskills.magic.MagicMapKt").getMethod("peekObservedMagic",java.util.UUID.class,type);
  Class<?> registry=Class.forName("com.willfp.ecoskills.magic.MagicTypes",false,api.getClassLoader());registry.getMethod("getByID",String.class);
  for(String name:new String[]{"FirstHitsLedger","NativeFirstHits","NativeMagicRecovery","NativeSwarm","SwarmQueryBudget","OriginalRuntime","NativeCombatProgression"})Class.forName("com.willfp.ecoenchants.progression."+name,false,api.getClassLoader()).getDeclaredMethods();
  Class.forName("com.willfp.eco.core.entities.BoundedLivingQueries").getDeclaredMethods();
  System.out.println("Composed utility runtime JVM smoke passed: real shaded linkage, exact fractional credit and native EcoSkills signatures. No Paper execution.");
 }
}
