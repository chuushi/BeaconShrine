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
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
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
    private final Map<Integer, ShrineCore> shrines = new HashMap<>();
    private final Map<Player, GuiView> whichGui = new LinkedHashMap<>();
    private final Set<Player> attuning = new LinkedHashSet<>();
    private final Set<Player> warping = new LinkedHashSet<>();
    private int nextId = 0;

    public record GuiView(ShrineCore shrine,
                          GuiType type) {
    }

    public enum GuiType {
        HOME, SHOP, WARP_LIST;
        // TODO add GUI types + Link it with shrine ID stuffs
    }

    public ShrineManager() throws IOException {
        this.configFile = new File(plugin.getDataFolder(), "shrines.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadShrines();
    }

    public void onDisable() {
        for (Map.Entry<Player, GuiView> e : whichGui.entrySet()) {
            Player p = e.getKey();
            closeShrineGui(p);
            p.closeInventory();
        }
        saveData();
    }

    public int getNextId() {
        return nextId;
    }

    public ShrineCore newShrine(ShulkerBox s, Beacon b) {
        ShrineCore ret = new ShrineCore(nextId, s, b);
        shrines.put(nextId, ret);
        nextId++;
        return ret;
    }

    public boolean removeShrine(int id) {
        ShrineCore s = shrines.remove(id);
        if (s == null || s.isValid()) return false;
        if (nextId == id + 1) nextId--;
        config.set("s" + id, null);
        return true;
    }

    public boolean openShrineGui(Player p, int id) {
        if (!plugin.getCloudManager().isTunedWithShrine(p, id)) {
            doAttuneAnimation(p, id);
            return true;
        }

        ShrineCore s = shrines.get(id);
        if (s == null || !s.isValid()) return false;

        Location loc = p.getLocation();
        Vector vector = ParticleUtils.getDiff(s.x(), s.y(), s.z(), loc);
        ParticleUtils.beam(loc, vector, s.dustColor());
        p.openInventory(s.getGui(p));
        whichGui.put(p, new GuiView(s, GuiType.HOME));
        return true;
    }

    private void doAttuneAnimation(Player p, int id) {
        if (attuning.contains(p)) return;
        attuning.add(p);

        ParticleUtils.shrineIgnitionSound(p);
        new BukkitRunnable() {
            final Location initLoc = p.getLocation();
            final double x = initLoc.getX();
            final double y = initLoc.getY();
            final double z = initLoc.getZ();
            final ShrineCore shrine = getShrine(id);
            final Vector vector = ParticleUtils.getDiff(shrine.x(), shrine.y(), shrine.z(), p.getLocation());
            final Particle.DustOptions dustColor = shrine.dustColor();
            private int step = 100;

            @Override
            public void run() {
                Location newLoc = p.getLocation();
                if (x != newLoc.getX() || y != newLoc.getY() || z != newLoc.getZ() || !shrine.isValid()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuning cancelled due to movement or invalid shrine").create());
                    attuning.remove(p);
                    this.cancel();
                } else if (step == 0) {
                    attuning.remove(p);
                    this.cancel();
                    plugin.getCloudManager().attuneShrine(p, id);
                    ParticleUtils.attuneBoom(initLoc, shrine.color());
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuned with " + shrine.name()).create());
                    shrine.startParticles();
                } else if (step%20 == 0) {
                    int secs = step /20;
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuning with " + shrine.name() + ", please wait " + secs + (secs == 1 ? " second" : " seconds")).create());
                }
                ParticleUtils.attuning(initLoc, vector, dustColor, step--);
            }
        }.runTaskTimer(plugin, 0L, 1L);
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

    Inventory getWarpGui(Player p, int currentId) {
        List<Integer> wids = plugin.getCloudManager().getTunedShrineList(p);
        int slots = (wids.size()/9 + 1) * 9;
        if (slots > 54) {
            // TODO pagination
            slots = 54;
        }

        Inventory ret = Bukkit.createInventory(null, slots, "Warp to...");

        int last = Math.min(wids.size(), slots);
        for (int i = 0; i < last; i++) {
            int id = wids.get(i);

            ShrineCore sm = shrines.get(id);
            ItemStack item = sm.createWarpScrollGuiItem(id == currentId);
            ret.setItem(i, item);
        }
        return ret;
    }

    public GuiView closeShrineGui(HumanEntity p) {
        @SuppressWarnings("SuspiciousMethodCalls")
        GuiView gui = whichGui.remove(p);
        if (gui == null)
            return null;
        if (gui.type == GuiType.SHOP) {
            gui.shrine.closeMerchant((Player) p);
        }
        return gui;
    }

    public GuiView getGuiView(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        return whichGui.get(p);
    }

    public void clickedGui(int id, ItemStack slot, Player p) {
        Material type = slot.getType();
        if (type == CLOUD_CHEST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getCloudManager().openInventory(p), 1L);
            return;
        }

        if (type == WARP_LIST_ITEM_TYPE) {
            ShrineCore shrine = shrines.get(id);
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                whichGui.put(p, new GuiView(shrine, GuiType.WARP_LIST));
                p.openInventory(getWarpGui(p, id));
            }, 1L);
            return;
        }

        if (type == ENDER_CHEST_ITEM_TYPE) {
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(p.getEnderChest()), 1L);
            return;
        }

        if (type == SHOP_ITEM_TYPE) {
            ShrineCore shrine = shrines.get(id);
            if (shrine.trader() != null) {
                return;
            }
            p.closeInventory();
            ShrineGUI.clickNoise(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                whichGui.put(p, new GuiView(shrine, GuiType.SHOP));
                shrine.openMerchant(p);
            }, 1L);
            return;
        }

        // Because of the colors of the Shulker boxes
        ItemMeta im = slot.getItemMeta();
        if (im instanceof BlockStateMeta) {
            BlockState bs = ((BlockStateMeta) im).getBlockState();
            if (bs instanceof ShulkerBox){
                // Shulker box within
                p.closeInventory();
                ShrineGUI.clickNoise(p);
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(shrines.get(id).getInventory()), 1L);
            }
        }
    }

    public void clickedWarpGui(Player p, int id, ShrineCore guiShrine) {
        ShrineGUI.clickNoise(p);
        p.closeInventory();
        long diff = plugin.getCloudManager().getNextWarp(p) - System.currentTimeMillis();
        if (diff > 0)
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ShrineGUI.warpTimeLeft(diff)));
        else
            getShrine(id).warp(p, guiShrine);
    }

    public ShrineCore updateShrine(int id, ShulkerBox s) {
        if (id == -1 || s.getCustomName() == null)
            return null;
        ShrineCore shrine = shrines.get(id);
        if (shrine == null) return null;
        shrine.setShulker(s, s.getType() != Material.SHULKER_BOX);
        return shrine;
    }

    private void loadShrines() {
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
            //noinspection ConstantConditions Never null since it's from an existing key
            shrines.put(id, new ShrineCore(id, cs));
        }
    }

    private void saveData() {
        for (Map.Entry<Integer, ShrineCore> is : shrines.entrySet()) {
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
        return shrines.get(id);
    }

    public Map<Integer, ShrineCore> getShrines() {
        return shrines;
    }
}
