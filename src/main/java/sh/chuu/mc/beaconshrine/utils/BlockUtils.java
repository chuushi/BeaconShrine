package sh.chuu.mc.beaconshrine.utils;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {
    public static Block[] getSurrounding8(Block b) {
        return new Block[]{
                b.getRelative(BlockFace.SOUTH),
                b.getRelative(BlockFace.SOUTH_WEST),
                b.getRelative(BlockFace.WEST),
                b.getRelative(BlockFace.NORTH_WEST),
                b.getRelative(BlockFace.NORTH),
                b.getRelative(BlockFace.NORTH_EAST),
                b.getRelative(BlockFace.EAST),
                b.getRelative(BlockFace.SOUTH_EAST)
        };
    }

    public static List<Block> getSurroundingInBeaconBeam(Block b, int radius, int tier) {
        final World w = b.getWorld();
        final int x = b.getX();
        final int y = b.getY();
        final int z = b.getZ();
        final int xh = x + radius;
        final int yh = y + radius;
        final int zh = z + radius;
        final List<Block> ret = new ArrayList<>();
        for (int i = x - radius; i <= xh; i++) {
            for (int k = z - radius; k <= zh; k++) {
                // Continue if only there's beacon below
                if (getBeaconBelow(w.getBlockAt(i, y, k), tier) != null) {
                    for (int j = y - radius; j <= yh; j++) {
                        ret.add(w.getBlockAt(i, j, k));
                    }
                }
            }
        }
        return ret;
    }

    public static Beacon getBeaconBelow(Block b, int tier) {
        while (b.getY() > 3) {
            BlockState state = b.getState();

            if (state instanceof Beacon && ((Beacon) state).getTier() >= tier) {
                return (Beacon) state;
            }
            b = b.getRelative(BlockFace.DOWN);
        }
        return null;
    }

    public static Material getShulkerBoxFromDyeColor(DyeColor color) {
        if (color == null) return Material.SHULKER_BOX;
        switch (color) {
            case WHITE:
                return Material.WHITE_SHULKER_BOX;
            case ORANGE:
                return Material.ORANGE_SHULKER_BOX;
            case MAGENTA:
                return Material.MAGENTA_SHULKER_BOX;
            case LIGHT_BLUE:
                return Material.LIGHT_BLUE_SHULKER_BOX;
            case YELLOW:
                return Material.YELLOW_SHULKER_BOX;
            case LIME:
                return Material.LIME_SHULKER_BOX;
            case PINK:
                return Material.PINK_SHULKER_BOX;
            case GRAY:
                return Material.GRAY_SHULKER_BOX;
            case LIGHT_GRAY:
                return Material.LIGHT_GRAY_SHULKER_BOX;
            case CYAN:
                return Material.CYAN_SHULKER_BOX;
            case PURPLE:
                return Material.PURPLE_SHULKER_BOX;
            case BLUE:
                return Material.BLUE_SHULKER_BOX;
            case BROWN:
                return Material.BROWN_SHULKER_BOX;
            case GREEN:
                return Material.GREEN_SHULKER_BOX;
            case RED:
                return Material.RED_SHULKER_BOX;
            case BLACK:
                return Material.BLACK_SHULKER_BOX;
        }
        return Material.SHULKER_BOX;
    }

    public static boolean hasInteraction(Material block) {
        switch (block) {
            // <editor-fold defaultstate="collapsed" desc="isInteractable">
            case ACACIA_BUTTON:
            case ACACIA_DOOR:
            case ACACIA_FENCE_GATE:
            case ACACIA_SIGN:
            case ACACIA_TRAPDOOR:
            case ACACIA_WALL_SIGN:
            case ANVIL:
            case BARREL:
            case BEACON:
            case BEEHIVE:
            case BEE_NEST:
            case BELL:
            case BIRCH_BUTTON:
            case BIRCH_DOOR:
            case BIRCH_FENCE_GATE:
            case BIRCH_SIGN:
            case BIRCH_TRAPDOOR:
            case BIRCH_WALL_SIGN:
            case BLACK_BED:
            case BLACK_SHULKER_BOX:
            case BLAST_FURNACE:
            case BLUE_BED:
            case BLUE_SHULKER_BOX:
            case BREWING_STAND:
            case BROWN_BED:
            case BROWN_SHULKER_BOX:
            case CAKE:
            case CAMPFIRE:
            case CARTOGRAPHY_TABLE:
            case CAULDRON:
            case CHAIN_COMMAND_BLOCK:
            case CHEST:
            case CHIPPED_ANVIL:
            case COMMAND_BLOCK:
            case COMPARATOR:
            case COMPOSTER:
            case CRAFTING_TABLE:
            case CRIMSON_BUTTON:
            case CRIMSON_DOOR:
            case CRIMSON_FENCE_GATE:
            case CRIMSON_SIGN:
            case CRIMSON_TRAPDOOR:
            case CRIMSON_WALL_SIGN:
            case CYAN_BED:
            case CYAN_SHULKER_BOX:
            case DAMAGED_ANVIL:
            case DARK_OAK_BUTTON:
            case DARK_OAK_DOOR:
            case DARK_OAK_FENCE_GATE:
            case DARK_OAK_SIGN:
            case DARK_OAK_TRAPDOOR:
            case DARK_OAK_WALL_SIGN:
            case DAYLIGHT_DETECTOR:
            case DISPENSER:
            case DRAGON_EGG:
            case DROPPER:
            case ENCHANTING_TABLE:
            case ENDER_CHEST:
            case FLETCHING_TABLE:
            case FLOWER_POT:
            case FURNACE:
            case GRAY_BED:
            case GRAY_SHULKER_BOX:
            case GREEN_BED:
            case GREEN_SHULKER_BOX:
            case GRINDSTONE:
            case HOPPER:
            case IRON_DOOR:
            case IRON_TRAPDOOR:
            case JIGSAW:
            case JUKEBOX:
            case JUNGLE_BUTTON:
            case JUNGLE_DOOR:
            case JUNGLE_FENCE_GATE:
            case JUNGLE_SIGN:
            case JUNGLE_TRAPDOOR:
            case JUNGLE_WALL_SIGN:
            case LECTERN:
            case LEVER:
            case LIGHT_BLUE_BED:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_BED:
            case LIGHT_GRAY_SHULKER_BOX:
            case LIME_BED:
            case LIME_SHULKER_BOX:
            case LOOM:
            case MAGENTA_BED:
            case MAGENTA_SHULKER_BOX:
            case MOVING_PISTON:
            case NOTE_BLOCK:
            case OAK_BUTTON:
            case OAK_DOOR:
            case OAK_FENCE_GATE:
            case OAK_SIGN:
            case OAK_TRAPDOOR:
            case OAK_WALL_SIGN:
            case ORANGE_BED:
            case ORANGE_SHULKER_BOX:
            case PINK_BED:
            case PINK_SHULKER_BOX:
            case POLISHED_BLACKSTONE_BUTTON:
            case POTTED_ACACIA_SAPLING:
            case POTTED_ALLIUM:
            case POTTED_AZURE_BLUET:
            case POTTED_BAMBOO:
            case POTTED_BIRCH_SAPLING:
            case POTTED_BLUE_ORCHID:
            case POTTED_BROWN_MUSHROOM:
            case POTTED_CACTUS:
            case POTTED_CORNFLOWER:
            case POTTED_CRIMSON_FUNGUS:
            case POTTED_CRIMSON_ROOTS:
            case POTTED_DANDELION:
            case POTTED_DARK_OAK_SAPLING:
            case POTTED_DEAD_BUSH:
            case POTTED_FERN:
            case POTTED_JUNGLE_SAPLING:
            case POTTED_LILY_OF_THE_VALLEY:
            case POTTED_OAK_SAPLING:
            case POTTED_ORANGE_TULIP:
            case POTTED_OXEYE_DAISY:
            case POTTED_PINK_TULIP:
            case POTTED_POPPY:
            case POTTED_RED_MUSHROOM:
            case POTTED_RED_TULIP:
            case POTTED_SPRUCE_SAPLING:
            case POTTED_WARPED_FUNGUS:
            case POTTED_WARPED_ROOTS:
            case POTTED_WHITE_TULIP:
            case POTTED_WITHER_ROSE:
            case PURPLE_BED:
            case PURPLE_SHULKER_BOX:
            case REDSTONE_ORE:
            case REDSTONE_WIRE:
            case RED_BED:
            case RED_SHULKER_BOX:
            case REPEATER:
            case REPEATING_COMMAND_BLOCK:
            case RESPAWN_ANCHOR:
            case SHULKER_BOX:
            case SMITHING_TABLE:
            case SMOKER:
            case SOUL_CAMPFIRE:
            case SPRUCE_BUTTON:
            case SPRUCE_DOOR:
            case SPRUCE_FENCE_GATE:
            case SPRUCE_SIGN:
            case SPRUCE_TRAPDOOR:
            case SPRUCE_WALL_SIGN:
            case STONECUTTER:
            case STONE_BUTTON:
            case STRUCTURE_BLOCK:
            case SWEET_BERRY_BUSH:
            case TNT:
            case TRAPPED_CHEST:
            case WARPED_BUTTON:
            case WARPED_DOOR:
            case WARPED_FENCE_GATE:
            case WARPED_SIGN:
            case WARPED_TRAPDOOR:
            case WARPED_WALL_SIGN:
            case WHITE_BED:
            case WHITE_SHULKER_BOX:
            case YELLOW_BED:
            case YELLOW_SHULKER_BOX:
                // </editor-fold>
                return true;
            default:
                return false;
        }
    }
}
