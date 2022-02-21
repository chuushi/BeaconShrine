package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;

public interface Vars {
    Material INGOT_ITEM_TYPE = Material.NETHERITE_INGOT;
    String SHIRE_ID_HEADER = ChatColor.DARK_GRAY + "ID: ";
    String SHIRE_YOU_ARE_HERE = ChatColor.GRAY + "You are here";
    Material CLOUD_CHEST_ITEM_TYPE = Material.CHEST_MINECART;
    Material SHOP_ITEM_TYPE = Material.EMERALD;
    Material ENDER_CHEST_ITEM_TYPE = Material.ENDER_CHEST;
    Material WARP_LIST_ITEM_TYPE = Material.SKULL_BANNER_PATTERN;
    Material WARP_SCROLL_ITEM_TYPE = Material.FLOWER_BANNER_PATTERN;
    int GLOBAL_WARP_COOLDOWN = 300000;

    BaseComponent SAME_DIMENSION_REQUIRED = new TextComponent("Shrine is in another dimension");
    BaseComponent NO_CLEARANCE = new TextComponent("Couldn't find any clearance for this shrine");
    BaseComponent INVALID_SHRINE = new TextComponent("Unable to teleport to the broken shrine");
    BaseComponent INVALID_WARPING = new TextComponent("Move to cancel the current warp first");
}
