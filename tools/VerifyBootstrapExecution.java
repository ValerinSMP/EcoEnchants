import java.util.*;
/** Executes the delivered handleLoad; registry storage is the only simulated boundary. */
public final class VerifyBootstrapExecution {
 public static void main(String[] args)throws Exception{
  Class<?> c=Class.forName("com.willfp.ecoenchants.EcoEnchantsPlugin");
  var unsafeClass=Class.forName("sun.misc.Unsafe");var field=unsafeClass.getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);
  Object plugin=unsafeClass.getMethod("allocateInstance",Class.class).invoke(unsafe,c);
  // Avoid JavaPlugin construction: no server, enable, scheduling, SQL or writes.
  var load=c.getDeclaredMethod("handleLoad");load.setAccessible(true);load.invoke(plugin);
  check("effects.Effects",Set.of("apply_random_enchant","guard_immature_crops","double_mob_xp","first_hits_multiplier","third_hit_multiplier","recent_kills_multiplier","swarm_multiplier","resonant_pulse","glacial_seal","native_burst","native_reactive","lethality_stack","last_resort","mana_recovery"));
  check("triggers.Triggers",Set.of("manual_crop_guard","confirmed_original_hit","confirmed_native_burst","native_reactive_action","confirmed_native_combat"));
  check("conditions.Conditions",Set.of("has_mining_rhythm","has_hunters_trail","out_of_confirmed_combat"));
  check("filters.Filters",Set.of("enchant_combat_mode","genuine_kill"));
  System.out.println("Delivered EcoEnchantsPlugin.handleLoad executed PASS; real progression register/constructors; simulated registry storage, not Paper activation.");
 }
 static void check(String name,Set<String> required)throws Exception{
  Class<?> c=Class.forName("com.willfp.libreforge."+name);Set<?> actual=(Set<?>)c.getField("ids").get(null);
  if(!actual.equals(required))throw new AssertionError(name+" missing "+required+" actual="+actual);
  System.out.println(name+" "+actual);
 }
}
