package com.willfp.ecoenchants.api

import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.EcoEnchantLike
import com.willfp.ecoenchants.enchant.wrap
import com.willfp.libreforge.refreshHolders
import com.willfp.libreforge.toDispatcher
import io.papermc.paper.registry.RegistryAccess
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.lang.reflect.Proxy
import java.util.UUID

/** Simulated registry/metadata/holders; does not bootstrap or claim a live Paper/Nexo server. */
class EnchantRemovalTest {
 private class Fixture : AutoCloseable {
  val access=mockStatic(RegistryAccess::class.java)
  init {
   // Supply all registry variants during Paper's static legacy-registry initialization.
   val provider=mock(RegistryAccess::class.java){ invocation ->
    if(invocation.method.name=="getRegistry") Proxy.newProxyInstance(Registry::class.java.classLoader,arrayOf(Registry::class.java)){_,method,_->
     when(method.name){"iterator"->emptyList<Any>().iterator();"stream"->java.util.stream.Stream.empty<Any>();else->null}
    } else org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation)
   }
   access.`when`<RegistryAccess>{RegistryAccess.registryAccess()}.thenReturn(provider)
  }
  val enchants=mockStatic(Enchantment::class.java)
  val bukkit=mockStatic(Bukkit::class.java)
  val wraps=mockStatic(Class.forName("com.willfp.ecoenchants.enchant.UtilKt"))
  val holders=mockStatic(Class.forName("com.willfp.libreforge.HolderProviderKt"))
  val player=mock(Player::class.java)
  val inventory=mock(PlayerInventory::class.java)
  val manager=mock(PluginManager::class.java)
  val id=UUID.randomUUID()
  var currentPlayer:Player?=player
  var current:ItemStack?=null
  var writes=0
  var refreshes=0
  var onEvent:(Event)->Unit={}
  val events=mutableListOf<Event>()
  init {
   `when`(player.uniqueId).thenReturn(id);`when`(player.isOnline).thenReturn(true)
   `when`(player.inventory).thenReturn(inventory);`when`(inventory.storageContents).thenReturn(arrayOfNulls<ItemStack>(36))
   `when`(inventory.getItem(0)).thenAnswer{current}
   doAnswer{current=it.getArgument(1);writes++;null}.`when`(inventory).setItem(eq(0),any())
   bukkit.`when`<Boolean>{Bukkit.isPrimaryThread()}.thenReturn(true)
   bukkit.`when`<Player?>{Bukkit.getPlayer(id)}.thenAnswer{currentPlayer}
   bukkit.`when`<PluginManager>{Bukkit.getPluginManager()}.thenReturn(manager)
   doAnswer{val event=it.getArgument<Event>(0);events.add(event);onEvent(event);null}.`when`(manager).callEvent(any())
   holders.`when`<Unit>{player.toDispatcher().refreshHolders()}.thenAnswer{refreshes++;Unit}
  }
  fun enchant(id:String,custom:Boolean=false,type:String="normal"):Pair<Enchantment,EcoEnchantLike>{
   val enchant=mock(Enchantment::class.java);val key=NamespacedKey.minecraft(id)
   `when`(enchant.key).thenReturn(key)
   enchants.`when`<Enchantment?>{Enchantment.getByKey(key)}.thenReturn(enchant)
   val native=if(custom)mock(EcoEnchant::class.java,RETURNS_DEEP_STUBS) else mock(EcoEnchantLike::class.java,RETURNS_DEEP_STUBS)
   `when`(native.type.id).thenReturn(type)
   wraps.`when`<EcoEnchantLike>{enchant.wrap()}.thenReturn(native)
   if(native is EcoEnchant) `when`(native.hasRequiredEnchantments(anyCollection())).thenReturn(true)
   return enchant to native
  }
  // Complete before/after images are cloned; opaque metadata stands for PDC/Nexo/lore.
  val images=java.util.IdentityHashMap<ItemStack,Image>()
  data class Image(val direct:MutableMap<Enchantment,Int>,val stored:MutableMap<Enchantment,Int>,val opaque:Map<String,String>,val book:Boolean)
  fun item(direct:Map<Enchantment,Int>,stored:Map<Enchantment,Int> = emptyMap(),book:Boolean=false,opaque:Map<String,String> = mapOf("nexo:id" to "warden_blade","lore" to "original","other:pdc" to "untouched")):ItemStack {
   return object : ItemStack() {
    var image=Image(direct.toMutableMap(),stored.toMutableMap(),opaque.toMap(),book)
    init {images[this]=image}
    override fun getType()=if(book)Material.ENCHANTED_BOOK else Material.DIAMOND_SWORD
    override fun getAmount()=1
    override fun clone():ItemStack=item(image.direct,image.stored,image.book,image.opaque)
    override fun equals(other:Any?):Boolean=other===this || (other is ItemStack && images[other]==image)
    override fun hashCode()=image.hashCode()
    override fun getItemMeta():ItemMeta {
     val local=Image(image.direct.toMutableMap(),image.stored.toMutableMap(),image.opaque,image.book)
     val meta=if(book)mock(EnchantmentStorageMeta::class.java) else mock(ItemMeta::class.java)
     metadata[meta]=local
     `when`(meta.enchants).thenAnswer{local.direct.toMap()}
     `when`(meta.getEnchantLevel(any())).thenAnswer{local.direct[it.getArgument<Enchantment>(0)]?:0}
     `when`(meta.removeEnchant(any())).thenAnswer{local.direct.remove(it.getArgument<Enchantment>(0))!=null}
     if(meta is EnchantmentStorageMeta){
      `when`(meta.storedEnchants).thenAnswer{local.stored.toMap()}
      `when`(meta.getStoredEnchantLevel(any())).thenAnswer{local.stored[it.getArgument<Enchantment>(0)]?:0}
      `when`(meta.removeStoredEnchant(any())).thenAnswer{local.stored.remove(it.getArgument<Enchantment>(0))!=null}
     };return meta
    }
    override fun setItemMeta(meta:ItemMeta?):Boolean {
     image=metadata[meta]?:return false;images[this]=image;return true
    }
   }
  }

