package sh.chuu.mc.beaconshrine.utils;

import com.google.common.collect.ImmutableList;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.shrine.ShrineMultiblock;

import java.util.List;
import java.util.UUID;

public class BeaconShireItemUtils {
    public static final Material WARP_SCROLL_MATERIAL = Material.FLOWER_BANNER_PATTERN;

    private static final String WARP_SCROLL_SHRINE_ID_PREFIX = ChatColor.DARK_GRAY + "Shrine ID: ";
    private static final String WARP_SCROLL_UUID_PREFIX = ChatColor.DARK_GRAY.toString();
    private static final String USE_IN_HAND_TO_CONSUME = ChatColor.RED.toString() + ChatColor.ITALIC + "Use in hand to consume";
    private static final BaseComponent[] INVALID_SHRINE = new BaseComponent[]{new TextComponent("Unable to teleport to the broken shrine")};

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

    public static ItemStack createWarpScroll(int id, String name, ChatColor cc, Player p) {
        ItemStack ret = new ItemStack(WARP_SCROLL_MATERIAL);
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

    public static WarpScroll getWarpScrollData(ItemStack item) {
        if (item == null || item.getType() != WARP_SCROLL_MATERIAL) return null;
        ItemMeta im = item.getItemMeta();
        if (im == null) return null;
        List<String> lore = im.getLore();
        if (lore == null || lore.size() < 5) return null;
        return new WarpScroll(
                Integer.parseInt(lore.get(4).substring(WARP_SCROLL_SHRINE_ID_PREFIX.length())),
                UUID.fromString(lore.get(3).substring(WARP_SCROLL_UUID_PREFIX.length())));
    }

    public static boolean useWarpScroll(Player p, WarpScroll ws) {
        ShrineMultiblock shrine = BeaconShrine.getInstance().getShrineManager().getShrine(ws.id);
        if (shrine.isValid()) {
            World w = shrine.getWorld();
            int x = shrine.getX();
            int z = shrine.getZ();
            // TODO make this a timed event
            Location l = p.getLocation();
            l.setX(x + 0.5d);
            l.setY(w.getHighestBlockYAt(x, z) + 30);
            l.setZ(z + 0.5d);
            p.teleport(l);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false, false));
            return true;
        } else {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return false;
        }
    }

    @Immutable
    public static class WarpScroll {
        public final int id;
        public final UUID owner;

        private WarpScroll(int id, UUID owner) {
            this.id = id;
            this.owner = owner;
        }
    }
}
