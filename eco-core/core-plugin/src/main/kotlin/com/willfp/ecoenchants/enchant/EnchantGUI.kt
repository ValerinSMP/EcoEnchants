package com.willfp.ecoenchants.enchant

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.gui.GUIComponent
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.Page
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.EnchantedBookBuilder
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.items.isEcoEmpty
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.lineWrap
import com.willfp.ecoenchants.display.EnchantSorter.sortForDisplay
import com.willfp.ecoenchants.display.HideStoredEnchantsProxy
import com.willfp.ecoenchants.display.getFormattedDescription
import com.willfp.ecoenchants.display.getFormattedName
import com.willfp.ecoenchants.enchant.DiscoveryType
import com.willfp.ecoenchants.dragdrop.dragAndDropPriceDisplay
import com.willfp.ecoenchants.dragdrop.isDragAndDropEnabled
import com.willfp.ecoenchants.plugin
import com.willfp.ecoenchants.target.EnchantmentTargets.applicableEnchantments
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import com.willfp.ecoenchants.rarity.EnchantmentRarities
import com.willfp.ecoenchants.type.EnchantmentTypes
import com.willfp.ecoenchants.target.EnchantmentTargets
import kotlin.math.ceil

object EnchantGUI {
    private lateinit var menu: Menu
    private var groupMenu: Menu? = null
    private val enchantInfoMenus = Caffeine.newBuilder().maximumSize(256).build<Pair<EcoEnchant, Int>, Menu>()
    private var allEnchantsSorted: List<Enchantment> = emptyList()
    private var searchNames: Map<Enchantment, String> = emptyMap()

    internal fun reload() {
        GuideBrowser.reload()
        cachedEnchantmentSlots.invalidateAll()
        enchantInfoMenus.invalidateAll()
        allEnchantsSorted = EcoEnchants.values().map { it.enchantment }.sortForDisplay()
        searchNames = allEnchantsSorted.associateWith { GuideQuery.normalize(it.wrap().getFormattedName(0) + " " + it.key.key) }

        menu = menu(plugin.configYml.getInt("enchant-gui.rows")) {
            title = plugin.configYml.getFormattedString("enchant-gui.title")

            allowChangingHeldItem()

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(
                        plugin.configYml.getStrings("enchant-gui.mask.items")
                    ),
                    *plugin.configYml.getStrings("enchant-gui.mask.pattern").toTypedArray()
                )
            )

            setSlot(
                plugin.configYml.getInt("enchant-gui.info.row"),
                plugin.configYml.getInt("enchant-gui.info.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("enchant-gui.info.item")))
                        .addLoreLines(plugin.configYml.getStrings("enchant-gui.info.lore"))
                        .build()
                )
            )

            val captiveRow = plugin.configYml.getInt("enchant-gui.item-row")
            val captiveColumn = plugin.configYml.getInt("enchant-gui.item-column")

            setSlot(
                captiveRow, captiveColumn, slot(ItemStack(Material.AIR)) {
                    setCaptive(true)
                }
            )

