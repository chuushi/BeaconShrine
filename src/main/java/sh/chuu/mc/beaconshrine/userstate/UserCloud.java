package sh.chuu.mc.beaconshrine.userstate;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.utils.BukkitSerialization;
import sh.chuu.mc.beaconshrine.BeaconShrine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.userstate.CloudInventoryLores.INVENTORY_NAME;

public class UserCloud {
    private static final int SIZE = 45;
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final File configFile;
    private final YamlConfiguration config;
    private Inventory inv = null;
    private List<Integer> tunedShrines = null;

    public UserCloud(UUID uuid) throws IOException {
        this.configFile = new File(plugin.getDataFolder() + "/inventories", uuid + ".yml");
        if (!configFile.exists()) {
            File outDir = new File(plugin.getDataFolder(), "inventories");
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            configFile.createNewFile();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadData();
    }

    private void loadData() throws IOException {
        tunedShrines = config.getIntegerList("ts");

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

    public boolean attuneShrine(int id) {
        if (tunedShrines.contains(id))
            return false;
        tunedShrines.add(id);
        return true;
    }

    public List<Integer> getTunedShrineList() {
        return tunedShrines;
    }

    public void save() {
        config.set("ts", tunedShrines);
        config.set("i", inv == null ? null : BukkitSerialization.itemStackArrayToBase64(inv.getContents()));
        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save userstate data to " + configFile, ex);
        }
    }
}
