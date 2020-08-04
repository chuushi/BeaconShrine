package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.scheduler.BukkitTask;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.shrine.ShrineGuiLores.*;

public class ShrineManager {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final File configFile;
    private final YamlConfiguration config;
    private final Map<Integer, ShrineMultiblock> shrines = new HashMap<>();
    private final Map<Player, ShrineMultiblock> viewing = new LinkedHashMap<>();
    private final Set<Player> attuning = new LinkedHashSet<>();
    private int nextId = 0;

    public ShrineManager() throws IOException{
        this.configFile = new File(plugin.getDataFolder(), "shrines.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadShrines();
    }

    public void onDisable() {
        for (Map.Entry<Player, ShrineMultiblock> e : viewing.entrySet()) {
            Player p = e.getKey();
            closeShrineGui(p);
            p.closeInventory();
        }
        saveData();
    }

    public int getNextId() {
        return nextId;
    }

    ShrineMultiblock newShrine(ShulkerBox s, Beacon b) {
        ShrineMultiblock ret = new ShrineMultiblock(nextId, s, b, s.getType() != Material.SHULKER_BOX);
        shrines.put(nextId, ret);
        nextId++;
        return ret;
    }

    public boolean removeShrine(int id) {
        ShrineMultiblock s = shrines.remove(id);
        if (s == null || s.isValid()) return false;
        if (nextId == id + 1) nextId--;
        config.set("s" + id, null);
        return true;
    }

    boolean openShrineGui(Player p, int id) {
        if (!plugin.getCloudManager().isTunedWithShrine(p, id)) {
            doAttuneAnimation(p, id);
            return true;
        }

        ShrineMultiblock s = shrines.get(id);
        if (s == null || !s.isValid()) return false;
        p.openInventory(s.getGui(p));
        viewing.put(p, s);
        return true;
    }

    private void doAttuneAnimation(Player p, int id) {
        if (attuning.contains(p)) return;
        attuning.add(p);
        Location l = p.getLocation();
        double x = l.getX();
        double y = l.getY();
        double z = l.getZ();
        ShrineMultiblock shrine = getShrine(id);

        new BukkitRunnable() {
            private int timer = 5;
            @Override
            public void run() {
                Location l = p.getLocation();
                if (x != l.getX() || y != l.getY() || z != l.getZ() || !shrine.isValid()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuning cancelled due to movement or invalid shrine").create());
                    attuning.remove(p);
                    this.cancel();
                    return;
                }
                if (timer == 0) {
                    attuning.remove(p);
                    this.cancel();
                    plugin.getCloudManager().attuneShrine(p, id);
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuned with " + shrine.getName()).create());
                    return;
                }
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new ComponentBuilder("Attuning with " + shrine.getName() + ", please wait " + timer + (timer-- == 1 ? " second" : " seconds")).create());
            }
        }.runTaskTimer(plugin, 0L, 20L);
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

            ShrineMultiblock sm = shrines.get(id);
            ItemStack item = sm.createWarpScrollGuiItem();
            if (id == currentId) {
                ItemMeta im = item.getItemMeta();
                //noinspection ConstantConditions if ws exists, im also exists
                im.setLore(ImmutableList.of(ChatColor.GRAY + "You are here"));
                item.setItemMeta(im);
            }
            ret.setItem(i, item);
        }
        return ret;
    }

    boolean closeShrineGui(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        ShrineMultiblock shrine = viewing.remove(p);
        if (shrine != null) {
            shrine.closeMerchant((Player) p);
            return false;
        }
        return true;
    }

    int getGuiViewingId(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        ShrineMultiblock n = viewing.get(p);
        return n == null ? -1 : n.getId();
    }

    public void clickedGui(int id, ItemStack slot, Player p) {
        Material type = slot.getType();
        if (type == CLOUD_CHEST_ITEM_TYPE) {
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getCloudManager().openInventory(p), 1L);
            return;
        }

        if (type == WARP_LIST_ITEM_TYPE) {
            ShrineMultiblock shrine = shrines.get(id);
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewing.put(p, shrine);
                p.openInventory(getWarpGui(p, id));
            }, 1L);
            return;
        }

        if (type == ENDER_CHEST_ITEM_TYPE) {
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(p.getEnderChest()), 1L);
            return;
        }

        if (type == SHOP_ITEM_TYPE) {
            ShrineMultiblock shrine = shrines.get(id);
            if (shrine.getTrader() != null) {
                return;
            }
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewing.put(p, shrine);
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
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(shrines.get(id).getInventory()), 1L);
            }
        }
    }

    ShrineMultiblock updateShrine(int id, ShulkerBox s) {
        if (id == -1 || s.getCustomName() == null)
            return null;
        ShrineMultiblock shrine = shrines.get(id);
        if (shrine == null) return null;
        shrine.updateShulker(s, s.getType() != Material.SHULKER_BOX);
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
            shrines.put(id, new ShrineMultiblock(id, cs));
        }
    }

    private void saveData() {
        for (Map.Entry<Integer, ShrineMultiblock> is : shrines.entrySet()) {
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

    public ShrineMultiblock getShrine(int id) {
        return shrines.get(id);
    }
}