            onRender { player, menu ->
                val atCaptive = menu.getCaptiveItem(player, captiveRow, captiveColumn)
                val hasItem = !atCaptive.isEcoEmpty && atCaptive != null && atCaptive.type != Material.BOOK

                val canSeeHidden = player.hasPermission("ecoenchants.seehidden")
                val baseEnchants = if (!hasItem) {
                    allEnchantsSorted.filter { (it.wrap() as? EcoEnchant)?.let { wrapped -> !wrapped.isHiddenFromGui || canSeeHidden } == true }
                } else {
                    atCaptive.applicableEnchantments.filter { !it.isHiddenFromGui || canSeeHidden }.map { it.enchantment }.sortForDisplay()
                        .subtract(atCaptive.fast().enchants.keys)
                        .toList()
                }

                // Apply group filter if a groupId is set in menu state
                val groupId = menu.getState<String>(player, "groupId")
                val filteredEnchants = if (groupId != null) {
                    val groupBy = plugin.configYml.getString("enchant-gui.group-by")
                    baseEnchants.filter { enchantment ->
                        val wrapped = enchantment.wrap()
                        when (groupBy) {
                            "type" -> wrapped.type.id == groupId
                            "rarity" -> wrapped.enchantmentRarity.id == groupId
                            "target" -> wrapped is EcoEnchant && wrapped.targets.any { it.id == groupId }
                            else -> true
                        }
                    }
                } else {
                    baseEnchants
                }

                val visibleEnchants = if (GuideBrowser.enabled) {
                    val state = GuideBrowser.state(menu, player)
                    val query = GuideQuery.normalize(state.query)
                    val targets = GuideBrowser.acceptedTargets(state.filter)
                    filteredEnchants.filter { enchantment ->
                        (searchNames[enchantment] ?: "").contains(query) &&
                            GuideQuery.matchesTargets((enchantment.wrap() as? EcoEnchant)?.targets?.map { it.id }?.toSet() ?: emptySet(), targets)
                    }
                } else filteredEnchants
                menu.setState(player, "enchants", visibleEnchants)

                // Reset to page 1 when an item is placed or removed from the captive slot
                val previousHasItem = menu.getState<Boolean>(player, "hasItem") ?: false
                if (hasItem != previousHasItem) {
                    menu.setState(player, Page.PAGE_KEY, 1)
                }
                menu.setState(player, "hasItem", hasItem)

                // Safety net: also reset if the current page now exceeds the new max.
                // Compute directly from filteredEnchants to avoid a stale getMaxPage() value
                // (maxPages may be evaluated before onRender fires).
                val perPage = plugin.configYml.getInt("enchant-gui.enchant-area.width") * plugin.configYml.getInt("enchant-gui.enchant-area.height")
                val maxPage = if (visibleEnchants.isEmpty()) {
                    1
                } else {
                    ceil(visibleEnchants.size.toDouble() / perPage).toInt()
                }
                if (menu.getPage(player) > maxPage) {
                    menu.setState(player, Page.PAGE_KEY, 1)
                }
            }

            GuideBrowser.controls(this, ::openBrowser)

            val pane = EnchantmentScrollPane()

            addComponent(
                plugin.configYml.getInt("enchant-gui.enchant-area.row"),
                plugin.configYml.getInt("enchant-gui.enchant-area.column"),
                pane
            )

            val pageChangeSound = PlayableSound.create(
                plugin.configYml.getSubsection("enchant-gui.page-change.sound")
            )

            for (direction in PageChanger.Direction.entries) {
                val directionName = direction.name.lowercase()

                addPageChanger(
                    plugin.configYml,
                    "enchant-gui.page-change.$directionName",
                    direction,
                    pageChangeSound
                )
            }

            if (plugin.configYml.getBool("enchant-gui.close-button.enabled")) {
                setSlot(
                    plugin.configYml.getInt("enchant-gui.close-button.row"),
                    plugin.configYml.getInt("enchant-gui.close-button.column"),
                    slot(
                        ItemStackBuilder(Items.lookup(plugin.configYml.getString("enchant-gui.close-button.item")))
                            .setDisplayName(plugin.configYml.getFormattedString("enchant-gui.close-button.name"))
                            .addLoreLines(plugin.configYml.getStrings("enchant-gui.close-button.lore"))
                            .build()
                    ) {
                        onLeftClick { event, _ -> event.whoClicked.closeInventory() }
                    }
                )
            }

            // Back button to return to the group selection menu
            if (plugin.configYml.getBool("enchant-gui.grouped")
                && plugin.configYml.getBool("enchant-gui.back-button.enabled")) {
                setSlot(
                    plugin.configYml.getInt("enchant-gui.back-button.row"),
                    plugin.configYml.getInt("enchant-gui.back-button.column"),
                    slot(
                        ItemStackBuilder(Items.lookup(plugin.configYml.getString("enchant-gui.back-button.item")))
                            .addLoreLines(plugin.configYml.getStrings("enchant-gui.back-button.lore"))
                            .build()
                    ) {
                        onLeftClick { event, _ ->
                            val groupGui = groupMenu ?: return@onLeftClick
                            val player = event.whoClicked as Player
                            GuideBrowser.navigate(player) { groupGui.open(player) }
                        }
                    }
                )
            }

