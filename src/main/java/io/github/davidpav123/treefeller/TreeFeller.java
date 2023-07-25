package io.github.davidpav123.treefeller;

import objs.Configuration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.*;

public class TreeFeller extends JavaPlugin implements Listener {
    // Options
    private static final String STRG_MAX_BLOCKS = "destroy limit";
    private static final String DESC_MAX_BLOCKS = "Sets the maximum number of logs and leaves that can be destroyed at once. -1 to unlimit.";
    private static final String STRG_AXE_NEEDED = "axe needed";
    private static final String DESC_AXE_NEEDED = "Sets if an axe is required to Cut down trees at once.";
    private static final String STRG_DAMAGE_AXE = "damage axe";
    private static final String DESC_DAMAGE_AXE = "If \"" + STRG_AXE_NEEDED + "\" is set to true, sets if axes used are damaged or not. If \"" + STRG_AXE_NEEDED + "\" is false, this option is ignored.";
    private static final String STRG_BREAK_AXE = "break axe";
    private static final String DESC_BREAK_AXE = "If \"" + STRG_AXE_NEEDED + "\" and \"" + STRG_DAMAGE_AXE + "\" are set to true, sets if the axe should not be broken. Otherwise this option is ignored.";
    private static final String STRG_REPLANT = "replant";
    private static final String DESC_REPLANT = "Sets if trees should be replanted automatically.";
    private static final String STRG_INVINCIBLE_REPLANT = "invincible replant";
    private static final String DESC_INVINCIBLE_REPLANT = "Sets if saplings replanted by this plugin should be unbreakable by regular players (including the block beneath).";
    private static final String META_INV_REPL = "inv_repl";
    private static final String STRG_ADMIT_NETHER_TREES = "cut nether \"trees\"";
    private static final String DESC_ADMIT_NETHER_TREES = "Sets if nether trees should be treated as regular trees, and cut down entirely.";
    private static final String STRG_START_ACTIVATED = "start activated";
    private static final String DESC_START_ACTIVATED = "Sets if this plugin starts activated for players when they enter the server. If false, players will need to use /tf toggle to activate it for themselves.";
    private static final String STRG_JOIN_MSG = "initial message";
    private static final String DESC_JOIN_MSG = "If true, it sends each player a message about /tf toggle when they join the server. The message changes depending on the value of \"" + STRG_START_ACTIVATED + "\".";
    private static final String STRG_IGNORE_LEAVES = "ignore leaves";
    private static final String DESC_IGNORE_LEAVES = "If true, leaves will not be destroyed and will not connect logs. In vanilla terrain forests this will prevent several trees to be cut down at once, but it will leave most big oak trees floating.";
    private static final String STRG_SNEAKING_PREVENTION = "crouch for prevention";
    private static final String DESC_SNEAKING_PREVENTION = "If true, crouching players won't trigger this plugin or only crouching players will. If \"inverted\", players will have to crouch to destroy trees instantly. False by default so updating from previous versions won't change this behaviour without notice.";
    // Metadata
    private static final String PLAYER_ENABLE_META = "davidpav_tree_feller_meta_disable";
    private final PluginDescriptionFile desc = getDescription();
    // Colors
    private final ChatColor mainColor = ChatColor.BLUE;
    private final ChatColor textColor = ChatColor.WHITE;
    private final ChatColor accentColor = ChatColor.GOLD;
    private final ChatColor errorColor = ChatColor.DARK_RED;
    private final String header = mainColor + "[" + desc.getName() + "] " + textColor;
    // Messages
    private final String joinMensajeActivated = header + "Remember " + accentColor + "{player}" + textColor + ", you can use " + accentColor + "/tf toggle" + textColor + " to avoid breaking things made of logs.";
    private final String joinMensajeDeactivated = header + "Remember " + accentColor + "{player}" + textColor + ", you can use " + accentColor + "/tf toggle" + textColor + " to cut down trees faster.";
    // Files
    private Configuration config;
    private int maxBlocks = -1;
    private boolean axeNeeded = true;
    private boolean damageAxe = true;
    private boolean breakAxe = false;
    private boolean replant = true;
    private boolean invincibleReplant = false;
    private boolean admitNetherTrees = true;
    private boolean startActivated = true;
    private boolean joinMsg = true;
    private boolean ignoreLeaves = false;
    private String sneakingPrevention = "false";
    private HashMap<Material, List<Material>> treeMap;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        config = new Configuration("plugins/DavidsTreeFeller/config.yml", "Davids Tree Feller");
        loadConfiguration();
        saveConfiguration();

