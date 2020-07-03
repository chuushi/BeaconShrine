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

public class ShireInventoryLores {
    public static final ItemStack CLOUD_CHEST;
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    public static final Material INGOT = Material.NETHERITE_INGOT;
    private static final String SHIRE_ID_HEADER = ChatColor.GRAY + "id: ";


    static {
        CLOUD_CHEST = new ItemStack(Material.CHEST_MINECART);
        ItemMeta cim = CLOUD_CHEST.getItemMeta();
        cim.setDisplayName(ChatColor.YELLOW + "Open Personal Cloud Chest");
        cim.setLore(ImmutableList.of(ChatColor.GRAY + "Access this chest from every Shrine!"));
        CLOUD_CHEST.setItemMeta(cim);
    }

    public static ItemStack createShrineItem(String name, int id, int x, int z) throws IllegalArgumentException {
        ItemStack ret = new ItemStack(INGOT);
        ItemMeta im = ret.getItemMeta();
        if (im == null) throw new IllegalArgumentException("Item does not have ItemMeta!");
        im.setDisplayName(name);
        im.setLore(ImmutableList.of(
                ChatColor.AQUA + "Shrine Activator",
                SHIRE_ID_HEADER + id,
                ChatColor.GRAY + "at " + x + ", " + z
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
        ItemStack ret = new ItemStack(Material.EMERALD);
        ItemMeta im = ret.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Scroll Shop");
        im.setLore(ImmutableList.of(
                ChatColor.GRAY.toString() + stock + " scrolls on stock"
        ));
        im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        ret.setItemMeta(im);
        return ret;
    }
}
