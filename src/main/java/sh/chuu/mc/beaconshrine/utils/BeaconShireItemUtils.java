package sh.chuu.mc.beaconshrine.utils;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static sh.chuu.mc.beaconshrine.Vars.*;

public interface BeaconShireItemUtils {

    static Inventory copyPlayerInventory(Player p, String name) {
        ItemStack[] im = p.getInventory().getContents();
        Inventory inv = Bukkit.createInventory(null, 45, name);
        for (int i = 0; i < im.length; i++) {
            if (im[i] == null) continue;
            inv.setItem(i, im[i]);
        }
        return inv;
    }

    static ItemStack copyEnderChestToShulkerBox(Player p) {
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

    static ItemStack createWarpScroll(int id, String name, ChatColor cc, Player p) {
        ItemStack ret = new ItemStack(WARP_SCROLL_ITEM_TYPE);
        ItemMeta im = ret.getItemMeta();
        String color = cc == ChatColor.RESET ? ChatColor.WHITE.toString() : ChatColor.RESET.toString() + cc;
        im.setDisplayName(color + name + " Warp Scroll");
        im.setLore(ImmutableList.of(
                ChatColor.GRAY + "Warp to '" + name + "' with this scroll.",
                ChatColor.GRAY + "Purchased by " + p.getName(),
                USE_IN_HAND_TO_CONSUME,
                WARP_SCROLL_UUID_PREFIX + p.getUniqueId(),
                WARP_SCROLL_SHRINE_ID_PREFIX + id
        ));
        im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        ret.setItemMeta(im);
        return ret;
    }

    static WarpScroll getWarpScrollData(ItemStack item) {
        if (item == null || item.getType() != WARP_SCROLL_ITEM_TYPE) return null;
        ItemMeta im = item.getItemMeta();
        if (im == null) return null;
        List<String> lore = im.getLore();
        if (lore == null || lore.size() < 5) return null;
        return new WarpScroll(
                Integer.parseInt(lore.get(4).substring(WARP_SCROLL_SHRINE_ID_PREFIX.length())),
                UUID.fromString(lore.get(3).substring(WARP_SCROLL_UUID_PREFIX.length())));
    }

    record WarpScroll(int id, UUID owner) {
    }

    static ItemStack shrineActivatorItem(Material type, boolean isCore, String name, ChatColor cc, int id, int x, int z) throws IllegalArgumentException {
        ItemStack ret = new ItemStack(type);

        ItemMeta im = ret.getItemMeta();
        if (im == null) throw new IllegalArgumentException("Item does not have ItemMeta!");
        String color = cc == ChatColor.RESET ? ChatColor.WHITE.toString() : ChatColor.RESET.toString() + cc;
        im.setDisplayName(color + (isCore ? SHRINE_CORE_ITEM_NAME : SHRINE_SHARD_ITEM_NAME));
        im.setLore(ImmutableList.of(
                color + name,
                SHIRE_ID_HEADER + id,
                ChatColor.DARK_GRAY + (isCore ? "at " : "Core at ") + x + ", " + z
        ));
        im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ret.setItemMeta(im);
        return ret;
    }

    static int shrineActivatorId(ItemStack item) {
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

    static int getShrineId(Inventory inventory, Material type) { // FIXME Go through this because shards
        for (ItemStack i : inventory) {
            if (i == null || i.getType() != type) continue;
            int itemId = shrineActivatorId(i);
            if (itemId != -1) return itemId;
        }
        return -1;
    }

    record ShrineIdResult(int id, ItemStack item) {}
    static ShrineIdResult getShrineId(Inventory inventory) { // FIXME Go through this because shards
        for (ItemStack i : inventory) {
            if (i == null
                    || i.getType() != SHRINE_CORE_ITEM_TYPE && i.getType() != SHRINE_SHARD_ITEM_TYPE
            ) continue;
            int itemId = shrineActivatorId(i);
            if (itemId != -1) return new ShrineIdResult(itemId, i);
        }
        return null;
    }
}
