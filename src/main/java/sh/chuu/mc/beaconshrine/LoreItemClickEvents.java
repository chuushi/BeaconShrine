package sh.chuu.mc.beaconshrine;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;

import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.*;

public class LoreItemClickEvents implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void beaconShrineItemUse(PlayerInteractEvent ev) {
        if (ev.useItemInHand() == Event.Result.DENY) return;
        Block b = ev.getClickedBlock();
        if (b != null && b.getType().isInteractable()) return;

        Action action = ev.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = ev.getItem();
        if (item == null) return;

        if (item.getType() == WARP_SCROLL_MATERIAL) {
            BeaconShireItemUtils.WarpScroll ws = getWarpScrollData(item);
            if (ws != null && useWarpScroll(ev.getPlayer(), ws)) {
                item.setAmount(item.getAmount() - 1);
                ev.setCancelled(true);
            }
        }
    }
}
