package sh.chuu.mc.beaconshrine.userstate;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.utils.BukkitSerialization;
import sh.chuu.mc.beaconshrine.BeaconShrine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.userstate.CloudInventoryLores.INVENTORY_NAME;

public class UserCloud {
    private static final int SIZE = 45;
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final File configFile;
    private final YamlConfiguration config;
    private final List<Integer> tunedShrines = new ArrayList<>();
    private final Map<Integer, List<Integer>> tunedShards = new HashMap<>();
    private Inventory inv = null;
    private long nextWarp = 0;

    public UserCloud(UUID uuid) throws IOException {
        this.configFile = new File(plugin.getDataFolder() + "/inventories", uuid + ".yml");
        if (!configFile.exists()) {
            File outDir = new File(plugin.getDataFolder(), "inventories");
            if (!outDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                outDir.mkdirs();
            }
            //noinspection ResultOfMethodCallIgnored
            configFile.createNewFile();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadData();
    }

    private void loadData() throws IOException {
        tunedShrines.addAll(config.getIntegerList("ts"));
        ConfigurationSection sh = config.getConfigurationSection("sh");
        if (sh != null) {
            sh.getKeys(false).forEach(key -> {
                // TODO unsafe operations gets caught in lint.
                //noinspection unchecked
                @SuppressWarnings("ConstantConditions") // Keys exist already
                ArrayList<Integer> zList = new ArrayList<>((List<Integer>) sh.getList(key));
                tunedShards.put(Integer.parseInt(key), zList);
            });
        }

        String s = config.getString("i");
        if (s == null) {
            inv = Bukkit.createInventory(null, SIZE, INVENTORY_NAME);
            return;
        }
        ItemStack[] items = BukkitSerialization.itemStackArrayFromBase64(s);
        inv = Bukkit.createInventory(null, items.length, INVENTORY_NAME);
        inv.setContents(items);
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void setInventory(Inventory inv) {
        this.inv = inv;
    }

    public boolean isTunedWithShrine(int id) {
        return tunedShrines.contains(id);
    }

    public boolean isTunedWithShard(int x, int z) {
        List<Integer> f = tunedShards.get(x);
        return f != null && f.contains(z);
    }

    public boolean attuneShrine(int id) {
        if (tunedShrines.contains(id))
            return false;
        tunedShrines.add(id);
        return true;
    }

    public boolean attuneShardLocation(int x, int z) {
        List<Integer> zList = tunedShards.computeIfAbsent(x, k -> new ArrayList<>());
        if (zList.contains(z))
            return false;
        zList.add(z);
        return true;
    }

    public List<Integer> getTunedShrineList() {
        return tunedShrines;
    }

    public long getNextWarp() {
        return nextWarp;
    }

    public void setNextWarp(long nextWarp) {
        this.nextWarp = nextWarp;
    }

    public void save() {
        config.set("ts", tunedShrines);
        // TODO Potentially go through tunedShards to clean up all the broken shards?
        config.set("sh", tunedShards);
        config.set("i", inv == null ? null : BukkitSerialization.itemStackArrayToBase64(inv.getContents()));
        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save userstate data to " + configFile, ex);
        }
    }
}
