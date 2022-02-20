package sh.chuu.mc.beaconshrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static sh.chuu.mc.beaconshrine.Vars.*;

public interface ShrineItemStack {
    default ItemStack getItemScroll(int id) {
        ItemStack item = new ItemStack(Material.FLOWER_BANNER_PATTERN);

        return item;
    }

    default int getId(ItemStack item) {
        return 0;
    }

    default ItemStack getGuiScroll(int id) {
        return null;
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

}
