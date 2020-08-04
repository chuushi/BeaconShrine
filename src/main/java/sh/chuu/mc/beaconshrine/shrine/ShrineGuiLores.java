package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShrineGuiLores {
    public static final ItemStack CLOUD_CHEST_ITEM;
    public static final ItemStack SHOP_ITEM;
    public static final ItemStack ENDER_CHEST_ITEM;
    public static final ItemStack WARP_LIST_ITEM;
    public static final Material CLOUD_CHEST_ITEM_TYPE = Material.CHEST_MINECART;
    public static final Material SHOP_ITEM_TYPE = Material.EMERALD;
    public static final Material ENDER_CHEST_ITEM_TYPE = Material.ENDER_CHEST;
    public static final Material WARP_LIST_ITEM_TYPE = Material.SKULL_BANNER_PATTERN;
    public static final Material WARP_SCROLL_ITEM_TYPE = Material.FLOWER_BANNER_PATTERN;
    public static final Material INGOT_ITEM_TYPE = Material.NETHERITE_INGOT;
    static final long RESTOCK_TIMER = 7200000; // 2 hours
    private static final String SHIRE_ID_HEADER = ChatColor.DARK_GRAY + "ID: ";
    private static final String SHIRE_YOU_ARE_HERE = ChatColor.GRAY + "You are here";

    static {
        CLOUD_CHEST_ITEM = createGuiItem("Open Personal Cloud Chest",
                CLOUD_CHEST_ITEM_TYPE,
                ImmutableList.of(ChatColor.GRAY + "Access this chest from every Shrine!"),
                true);

        SHOP_ITEM = createGuiItem("Open Scroll Shop", SHOP_ITEM_TYPE, null, true);

        ENDER_CHEST_ITEM = createGuiItem("Open Ender Chest", ENDER_CHEST_ITEM_TYPE, null, false);

        WARP_LIST_ITEM = createGuiItem("Warp to...",
                WARP_LIST_ITEM_TYPE,
                ImmutableList.of(ChatColor.GRAY + "Warp to any tuned shrines"),
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

    public static ItemStack createShrineActivatorItem(String name, ChatColor cc, int id, int x, int z) throws IllegalArgumentException {
        ItemStack ret = new ItemStack(INGOT_ITEM_TYPE);
        ItemMeta im = ret.getItemMeta();
        if (im == null) throw new IllegalArgumentException("Item does not have ItemMeta!");
        String color = cc == ChatColor.RESET ? ChatColor.WHITE.toString() : ChatColor.RESET.toString() + cc;
        im.setDisplayName(color + "Shrine Activator");
        im.setLore(ImmutableList.of(
                color + name,
                SHIRE_ID_HEADER + id,
                ChatColor.DARK_GRAY + "at " + x + ", " + z
        ));
        im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        ret.setItemMeta(im);
        return ret;
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

    public static int getShrineId(Inventory inventory) {
        HashMap<Integer, ? extends ItemStack> ingots = inventory.all(INGOT_ITEM_TYPE);
        for (Map.Entry<Integer, ? extends ItemStack> e : ingots.entrySet()) {
            int itemId = getShrineId(e.getValue());
            if (itemId != -1) return itemId;
        }
        return -1;
    }

    public static int getShrineId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 3) return -1;
        try {
            return Integer.parseInt(lore.get(1).substring(SHIRE_ID_HEADER.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return -1;
        }
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
                ChatColor.GRAY + timeLeft(restock)
        ));
        ret.setItemMeta(im);
        return ret;
    }

    private static final String FULLY_STOCKED = "Fully stocked!";
    private static String timeLeft(long restock) {
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
}