        getLogger().info("Enabled");
    }

    private void loadConfiguration() {
        config.reloadConfig();

        maxBlocks = config.getInt(STRG_MAX_BLOCKS, maxBlocks);
        config.setInfo(STRG_MAX_BLOCKS, DESC_MAX_BLOCKS);


        axeNeeded = config.getBoolean(STRG_AXE_NEEDED, axeNeeded);
        config.setInfo(STRG_AXE_NEEDED, DESC_AXE_NEEDED);

        damageAxe = config.getBoolean(STRG_DAMAGE_AXE, damageAxe);
        config.setInfo(STRG_DAMAGE_AXE, DESC_DAMAGE_AXE);

        breakAxe = config.getBoolean(STRG_BREAK_AXE, damageAxe);
        config.setInfo(STRG_BREAK_AXE, DESC_BREAK_AXE);

        replant = config.getBoolean(STRG_REPLANT, replant);
        config.setInfo(STRG_REPLANT, DESC_REPLANT);

        invincibleReplant = config.getBoolean(STRG_INVINCIBLE_REPLANT, invincibleReplant);
        config.setInfo(STRG_INVINCIBLE_REPLANT, DESC_INVINCIBLE_REPLANT);

        admitNetherTrees = config.getBoolean(STRG_ADMIT_NETHER_TREES, admitNetherTrees);
        config.setInfo(STRG_ADMIT_NETHER_TREES, DESC_ADMIT_NETHER_TREES);

        startActivated = config.getBoolean(STRG_START_ACTIVATED, startActivated);
        config.setInfo(STRG_START_ACTIVATED, DESC_START_ACTIVATED);

        joinMsg = config.getBoolean(STRG_JOIN_MSG, joinMsg);
        config.setInfo(STRG_JOIN_MSG, DESC_JOIN_MSG);

        ignoreLeaves = config.getBoolean(STRG_IGNORE_LEAVES, ignoreLeaves);
        config.setInfo(STRG_IGNORE_LEAVES, DESC_IGNORE_LEAVES);

        String defaultSP = sneakingPrevention;
        sneakingPrevention = config.getString(STRG_SNEAKING_PREVENTION, defaultSP).toLowerCase();
        config.setInfo(STRG_SNEAKING_PREVENTION, DESC_SNEAKING_PREVENTION);

        if (!sneakingPrevention.equalsIgnoreCase("true") && !sneakingPrevention.equals("inverted") && !sneakingPrevention.equals("false")) {
            sneakingPrevention = defaultSP;
        }
    }

    private void saveConfiguration() {
        try {
            config.setValue(STRG_MAX_BLOCKS, maxBlocks);
            config.setValue(STRG_AXE_NEEDED, axeNeeded);
            config.setValue(STRG_DAMAGE_AXE, damageAxe);
            config.setValue(STRG_BREAK_AXE, breakAxe);
            config.setValue(STRG_REPLANT, replant);
            config.setValue(STRG_INVINCIBLE_REPLANT, invincibleReplant);
            config.setValue(STRG_ADMIT_NETHER_TREES, admitNetherTrees);
            config.setValue(STRG_START_ACTIVATED, startActivated);
            config.setValue(STRG_JOIN_MSG, joinMsg);
            config.setValue(STRG_IGNORE_LEAVES, ignoreLeaves);
            config.setValue(STRG_SNEAKING_PREVENTION, sneakingPrevention);
            config.saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled");
        saveConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (joinMsg) {
            Player p = e.getPlayer();
            boolean enabled = startActivated;
            List<MetadataValue> metas = p.getMetadata(PLAYER_ENABLE_META);
            for (MetadataValue meta : metas) {
                enabled = meta.asBoolean();
            }
            if (enabled) p.sendMessage(joinMensajeActivated.replace("{player}", p.getDisplayName()));
            else p.sendMessage(joinMensajeDeactivated.replace("{player}", p.getDisplayName()));
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        final Block firstBrokenB = event.getBlock();
        final Material material = firstBrokenB.getBlockData().getMaterial();
        final Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (invincibleReplant && !(canPlant(firstBrokenB.getWorld().getBlockAt(firstBrokenB.getX(), firstBrokenB.getY() - 1, firstBrokenB.getZ()), material) || canPlant(firstBrokenB, firstBrokenB.getWorld().getBlockAt(firstBrokenB.getX(), firstBrokenB.getY() + 1, firstBrokenB.getZ()).getType()))) {
            List<MetadataValue> fbbReplantMetas = firstBrokenB.getMetadata(META_INV_REPL);
            for (MetadataValue replantMeta : fbbReplantMetas) {
                if (replantMeta.asBoolean()) {
                    long actual = System.currentTimeMillis();
                    List<MetadataValue> metasMsg = player.getMetadata("msged");
                    if (metasMsg.isEmpty() || actual - 5000 > metasMsg.get(0).asLong()) {
                        player.sendMessage(header + "This sapling is protected, please don't try to break it.");
                        player.setMetadata("msged", new FixedMetadataValue(this, actual));
                    }
                    event.setCancelled(true);
                    return;
                }
            }
        }
        firstBrokenB.removeMetadata(META_INV_REPL, this);
        firstBrokenB.getWorld().getBlockAt(firstBrokenB.getX(), firstBrokenB.getY() - 1, firstBrokenB.getZ()).removeMetadata(META_INV_REPL, this);
        firstBrokenB.getWorld().getBlockAt(firstBrokenB.getX(), firstBrokenB.getY() + 1, firstBrokenB.getZ()).removeMetadata(META_INV_REPL, this);

        if ((sneakingPrevention.equals("true") && player.getPose().equals(Pose.SNEAKING)) || (sneakingPrevention.equals("inverted") && !player.getPose().equals(Pose.SNEAKING)) || (!player.getGameMode().equals(GameMode.SURVIVAL))) {
            return;
        }

        boolean enabled = startActivated;
        List<MetadataValue> metas = player.getMetadata(PLAYER_ENABLE_META);
        for (MetadataValue meta : metas) {
            enabled = meta.asBoolean();
        }

        if (enabled && !event.isCancelled() && isLog(material) && player.hasPermission("davidstreefeller.user")) {
            try {
                // Yes it could use some tuning
                if (!tool.getType().name().contains("_AXE")) {
                    tool = null;
                }

                boolean cutDown = !axeNeeded || (tool != null && tool.getType().name().endsWith("_AXE"));
                if (cutDown && axeNeeded && !breakAxe && (tool.hasItemMeta() && tool.getItemMeta() instanceof Damageable && ((Damageable) tool.getItemMeta()).getDamage() >= tool.getType().getMaxDurability())) {
                    cutDown = false;
                }
                if (cutDown) {
                    if (replant) {
                        breakRecReplant(player, tool, firstBrokenB, material, 0, false);
                    } else {
                        breakRecNoReplant(player, tool, firstBrokenB, material, 0, false);
                    }
                    event.setCancelled(true);
                }
            } catch (StackOverflowError e) {
                Bukkit.getLogger().throwing("TreeFeller.java", "onBlockBreak(Event)", e);
            }
        }

    }

    private int breakRecNoReplant(Player player, ItemStack tool, Block lego, Material type, int destroyed, boolean stop) {
        if (stop) return destroyed;
        Material material = lego.getBlockData().getMaterial();
        if (isLog(material) || isLeaves(material)) {
            if (destroyed > maxBlocks && maxBlocks > 0) {
                return destroyed;
            }
            World mundo = lego.getWorld();
            if (damageItem(player, tool, material)) {
                stop = true;
            } else {
                if (lego.breakNaturally()) {
                    destroyed++;
                } else {
                    return destroyed;
                }
            }

            int x = lego.getX(), y = lego.getY(), z = lego.getZ();

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x, y - 1, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x, y + 1, z), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z + 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z - 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z + 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z - 1), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x + 1, y, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x, y, z + 1), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x - 1, y, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecNoReplant(player, tool, mundo.getBlockAt(x, y, z - 1), type, destroyed, stop);
        }

        return destroyed;
    }

    private int breakRecReplant(Player player, ItemStack tool, Block lego, Material type, int destroyed, boolean stop) {
        if (stop || (maxBlocks > 0 && destroyed > maxBlocks)) return destroyed;
        Material material = lego.getBlockData().getMaterial();
        if (isLog(material) || isLeaves(material)) {
            World mundo = lego.getWorld();
            int x = lego.getX(), y = lego.getY(), z = lego.getZ();
            Block below = mundo.getBlockAt(x, y - 1, z);

            if (canPlant(below, lego.getType())) {
                Material saplingType = null;
                switch (lego.getType()) {
                    case ACACIA_LOG -> saplingType = Material.ACACIA_SAPLING;
                    case BIRCH_LOG -> saplingType = Material.BIRCH_SAPLING;
                    case DARK_OAK_LOG -> saplingType = Material.DARK_OAK_SAPLING;
                    case JUNGLE_LOG -> saplingType = Material.JUNGLE_SAPLING;
                    case OAK_LOG -> saplingType = Material.OAK_SAPLING;
                    case SPRUCE_LOG -> saplingType = Material.SPRUCE_SAPLING;
                    case MANGROVE_LOG -> saplingType = Material.MANGROVE_PROPAGULE;
                    case CRIMSON_STEM -> saplingType = Material.CRIMSON_FUNGUS;
                    case WARPED_STEM -> saplingType = Material.WARPED_FUNGUS;
                    case CHERRY_LOG -> saplingType = Material.CHERRY_SAPLING;
                    default -> {
                    }
                }

                if (damageItem(player, tool, material)) {
                    return destroyed;
                } else {
                    if (lego.breakNaturally()) {
                        if (saplingType != null) {
                            lego.setType(saplingType);
                            lego.setMetadata(META_INV_REPL, new FixedMetadataValue(this, true));
                            below.setMetadata(META_INV_REPL, new FixedMetadataValue(this, true));
                        }
                        destroyed++;
                    } else {
                        return destroyed;
                    }
                }

            } else {
                if (damageItem(player, tool, material)) {
                    return destroyed;
                } else {
                    if (lego.breakNaturally()) {
                        destroyed++;
                    } else {
                        return destroyed;
                    }
                }
            }

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x, y - 1, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x, y + 1, z), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z + 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z - 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z + 1), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z - 1), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x + 1, y, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x, y, z + 1), type, destroyed, stop);

            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x - 1, y, z), type, destroyed, stop);
            if (destroyed < maxBlocks || maxBlocks < 0)
                destroyed = breakRecReplant(player, tool, mundo.getBlockAt(x, y, z - 1), type, destroyed, stop);
        }

        return destroyed;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        label = label.toLowerCase();
        boolean good = label.equals(command.getLabel());
        String[] cmds = command.getAliases().toArray(new String[]{});
        for (int i = 0; i < cmds.length && !good; i++) {
            cmds[i] = cmds[i].toLowerCase();
            if (label.equals(cmds[i])) {
                good = true;
            }
        }

        boolean noPerms = false;
        if (good) {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "help" ->
                            sender.sendMessage(header + "Commands:\n", accentColor + "/" + label + " help: " + textColor + "Shows this help message.", accentColor + "/" + label + " toggle <true/false>: " + textColor + "Toggles tree felling on and off.");
                    case "toggle" -> {
                        if (sender instanceof Player) {
                            boolean enabled = startActivated;
                            List<MetadataValue> metas = ((Player) sender).getMetadata(PLAYER_ENABLE_META);
                            for (MetadataValue meta : metas) {
                                enabled = meta.asBoolean();
                            }
                            enabled = !enabled;
                            ((Player) sender).setMetadata(PLAYER_ENABLE_META, new FixedMetadataValue(this, enabled));
                            sender.sendMessage(header + " You " + (enabled ? "enabled" : "disabled") + " tree felling.");
                        } else {
                            sender.sendMessage(header + "This command can only be used by players");
                        }
                    }
                    default ->
                            sender.sendMessage(header + errorColor + "Command not found, please check \"/" + label + " help\".");
                }
            } else {
                return false;
            }
        }

        if (noPerms) {
            sender.sendMessage(header + errorColor + "You don't have permission to use this command.");
        }
        return good;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 0 -> {
                list.add("help");
                list.add("toggle");
            }
            case 1 -> {
                args[0] = args[0].toLowerCase();
                switch (args[0]) {
                    case "help":
                    case "toggle":
                        break;

                    default:
                        if ("help".contains(args[0])) list.add("help");
                        if ("toggle".contains(args[0])) list.add("toggle");
                        break;
                }
            }
            default -> {
            }
        }
        return list;
    }

    /**
     * Deals 1 damage to an item, if possible
     *
     * @param player
     * @param tool
     * @return true if item is destroyed or should not be damaged anymore, false if
     * not damageable or damaged but not destroyed
     */
    private boolean damageItem(Player player, ItemStack tool, Material material) {
        if (axeNeeded && damageAxe && tool != null && isLog(material)) {
            ItemMeta meta = tool.getItemMeta();
            if (meta instanceof Damageable damageable) {
                short maxDmg = tool.getType().getMaxDurability();
                int dmg = damageable.getDamage();

                // damageable.setDamage(++dmg);
                // Substituted for the following code by exwundee (https://github.com/exwundee)
                // This adds support for any level of the Durability enchantment
                {
                    Random rand = new Random();

                    int unbLevel = tool.getEnchantmentLevel(Enchantment.DURABILITY);

                    if (rand.nextInt(unbLevel + 1) == 0) {
                        damageable.setDamage(++dmg);
                    }
                }
                tool.setItemMeta(damageable);

                if (dmg >= maxDmg) {
                    if (breakAxe) {
                        tool.setAmount(0);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                    } else {
                        damageable.setDamage(maxDmg - 1);
                        tool.setItemMeta(damageable);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLog(Material mat) {
        boolean ret = !mat.name().contains("STRIPPED_") && mat.name().contains("_LOG");
        if (!ret && admitNetherTrees) return mat.name().equals("CRIMSON_STEM") || mat.name().equals("WARPED_STEM");
        return ret;
    }

    private boolean isLeaves(Material mat) {
        if (ignoreLeaves) return false;
        boolean ret = mat.name().contains("LEAVES");
        if (!ret && admitNetherTrees)
            return mat.name().equals("NETHER_WART_BLOCK") || mat.name().equals("WARPED_WART_BLOCK") || mat.name().equals("SHROOMLIGHT");
        return ret;
    }

    private boolean canPlant(Block below, Material woodType) {
        if (treeMap == null) {
            treeMap = new HashMap<>(10);

            // Elegance is my passion /s
            ArrayList<Material> woods = new ArrayList<>(9);
            try {
                woods.add(Material.OAK_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.DARK_OAK_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.SPRUCE_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.ACACIA_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.AZALEA);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.BIRCH_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.JUNGLE_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.MANGROVE_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            try {
                woods.add(Material.CHERRY_LOG);
            } catch (NoSuchFieldError e) {
                // Material doesn't exist in this version
            }
            for (Material wood : woods) {
                treeMap.put(wood, new ArrayList<>(Arrays.asList(Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM, Material.ROOTED_DIRT, Material.MOSS_BLOCK, Material.FARMLAND, Material.MUD)));
            }

            treeMap.get(Material.MANGROVE_LOG).add(Material.CLAY);
        }

        if (admitNetherTrees && !treeMap.containsKey(Material.WARPED_STEM)) {
            treeMap.put(Material.WARPED_STEM, List.of(Material.WARPED_NYLIUM));
            treeMap.put(Material.CRIMSON_STEM, List.of(Material.CRIMSON_NYLIUM));
        } else if (!admitNetherTrees && treeMap.containsKey(Material.WARPED_STEM)) {
            treeMap.remove(Material.WARPED_STEM);
            treeMap.remove(Material.CRIMSON_STEM);
        }

        return treeMap.getOrDefault(woodType, new ArrayList<>(0)).contains(below.getType());
    }
}