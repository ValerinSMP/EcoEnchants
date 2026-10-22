package com.willfp.ecoenchants.progression
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID

class SnipeDistanceTest {
 private fun world()=mock(World::class.java).also{`when`(it.uid).thenReturn(UUID.randomUUID())}
 @Test fun displacementIsThreeDimensionalFrozenAndIndependentOfThePlayersCurrentPosition(){
  val world=world();val launch=Location(world,0.0,64.0,0.0);val frozen=launch.clone()
  launch.x=9999.0
  assertEquals(10.0,launchDisplacement(frozen,Location(world,0.0,70.0,8.0))!!,1e-12)
  assertEquals(50.0,launchDisplacement(frozen,Location(world,30.0,64.0,40.0))!!,1e-12)
  assertNull(launchDisplacement(frozen,Location(world(),0.0,64.0,0.0)))
  assertNull(launchDisplacement(null,Location(world,0.0,64.0,0.0)))
  assertNull(launchDisplacement(frozen,Location(world,Double.NaN,64.0,0.0)))
 }
 @Test fun tierOneThroughFourUseExactlyOnePercentPerTierPerTenBlocksWithoutChangingOtherDistancePlaceholders(){
  for(tier in 1..4){
   assertEquals(1+tier/100.0,snipeMultiplier(tier.toDouble(),10.0)!!,1e-12)
   assertEquals(1+tier*0.025,snipeMultiplier(tier.toDouble(),25.0)!!,1e-12)
  }
  assertEquals(1.009,snipeMultiplier(1.0,9.0)!!,1e-12)
  assertEquals(1.0,snipeMultiplier(4.0,0.0))
  for(bad in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0))assertNull(snipeMultiplier(4.0,bad))
  assertNull(snipeMultiplier(Double.NaN,10.0))
 }
 @Test fun actualNativeShotLookupKeepsOriginalBowAndSessionAndRejectsShooterChangesAndRetiredShots(){
  val world=world();val player=mock(Player::class.java);val arrow=mock(Projectile::class.java)
  val owner=UUID.randomUUID();val projectile=UUID.randomUUID()
  `when`(player.uniqueId).thenReturn(owner);`when`(player.world).thenReturn(world)
  `when`(player.location).thenReturn(Location(world,9000.0,64.0,9000.0))
  `when`(arrow.uniqueId).thenReturn(projectile);`when`(arrow.world).thenReturn(world);`when`(arrow.shooter).thenReturn(player)
  val token=OriginalRuntime.session(player)!!
  val context=NativeBurstProgression.Context(owner,token,world.uid,mock(ItemStack::class.java),UUID.randomUUID(),mapOf("snipe" to 4),"arrow",false,false,Location(world,300.0,64.0,300.0))
  val shotType=Class.forName("com.willfp.ecoenchants.progression.NativeBurstProgression\$Shot")
  val ctor=shotType.getDeclaredConstructor(List::class.java).apply{isAccessible=true}
  val shot=ctor.newInstance(listOf(context))
  shotType.getDeclaredMethod("setOrigin",Location::class.java).apply{isAccessible=true}.invoke(shot,Location(world,0.0,64.0,0.0))
  val field=NativeBurstProgression::class.java.getDeclaredField("shots").apply{isAccessible=true}
  @Suppress("UNCHECKED_CAST") val ledger=field.get(NativeBurstProgression) as ShotLedger<Any>
  assertTrue(ledger.remember(projectile,owner,shot))
  try {
   clearInvocations(player)
   assertEquals(50.0,NativeBurstProgression.projectileDistance(arrow,player,Location(world,30.0,64.0,40.0))!!,1e-12)
   verify(player,never()).inventory;verify(player,never()).location
   `when`(arrow.shooter).thenReturn(mock(Player::class.java))
   assertNull(NativeBurstProgression.projectileDistance(arrow,player,Location(world,30.0,64.0,40.0)))
   `when`(arrow.shooter).thenReturn(player);ledger.removeOwner(owner)
   assertNull(NativeBurstProgression.projectileDistance(arrow,player,Location(world,30.0,64.0,40.0)))
  } finally {ledger.removeOwner(owner)}
 }
}
