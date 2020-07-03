package sh.chuu.mc.beaconshrine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LoreItemClickEvents implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void inventoryItemClick(InventoryClickEvent ev) {
        ItemStack item = ev.getCurrentItem();
        if (item == null) return;
    }
}
