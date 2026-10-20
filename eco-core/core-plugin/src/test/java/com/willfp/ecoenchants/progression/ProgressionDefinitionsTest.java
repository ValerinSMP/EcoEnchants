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
  assertEquals(5,((Number)((Map<?,?>)i.getMapList("conditions").getFirst().get("args")).get("level")).intValue());
 }
 @Test void blockedFamiliesNeverReferencePretendEffectsOrAdvertiseAvailability()throws Exception{
  for(String id:List.of("eco_resonante","sello_glacial")){
   var c=load("_"+id+".yml");
   assertFalse(c.getBoolean("enabled"));assertFalse(c.getBoolean("tradeable"));assertFalse(c.getBoolean("discoverable"));assertFalse(c.getBoolean("enchantable"));
   assertTrue(c.getMapList("effects").isEmpty());assertEquals(3,c.getInt("max-level"));
   assertEquals("has_skill_level",c.getMapList("conditions").getFirst().get("id"));
  }
  assertTrue(load("_eco_resonante.yml").getStringList("conflicts").contains("cleave"));
  assertEquals(List.of("frost"),load("_sello_glacial.yml").getStringList("conflicts"));
 }
}
