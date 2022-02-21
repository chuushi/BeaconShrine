package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

public class ShrineShard extends AbstractShrine {
    private final int parentId;

    ShrineShard(int id, int parentId, ShulkerBox shulker) {
        super(id, shulker);
        this.parentId = parentId;
    }

    ShrineShard(int id, int parentId, ConfigurationSection cs) {
        super(id, cs);
        this.parentId = parentId;
    }

    @Override
    public boolean isValid() {
        Block shulker = w.getBlockAt(x, y, z);
        Block netherite = shulker.getRelative(BlockFace.DOWN);
        return shulker.getState() instanceof ShulkerBox s
                && s.getCustomName() != null
                && netherite.getType() == Material.NETHERITE_BLOCK;
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

    public int parentId() {
        return parentId;
    }

    public void save(ConfigurationSection cs) {
        cs.set("parentId", parentId);
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, y, z});
        cs.set("symIT", symbolItemType == null ? null : symbolItemType.name());
    }
}