            maxPages { player ->
                val enchants = menu.getState<List<Enchantment>>(player, "enchants") ?: emptyList()
                val total = enchants.size
                val perPage = pane.size

                val pages = if (total == 0) {
                    1
                } else {
                    ceil(total.toDouble() / perPage).toInt()
                }
                pages
            }

            onClose { event, menu ->
                val player = event.player as Player
                returnCaptiveItems(player, menu.getCaptiveItems(player))
            }

            for (config in plugin.configYml.getSubsections("enchant-gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }

        // Build the group selection menu (only when grouped mode is enabled)
        if (plugin.configYml.getBool("enchant-gui.grouped")) {
            groupMenu = menu(plugin.configYml.getInt("group-gui.rows")) {
                title = plugin.configYml.getFormattedString("group-gui.title")

                setMask(
                    FillerMask(
                        MaskItems.fromItemNames(
                            plugin.configYml.getStrings("group-gui.mask.items")
                        ),
                        *plugin.configYml.getStrings("group-gui.mask.pattern").toTypedArray()
                    )
                )

                // Add a clickable slot for each configured group
                for (config in plugin.configYml.getSubsections("group-gui.groups")) {
                    val groupId = config.getString("id")

                    // Validate the group ID exists in the registry matching the group-by axis
                    val groupBy = plugin.configYml.getString("enchant-gui.group-by")
                    val valid = when (groupBy) {
                        "type" -> EnchantmentTypes[groupId] != null
                        "rarity" -> EnchantmentRarities[groupId] != null
                        "target" -> EnchantmentTargets[groupId] != null
                        else -> false
                    }

                    if (!valid) {
                        continue
                    }

                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        slot(
                            ItemStackBuilder(Items.lookup(config.getString("item")))
                                .addLoreLines(config.getStrings("lore"))
                                .build()
                        ) {
                            onLeftClick { event, _ ->
                                openGroupGUI(event.whoClicked as Player, groupId)
                            }
                        }
                    )
                }

                // Custom decorator slots for the group menu
                for (config in plugin.configYml.getSubsections("group-gui.custom-slots")) {
                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        ConfigSlot(config)
                    )
                }
            }
        } else {
            groupMenu = null
        }
    }

    private fun returnCaptiveItems(player: Player, items: List<ItemStack>) {
        if (items.isEmpty()) return

        val leftovers = player.inventory.addItem(*items.toTypedArray()).values
        if (leftovers.isNotEmpty()) {
            DropQueue(player)
                .addItems(leftovers)
                .forceTelekinesis()
                .push()
        }
    }

    fun openGUI(player: Player) {
        if (plugin.configYml.getBool("enchant-gui.grouped") && groupMenu != null) {
            groupMenu!!.open(player)
        } else {
            menu.open(player)
        }
    }

    internal fun openBrowser(player: Player, state: GuideState) {
        menu.open(player)
        GuideBrowser.restore(menu, player, state)
    }

    fun openInfoGUI(player: Player, enchant: EcoEnchant, level: Int = -1) {
        openGuideInfo(player, enchant, level, null)
    }

    internal fun openGuideInfo(player: Player, enchant: EcoEnchant, level: Int, returnState: GuideState?) {
        val effectiveLevel = if (level == -1) {
            if (plugin.configYml.getBool("enchantinfo.item.show-max-level")) enchant.maximumLevel else 1
        } else {
            level.coerceIn(1, enchant.maximumLevel)
        }

        val infoMenu = enchantInfoMenus.get(enchant to effectiveLevel) {
            menu(plugin.configYml.getInt("enchantinfo.rows")) {
                title = enchant.getFormattedName(effectiveLevel)

                setSlot(
                    plugin.configYml.getInt("enchantinfo.item.row"),
                    plugin.configYml.getInt("enchantinfo.item.column"),
                    slot {
                        setUpdater { viewer, _, _ -> enchant.getInformationSlot(viewer, effectiveLevel).getItemStack(viewer) }
                    }
                )

                setMask(
                    FillerMask(
                        MaskItems.fromItemNames(plugin.configYml.getStrings("enchantinfo.mask.items")),
                        *plugin.configYml.getStrings("enchantinfo.mask.pattern").toTypedArray()
                    )
                )

                if (GuideBrowser.enabled) {
                    fun button(key: String, delta: Int) {
                        val path = "enchant-guide.levels.$key"
                        setSlot(plugin.configYml.getInt("$path.row"), plugin.configYml.getInt("$path.column"), slot {
                            setUpdater { _, _, _ ->
                                ItemStackBuilder(Items.lookup(plugin.configYml.getString("$path.item")))
                                    .setDisplayName(plugin.configYml.getFormattedString("$path.name"))
                                    .addLoreLines(plugin.configYml.getFormattedStrings("$path.lore").map {
                                        it.replace("%level%", effectiveLevel.toString()).replace("%max_level%", enchant.maximumLevel.toString())
                                    }).build()
                            }
                            onLeftClick { event, _, current ->
                                val viewer = event.whoClicked as Player
                                val next = GuideQuery.level(effectiveLevel, delta, enchant.maximumLevel)
                                if (next != effectiveLevel) {
                                    val previous = current.getState<GuideState>(viewer, "guideReturn")
                                    GuideBrowser.navigate(viewer) { openGuideInfo(viewer, enchant, next, previous) }
                                }
                            }
                        })
                    }
                    button("previous", -1)
                    button("next", 1)
                    val path = "enchant-guide.levels.back"
                    setSlot(plugin.configYml.getInt("$path.row"), plugin.configYml.getInt("$path.column"), slot(
                        ItemStackBuilder(Items.lookup(plugin.configYml.getString("$path.item")))
                            .setDisplayName(plugin.configYml.getFormattedString("$path.name"))
                            .addLoreLines(plugin.configYml.getFormattedStrings("$path.lore")).build()
                    ) {
                        onLeftClick { event, _, current ->
                            val viewer = event.whoClicked as Player
                            val previous = current.getState<GuideState>(viewer, "guideReturn") ?: GuideState()
                            GuideBrowser.navigate(viewer) { openBrowser(viewer, previous) }
                        }
                    })
                }

                for (config in plugin.configYml.getSubsections("enchantinfo.custom-slots")) {
                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        ConfigSlot(config)
                    )
                }
            }
        }
        infoMenu.open(player)
        infoMenu.setState(player, "guideReturn", returnState)
    }

    private fun openGroupGUI(player: Player, groupId: String) {
        menu.open(player)
        menu.setState(player, "groupId", groupId)
        menu.setState(player, Page.PAGE_KEY, 1)
    }
}

