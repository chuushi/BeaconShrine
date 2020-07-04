package sh.chuu.mc.beaconshrine.userstate;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.utils.ExperienceUtils;

import java.util.List;

public class CloudInventoryLores {
    static final String INVENTORY_NAME = "Shrine Cloud Chest";
    static final Material EXP_ITEM_TYPE = Material.EXPERIENCE_BOTTLE;
    static final Material TELEPORT_ITEM_TYPE = Material.FLOWER_BANNER_PATTERN;

    private static final String EXP_VALUE_HEADER = ChatColor.DARK_GRAY + "Total exp: ";
    private static final String OLD_LOCATION_HEADER = ChatColor.GRAY + "Old location: ";
    private static final String RIGHT_CLICK_TO_CONSUME = ChatColor.RED.toString() + ChatColor.ITALIC + "Right click to consume";

    public static ItemStack createExpItem(Player p) {
        ItemStack item = new ItemStack(EXP_ITEM_TYPE);
        int level = p.getLevel();
        if (level == 0) {
            if (p.getExp() == 0)
                return null;
        } else {
            item.setAmount(Math.min(level, 64));
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Restore Experience");
        meta.setLore(ImmutableList.of(
                ChatColor.GREEN.toString() + level + " levels",
                ChatColor.GRAY + "Progress: " + (int) (p.getExp() * 100) + "%",
                RIGHT_CLICK_TO_CONSUME,
                EXP_VALUE_HEADER + ExperienceUtils.getExp(p)
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static int getExpItemValue(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 4) return -1;
        try {
            return Integer.parseInt(lore.get(3).substring(EXP_VALUE_HEADER.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return -1;
        }
    }

    public static ItemStack createTeleportationScroll(Location loc) {
        ItemStack item = new ItemStack(TELEPORT_ITEM_TYPE);
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Teleport to old location");
        im.setLore(ImmutableList.of(
                OLD_LOCATION_HEADER + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(),
                RIGHT_CLICK_TO_CONSUME
        ));
        im.addEnchant(Enchantment.DURABILITY, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        item.setItemMeta(im);
        return item;
    }

    public static Location getTeleportLocation(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 2) return null;
        String[] vals = lore.get(0).substring(OLD_LOCATION_HEADER.length()).split(" ");
        if (vals.length != 4) return null;
        World w = Bukkit.getWorld(vals[0]);
        if (w == null) return null;
        try {
            double[] c = {
                    Integer.parseInt(vals[1]) + 0.5,
                    Integer.parseInt(vals[2]) + 0.5,
                    Integer.parseInt(vals[3]) + 0.5
            };
            return new Location(w, c[0], c[1], c[2]);
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return null;
        }
    }
}
