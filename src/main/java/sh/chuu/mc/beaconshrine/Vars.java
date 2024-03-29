package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;

public interface Vars {
    Material SHRINE_CORE_ACTIVATOR_ITEM_TYPE = Material.NETHERITE_INGOT;
    Material SHRINE_SHARD_ACTIVATOR_ITEM_TYPE = Material.AMETHYST_SHARD;
    String SHRINE_CORE_ITEM_NAME = "Shrine Activator";
    String SHRINE_SHARD_ITEM_NAME = "Shrine Shard Activator";
    String SHRINE_ID_HEADER = ChatColor.GRAY + "ID: ";
    String SHARD_INDEX_HEADER = ChatColor.GRAY + "Index: ";
    String SHARD_INDEX_NOT_TUNED = ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + "Not yet tuned";
    String SHRINE_YOU_ARE_HERE = ChatColor.GRAY + "You are here";
    String KEEP_INSIDE_SHULKER_MSG = ChatColor.RED.toString() + ChatColor.ITALIC + "Put and keep this item in shulker box";
    Material CLOUD_CHEST_ITEM_TYPE = Material.CHEST_MINECART;
    Material SHOP_ITEM_TYPE = Material.EMERALD;
    Material ENDER_CHEST_ITEM_TYPE = Material.ENDER_CHEST;
    Material WARP_LIST_ITEM_TYPE = Material.SKULL_BANNER_PATTERN;
    Material WARP_SCROLL_ITEM_TYPE = Material.FLOWER_BANNER_PATTERN;
    Material SHARD_LIST_ITEM_TYPE = Material.AMETHYST_SHARD;
    int GLOBAL_WARP_COOLDOWN = 300000;

    String WARP_SCROLL_SHRINE_ID_PREFIX = ChatColor.DARK_GRAY + "Shrine ID: ";
    String WARP_SCROLL_UUID_PREFIX = ChatColor.DARK_GRAY.toString();
    String USE_IN_HAND_TO_CONSUME = ChatColor.RED.toString() + ChatColor.ITALIC + "Use in hand to consume";

    BaseComponent SAME_DIMENSION_REQUIRED = new TextComponent("Shrine is in another dimension");
    BaseComponent NO_CLEARANCE = new TextComponent("Couldn't find any clearance for this shrine");
    BaseComponent INVALID_SHRINE = new TextComponent("Unable to teleport to the broken shrine");
    BaseComponent INVALID_WARPING = new TextComponent("Move to cancel the current warp first");
    BaseComponent INVALID_SHRINE_SETUP = new TextComponent("Shrine is not set up properly");
}
