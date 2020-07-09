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
    public static final Material CLOUD_CHEST_ITEM_TYPE = Material.CHEST_MINECART;
    public static final Material SHOP_ITEM_TYPE = Material.EMERALD;
    public static final Material ENDER_CHEST_ITEM_TYPE = Material.ENDER_CHEST;
    public static final Material INGOT = Material.NETHERITE_INGOT;
    static final long RESTOCK_TIMER = 21600000;
    private static final String SHIRE_ID_HEADER = ChatColor.DARK_GRAY + "ID: ";

    static {
        CLOUD_CHEST_ITEM = new ItemStack(CLOUD_CHEST_ITEM_TYPE);
        ItemMeta cim = CLOUD_CHEST_ITEM.getItemMeta();
        //noinspection ConstantConditions Existing item always have ItemMeta
        cim.setDisplayName(ChatColor.YELLOW + "Open Personal Cloud Chest");
        cim.setLore(ImmutableList.of(ChatColor.GRAY + "Access this chest from every Shrine!"));
        cim.addEnchant(Enchantment.DURABILITY, 1, true);
        cim.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        CLOUD_CHEST_ITEM.setItemMeta(cim);

        SHOP_ITEM = new ItemStack(SHOP_ITEM_TYPE);
        ItemMeta sim = SHOP_ITEM.getItemMeta();
        //noinspection ConstantConditions Existing item always have ItemMeta
        sim.setDisplayName(ChatColor.YELLOW + "Open Scroll Shop");
        sim.addEnchant(Enchantment.DURABILITY, 1, true);
        sim.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        SHOP_ITEM.setItemMeta(sim);

        ENDER_CHEST_ITEM = new ItemStack(ENDER_CHEST_ITEM_TYPE);
        ItemMeta eim = ENDER_CHEST_ITEM.getItemMeta();
        //noinspection ConstantConditions Existing item always have ItemMeta
        eim.setDisplayName(ChatColor.YELLOW + "Open Ender Chest");
        eim.addEnchant(Enchantment.DURABILITY, 1, true);
        eim.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        ENDER_CHEST_ITEM.setItemMeta(eim);
    }

    public static ItemStack createShrineItem(String name, ChatColor cc, int id, int x, int z) throws IllegalArgumentException {
        ItemStack ret = new ItemStack(INGOT);
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

    public static int getShrineId(Inventory inventory) {
        HashMap<Integer, ? extends ItemStack> ingots = inventory.all(INGOT);
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
