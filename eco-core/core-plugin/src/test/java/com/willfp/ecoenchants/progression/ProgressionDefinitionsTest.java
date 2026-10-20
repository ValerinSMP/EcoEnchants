package com.willfp.ecoenchants.progression;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ProgressionDefinitionsTest {
 YamlConfiguration load(String path)throws Exception{
  try(var in=getClass().getClassLoader().getResourceAsStream("enchants/"+path)){
   assertNotNull(in);return YamlConfiguration.loadConfiguration(new InputStreamReader(in,StandardCharsets.UTF_8));
  }
 }
 @Test void existingFamiliesUseNativeEffectsAndOneFamilyUnlock()throws Exception{
  var a=load("ecoskills/angler.yml");var i=load("infernal_touch.yml");
  assertEquals(2,a.getInt("max-level"));assertEquals("15 * %level%",a.getString("placeholder"));
  assertEquals("skill_xp_multiplier",a.getMapList("effects").getFirst().get("id"));
  assertEquals("1 + 0.15 * %level%",((Map<?,?>)a.getMapList("effects").getFirst().get("args")).get("multiplier"));
  assertEquals(4,((Number)((Map<?,?>)a.getMapList("conditions").getFirst().get("args")).get("level")).intValue());
  assertEquals(1,i.getInt("max-level"));assertEquals("autosmelt",i.getMapList("effects").getFirst().get("id"));
  assertEquals(List.of("silk_touch"),i.getStringList("conflicts"));
  assertEquals(true,((Map<?,?>)i.getMapList("effects").getFirst().get("args")).get("drop_xp"));
  assertEquals(12,((Number)((Map<?,?>)i.getMapList("conditions").getFirst().get("args")).get("level")).intValue());
 }
 @Test void executableDefinitionsUseRegisteredEffectsAndOneNativeFamilyRequirement()throws Exception{
  for(String id:List.of("eco_resonante","sello_glacial")){
   var c=load(id+".yml");
   assertTrue(c.getBoolean("enabled"));assertTrue(c.getBoolean("tradeable"));assertTrue(c.getBoolean("discoverable"));assertTrue(c.getBoolean("enchantable"));
   assertEquals(3,c.getInt("max-level"));assertEquals(1,c.getMapList("effects").size());
   var effect=c.getMapList("effects").getFirst();
   assertEquals(id.equals("eco_resonante")?"resonant_pulse":"glacial_seal",effect.get("id"));
   assertEquals(List.of("confirmed_original_hit"),effect.get("triggers"));
   var args=(Map<?,?>)effect.get("args");
   assertEquals(id.equals("eco_resonante")?8:10,((Number)args.get("proc_cooldown")).intValue());
   assertEquals(id.equals("eco_resonante")?3:2,((Number)args.get("hits")).intValue());
   assertEquals(id.equals("eco_resonante")?5:4,((Number)args.get("window")).intValue());
   assertEquals("has_skill_level",c.getMapList("conditions").getFirst().get("id"));
   assertEquals(id.equals("eco_resonante")?16:18,((Number)((Map<?,?>)c.getMapList("conditions").getFirst().get("args")).get("level")).intValue());
  }
  assertEquals(List.of("cleave"),load("eco_resonante.yml").getStringList("conflicts"));
  assertEquals(List.of("frost"),load("sello_glacial.yml").getStringList("conflicts"));
 }
 @Test void approvedRebalanceDefaultsRemainAtTheirNineMovementLevels()throws Exception{
  Map<String,Integer> requirements=Map.of("feather_step",7,"infernal_touch",12,"lifesteal",15,"adrenaline",18,"veinminer",28,"first_strike",16,"criticals",14);
  for(var e:requirements.entrySet()){
   var conditions=load(e.getKey()+".yml").getMapList("conditions");
   var c=conditions.stream().filter(x->x.get("id").equals("has_skill_level")).findFirst().orElseThrow();
   assertEquals(e.getValue(),((Number)((Map<?,?>)c.get("args")).get("level")).intValue(),e.getKey());
  }
 }
}
