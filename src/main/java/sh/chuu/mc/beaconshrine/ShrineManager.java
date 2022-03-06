package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Beacon;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.shrine.AbstractShrine;
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
import sh.chuu.mc.beaconshrine.shrine.ShrineShard;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class ShrineManager {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final File configFile;
    private final YamlConfiguration config;
    private final Map<Integer, ShrineCore> cores = new HashMap<>();
    private final Map<Player, GuiView> whichGui = new LinkedHashMap<>();
    private final Set<Player> attuning = new LinkedHashSet<>();
    private final Set<Player> warping = new LinkedHashSet<>();
    private int nextId = 0;

    public record GuiView(AbstractShrine shrine, GuiType type) {}

    /**
     * Enum describes what kind of view the player is viewing
     */
    public enum GuiType {
        /** Shrine Core main view */
        HOME_CORE,
        /** Shrine Core main view */
        HOME_SHARD,
        /** Shrine shop view */
        SHOP,
        /** Shrine Core Warp view */
        CORE_WARP_LIST,
        /** Shrine Shard Warp view */
        SHARD_WARP_LIST
        // TODO add GUI types + Link it with shrine ID stuffs
    }

    public ShrineManager() throws IOException {
        this.configFile = new File(plugin.getDataFolder(), "shrines.yml");
        if (!configFile.exists()) {
            //noinspection ResultOfMethodCallIgnored This creates a new file. Result can be ignored.
            configFile.createNewFile();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void onDisable() {
        for (Map.Entry<Player, GuiView> e : whichGui.entrySet()) {
            Player p = e.getKey();
            closedShrineGui(p);
            p.closeInventory();
        }
        saveData();
    }

    public int getNextId() {
        return nextId;
    }

    public ShrineCore newShrine(ShulkerBox s, Beacon b) {
        ShrineCore ret = new ShrineCore(nextId, s, b);
        cores.put(nextId, ret);
        nextId++;
        return ret;
    }

    public boolean removeShrine(int id) {
        ShrineCore s = cores.remove(id);
        if (s == null || s.isValid()) return false;
        if (nextId == id + 1) nextId--;
        config.set("s" + id, null);
        return true;
    }

    /**
     * Opens GUI for the corresponding shrine
     * @param p Player
     * @param id ID
     * @param shulker Shulker box
     * @param isCore Whether the Shrine type is Core
     */
    public void openShrineGui(Player p, int id, ShulkerBox shulker, boolean isCore) {

        if (!plugin.getCloudManager().isTunedWithShrine(p, id)) {
            if (isCore) {
                // TODO repeated code A
                if (attuning.contains(p)) return;
                attuning.add(p);
                getShrine(id).doAttuneAnimation(p).thenAccept(res -> attuning.remove(p));
            } else {
                // TODO Move to Vars
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You cannot decipher this shard's origin"));
            }
            return;
        }

        ShrineCore core = getShrine(id);
        if (!core.isWithinDistance(shulker.getX(), shulker.getZ())) {
            // TODO move to Vars
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("The shrine is too far away"));
            return;
        }
        AbstractShrine s = null;
        if (isCore) {
            s = core;
        } else {
            for (ShrineShard ss : core.getShards()) {
                if (ss.getShulkerLocation(false).equals(shulker.getLocation())) {
                    s = ss;
                    break;
                }
            }
        }

        if (s == null || !s.isValid()) {
            // TODO Move to Vars
            if (isCore)
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE_SETUP);
            else
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Add lodestone compass to main shrine first"));
            return;
        }

        if (!isCore && !plugin.getCloudManager().isTunedWithShardLocation(p, s.x(), s.z())) {
            // TODO repeated code A
            if (attuning.contains(p)) return;
            attuning.add(p);
            s.doAttuneAnimation(p).thenAccept(res -> attuning.remove(p));
            return;
        }

        Location loc = p.getLocation();
        Vector vector = ParticleUtils.getDiff(s.x(), s.y(), s.z(), loc);
        ParticleUtils.beam(loc, vector, s.dustColor()); // TODO Move particle logic to ShrineCore/ShrineShard
        p.openInventory(s.getGui(p));
        whichGui.put(p, new GuiView(s, isCore ? GuiType.HOME_CORE : GuiType.HOME_SHARD));
    }

    public boolean warpContains(Player p) {
        return warping.contains(p);
    }

    public void warpAdd(Player p) {
        warping.add(p);
    }

    public void warpDone(Player p) {
        warping.remove(p);
    }

    /**
     *
     * @param p Player actioning the GUI
     * @param shrine This current shrine
     * @param isCore If shrine is core (effectively shrine instanceof ShrineCore)
     * @return Inventory of warp GUI
     */
    private Inventory getWarpGui(Player p, AbstractShrine shrine, boolean isCore) {
        //if (isCore)
        List<?> wids = isCore ? plugin.getCloudManager().getTunedShrineList(p) : shrine.getShards();
        // TODO Tuned Shrines only

        int slots = (wids.size()/9 + 1) * 9;
        if (slots > 54) {
            // TODO pagination
            slots = 54;
        }

        Inventory ret = Bukkit.createInventory(null, slots, "Warp to...");

        if (isCore) {
            int last = Math.min(wids.size(), slots);
            for (int i = 0; i < last; i++) {
                int id = (Integer) wids.get(i);

                ShrineCore sm = cores.get(id);
                ItemStack item = sm.createWarpScrollGuiItem(id == shrine.id(), p);
                ret.setItem(i, item);
            }
        } else {
            ShrineCore sc = shrine instanceof ShrineShard ss ? ss.parent() : (ShrineCore) shrine;
            ItemStack itemCore = sc.createWarpScrollGuiItem(shrine instanceof ShrineCore, p);
            ret.setItem(0, itemCore);

            int last1 = Math.min(wids.size(), slots) + 1;
            for (int i = 1; i < last1; i++) {
                ShrineShard ss = (ShrineShard) wids.get(i-1);

                ItemStack item = ss.createWarpScrollGuiItem(ss == shrine, p);
                ret.setItem(i, item);
            }
        }
        return ret;
    }

    public GuiView closedShrineGui(HumanEntity p) {
        GuiView gui = p instanceof Player ? whichGui.remove(p) : null;
        if (gui == null)
            return null;
        if (gui.shrine instanceof ShrineCore sc && gui.type == GuiType.SHOP) {
            sc.closeMerchant((Player) p);
        }
        return gui;
    }

    public GuiView getGuiView(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        return whichGui.get(p);
    }

    public void clickedGui(AbstractShrine shrine, ItemStack slot, Player p) {
        Material type = slot.getType();
        if (type == CLOUD_CHEST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getCloudManager().openInventory(p), 1L);
            return;
        }

        if (type == WARP_LIST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                whichGui.put(p, new GuiView(shrine, GuiType.CORE_WARP_LIST));
                p.openInventory(getWarpGui(p, shrine, true));
            }, 1L);
            return;
        }

        if (type == SHARD_LIST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                whichGui.put(p, new GuiView(shrine, GuiType.SHARD_WARP_LIST));
                p.openInventory(getWarpGui(p, shrine, false));
            }, 1L);
            return;
        }

        if (type == ENDER_CHEST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(p.getEnderChest()), 1L);
            return;
        }

        if (type == SHOP_ITEM_TYPE && shrine instanceof ShrineCore sc) {
            if (sc.trader() != null) {
                return;
            }
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                whichGui.put(p, new GuiView(sc, GuiType.SHOP));
                sc.openMerchant(p);
            }, 1L);
            return;
        }

        // Because of the colors of the Shulker boxes
        ItemMeta im = slot.getItemMeta();
        if (im instanceof BlockStateMeta) {
            BlockState bs = ((BlockStateMeta) im).getBlockState();
            if (bs instanceof ShulkerBox) {
                // Shulker box within
                p.closeInventory();
                ShrineGUI.clickNoise(p);
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(shrine.getInventory()), 1L);
            }
        }
    }

    public void clickedWarpGui(Player p, AbstractShrine destinationShrine, AbstractShrine guiSourceShrine) {
        ShrineGUI.clickNoise(p);
        p.closeInventory();
        long diff = destinationShrine.id() == guiSourceShrine.id()
                ? 0
                : plugin.getCloudManager().getNextWarp(p) - System.currentTimeMillis();
        if (diff > 0)
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ShrineGUI.warpTimeLeft(diff)));
        else
            destinationShrine.warp(p, guiSourceShrine);
    }

    public void loadShrines() {
        if (!cores.isEmpty()) return; // TODO Clean up logic
        for (String key : config.getKeys(false)) {
            if (!key.startsWith("s"))
                continue;
            int id;
            try {
                id = Integer.parseInt(key.substring(1));
            } catch (NumberFormatException ex) {
                continue;
            }
            ConfigurationSection cs = config.getConfigurationSection(key);
            nextId = Math.max(id + 1, nextId);
            cores.put(id, new ShrineCore(id, cs));
        }
    }

    private void saveData() {
        for (Map.Entry<Integer, ShrineCore> is : cores.entrySet()) {
            String section = "s" + is.getKey();
            ConfigurationSection cs = config.getConfigurationSection(section);
            if (cs == null) cs = config.createSection(section);
            is.getValue().save(cs);
        }
        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save Shrine data to " + configFile, ex);
        }
    }

    public ShrineCore getShrine(int id) {
        return cores.get(id);
    }

    public Map<Integer, ShrineCore> getShrineCores() {
        return cores;
    }
}
