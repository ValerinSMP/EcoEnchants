package com.willfp.ecoenchants.progression

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import com.willfp.libreforge.triggers.event.TriggerDispatchEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID

class NativeProcAdaptersTest {
    @Test fun `native damage adapter observes cancellation last cause and actual health`() {
        val event=mock(EntityDamageByEntityEvent::class.java);val victim=mock(LivingEntity::class.java)
        `when`(victim.lastDamageCause).thenReturn(event);`when`(victim.health).thenReturn(15.0);`when`(event.finalDamage).thenReturn(10.0)
        assertEquals(10.0,NativeProcAdapters.confirmed(event,victim,25.0))
        `when`(event.isCancelled).thenReturn(true);assertEquals(0.0,NativeProcAdapters.confirmed(event,victim,25.0))
        `when`(event.isCancelled).thenReturn(false);`when`(victim.health).thenReturn(25.0)
        assertEquals(0.0,NativeProcAdapters.confirmed(event,victim,25.0))
        `when`(victim.health).thenReturn(15.0);`when`(victim.lastDamageCause).thenReturn(mock(EntityDamageByEntityEvent::class.java))
        assertEquals(0.0,NativeProcAdapters.confirmed(event,victim,25.0))
    }
    @Test fun `captured item is a clone and stable identity survives later weapon replacement`() {
        val item=mock(ItemStack::class.java);val copy=mock(ItemStack::class.java);val meta=mock(ItemMeta::class.java)
        val pdc=mock(PersistentDataContainer::class.java);val key=NamespacedKey("ecoenchants","original_item_id");val id=UUID.randomUUID()
        `when`(item.amount).thenReturn(1);`when`(item.clone()).thenReturn(copy);`when`(copy.itemMeta).thenReturn(meta)
        `when`(meta.persistentDataContainer).thenReturn(pdc);`when`(pdc.get(key,PersistentDataType.STRING)).thenReturn(id.toString())
        val frozen=NativeProcAdapters.capture(item,key)!!
        assertSame(copy,frozen.item);assertNotSame(item,frozen.item);assertEquals(id,frozen.id);assertFalse(frozen.stamped)
        verify(pdc,never()).set(any(NamespacedKey::class.java),eq(PersistentDataType.STRING),anyString())
        verify(item,never()).setItemMeta(any(ItemMeta::class.java))
    }
    @Test fun `missing item identity writes only its own PDC to the copied metadata`() {
        val item=mock(ItemStack::class.java);val copy=mock(ItemStack::class.java);val meta=mock(ItemMeta::class.java);val pdc=mock(PersistentDataContainer::class.java)
        val key=NamespacedKey("ecoenchants","original_item_id")
        `when`(item.amount).thenReturn(1);`when`(item.clone()).thenReturn(copy);`when`(copy.itemMeta).thenReturn(meta);`when`(meta.persistentDataContainer).thenReturn(pdc)
        val frozen=NativeProcAdapters.capture(item,key)!!
        assertTrue(frozen.stamped);verify(pdc).set(key,PersistentDataType.STRING,frozen.id.toString());verify(copy).setItemMeta(meta)
        verify(pdc,never()).remove(any(NamespacedKey::class.java));verify(item,never()).setItemMeta(any(ItemMeta::class.java))
    }
    @Test fun `glacial slow is exact transient percentage and never alters base stats or other modifiers`() {
        val attribute=mock(AttributeInstance::class.java);val key=NamespacedKey("ecoenchants","glacial_seal")
        val other=AttributeModifier(NamespacedKey("other","speed"),0.25,AttributeModifier.Operation.MULTIPLY_SCALAR_1)
        val old=AttributeModifier(key,-0.10,AttributeModifier.Operation.MULTIPLY_SCALAR_1)
        `when`(attribute.modifiers).thenReturn(listOf(other,old))
        val actual=NativeProcAdapters.slow(attribute,key,0.15)
        assertEquals(-0.15,actual.amount);assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1,actual.operation)
        verify(attribute).removeModifier(old);verify(attribute,never()).removeModifier(other);verify(attribute).addTransientModifier(actual)
        verify(attribute,never()).setBaseValue(anyDouble())
        assertThrows(IllegalArgumentException::class.java){NativeProcAdapters.slow(attribute,key,0.21)}
    }
    @Test fun `actual native trigger listener suppresses secondary procs and restores scope after exceptions`() {
        val event=mock(TriggerDispatchEvent::class.java)
        val dispatched=mock(com.willfp.libreforge.triggers.DispatchedTrigger::class.java)
        val trigger=mock(com.willfp.libreforge.triggers.Trigger::class.java)
        `when`(trigger.id).thenReturn("melee_attack");`when`(dispatched.trigger).thenReturn(trigger);`when`(event.trigger).thenReturn(dispatched)
        OriginalRuntime.withSecondaryDamage {
            OriginalRuntime.noSecondaryProcs(event)
            OriginalRuntime.withSecondaryDamage { OriginalRuntime.noSecondaryProcs(event) }
        }
        verify(event,times(2)).setCancelled(true)
        clearInvocations(event);OriginalRuntime.noSecondaryProcs(event);verifyNoInteractions(event)
        assertThrows(IllegalStateException::class.java){OriginalRuntime.withSecondaryDamage { throw IllegalStateException("damage callback") }}
        OriginalRuntime.noSecondaryProcs(event);verifyNoInteractions(event)
    }
}
