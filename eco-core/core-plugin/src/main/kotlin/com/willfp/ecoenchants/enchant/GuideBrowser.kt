package com.willfp.ecoenchants.enchant

import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.page.Page
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecoenchants.target.EnchantmentTargets
import com.willfp.ecoenchants.plugin
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Uses native eco menus and Bukkit conversations; no per-tick task, network or database access. */
internal object GuideBrowser : Listener {
    private var registered = false
    private var generation = 0L
    private var equipmentTargets: List<Set<String>> = emptyList()
    private val searches = mutableMapOf<UUID, Conversation>()
    val enabled get() = plugin.configYml.getBool("enchant-guide.enabled")

    fun reload() {
        generation++
        val pending = searches.values.toList()
        searches.clear()
        pending.forEach { it.abandon() }
        equipmentTargets = if (!enabled || !plugin.isEnabled) emptyList() else filters().map { config ->
            val ids = config.getStrings("targets").toSet()
            if (ids.isEmpty()) emptySet() else {
                // Expand combined targets (for example melee and armor) once per reload,
                // using the configured native matchers and their representative items.
                val examples = ids.mapNotNull { EnchantmentTargets[it] }.flatMap { it.items }.map { it.item }
                ids + EnchantmentTargets.values().filter { target -> examples.any { target.matches(it) } }.map { it.id }
            }
        }
        if (!registered && enabled) {
            plugin.server.pluginManager.registerEvents(this, plugin)
            registered = true
        }
    }

    fun state(menu: Menu, player: Player) = GuideState(
        menu.getState<String>(player, "guideQuery") ?: "",
        menu.getState<Int>(player, "guideFilter") ?: 0,
        menu.getState<String>(player, "groupId"), menu.getPage(player)
    )

    fun restore(menu: Menu, player: Player, state: GuideState) {
        menu.setState(player, "guideQuery", state.query)
        menu.setState(player, "guideFilter", state.filter)
        menu.setState(player, "groupId", state.group)
        menu.setState(player, Page.PAGE_KEY, state.page)
        menu.refresh(player)
    }

    fun acceptedTargets(index: Int): Set<String> = equipmentTargets.getOrNull(index) ?: emptySet()
    private fun filters() = plugin.configYml.getSubsections("enchant-guide.filters")

    /** Close before opening so native onClose returns any captive item exactly once. */
    fun navigate(player: Player, action: () -> Unit) {
        val epoch = generation
        val inventory = player.openInventory.topInventory
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (plugin.isEnabled && generation == epoch && player.isOnline &&
                player.openInventory.topInventory === inventory) {
                player.closeInventory()
                action()
            }
        })
    }

    fun controls(builder: MenuBuilder, resume: (Player, GuideState) -> Unit) {
        if (!enabled) return
        fun button(key: String, init: com.willfp.eco.core.gui.slot.SlotBuilder.() -> Unit) {
            val path = "enchant-guide.$key"
            builder.setSlot(plugin.configYml.getInt("$path.row"), plugin.configYml.getInt("$path.column"), slot {
                setUpdater { player, menu, _ ->
                    val state = state(menu, player)
                    val label = filters().getOrNull(state.filter)?.getFormattedString("name") ?: ""
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("$path.item")))
                        .setDisplayName(plugin.configYml.getFormattedString("$path.name"))
                        .addLoreLines(plugin.configYml.getFormattedStrings("$path.lore").map {
                            // Insert literal user input only AFTER formatting the configuration.
                            it.replace("%query%", state.query.replace('§', ' ').replace('&', ' ').replace('<', ' ').replace('>', ' '))
                                .replace("%filter%", label)
                                .replace("%count%", (menu.getState<List<*>>(player, "enchants")?.size ?: 0).toString())
                        }).build()
                }
                init()
            })
        }
        button("search") {
            onLeftClick { event, _, menu -> search(event.whoClicked as Player, state(menu, event.whoClicked as Player), resume) }
            onRightClick { event, _, menu ->
                val player = event.whoClicked as Player
                restore(menu, player, state(menu, player).copy(query = "", page = 1))
            }
        }
        button("filter") {
            fun change(player: Player, menu: Menu, direction: Int) {
                val current = state(menu, player)
                restore(menu, player, current.copy(filter = GuideQuery.next(current.filter, direction, filters().size), page = 1))
            }
            onLeftClick { event, _, menu -> change(event.whoClicked as Player, menu, 1) }
            onRightClick { event, _, menu -> change(event.whoClicked as Player, menu, -1) }
        }
        button("clear") {
            onLeftClick { event, _, menu -> restore(menu, event.whoClicked as Player, GuideState()) }
        }
    }

    private fun search(player: Player, state: GuideState, resume: (Player, GuideState) -> Unit) {
        val inventory = player.openInventory.topInventory
        val epoch = generation
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (plugin.isEnabled && generation == epoch && player.isOnline &&
                player.openInventory.topInventory === inventory) beginSearch(player, state, resume)
        })
    }

    private fun beginSearch(player: Player, state: GuideState, resume: (Player, GuideState) -> Unit) {
        if (player.isConversing) {
            player.sendMessage(plugin.configYml.getFormattedString("enchant-guide.search.busy"))
            return
        }
        val epoch = generation
        val answer = AtomicReference<String?>()
        val prompt = plugin.configYml.getFormattedString("enchant-guide.search.prompt")
        lateinit var conversation: Conversation
        conversation = ConversationFactory(plugin)
            .withLocalEcho(false).withModality(true).withTimeout(30).withEscapeSequence("cancelar")
            .withFirstPrompt(object : StringPrompt() {
                override fun getPromptText(context: ConversationContext): String =
                    prompt
                override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                    answer.set(GuideQuery.input(input ?: ""))
                    return Prompt.END_OF_CONVERSATION
                }
            })
            .addConversationAbandonedListener {
                // Chat may complete off-thread. All menu/session operations return to main.
                if (plugin.isEnabled) plugin.server.scheduler.runTask(plugin, Runnable {
                    if (searches[player.uniqueId] !== conversation) return@Runnable
                    searches.remove(player.uniqueId)
                    if (generation == epoch && player.isOnline &&
                        player.openInventory.type == org.bukkit.event.inventory.InventoryType.CRAFTING) {
                        resume(player, state.copy(query = answer.get() ?: state.query, page = 1))
                    }
                })
            }.buildConversation(player)
        player.closeInventory()
        searches[player.uniqueId] = conversation
        conversation.begin()
    }

    @EventHandler fun onOpen(event: InventoryOpenEvent) { cancel(event.player.uniqueId) }
    @EventHandler fun onQuit(event: PlayerQuitEvent) { cancel(event.player.uniqueId) }
    @EventHandler fun onDisable(event: PluginDisableEvent) {
        if (event.plugin == plugin) { reload(); registered = false }
    }
    private fun cancel(uuid: UUID) { searches.remove(uuid)?.abandon() }
}
