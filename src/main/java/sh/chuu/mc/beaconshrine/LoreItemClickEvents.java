package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sh.chuu.mc.beaconshrine.shrine.ShrineMultiblock;

import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.*;

public class LoreItemClickEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final BaseComponent[] INVALID_SHRINE = new BaseComponent[]{new TextComponent("Unable to teleport to the broken shrine")};

    @EventHandler(priority = EventPriority.HIGH)
    public void beaconShrineItemUse(PlayerInteractEvent ev) {
        if (ev.useItemInHand() == Event.Result.DENY) return;
        Block b = ev.getClickedBlock();
        if (b != null && b.getType().isInteractable()) return;

        Action action = ev.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = ev.getItem();
        if (item == null) return;
        if (item.getType() == WARP_SCROLL_MATERIAL && useWarpScroll(ev.getPlayer(), item)) {
            ev.setCancelled(true);
        }
    }

    private boolean useWarpScroll(Player p, ItemStack item) {
        WarpScroll data = getWarpScrollData(item);
        if (data == null) return false;

        ShrineMultiblock shrine = plugin.getShrineManager().getShrine(data.id);
        if (shrine.isValid()) {
            World w = shrine.getWorld();
            int x = shrine.getX();
            int z = shrine.getZ();
            // TODO make this a timed event
            Location l = p.getLocation();
            l.setX(x + 0.5d);
            l.setY(w.getHighestBlockYAt(x, z) + 30);
            l.setZ(z + 0.5d);
            p.teleport(l);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false, false));
            item.setAmount(item.getAmount() - 1);
            return true;
        } else {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return false;
        }
    }
}
