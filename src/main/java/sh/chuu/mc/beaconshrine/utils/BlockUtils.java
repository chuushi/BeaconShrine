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
}
