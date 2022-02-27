package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sh.chuu.mc.beaconshrine.Vars.SHRINE_CORE_ACTIVATOR_ITEM_TYPE;

public class ShrineShard extends AbstractShrine {
    private final ShrineCore parent;
    private Location lodestone; // TODO Replace with relative block face of Lodestone instead

    ShrineShard(int id, ShrineCore parent, ShulkerBox shulker, Location lodestone) {
        super(id, shulker);
        this.parent = parent;
        this.lodestone = lodestone;
    }

    public ShrineShard(int id, ShrineCore parent, Map<?, ?> ss) {
        super(id, ss);
        this.parent = parent;
        @SuppressWarnings("unchecked")
        List<Integer> loc = (List<Integer>) ss.get("lodestone");
        this.lodestone = new Location(w, loc.get(0), loc.get(1), loc.get(2));
    }

    @Override
    public ItemStack activatorItem() {
        return parent.shardActivatorItem();
    }

    /**
     * Is valid if
     * the shulker box contains a Netherite ingot,
     * A block adjacent is a Netherite block
     * the parent shrine is not far away
     *
     * @return true if this shrine is valid
     */
    @Override
    public boolean isValid() {
        Block shulker = w.getBlockAt(x, y, z);
        return shulker.getState() instanceof ShulkerBox s
                && s.getCustomName() != null
                && parent.isValid()
                && parent.containsShard(this)
                && getLodestone() != null;
    }

    public Block getLodestone() {
        Block lode = lodestone.getBlock();
        if (lode.getType() == Material.LODESTONE)
            return lode;

        Block shulker = w.getBlockAt(x, y, z);
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST}) {
            Block b = shulker.getRelative(face);
            if (b.getType() == Material.LODESTONE) {
                this.lodestone = b.getLocation();
                return b;
            }
        }
        return null;
    }

    @Override
    public Inventory getGui(Player p) {
        ShulkerBox invState = (ShulkerBox) w.getBlockAt(x, y, z).getState();

        // Start making inventory
        Inventory gui = Bukkit.createInventory(null, InventoryType.HOPPER, chatColor + name);
        gui.setItem(0, ShrineGUI.shulkerBox(invState, color));

        // Add Ender Chest if it exists in Shulker inventory
        if (invState.getInventory().contains(Material.ENDER_CHEST)) gui.setItem(4, ShrineGUI.ENDER_CHEST_ITEM);
        // TODO add other shard list
        // TODO add direct to main shrine
        return gui;
    }

    @Override
    protected void theParticles(int step) {
        ParticleUtils.shrineSpin(new Location(w, midX, midY, midZ), dustColor(), 2, step * 0.5);
    }

    @Override
    protected boolean warpSequence(int step, WarpSequence ws, Player p) {
        return step == 0;
    }

    public ShrineCore parent() {
        return parent;
    }

    @Override
    public HashMap<String, Object> save() {
        HashMap<String, Object> ret = super.save();
        ret.put("lodestone", new int[]{lodestone.getBlockX(), lodestone.getBlockY(), lodestone.getBlockZ()});
        return ret;
    }
}
