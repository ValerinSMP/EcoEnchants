import java.lang.reflect.*;
import java.util.*;
import com.willfp.eco.core.config.interfaces.Config;
import org.bukkit.entity.*;
/** Calls the shipped shaded policy and scope with simulated config/entities; no Paper lifecycle. */
public final class VerifyCombatPolicyRuntime {
 static Object unit()throws Exception{return Class.forName("com.willfp.eco.libs.kotlin.Unit").getField("INSTANCE").get(null);}
 static Class<?> function(int n)throws Exception{return Class.forName("com.willfp.eco.libs.kotlin.jvm.functions.Function"+n);}
 static Object proxy(Class<?> type,InvocationHandler fn){return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},fn);}
 static int reads;
 static Config config(Map<String,Object> values){return (Config)proxy(Config.class,(p,m,a)->{
  reads++;String key=a!=null&&a.length>0&&a[0] instanceof String?(String)a[0]:null;Object value=key==null?null:values.get(key);
  return switch(m.getName()){
   case "has"->values.containsKey(key);case "getKeys"->new ArrayList<>(values.keySet());
   case "getSubsectionOrNull"->value instanceof Map?config((Map<String,Object>)value):null;
   case "getBoolOrNull","getStringOrNull","getStringsOrNull"->value;
   default->m.getReturnType()==boolean.class?false:m.getReturnType()==int.class?0:null;
  };
 });}
 static Object compile(Map<String,Object> values,List<String> errors)throws Exception{
  Class<?> c=Class.forName("com.willfp.ecoenchants.progression.CombatTargetPolicy");Object companion=c.getField("Companion").get(null);
  return companion.getClass().getMethod("compile",Config.class,function(1)).invoke(companion,config(values),proxy(function(1),(p,m,a)->{if(m.getName().equals("invoke"))errors.add((String)a[0]);return unit();}));
 }
 static boolean selects(Object policy,EntityType type)throws Exception{return (boolean)policy.getClass().getMethod("selects",EntityType.class,boolean.class).invoke(policy,type,type==EntityType.PLAYER);}
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  List<String> errors=new ArrayList<>();Object any=compile(Map.of(),errors);int count=reads;
  for(int i=0;i<2000;i++)for(EntityType t:List.of(EntityType.COW,EntityType.SHEEP,EntityType.ZOMBIE,EntityType.PLAYER,EntityType.WARDEN))check(selects(any,t),t.name());
  check(reads==count,"Per-hit config read");check(errors.isEmpty(),"Unexpected default diagnostic");
  Object pve=compile(Map.of("combat-mode","pve","combat-targets",Map.of("pvp",true,"exclude-types",List.of("COW"))),errors);
  check(!selects(pve,EntityType.PLAYER)&&!selects(pve,EntityType.COW)&&selects(pve,EntityType.SHEEP),"Narrowing/precedence");
  Object invalid=compile(Map.of("combat-targets",Map.of("include-types",List.of("not_a_species"))),errors);
  check(!selects(invalid,EntityType.ZOMBIE)&&!selects(invalid,EntityType.PLAYER)&&!errors.isEmpty(),"Invalid config opened policy");
  Class<?> holderClass=Class.forName("com.willfp.libreforge.CombatTargetHolder");Object[] session={"original"};
  Object holder=proxy(holderClass,(p,m,a)->switch(m.getName()){case "combatTargetSession"->session[0];case "allowsCombatTarget"->true;default->false;});
  Player actor=(Player)proxy(Player.class,(p,m,a)->null);LivingEntity target=(LivingEntity)proxy(LivingEntity.class,(p,m,a)->null);
  Class<?> contextClass=Class.forName("com.willfp.libreforge.CombatTargets$Context");Object context=contextClass.getConstructor(holderClass,Player.class,boolean.class).newInstance(holder,actor,false);
  Method allows=contextClass.getMethod("allows",LivingEntity.class,boolean.class,boolean.class);
  check((boolean)allows.invoke(context,target,false,false),"Original session blocked");session[0]="replacement";check(!(boolean)allows.invoke(context,target,false,false),"Stale session resumed");
  Class<?> scopes=Class.forName("com.willfp.libreforge.CombatTargets");Object instance=scopes.getField("INSTANCE").get(null);
  Method capture=scopes.getMethod("capture");check(capture.invoke(instance)==null,"Scope leaked before call");
  Object action=proxy(function(0),(p,m,a)->{check(capture.invoke(instance)==context,"Missing scoped context");return true;});
  scopes.getMethod("with",contextClass,function(0)).invoke(instance,context,action);check(capture.invoke(instance)==null,"Scope leaked after call");
  System.out.println("Actual composed policy/scope runtime PASS: all living defaults, precedence, invalid config, 10000 selections without config reads, original session and scope restoration. Simulated entities/config; not Paper gameplay.");
 }
}