private class EnchantmentScrollPane : GUIComponent {
    private val defaultSlot = slot(Items.lookup(plugin.configYml.getString("enchant-gui.empty-item")))

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot {
        val index = column + ((row - 1) * columns) - 1
        val page = menu.getPage(player)

        val enchants = menu.getState<List<Enchantment>>(player, "enchants") ?: return defaultSlot
        if (enchants.isEmpty()) {
            if (GuideBrowser.enabled && index == 0) return slot(
                ItemStackBuilder(Items.lookup(plugin.configYml.getString("enchant-guide.empty.item")))
                    .setDisplayName(plugin.configYml.getFormattedString("enchant-guide.empty.name"))
                    .addLoreLines(plugin.configYml.getFormattedStrings("enchant-guide.empty.lore")).build()
            )
            return defaultSlot
        }

        val enchant = enchants.getOrNull(index + size * (page - 1))?.wrap() as? EcoEnchant ?: return defaultSlot

        val displayLevel = if (plugin.configYml.getBool("enchantinfo.item.show-max-level")) enchant.maximumLevel else 1
        val information = enchant.getInformationSlot(player, displayLevel)
        if (!GuideBrowser.enabled) return information
        return slot(information.getItemStack(player)) {
            onLeftClick { event, _, current ->
                val viewer = event.whoClicked as Player
                val previous = GuideBrowser.state(current, viewer)
                GuideBrowser.navigate(viewer) { EnchantGUI.openGuideInfo(viewer, enchant, displayLevel, previous) }
            }
        }
    }

    override fun getRows() = plugin.configYml.getInt("enchant-gui.enchant-area.height")
    override fun getColumns() = plugin.configYml.getInt("enchant-gui.enchant-area.width")

