package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class ShrineShard extends AbstractShrine {

    ShrineShard(ShulkerBox shulker) {
        super(shulker);
    }

    ShrineShard(ConfigurationSection cs) {
        super(cs);
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
        if (invState.getInventory().contains(Material.ENDER_CHEST)) gui.setItem(1, ShrineGUI.ENDER_CHEST_ITEM);
        return gui;
    }
}
