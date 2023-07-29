package com.github.davidpav123.davidstreefeller

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException
import java.util.*

class DavidsTreeFeller : JavaPlugin(), Listener {
    private val header = Component.text(name).color(NamedTextColor.BLUE)

    // Files
    private var config: Configuration? = null
    private var maxBlocks = -1
    private var axeNeeded = true
    private var damageAxe = true
    private var breakAxe = false
    private var replant = true
    private var invincibleReplant = false
    private var admitNetherTrees = true
    private var startActivated = true
    private var joinMsg = true
    private var ignoreLeaves = false
    private var sneakingPrevention = "false"
    private var treeMap: HashMap<Material, MutableList<Material>>? = null

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        config = Configuration(
            "plugins/DavidsTreeFeller/config.yml", "Davids Tree Feller"
        )
        loadConfiguration()
        saveConfiguration()
        logger.info("Enabled")
    }

    private fun loadConfiguration() {
        config!!.reloadConfig()
        maxBlocks = config!!.getInt(STRG_MAX_BLOCKS, maxBlocks)
        config!!.setInfo(STRG_MAX_BLOCKS, DESC_MAX_BLOCKS)
        axeNeeded = config!!.getBoolean(STRG_AXE_NEEDED, axeNeeded)
        config!!.setInfo(STRG_AXE_NEEDED, DESC_AXE_NEEDED)
        damageAxe = config!!.getBoolean(STRG_DAMAGE_AXE, damageAxe)
        config!!.setInfo(STRG_DAMAGE_AXE, DESC_DAMAGE_AXE)
        breakAxe = config!!.getBoolean(STRG_BREAK_AXE, damageAxe)
        config!!.setInfo(STRG_BREAK_AXE, DESC_BREAK_AXE)
        replant = config!!.getBoolean(STRG_REPLANT, replant)
        config!!.setInfo(STRG_REPLANT, DESC_REPLANT)
        invincibleReplant = config!!.getBoolean(STRG_INVINCIBLE_REPLANT, invincibleReplant)
        config!!.setInfo(STRG_INVINCIBLE_REPLANT, DESC_INVINCIBLE_REPLANT)
        admitNetherTrees = config!!.getBoolean(STRG_ADMIT_NETHER_TREES, admitNetherTrees)
        config!!.setInfo(STRG_ADMIT_NETHER_TREES, DESC_ADMIT_NETHER_TREES)
        startActivated = config!!.getBoolean(STRG_START_ACTIVATED, startActivated)
        config!!.setInfo(STRG_START_ACTIVATED, DESC_START_ACTIVATED)
        joinMsg = config!!.getBoolean(STRG_JOIN_MSG, joinMsg)
        config!!.setInfo(STRG_JOIN_MSG, DESC_JOIN_MSG)
        ignoreLeaves = config!!.getBoolean(STRG_IGNORE_LEAVES, ignoreLeaves)
        config!!.setInfo(STRG_IGNORE_LEAVES, DESC_IGNORE_LEAVES)
        val defaultSP = sneakingPrevention
        sneakingPrevention = config!!.getString(STRG_SNEAKING_PREVENTION, defaultSP).lowercase(Locale.getDefault())
        config!!.setInfo(STRG_SNEAKING_PREVENTION, DESC_SNEAKING_PREVENTION)
        if (!sneakingPrevention.equals(
                "true", ignoreCase = true
            ) && sneakingPrevention != "inverted" && sneakingPrevention != "false"
        ) {
            sneakingPrevention = defaultSP
        }
    }

    private fun saveConfiguration() {
        try {
            config!!.setValue(STRG_MAX_BLOCKS, maxBlocks)
            config!!.setValue(STRG_AXE_NEEDED, axeNeeded)
            config!!.setValue(STRG_DAMAGE_AXE, damageAxe)
            config!!.setValue(STRG_BREAK_AXE, breakAxe)
            config!!.setValue(STRG_REPLANT, replant)
            config!!.setValue(STRG_INVINCIBLE_REPLANT, invincibleReplant)
            config!!.setValue(STRG_ADMIT_NETHER_TREES, admitNetherTrees)
            config!!.setValue(STRG_START_ACTIVATED, startActivated)
            config!!.setValue(STRG_JOIN_MSG, joinMsg)
            config!!.setValue(STRG_IGNORE_LEAVES, ignoreLeaves)
            config!!.setValue(STRG_SNEAKING_PREVENTION, sneakingPrevention)
            config!!.saveConfig()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDisable() {
        saveConfig()
        logger.info("Disabled")
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (joinMsg) {
            val p = e.player
            var enabled = startActivated
            val metas = p.getMetadata(PLAYER_ENABLE_META)
            for (meta in metas) {
                enabled = meta.asBoolean()
            }
            val playerName = p.name
            if (enabled) {
                p.sendMessage(
                    Component.text("Remember ").color(NamedTextColor.BLUE)
                        .append(Component.text(playerName).color(NamedTextColor.GOLD))
                        .append(Component.text(", you can use ", NamedTextColor.WHITE))
                        .append(Component.text("/tf toggle", NamedTextColor.GOLD))
                        .append(Component.text(" to avoid breaking things made of logs.", NamedTextColor.WHITE))
                )
            } else {
                p.sendMessage(
                    Component.text("Remember ").color(NamedTextColor.BLUE)
                        .append(Component.text(playerName).color(NamedTextColor.GOLD))
                        .append(Component.text(", you can use ", NamedTextColor.WHITE))
                        .append(Component.text("/tf toggle", NamedTextColor.GOLD))
                        .append(Component.text(" to cut down trees faster.", NamedTextColor.WHITE))
                )
            }

        }
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        val firstBrokenB = event.block
        val material = firstBrokenB.blockData.material
        val player = event.player
        var tool: ItemStack? = player.inventory.itemInMainHand
        if (invincibleReplant && !(canPlant(
                firstBrokenB.world.getBlockAt(
                    firstBrokenB.x, firstBrokenB.y - 1, firstBrokenB.z
                ), material
            ) || canPlant(
                firstBrokenB, firstBrokenB.world.getBlockAt(firstBrokenB.x, firstBrokenB.y + 1, firstBrokenB.z).type
            ))
        ) {
            val fbbReplantMetas = firstBrokenB.getMetadata(META_INV_REPL)
            for (replantMeta in fbbReplantMetas) {
                if (replantMeta.asBoolean()) {
                    val actual = System.currentTimeMillis()
                    val metasMsg = player.getMetadata("msged")
                    if (metasMsg.isEmpty() || actual - 5000 > metasMsg[0].asLong()) {
                        player.sendMessage(
                            header.append(
                                Component.text("This sapling is protected, please don't try to break it.")
                                    .color(NamedTextColor.WHITE)
                            )
                        )
                        player.setMetadata("msged", FixedMetadataValue(this, actual))
                    }
                    event.isCancelled = true
                    return
                }
            }
        }
        firstBrokenB.removeMetadata(META_INV_REPL, this)
        firstBrokenB.world.getBlockAt(firstBrokenB.x, firstBrokenB.y - 1, firstBrokenB.z)
            .removeMetadata(META_INV_REPL, this)
        firstBrokenB.world.getBlockAt(firstBrokenB.x, firstBrokenB.y + 1, firstBrokenB.z)
            .removeMetadata(META_INV_REPL, this)
        if (sneakingPrevention == "true" && player.pose == Pose.SNEAKING || sneakingPrevention == "inverted" && player.pose != Pose.SNEAKING || player.gameMode != GameMode.SURVIVAL) {
            return
        }
        var enabled = startActivated
        val metas = player.getMetadata(PLAYER_ENABLE_META)
        for (meta in metas) {
            enabled = meta.asBoolean()
        }
        if (enabled && !event.isCancelled && isLog(material) && player.hasPermission("davidstreefeller.user")) {
            try {
                // Yes it could use some tuning
                if (!tool!!.type.name.contains("_AXE")) {
                    tool = null
                }
                var cutDown = !axeNeeded || tool != null && tool.type.name.endsWith("_AXE")
                if (cutDown && axeNeeded && !breakAxe && tool!!.hasItemMeta() && tool.itemMeta is Damageable && (tool.itemMeta as Damageable).damage >= tool.type.maxDurability) {
                    cutDown = false
                }
                if (cutDown) {
                    if (replant) {
                        breakRecReplant(player, tool, firstBrokenB, material, 0, false)
                    } else {
                        breakRecNoReplant(player, tool, firstBrokenB, material, 0, false)
                    }
                    event.isCancelled = true
                }
            } catch (e: StackOverflowError) {
                Bukkit.getLogger().throwing("DavidsTreeFeller.kt", "onBlockBreak(Event)", e)
            }
        }
    }

    private fun breakRecNoReplant(
        player: Player, tool: ItemStack?, lego: Block, type: Material, destroyed: Int, stop: Boolean
    ): Int {
        var destroyed = destroyed
        var stop = stop
        if (stop) return destroyed
        val material = lego.blockData.material
        if (isLog(material) || isLeaves(material)) {
            if (maxBlocks in 1..<destroyed) {
                return destroyed
            }
            val mundo = lego.world
            if (damageItem(player, tool, material)) {
                stop = true
            } else {
                if (lego.breakNaturally()) {
                    destroyed++
                } else {
                    return destroyed
                }
            }
            val x = lego.x
            val y = lego.y
            val z = lego.z
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x, y - 1, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x, y + 1, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z - 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z - 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecNoReplant(player, tool, mundo.getBlockAt(x, y, z - 1), type, destroyed, stop)
        }
        return destroyed
    }

    private fun breakRecReplant(
        player: Player, tool: ItemStack?, lego: Block, type: Material, destroyed: Int, stop: Boolean
    ): Int {
        var destroyed = destroyed
        if (stop || maxBlocks in 1..<destroyed) return destroyed
        val material = lego.blockData.material
        if (isLog(material) || isLeaves(material)) {
            val mundo = lego.world
            val x = lego.x
            val y = lego.y
            val z = lego.z
            val below = mundo.getBlockAt(x, y - 1, z)
            if (canPlant(below, lego.type)) {
                var saplingType: Material? = null
                when (lego.type) {
                    Material.ACACIA_LOG -> saplingType = Material.ACACIA_SAPLING
                    Material.BIRCH_LOG -> saplingType = Material.BIRCH_SAPLING
                    Material.DARK_OAK_LOG -> saplingType = Material.DARK_OAK_SAPLING
                    Material.JUNGLE_LOG -> saplingType = Material.JUNGLE_SAPLING
                    Material.OAK_LOG -> saplingType = Material.OAK_SAPLING
                    Material.SPRUCE_LOG -> saplingType = Material.SPRUCE_SAPLING
                    Material.MANGROVE_LOG -> saplingType = Material.MANGROVE_PROPAGULE
                    Material.CRIMSON_STEM -> saplingType = Material.CRIMSON_FUNGUS
                    Material.WARPED_STEM -> saplingType = Material.WARPED_FUNGUS
                    Material.CHERRY_LOG -> saplingType = Material.CHERRY_SAPLING
                    else -> {}
                }
                if (damageItem(player, tool, material)) {
                    return destroyed
                } else {
                    if (lego.breakNaturally()) {
                        if (saplingType != null) {
                            lego.type = saplingType
                            lego.setMetadata(META_INV_REPL, FixedMetadataValue(this, true))
                            below.setMetadata(META_INV_REPL, FixedMetadataValue(this, true))
                        }
                        destroyed++
                    } else {
                        return destroyed
                    }
                }
            } else {
                if (damageItem(player, tool, material)) {
                    return destroyed
                } else {
                    if (lego.breakNaturally()) {
                        destroyed++
                    } else {
                        return destroyed
                    }
                }
            }
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x, y - 1, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x, y + 1, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z - 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z - 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x, y, z + 1), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z), type, destroyed, stop)
            if (destroyed < maxBlocks || maxBlocks < 0) destroyed =
                breakRecReplant(player, tool, mundo.getBlockAt(x, y, z - 1), type, destroyed, stop)
        }
        return destroyed
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        var label = label
        label = label.lowercase(Locale.getDefault())
        var good = label == command.label
        val cmds = command.aliases.toTypedArray()
        var i = 0
        while (i < cmds.size && !good) {
            cmds[i] = cmds[i].lowercase(Locale.getDefault())
            if (label == cmds[i]) {
                good = true
            }
            i++
        }
        val noPerms = false
        if (good) {
            if (args.isNotEmpty()) {
                when (args[0].lowercase(Locale.getDefault())) {
                    "help" -> sender.sendMessage(
                        header.append(Component.text(" Commands:\n").color(NamedTextColor.WHITE))
                            .append(Component.text("/$label help: ").color(NamedTextColor.GOLD)).append(
                                Component.text("Shows this help message.\n").color(NamedTextColor.WHITE).append(
                                    Component.text("/$label toggle <true/false>: ").color(NamedTextColor.GOLD)
                                ).append(
                                    Component.text("Toggles tree felling on and off.").color(NamedTextColor.WHITE)
                                )
                            )
                    )

                    "toggle" -> {
                        if (sender is Player) {
                            var enabled = startActivated
                            val metas = sender.getMetadata(PLAYER_ENABLE_META)
                            for (meta in metas) {
                                enabled = meta.asBoolean()
                            }
                            enabled = !enabled
                            sender.setMetadata(PLAYER_ENABLE_META, FixedMetadataValue(this, enabled))
                            sender.sendMessage(
                                header.append(
                                    Component.text(" You " + (if (enabled) "enabled" else "disabled") + " tree felling.")
                                        .color(NamedTextColor.WHITE)
                                )
                            )
                        } else {
                            sender.sendMessage(
                                header.append(
                                    Component.text("This command can only be used by players")
                                        .color(NamedTextColor.WHITE)
                                )
                            )
                        }
                    }


                    else -> sender.sendMessage(
                        header.append(
                            Component.text("Command not found, please check \\\"/\" + label + \" help\\\".")
                                .color(NamedTextColor.DARK_RED)
                        )
                    )
                }
            } else {
                return false
            }
        }
        if (noPerms) {
            sender.sendMessage(
                header.append(
                    Component.text("You don't have permission to use this command.").color(NamedTextColor.DARK_RED)
                )
            )
        }
        return good
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<String>
    ): List<String> {
        val list: MutableList<String> = ArrayList()
        when (args.size) {
            0 -> {
                list.add("help")
                list.add("toggle")
            }

            1 -> {
                args[0] = args[0].lowercase(Locale.getDefault())
                when (args[0]) {
                    "help", "toggle" -> {}
                    else -> {
                        if ("help".contains(args[0])) list.add("help")
                        if ("toggle".contains(args[0])) list.add("toggle")
                    }
                }
            }

            else -> {}
        }
        return list
    }

    /**
     * Deals 1 damage to an item, if possible
     *
     * @param player
     * @param tool
     * @return true if item is destroyed or should not be damaged anymore, false if
     * not damageable or damaged but not destroyed
     */
    private fun damageItem(player: Player, tool: ItemStack?, material: Material): Boolean {
        if (axeNeeded && damageAxe && tool != null && isLog(material)) {
            val meta = tool.itemMeta
            if (meta is Damageable) {
                val maxDmg = tool.type.maxDurability
                var dmg: Int = meta.damage

                // damageable.setDamage(++dmg);
                // Substituted for the following code by exwundee (https://github.com/exwundee)
                // This adds support for any level of the Durability enchantment
                run {
                    val rand = Random()
                    val unbLevel = tool.getEnchantmentLevel(Enchantment.DURABILITY)
                    if (rand.nextInt(unbLevel + 1) == 0) {
                        meta.damage = ++dmg
                    }
                }
                tool.setItemMeta(meta)
                if (dmg >= maxDmg) {
                    if (breakAxe) {
                        tool.amount = 0
                        player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
                    } else {
                        meta.damage = maxDmg - 1
                        tool.setItemMeta(meta)
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun isLog(mat: Material): Boolean {
        val ret = !mat.name.contains("STRIPPED_") && mat.name.contains("_LOG")
        return if (!ret && admitNetherTrees) mat.name == "CRIMSON_STEM" || mat.name == "WARPED_STEM" else ret
    }

    private fun isLeaves(mat: Material): Boolean {
        if (ignoreLeaves) return false
        val ret = mat.name.contains("LEAVES")
        return if (!ret && admitNetherTrees) mat.name == "NETHER_WART_BLOCK" || mat.name == "WARPED_WART_BLOCK" || mat.name == "SHROOMLIGHT" else ret
    }

    private fun canPlant(below: Block, woodType: Material): Boolean {
        if (treeMap == null) {
            treeMap = HashMap(10)

            // Elegance is my passion /s
            val woods = ArrayList<Material>(9)
            try {
                woods.add(Material.OAK_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.DARK_OAK_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.SPRUCE_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.ACACIA_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.AZALEA)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.BIRCH_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.JUNGLE_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.MANGROVE_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.CHERRY_LOG)
            } catch (e: NoSuchFieldError) {
                // Material doesn't exist in this version
            }
            for (wood in woods) {
                treeMap!![wood] = ArrayList(
                    listOf(
                        Material.DIRT,
                        Material.GRASS_BLOCK,
                        Material.COARSE_DIRT,
                        Material.PODZOL,
                        Material.MYCELIUM,
                        Material.ROOTED_DIRT,
                        Material.MOSS_BLOCK,
                        Material.FARMLAND,
                        Material.MUD
                    )
                )
            }
            treeMap!![Material.MANGROVE_LOG]!!.add(Material.CLAY)
        }
        if (admitNetherTrees && !treeMap!!.containsKey(Material.WARPED_STEM)) {
            treeMap!![Material.WARPED_STEM] = listOf(Material.WARPED_NYLIUM).toMutableList()
            treeMap!![Material.CRIMSON_STEM] = listOf(Material.CRIMSON_NYLIUM).toMutableList()
        } else if (!admitNetherTrees && treeMap!!.containsKey(Material.WARPED_STEM)) {
            treeMap!!.remove(Material.WARPED_STEM)
            treeMap!!.remove(Material.CRIMSON_STEM)
        }
        return treeMap!!.getOrDefault(woodType, ArrayList(0)).contains(below.type)
    }

    companion object {
        // Options
        private const val STRG_MAX_BLOCKS = "destroy limit"
        private const val DESC_MAX_BLOCKS =
            "Sets the maximum number of logs and leaves that can be destroyed at once. -1 to unlimit."
        private const val STRG_AXE_NEEDED = "axe needed"
        private const val DESC_AXE_NEEDED = "Sets if an axe is required to Cut down trees at once."
        private const val STRG_DAMAGE_AXE = "damage axe"
        private const val DESC_DAMAGE_AXE =
            "If \"$STRG_AXE_NEEDED\" is set to true, sets if axes used are damaged or not. If \"$STRG_AXE_NEEDED\" is false, this option is ignored."
        private const val STRG_BREAK_AXE = "break axe"
        private const val DESC_BREAK_AXE =
            "If \"$STRG_AXE_NEEDED\" and \"$STRG_DAMAGE_AXE\" are set to true, sets if the axe should not be broken. Otherwise this option is ignored."
        private const val STRG_REPLANT = "replant"
        private const val DESC_REPLANT = "Sets if trees should be replanted automatically."
        private const val STRG_INVINCIBLE_REPLANT = "invincible replant"
        private const val DESC_INVINCIBLE_REPLANT =
            "Sets if saplings replanted by this plugin should be unbreakable by regular players (including the block beneath)."
        private const val META_INV_REPL = "inv_repl"
        private const val STRG_ADMIT_NETHER_TREES = "cut nether \"trees\""
        private const val DESC_ADMIT_NETHER_TREES =
            "Sets if nether trees should be treated as regular trees, and cut down entirely."
        private const val STRG_START_ACTIVATED = "start activated"
        private const val DESC_START_ACTIVATED =
            "Sets if this plugin starts activated for players when they enter the server. If false, players will need to use /tf toggle to activate it for themselves."
        private const val STRG_JOIN_MSG = "initial message"
        private const val DESC_JOIN_MSG =
            "If true, it sends each player a message about /tf toggle when they join the server. The message changes depending on the value of \"$STRG_START_ACTIVATED\"."
        private const val STRG_IGNORE_LEAVES = "ignore leaves"
        private const val DESC_IGNORE_LEAVES =
            "If true, leaves will not be destroyed and will not connect logs. In vanilla terrain forests this will prevent several trees to be cut down at once, but it will leave most big oak trees floating."
        private const val STRG_SNEAKING_PREVENTION = "crouch for prevention"
        private const val DESC_SNEAKING_PREVENTION =
            "If true, crouching players won't trigger this plugin or only crouching players will. If \"inverted\", players will have to crouch to destroy trees instantly. False by default so updating from previous versions won't change this behaviour without notice."

        // Metadata
        private const val PLAYER_ENABLE_META = "davidpav_tree_feller_meta_disable"
    }
}
