package sh.chuu.mc.beaconshrine;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;

import java.util.ArrayList;
import java.util.List;

import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.shrineActivatorId;

public class ShrineCommands implements TabExecutor {
    private static final String MANAGE_PERM = "beaconshrine.admin";
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private static final String SHRINE_CMD = "shrine";
    private static final String SHRINE_DELETE_CMD = "delete";
    private static final String SHRINE_ACTIVATORITEM_CMD = "activatoritem";
    private static final String SHRINE_ACTIVATORITEM_SHARD_CMD = "shard";
    private static final String SHRINE_ACTIVATORITEM_CORE_CMD = "core";

    private void giveId(CommandSender sender, InventoryHolder p, int id, boolean isCore) {
        ShrineCore s = plugin.getShrineManager().getShrine(id);
        if (s == null) {
            sender.sendMessage("This Shrine does not exist");
        } else {
            p.getInventory().addItem(isCore
                    ? s.activatorItem()
                    : s.shardActivatorItem());
            sender.sendMessage("Item created");
        }
    }

    private int clearIdFromItem(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        int id = shrineActivatorId(item);
        ItemMeta meta = item.getItemMeta();
        //noinspection ConstantConditions meta is never null at this point
        meta.setDisplayName(null);
        meta.setLore(null);
        meta.removeEnchant(Enchantment.DURABILITY);
        item.setItemMeta(meta);
        return id;
    }

    private void deleteId(CommandSender sender, int id) {
        if (plugin.getShrineManager().removeShrine(id)) {
            sender.sendMessage("Shrine ID " + id + " was cleared!");
        } else {
            sender.sendMessage("Shrine ID " + id + " does not exist or is still valid");
        }
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String [] args) {
        if (args.length == 0 || !sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage("BeaconShrine v" + plugin.getDescription().getVersion() + "by Simon Chuu");
            sender.sendMessage("https://chuu.sh/mc/BeaconShrine (site doesn't exist yet)");
            return true;
        }

        if (args[0].equalsIgnoreCase(SHRINE_CMD)) {
            if (args.length == 1) {
                sender.sendMessage(String.format("Subcommands: %s %s", SHRINE_DELETE_CMD, SHRINE_ACTIVATORITEM_CMD));
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
                } else if (sender instanceof Player p) {
                    id = clearIdFromItem(p);
                    if (id == -1) {
                        p.sendMessage("This item doesn't contain shrine id information");
                        return true;
                    }
                } else {
                    sender.sendMessage("Must add a shrine ID or be a player!");
                    return true;
                }
                deleteId(sender, id);
                return true;
            }
            if (args[1].equalsIgnoreCase(SHRINE_ACTIVATORITEM_CMD)) {
                int id;
                if (args.length < 3) {
                    sender.sendMessage("Usage: /"
                            + label
                            + SHRINE_CMD
                            + SHRINE_ACTIVATORITEM_CMD
                            + "<shrine ID>"
                            + "<" + SHRINE_ACTIVATORITEM_CORE_CMD + "|" + SHRINE_ACTIVATORITEM_SHARD_CMD + ">"
                            + "[player]"
                    );
                    return true;
                }

                try {
                    id = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("'" + args[2] + "' is not a number");
                    return true;
                }

                boolean isCore = !args[3].equalsIgnoreCase(SHRINE_ACTIVATORITEM_SHARD_CMD);

                Player p;
                if (args.length > 4) {
                    p = Bukkit.getPlayer(args[4]);
                } else {
                    if (sender instanceof Player) {
                        p = (Player) sender;
                    } else {
                        sender.sendMessage("Add a player or send as player");
                        return true;
                    }
                }

                if (id < 0) {
                    sender.sendMessage("The Shrine ID cannot be negative");
                    return true;
                }
                giveId(sender, p, id, isCore);
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
                addIfStartsWith(ret, SHRINE_ACTIVATORITEM_CMD, args[1]);
            }
            return ret;
        }
        if (args.length == 3) {
            List<String> ret = new ArrayList<>();
            if (args[0].equalsIgnoreCase(SHRINE_CMD) && args[1].equalsIgnoreCase(SHRINE_ACTIVATORITEM_CMD)) {
                addIfStartsWith(ret, SHRINE_ACTIVATORITEM_CORE_CMD, args[1]);
                addIfStartsWith(ret, SHRINE_ACTIVATORITEM_SHARD_CMD, args[1]);
            }
            return ret;
        }
        if (args.length == 4) {
            List<String> ret = new ArrayList<>();
            if (args[0].equalsIgnoreCase(SHRINE_CMD) && args[1].equalsIgnoreCase(SHRINE_ACTIVATORITEM_CMD)) {
                return null;
            }
        }

        return ImmutableList.of();
    }

    private void addIfStartsWith(List<String> l, String arg, String s) {
        if (s.startsWith(arg.toLowerCase()))
            l.add(s);
    }
}
