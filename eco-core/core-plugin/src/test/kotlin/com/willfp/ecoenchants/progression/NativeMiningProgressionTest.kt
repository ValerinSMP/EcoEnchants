package com.willfp.ecoenchants.progression
import org.bukkit.Material
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID
class NativeMiningProgressionTest {
 @Test fun `five crop ages guard immature and allow mature`(){
  for(crop in listOf(Material.WHEAT,Material.CARROTS,Material.POTATOES,Material.BEETROOTS,Material.NETHER_WART)){
   assertTrue(protectsImmatureCrop(crop,0,7));assertTrue(protectsImmatureCrop(crop,6,7));assertFalse(protectsImmatureCrop(crop,7,7));assertFalse(protectsImmatureCrop(crop,null,null))
  }
 }
 @Test fun `all stems guarded without blocking fruits or unrelated ageable blocks`(){
  for(stem in listOf(Material.MELON_STEM,Material.PUMPKIN_STEM,Material.ATTACHED_MELON_STEM,Material.ATTACHED_PUMPKIN_STEM))assertTrue(protectsImmatureCrop(stem,null,null))
  for(other in listOf(Material.MELON,Material.PUMPKIN,Material.COCOA,Material.SWEET_BERRY_BUSH,Material.STONE,Material.AIR))assertFalse(protectsImmatureCrop(other,0,7))
 }
 @Test fun `ten admitted manual hits activate exactly once and gaps expire the sequence`(){
  for(tier in 1..3){
   val state=MiningRhythmState(UUID.randomUUID(),tier,UUID.randomUUID(),0)
   for(i in 1..9)assertFalse(state.record(i*100L))
   assertTrue(state.record(1000));assertTrue(state.active(10999));assertFalse(state.active(11000))
   assertFalse(state.record(11000));assertEquals(1,state.count)
   repeat(8){assertFalse(state.record(11001L+it))};assertTrue(state.record(11010))
   repeat(100){assertFalse(state.record(11011L+it))};assertEquals(10,state.count)
  }
 }
 @Test fun `native event proof scope is exact and restored even on exception`(){
  val block=mock(Block::class.java);val player=mock(Player::class.java)
  val actual=BlockBreakEvent(block,player);val other=BlockBreakEvent(block,player)
  assertFalse(NativeMiningProgression.guarding(actual))
  assertThrows(IllegalStateException::class.java){NativeMiningProgression.withGuard(actual){
   assertTrue(NativeMiningProgression.guarding(actual));assertFalse(NativeMiningProgression.guarding(other));error("scope")
  }}
  assertFalse(NativeMiningProgression.guarding(actual));assertFalse(actual.isCancelled)
 }
}
