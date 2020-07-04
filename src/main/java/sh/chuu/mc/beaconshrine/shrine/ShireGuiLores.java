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

public class ShireGuiLores {
    public static final ItemStack CLOUD_CHEST_ITEM;
    public static final ItemStack SHOP_ITEM;
    public static final Material INGOT = Material.NETHERITE_INGOT;
    private static final String SHIRE_ID_HEADER = ChatColor.DARK_GRAY + "ID: ";


    static {
        CLOUD_CHEST_ITEM = new ItemStack(Material.CHEST_MINECART);
        ItemMeta cim = CLOUD_CHEST_ITEM.getItemMeta();
        cim.setDisplayName(ChatColor.YELLOW + "Open Personal Cloud Chest");
        cim.setLore(ImmutableList.of(ChatColor.GRAY + "Access this chest from every Shrine!"));
        cim.addEnchant(Enchantment.DURABILITY, 1, true);
        cim.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        CLOUD_CHEST_ITEM.setItemMeta(cim);

        SHOP_ITEM = new ItemStack(Material.EMERALD);
        ItemMeta sim = SHOP_ITEM.getItemMeta();
        sim.setDisplayName(ChatColor.YELLOW + "Scroll Shop");
        sim.addEnchant(Enchantment.DURABILITY, 1, true);
        sim.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        SHOP_ITEM.setItemMeta(sim);
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

    public static ItemStack createShopItem(int stock) {
        ItemStack ret = SHOP_ITEM.clone();
        ItemMeta im = ret.getItemMeta();
        String s;
        if (stock == -1) {
            s = "Currently trading";
        }
        else {
            s = stock + " scrolls on stock";
        }
        im.setLore(ImmutableList.of(
                ChatColor.GRAY.toString() + s
        ));
        ret.setItemMeta(im);
        return ret;
    }
}
