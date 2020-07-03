package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import sh.chuu.mc.beaconshrine.shrine.ShrineEvents;
import sh.chuu.mc.beaconshrine.shrine.ShrineManager;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;

import java.io.IOException;
import java.util.logging.Level;

public class BeaconShrine extends JavaPlugin {
    static final String REFRESH_ENABLED_NODE = "refresh-check.enabled";
    static final String REFRESH_TIME_NODE = "refresh-check.time";

    private static BeaconShrine instance = null;
    private CloudManager cloudManager = null;
    private ShrineManager shrineManager = null;
    private RefreshChecker refreshChecker = null;

    public static BeaconShrine getInstance() {
        return BeaconShrine.instance;
    }

    public CloudManager getCloudManager() {
        return cloudManager;
    }

    public ShrineManager getShrineManager() {
        return shrineManager;
    }

    @Override
    public void onEnable() {
        BeaconShrine.instance = this;
        saveDefaultConfig();
        cloudManager = new CloudManager();
        getServer().getPluginManager().registerEvents(cloudManager, this);
        try {
            shrineManager = new ShrineManager();
            getServer().getPluginManager().registerEvents(new ShrineEvents(), this);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load Shrine storage", e);
        }

        if (getConfig().getBoolean(REFRESH_ENABLED_NODE))
            getServer().getPluginManager().registerEvents(refreshChecker = new RefreshChecker(), this);
    }

    @Override
    public void onDisable() {
        cloudManager.onDisable();
        shrineManager.onDisable();
        cloudManager = null;
        shrineManager = null;
        refreshChecker = null;
        saveConfig();
    }

    private static final String MANAGE_PERM = "beaconshrine.admin";
    private static final String REFRESH_CMD = "refreshcheck";
    private static final String REFRESH_ENABLE_CMD = "enable";
    private static final String REFRESH_DISABLE_CMD = "disable";
    private static final String REFRESH_SETTIME_CMD = "settime";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !sender.hasPermission(MANAGE_PERM)) {
            // TODO plugin info
            sender.sendMessage("BeaconShrine v" + getDescription().getVersion() + "by Simon Chuu");
            return true;
        }

        if (args[0].equalsIgnoreCase(REFRESH_CMD)) {
            if (args.length == 1) {
                sender.sendMessage("Subcommands: " + REFRESH_ENABLE_CMD + " " + REFRESH_DISABLE_CMD + " " + REFRESH_SETTIME_CMD);
                return true;
            }

            boolean enabled = getConfig().getBoolean(REFRESH_ENABLED_NODE, false);

            if (args[1].equalsIgnoreCase(REFRESH_ENABLE_CMD)) {
                if (enabled) {
                    sender.sendMessage("Refresh is already enabled");
                } else {
                    getConfig().set(REFRESH_ENABLED_NODE, true);
                    getServer().getPluginManager().registerEvents(refreshChecker = new RefreshChecker(), this);
                    sender.sendMessage("Refresh is now enabled");
                }
                return true;
            }

            if (args[1].equalsIgnoreCase(REFRESH_DISABLE_CMD)) {
                if (!enabled) {
                    sender.sendMessage("Refresh is already disabled");
                } else {
                    getConfig().set(REFRESH_ENABLED_NODE, false);
                    HandlerList.unregisterAll(refreshChecker);
                    refreshChecker = null;
                    sender.sendMessage("Refresh is now disabled");
                }
                return true;
            }

            if (args[1].equalsIgnoreCase(REFRESH_SETTIME_CMD)) {
                refreshChecker.setTime();
                return true;
            }
        }

        sender.sendMessage("Commands: " + REFRESH_CMD);
        return true;
    }
}
