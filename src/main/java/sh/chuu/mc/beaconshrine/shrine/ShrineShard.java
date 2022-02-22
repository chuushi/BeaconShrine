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
import sh.chuu.mc.beaconshrine.ShrineItemStack;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class ShrineShard extends AbstractShrine {
    private final ShrineCore parent;

    ShrineShard(int id, ShrineCore parent, ShulkerBox shulker) {
        super(id, shulker);
        this.parent = parent;
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
        Block netherite = shulker.getRelative(BlockFace.DOWN);
        return shulker.getState() instanceof ShulkerBox s
                && s.getCustomName() != null
                && netherite.getType() == Material.NETHERITE_BLOCK
                && parent.isValid()
                && parent.containsShard(this);
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

    @Override
    public ItemStack makeShrineActivatorItem() {
        return ShrineItemStack.shrineActivatorItem(SHRINE_SHARD_ITEM_TYPE, name, chatColor, id, x, z);
    }

    public AbstractShrine parent() {
        return parent;
    }

}
