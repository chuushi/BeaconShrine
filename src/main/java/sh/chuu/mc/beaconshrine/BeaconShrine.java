package sh.chuu.mc.beaconshrine;

import com.google.common.collect.ImmutableList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import sh.chuu.mc.beaconshrine.listeners.GUIEvents;
import sh.chuu.mc.beaconshrine.listeners.LoreItemUseEvents;
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.listeners.ShrineEvents;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.shrineActivatorId;

public class BeaconShrine extends JavaPlugin {
    private static BeaconShrine instance = null;

    private CloudManager cloudManager = null;
    private ShrineManager shrineManager = null;

    public static BeaconShrine getInstance() {
        return BeaconShrine.instance;
    }

    public CloudManager getCloudManager() {
        return cloudManager;
    }

    public ShrineManager getShrineManager() {
        return shrineManager;
    }

    // TODO For GUI, remove hand item before closing the GUI or item may dupe on plugin disable

    @Override
    public void onEnable() {
        BeaconShrine.instance = this;
        saveDefaultConfig();
        cloudManager = new CloudManager();

        try {
            shrineManager = new ShrineManager();
            shrineManager.loadShrines();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load Shrine storage", e);
            getPluginLoader().disablePlugin(this);
            return;
        }

        // Events
        getServer().getPluginManager().registerEvents(cloudManager, this);
        getServer().getPluginManager().registerEvents(new ShrineEvents(), this);
        getServer().getPluginManager().registerEvents(new LoreItemUseEvents(), this);
        getServer().getPluginManager().registerEvents(new GUIEvents(), this);

        // Commands
        //noinspection ConstantConditions
        getCommand("beaconshrine").setExecutor(new ShrineCommands());

        shrineManager.getShrineCores().values().forEach(s -> {
            if (!s.hasParticles() && s.world().isChunkLoaded(s.x() >> 4, s.z() >> 4) && s.isValid()) {
                s.startParticles();
            }
        });
    }

    @Override
    public void onDisable() {
        cloudManager.onDisable();
        shrineManager.onDisable();
        cloudManager = null;
        shrineManager = null;
        BeaconShrine.instance = null;
        saveConfig();
    }
}
