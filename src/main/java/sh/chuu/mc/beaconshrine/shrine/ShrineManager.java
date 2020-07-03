package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import sh.chuu.mc.beaconshrine.BeaconShrine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ShrineManager implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final File configFile;
    private final YamlConfiguration config;
    private final Map<Integer, ShrineMultiblock> shrines = new HashMap<>();
    private final Map<Player, Integer> viewing = new LinkedHashMap<>();
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
        saveData();
    }

    ShrineMultiblock newShrine(ShulkerBox s, Beacon b) {
        ShrineMultiblock ret = new ShrineMultiblock(nextId, s, b, s.getType() != Material.SHULKER_BOX);
        shrines.put(nextId, ret);
        nextId++;
        return ret;
    }

    boolean openShrineGui(Player p, int id) {
        ShrineMultiblock s = shrines.get(id);
        if (s == null || !s.isValid()) return false;
        p.openInventory(s.getGui(p));
        viewing.put(p, id);
        return true;
    }

    int getGuiViewingId(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        Integer n = viewing.get(p);
        return n == null ? -1 : n;
    }

    public void clickedGui(int id, int slot, Player p) {
        ShrineMultiblock shrine = shrines.get(id);
        switch (slot) {
            case 1:
                // Cloud chest
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getCloudManager().openInventory(p), 1L);
                break;
            case 4:
                // Shulker box within
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(shrine.getInventory()), 1L);
                break;
            case 7:
                // Shop
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.openMerchant(shrine.getMerchant(), true), 1L);
                break;
        }
    }

    void closeShrineGui(HumanEntity p) {
        //noinspection SuspiciousMethodCalls
        viewing.remove(p);
    }

    ShrineMultiblock updateShrine(int id, ShulkerBox s) {
        if (id < 0 || s.getCustomName() == null)
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
            int id = Integer.parseInt(key.substring(1));
            ConfigurationSection cs = config.getConfigurationSection(key);
            nextId = Math.max(id + 1, nextId);
            String name = cs.getString("name");
            String colorStr = cs.getString("color");
            DyeColor color = colorStr == null ? null : DyeColor.valueOf(colorStr);
            World w = Bukkit.getWorld(cs.getString("world"));
            Iterator<Integer> loc = cs.getIntegerList("loc").iterator();
            int x = loc.next();
            int z = loc.next();
            int shulkerY = loc.next();
            int beaconY = loc.next();
            shrines.put(id, new ShrineMultiblock(id, w, x, z, shulkerY, beaconY, name, color));
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
}
