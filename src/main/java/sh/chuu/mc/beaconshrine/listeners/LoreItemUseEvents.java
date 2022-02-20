package sh.chuu.mc.beaconshrine.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;

import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.*;

public class LoreItemUseEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();

    @EventHandler(priority = EventPriority.HIGH)
    public void beaconShrineItemUse(PlayerInteractEvent ev) {
        Block b = ev.getClickedBlock();
        Action action = ev.getAction();
        ItemStack item = ev.getItem();
        if (item == null || b == null
                || ev.useItemInHand() == Event.Result.DENY
                || action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                || item.getType() != WARP_SCROLL_MATERIAL
                || b.getType().isInteractable()
        ) return;

        BeaconShireItemUtils.WarpScroll ws = getWarpScrollData(item);
        if (ws == null) return;

        Player p = ev.getPlayer();
        World w = p.getWorld();
        Location l = p.getLocation();
        // TODO Do we want to continue supporting the nether/the end?
        if (w.getEnvironment() != World.Environment.NETHER && w.getHighestBlockYAt(l, HeightMap.MOTION_BLOCKING) > l.getBlockY()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You require access to sky to warp"));
            return;
        }

        // TODO Separate this part into its own function
        ev.setCancelled(true);

        // Animation
        if (action == Action.RIGHT_CLICK_AIR) {
            if (ev.getHand() == EquipmentSlot.OFF_HAND)
                p.swingOffHand();
            else
                p.swingMainHand();
        }

        plugin.getShrineManager().getShrine(ws.id()).warpPlayer(p, null).thenAccept(warped -> {
            if (warped) item.setAmount(item.getAmount() - 1);
        });
    }
}
