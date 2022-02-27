package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sh.chuu.mc.beaconshrine.Vars.*;
import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.shrineActivatorId;

public class ShrineGUI {
    public static final ItemStack CLOUD_CHEST_ITEM;
    public static final ItemStack SHOP_ITEM;
    public static final ItemStack ENDER_CHEST_ITEM;
    public static final ItemStack WARP_LIST_ITEM;
    public static final ItemStack SHARD_LIST_ITEM;
    static final long RESTOCK_TIMER = 3600000; // 1 hour

    static {
        CLOUD_CHEST_ITEM = createGuiItem("Open Personal Cloud Chest",
                CLOUD_CHEST_ITEM_TYPE,
                ImmutableList.of(ChatColor.GRAY + "Access this chest from every Shrine!"),
                true);

        SHOP_ITEM = createGuiItem("Open Scroll Shop", SHOP_ITEM_TYPE, null, true);

        ENDER_CHEST_ITEM = createGuiItem("Open Ender Chest", ENDER_CHEST_ITEM_TYPE, null, false);

        WARP_LIST_ITEM = createGuiItem("Warp to another Shrine...",
                WARP_LIST_ITEM_TYPE,
                ImmutableList.of(ChatColor.GRAY + "Warp to any tuned shrines"),
                true);

        SHARD_LIST_ITEM = createGuiItem("Warp to a Shard...",
                SHARD_LIST_ITEM_TYPE,
                ImmutableList.of(ChatColor.GRAY + "Warp to any tuned shards within this shrine cluster"),
                true);
        // TODO change item and meta, and add gui event to this
    }

    private static ItemStack createGuiItem(String name, Material material, List<String> lore, boolean shiny) {
        ItemStack ret = new ItemStack(material);  // TODO change item and meta, and add gui event to this
        ItemMeta im = ret.getItemMeta();
        //noinspection ConstantConditions Existing item always have ItemMeta
        im.setDisplayName(ChatColor.YELLOW + name);
        if (lore != null)
            im.setLore(lore);
        if (shiny)
            im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
        ret.setItemMeta(im);
        return ret;
    }

    public static ItemStack shulkerBox(BlockState inventoryState, DyeColor color) {
        // TODO figure out if setting material is required (ItemMeta contains item info)
        ItemStack shulker = new ItemStack(BlockUtils.getShulkerBoxFromDyeColor(color));

        BlockStateMeta m = (BlockStateMeta) shulker.getItemMeta();
        m.setBlockState(inventoryState);
        m.setDisplayName(ChatColor.YELLOW + "Open Shrine Shulker Box");
        shulker.setItemMeta(m);

        return shulker;
    }

    public static ItemStack createWarpGui(int id, String name, Material symbol, ChatColor cc, boolean urHere) {
        ItemStack ret = new ItemStack(symbol == null ? WARP_LIST_ITEM_TYPE : symbol);
        ItemMeta im = ret.getItemMeta();
        String color = cc == ChatColor.RESET ? ChatColor.WHITE.toString() : ChatColor.RESET.toString() + cc;
        //noinspection ConstantConditions meta always exists
        im.setDisplayName(ChatColor.YELLOW + "Warp to: " + color + name);
        if (urHere) {
            im.setLore(ImmutableList.of(SHIRE_YOU_ARE_HERE));
        } else {
            im.setLore(ImmutableList.of(SHIRE_ID_HEADER + id));
            im.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);
        ret.setItemMeta(im);
        return ret;
    }

    public static boolean isWarpGui(ItemStack item) {
        ItemMeta im = item.getItemMeta();
        if (im != null && im.hasLore()) {
            List<String> l = im.getLore();
            if (l.size() == 0) return false;
            try {
                String line1 = l.get(0);
                return line1.equals(SHIRE_YOU_ARE_HERE) || line1.startsWith(SHIRE_ID_HEADER);
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    public static int getWarpIdGui(ItemStack item) {
        if (item == null) return -1;
        ItemMeta im = item.getItemMeta();
        if (im.hasLore()) {
            List<String> l = im.getLore();
            if (l.size() == 0) return -1;
            try {
                return Integer.parseInt(l.get(0).substring(SHIRE_ID_HEADER.length()));
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        return -1;
    }

    public static ItemStack createShopItem(int stock, long restock) {
        ItemStack ret = SHOP_ITEM.clone();
        ItemMeta im = ret.getItemMeta();
        String s;
        if (stock == -1) {
            s = "Currently trading";
        }
        else {
            s = stock + " scrolls on stock";
        }
        //noinspection ConstantConditions Existing item always have ItemMeta
        im.setLore(ImmutableList.of(
                ChatColor.GRAY + s,
                ChatColor.GRAY + restockTimeLeft(restock)
        ));
        ret.setItemMeta(im);
        return ret;
    }

    private static final String FULLY_STOCKED = "Fully stocked!";
    private static String restockTimeLeft(long restock) {
        if (restock == -1) {
            return FULLY_STOCKED;
        }
        restock = System.currentTimeMillis() - restock;
        if (restock >= RESTOCK_TIMER) {
            return FULLY_STOCKED;
        }
        restock = RESTOCK_TIMER - restock;
        if (restock > 3600000) {
            long ret = (restock/3600000) + 1;
            return "Restocking in " + ret + " hours";
        } else if (restock > 60000) {
            long ret = (restock/60000) + 1;
            return "Restocking in " + ret + " minutes";
        } else {
            return "Restocking in less than a minute";
        }
    }

    public static String warpTimeLeft(long diff) {
        if (diff > 60000) {
            long ret = (diff/60000) + 1;
            return "You can warp in " + ret + " minutes";
        } else {
            long ret = (diff/1000) + 1;
            return "You can warp in " + ret + " seconds";
        }
    }


    public static List<Integer> getWarpOrderGui(Inventory inv, int currentId) {
        List<Integer> ret = new ArrayList<>();
        for (ItemStack item : inv) {
            if (item == null || item.getType() == Material.AIR) continue;
            int id = getWarpIdGui(item);
            if (id == -1) ret.add(currentId);
            else ret.add(id);
        }
        return ret;
    }

    public static void clickNoise(Player p) {
        p.playSound(p, Sound.UI_BUTTON_CLICK, 0.25f, 1f);
    }
}
