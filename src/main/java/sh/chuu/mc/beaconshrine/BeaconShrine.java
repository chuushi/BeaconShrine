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

    private static final String MANAGE_PERM = "beaconshrine.admin";
    private static final String SHRINE_CMD = "shrine";
    private static final String SHRINE_DELETE_CMD = "delete";
    private static final String SHRINE_SETID_CMD = "setid";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !sender.hasPermission(MANAGE_PERM)) {
            // TODO plugin info
            sender.sendMessage("BeaconShrine v" + getDescription().getVersion() + "by Simon Chuu");
            return true;
        }

        if (args[0].equalsIgnoreCase(SHRINE_CMD)) {
            if (args.length == 1) {
                sender.sendMessage(String.format("Subcommands: %s %s", SHRINE_DELETE_CMD, SHRINE_SETID_CMD));
                return true;
            }

            if (args[1].equalsIgnoreCase(SHRINE_DELETE_CMD)) {
                int id;
                if (args.length == 3) {
                    try {
                        id = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("'" + args[2] + "' is not a number");
                        return true;
                    }
                    if (id < 0) {
                        sender.sendMessage("The Shrine ID cannot be negative");
                        return true;
                    }
                } else if (sender instanceof Player) {
                    Player p = (Player) sender;
                    ItemStack item = p.getInventory().getItemInMainHand();
                    id = shrineActivatorId(item);
                    if (id == -1) {
                        sender.sendMessage("This item doesn't contain shrine id information");
                        return true;
                    }
                    ItemMeta meta = item.getItemMeta();
                    //noinspection ConstantConditions meta is never null at this point
                    meta.setDisplayName(null);
                    meta.setLore(null);
                    meta.removeEnchant(Enchantment.DURABILITY);
                    item.setItemMeta(meta);
                } else {
                    sender.sendMessage("Must add a shrine ID or be a player!");
                    return true;
                }
                if (shrineManager.removeShrine(id)) {
                    sender.sendMessage("Shrine ID " + id + " was cleared!");
                } else {
                    sender.sendMessage("Shrine ID " + id + " does not exist or is still valid");
                }
                return true;
            }
            if (args[1].equalsIgnoreCase(SHRINE_SETID_CMD)) {
                int id;
                if (args.length != 3) {
                    sender.sendMessage("Requires additional number argument");
                    return true;
                }

                try {
                    id = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("'" + args[2] + "' is not a number");
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage("The Shrine ID cannot be negative");
                    return true;
                }

                ShrineCore s = shrineManager.getShrine(id);
                if (s == null) {
                    sender.sendMessage("This Shrine does not exist");
                } else {
                    ((Player) sender).getInventory().addItem(s.activatorItem());
                    sender.sendMessage("Item created");
                    return true;
                }
                return true;
            }
        }

        sender.sendMessage("Commands: %s".formatted(SHRINE_CMD));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission(MANAGE_PERM))
            return ImmutableList.of();

        if (args.length == 1) {
            List<String> ret = new ArrayList<>();
            addIfStartsWith(ret, SHRINE_CMD, args[0]);
            return ret;
        }
        if (args.length == 2) {
            List<String> ret = new ArrayList<>();
            if (args[0].equalsIgnoreCase(SHRINE_CMD)) {
                addIfStartsWith(ret, SHRINE_DELETE_CMD, args[1]);
                addIfStartsWith(ret, SHRINE_SETID_CMD, args[1]);
            }
            return ret;
        }

        return ImmutableList.of();
    }

    private void addIfStartsWith(List<String> l, String arg, String s) {
        if (s.startsWith(arg.toLowerCase()))
            l.add(s);
    }
}