    val size = rows * columns
}

private val cachedEnchantmentSlots = Caffeine.newBuilder()
    .build<Pair<EcoEnchant, Int>, Slot>()

private fun EcoEnchant.getInformationSlot(player: Player, level: Int): Slot {
    fun build(): Slot = slot(
            EnchantedBookBuilder()
                .addStoredEnchantment(enchantment, level)
                .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                .setDisplayName(this.getFormattedName(level))
                .addLoreLines(this.getFormattedDescription(level, player))
                .addLoreLines(if (GuideBrowser.enabled) {
                    val sources = mutableListOf<String>()
                    fun add(id: String, available: Boolean) {
                        if (available) sources += plugin.configYml.getFormattedString("enchant-guide.sources.$id")
                    }
                    add("table", plugin.configYml.getBool("enchanting-table.enabled") && isObtainableThroughEnchanting)
                    add("trading", plugin.configYml.getBool("villager.enabled") && isObtainableThroughTrading)
                    add("loot", plugin.configYml.getBool("loot.enabled") && isObtainableThroughDiscovery)
                    plugin.configYml.getFormattedStrings("enchant-guide.book-lore").map {
                        it.replace("%level%", level.toString()).replace("%sources%", sources.joinToString(", ").ifEmpty {
                            plugin.configYml.getFormattedString("enchant-guide.sources.other")
                        })
                    }
                } else emptyList())
                .addLoreLines {
                    plugin.configYml.getStrings("enchantinfo.item.lore")
                        .map {
                            it.replace("%max_level%", enchantment.maxLevel.toString())
                                .replace("%rarity%", this.enchantmentRarity.displayName)
                                .replace(
                                    "%targets%",
                                    this.targets.joinToString(", ") { target -> target.displayName }
                                )
                                .replace(
                                    "%conflicts%",
                                    if (this.conflictsWithEverything) {
                                        plugin.langYml.getFormattedString("all-conflicts")
                                    } else {
                                        this.conflicts.joinToString(", ") { conflict ->
                                            conflict.wrap().getFormattedName(0)
                                        }.ifEmpty { plugin.langYml.getFormattedString("no-conflicts") }
                                    }
                                )
                                .replace(
                                    "%required%",
                                    this.required.joinToString(", ") { required ->
                                        required.wrap().getFormattedName(0)
                                    }.ifEmpty { plugin.langYml.getFormattedString("no-required") }
                                )
                                .replace("%tradeable%", this.isObtainableThroughTrading.parseYesOrNo())
                                .replace("%discoverable%", this.isObtainableThroughDiscovery.parseYesOrNo())
                                .replace("%discoverable_chests%", this.isObtainableThrough(DiscoveryType.CHESTS).parseYesOrNo())
                                .replace("%discoverable_fishing%", this.isObtainableThrough(DiscoveryType.FISHING).parseYesOrNo())
                                .replace("%discoverable_mob_drops%", this.isObtainableThrough(DiscoveryType.MOB_DROPS).parseYesOrNo())
                                .replace("%discoverable_raids%", this.isObtainableThrough(DiscoveryType.RAIDS).parseYesOrNo())
                                .replace("%enchantable%", this.isObtainableThroughEnchanting.parseYesOrNo())
                                .replace("%drag_and_drop%", this.isDragAndDropEnabled().parseYesOrNo())
                                .replace("%drag_and_drop_price%", if (this.isDragAndDropEnabled()) this.dragAndDropPriceDisplay(player, level) else false.parseYesOrNo())
                        }
                        .formatEco()
                        .flatMap {
                            it.lineWrap(32, true)
                        }
                }
                .build()
                .fast()
                .apply {
                    plugin.getProxy(HideStoredEnchantsProxy::class.java).hideStoredEnchants(this)
                }
                .unwrap()
        )
    return if (GuideBrowser.enabled) build() else cachedEnchantmentSlots.get(this to level) { build() }
}

fun Boolean.parseYesOrNo(): String =
    if (this) plugin.langYml.getFormattedString("yes") else plugin.langYml.getFormattedString("no")
