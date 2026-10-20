package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.Vector
class NativeReactiveOutcomeTest {
 @Test fun `invalid or unknown outcomes cannot authorize retry while explicit veto can`(){
  val event=mock(EntityDamageEvent::class.java)
  for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,10.0)){`when`(event.finalDamage).thenReturn(value);assertFalse(explicitlyRejectedDamage(event))}
  `when`(event.finalDamage).thenReturn(0.0);assertTrue(explicitlyRejectedDamage(event));`when`(event.finalDamage).thenReturn(10.0);`when`(event.isCancelled).thenReturn(true);assertTrue(explicitlyRejectedDamage(event))
 }
 @Test fun `native impulse rejects nonfinite or zero physics values`(){
  assertTrue(finiteNativeImpulse(Vector(1.0,.2,0.0)));assertFalse(finiteNativeImpulse(Vector()))
  assertFalse(finiteNativeImpulse(Vector(Double.NaN,0.0,0.0)));assertFalse(finiteNativeImpulse(Vector(Double.POSITIVE_INFINITY,0.0,0.0)));assertFalse(finiteNativeImpulse(Vector(Double.MAX_VALUE,0.0,0.0)))
 }
}
