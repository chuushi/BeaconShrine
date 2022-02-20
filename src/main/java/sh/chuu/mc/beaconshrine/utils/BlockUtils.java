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

    public static List<Block> getSurrounding(Block b, int radius) {
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
                for (int j = y - radius; j <= yh; j++) {
                    ret.add(w.getBlockAt(i, j, k));
                }
            }
        }
        return ret;
    }

    public static Beacon getBeaconBelow(Block b, int tier) {
        while (b.getY() > -61) {
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
        return switch (color) {
            // <editor-fold defaultstate="collapsed" desc="shulkerBoxFromDyeColor">
            case WHITE -> Material.WHITE_SHULKER_BOX;
            case ORANGE -> Material.ORANGE_SHULKER_BOX;
            case MAGENTA -> Material.MAGENTA_SHULKER_BOX;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_SHULKER_BOX;
            case YELLOW -> Material.YELLOW_SHULKER_BOX;
            case LIME -> Material.LIME_SHULKER_BOX;
            case PINK -> Material.PINK_SHULKER_BOX;
            case GRAY -> Material.GRAY_SHULKER_BOX;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_SHULKER_BOX;
            case CYAN -> Material.CYAN_SHULKER_BOX;
            case PURPLE -> Material.PURPLE_SHULKER_BOX;
            case BLUE -> Material.BLUE_SHULKER_BOX;
            case BROWN -> Material.BROWN_SHULKER_BOX;
            case GREEN -> Material.GREEN_SHULKER_BOX;
            case RED -> Material.RED_SHULKER_BOX;
            case BLACK -> Material.BLACK_SHULKER_BOX;
            // </editor-fold>
        };
    }

    public static boolean hasInteraction(Material block) {
        String n = block.name();
        if (n.endsWith("_STAIRS")
                || n.endsWith("_FENCE")
        ) return false;
        return block.isInteractable();
    }
}