  val metadata=java.util.IdentityHashMap<ItemMeta,Image>()
  fun plan(enchant:Enchantment,level:Int=5)=EnchantUpgradeAPI.planRemoval(player,0,current!!,enchant,level,UUID.randomUUID())
  override fun close(){holders.close();wraps.close();bukkit.close();enchants.close();access.close()}
 }
 @Test fun removesOneOrdinaryEnchantAndRefreshesHoldersOnceWithoutPurchaseOrXpEvents(){Fixture().use{f->
  val (target,native)=f.enchant("sharpness");val (other,_)=f.enchant("smite")
  val original=f.item(mapOf(target to 5,other to 3));f.current=original
  val validation=f.plan(target);assertTrue(validation.allowed,validation.reason)
  val result=EnchantUpgradeAPI.applyRemoval(f.player,validation.plan!!)
  assertEquals(EnchantUpgradeAPI.Status.APPLIED,result.status);assertEquals(1,f.writes);assertEquals(1,f.refreshes)
  assertEquals(mapOf(target to 5,other to 3),f.images[original]!!.direct)
  assertEquals(mapOf(other to 3),f.images[f.current]!!.direct)
  assertEquals(f.images[original]!!.opaque,f.images[f.current]!!.opaque)
  assertEquals(listOf(EnchantRemovalPrepareEvent::class.java,EnchantRemovalAppliedEvent::class.java),f.events.map{it.javaClass})
  verify(native,never()).canEnchantItem(original,emptyList())
 }}
 @Test fun storedRemovalPreservesOtherStoredEnchantsAndRejectsAmbiguousEntries(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");val (other,_)=f.enchant("smite")
  f.current=f.item(emptyMap(),mapOf(target to 5,other to 4),true)
  val plan=f.plan(target).plan!!;assertTrue(plan.removal.stored)
  assertEquals(EnchantUpgradeAPI.Status.APPLIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertEquals(mapOf(other to 4),f.images[f.current]!!.stored)
  f.current=f.item(mapOf(target to 5),mapOf(target to 5),true)
  assertEquals("ambiguous_enchantment_storage",f.plan(target).reason)
 }}
 @Test fun reservedSpecialMissingChangedLevelAndRemainingPrerequisitesAreRejected(){Fixture().use{f->
  val (reserved,_)=f.enchant("soulbound",true);f.current=f.item(mapOf(reserved to 5));assertFalse(f.plan(reserved).allowed)
  val (special,_)=f.enchant("special",true,"special");f.current=f.item(mapOf(special to 5));assertFalse(f.plan(special).allowed)
  val (target,_)=f.enchant("sharpness");val (dependent,native)=f.enchant("dependent",true)
  f.current=f.item(mapOf(target to 5,dependent to 1));`when`((native as EcoEnchant).hasRequiredEnchantments(anyCollection())).thenReturn(false)
  assertTrue(f.plan(target).reason!!.startsWith("remaining_prerequisite_missing:"))
  f.current=f.item(mapOf(target to 4));assertFalse(f.plan(target).allowed)
  f.current=f.item(emptyMap());assertFalse(f.plan(target).allowed)
  assertEquals(0,f.writes);assertEquals(0,f.refreshes);assertTrue(f.events.isEmpty())
 }}
 @Test fun cancellationAndCallbackItemChangesNeverApplyThePlannedRemoval(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");f.current=f.item(mapOf(target to 5));val plan=f.plan(target).plan!!
  f.onEvent={if(it is EnchantRemovalPrepareEvent)it.isCancelled=true}
  assertEquals(EnchantUpgradeAPI.Status.CANCELLED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  f.onEvent={if(it is EnchantRemovalPrepareEvent)f.current=f.item(mapOf(target to 4))}
  assertNotEquals(EnchantUpgradeAPI.Status.APPLIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertEquals(0,f.writes);assertEquals(0,f.refreshes)
 }}
 @Test fun callbackRequirementChangesAndReconnectionInvalidateThePlan(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");val (other,native)=f.enchant("dependent",true)
  f.current=f.item(mapOf(target to 5,other to 1));val plan=f.plan(target).plan!!
  f.onEvent={if(it is EnchantRemovalPrepareEvent)`when`((native as EcoEnchant).hasRequiredEnchantments(anyCollection())).thenReturn(false)}
  assertEquals(EnchantUpgradeAPI.Status.DENIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  f.currentPlayer=mock(Player::class.java)
  assertEquals("plan_session_changed",EnchantUpgradeAPI.applyRemoval(f.player,plan).reason)
  assertEquals(0,f.writes)
 }}
 @Test fun appliedCallbackMutationIsReviewAndCannotReplayTheOriginalImage(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");f.current=f.item(mapOf(target to 5));val plan=f.plan(target).plan!!
  f.onEvent={if(it is EnchantRemovalAppliedEvent)f.current=f.item(emptyMap(),opaque=mapOf("changed" to "callback"))}
  assertEquals(EnchantUpgradeAPI.Status.APPLIED_CALLBACK_REVIEW,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertNotEquals(EnchantUpgradeAPI.Status.APPLIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertEquals(1,f.writes);assertEquals(1,f.refreshes)
 }}
 @Test fun planAndEventsExportDefensiveImagesAndRejectAsyncCalls(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");f.current=f.item(mapOf(target to 5));val plan=f.plan(target).plan!!
  assertNotSame(plan.getExpectedItem(),plan.getExpectedItem());assertEquals(5,plan.removal.level)
  f.bukkit.`when`<Boolean>{Bukkit.isPrimaryThread()}.thenReturn(false)
  assertThrows(IllegalStateException::class.java){f.plan(target)}
  assertThrows(IllegalStateException::class.java){EnchantUpgradeAPI.applyRemoval(f.player,plan)}
 }}
 @Test fun uncertainInventoryWriteIsReviewAndDoesNotAutomaticallyRetryOrRefreshHolders(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");f.current=f.item(mapOf(target to 5));val plan=f.plan(target).plan!!
  doAnswer{f.current=it.getArgument(1);f.writes++;throw IllegalStateException("simulated uncertain write")}.`when`(f.inventory).setItem(eq(0),any())
  assertEquals(EnchantUpgradeAPI.Status.UNKNOWN_APPLY_REVIEW,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertNotEquals(EnchantUpgradeAPI.Status.APPLIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  assertEquals(1,f.writes);assertEquals(0,f.refreshes)
 }}
 @Test fun ordinaryCustomRemovalNeverEvaluatesPurchaseSkillConditions(){Fixture().use{f->
  val (target,native)=f.enchant("ordinary_custom",true)
  doThrow(AssertionError("Purchase conditions must not run for removal")).`when`(native as EcoEnchant).conditions
  f.current=f.item(mapOf(target to 5));val plan=f.plan(target).plan!!
  assertEquals(EnchantUpgradeAPI.Status.APPLIED,EnchantUpgradeAPI.applyRemoval(f.player,plan).status)
  verify(native,never()).conditions
 }}
 @Test fun invalidSlotOfflinePlayerAndChangesOutsideEnchantmentsDenyBeforeCallbacks(){Fixture().use{f->
  val (target,_)=f.enchant("sharpness");f.current=f.item(mapOf(target to 5))
  assertEquals("invalid_storage_slot",EnchantUpgradeAPI.planRemoval(f.player,36,f.current!!,target,5,UUID.randomUUID()).reason)
  val altered=f.item(mapOf(target to 5),opaque=mapOf("nexo:id" to "different"))
  assertEquals("original_item_changed",EnchantUpgradeAPI.planRemoval(f.player,0,altered,target,5,UUID.randomUUID()).reason)
  `when`(f.player.isOnline).thenReturn(false)
  assertEquals("player_session_unavailable",f.plan(target).reason)
  assertEquals(0,f.writes);assertTrue(f.events.isEmpty())
 }}
}
