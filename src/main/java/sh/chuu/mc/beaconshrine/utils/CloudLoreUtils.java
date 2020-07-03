package sh.chuu.mc.beaconshrine.utils;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudLoreUtils {
    public static final String INVENTORY_NAME = "Shrine Cloud Chest";
    public static final ItemStack CLOUD_CHEST;

    static {
        CLOUD_CHEST = new ItemStack(Material.CHEST_MINECART);
        ItemMeta cim = CLOUD_CHEST.getItemMeta();
        cim.setDisplayName(ChatColor.YELLOW + "Personal Cloud Chest");
        cim.setLore(ImmutableList.of(ChatColor.WHITE + "Access this chest from every Shrine!"));
        CLOUD_CHEST.setItemMeta(cim);
    }

    public static Inventory getInventory(Player p, String name) {
        ItemStack[] im = p.getInventory().getContents();
        Inventory inv = Bukkit.createInventory(null, 45, name);
        for (int i = 0; i < im.length; i++) {
            if (im[i] == null) continue;
            inv.setItem(i, im[i]);
        }
        return inv;
    }

    public static ItemStack createEnderChestItem(Player p) {
        ItemStack[] ender = p.getEnderChest().getContents();
        ItemStack item = new ItemStack(Material.GREEN_SHULKER_BOX);

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        @SuppressWarnings("ConstantConditions")
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        Inventory sInv = box.getInventory();
        sInv.setContents(ender);
        meta.setBlockState(box);
        meta.setDisplayName(ChatColor.DARK_GREEN + p.getName() + "'s old ender chest");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createExpItem(Player p) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        int level = p.getLevel();
        if (level == 0) {
            if (p.getExp() == 0)
                return null;
        } else {
            item.setAmount(Math.min(level, 64));
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Restore Experience");
        meta.setLore(ImmutableList.of(
                ChatColor.GREEN.toString() + level + " levels",
                ChatColor.GRAY + "Progress: " + (int) (p.getExp() * 100) + "%",
                ChatColor.WHITE.toString() + ChatColor.ITALIC + "Right click to consume"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    public static final Material INGOT = Material.NETHERITE_INGOT;
    private static final String SHIRE_ID_HEADER = ChatColor.GRAY + "id: ";

    public static ItemStack createShrineItem(String name, int id, int x, int z) throws IllegalArgumentException {
        ItemStack ret = new ItemStack(INGOT);
        ItemMeta meta = ret.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("Item does not have ItemMeta!");
        meta.setDisplayName(name);
        meta.setLore(ImmutableList.of(
                ChatColor.AQUA + "Shrine Activator",
                SHIRE_ID_HEADER + id,
                ChatColor.GRAY + "at " + x + ", " + z
        ));
        ret.setItemMeta(meta);
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
        if (lore == null) return -1;
        try {
            return Integer.parseInt(lore.get(1).substring(SHIRE_ID_HEADER.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return -1;
        }
    }

    public static int createCloudItemStack(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        List<String> lore = meta.getLore();
        if (lore == null) return -1;
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
        im.addEnchant(Enchantment.LOYALTY, 1, true);
        ret.setItemMeta(im);
        return ret;
    }

    public static ItemStack createWarpScroll(int id, String name, ChatColor cc) {
        ItemStack ret = new ItemStack(Material.FLOWER_BANNER_PATTERN);
        ItemMeta im = ret.getItemMeta();
        im.setDisplayName(cc + name + " Warp Scroll");
        im.setLore(ImmutableList.of(
                ChatColor.GRAY + "Warp to '" + name + "' with this scroll.",
                ChatColor.GRAY + "Check any Shrine shop to learn how to use this item."
        ));
        im.addEnchant(Enchantment.LOYALTY, 1, true);
        ret.setItemMeta(im);
        return ret;
    }
}
