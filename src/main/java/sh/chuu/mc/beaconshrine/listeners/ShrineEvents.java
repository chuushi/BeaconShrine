package sh.chuu.mc.beaconshrine.listeners;

import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import static sh.chuu.mc.beaconshrine.Vars.*;
import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.shrineActivatorId;

public class ShrineEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final ShrineManager manager = plugin.getShrineManager();

    @EventHandler
    public void shrineFirework(EntityDamageByEntityEvent ev) {
        Entity damager = ev.getDamager();
        if (damager instanceof Firework && damager.hasMetadata("noDamage"))
            ev.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL) // Keep this lower than LoreItemClickEvents's
    public void shrineClick(PlayerInteractEvent ev) { // FIXME Excessive Event Calls
        Player p = ev.getPlayer();
        if (p.isSneaking()
                || ev.useInteractedBlock() == Event.Result.DENY
                || ev.getHand() != EquipmentSlot.HAND
                || ev.getAction() != Action.RIGHT_CLICK_BLOCK
                || ev.getClickedBlock() == null
                || BlockUtils.hasInteraction(ev.getClickedBlock().getType())
        ) return;

        ItemStack item = ev.getItem();
        if (item != null) {
            if (item.getType() == Material.COMPASS && ev.getClickedBlock().getType() == Material.LODESTONE)
                return;

            // New shrine detection - take away ingot
            if (ev.useItemInHand() == Event.Result.DENY) return;
            if (item.getType() == SHRINE_CORE_ITEM_TYPE) { // TODO  || item.getType() == SHRINE_SHARD_ITEM_TYPE
                ClickedShulker cs = getValidShulkerNear(ev.getClickedBlock(), 4, false);
                ShulkerBox shulker = cs.shulker; // FIXME Review what changes!!!
                if (shulker != null) {
                    Inventory inv = shulker.getInventory();
                    int shrineId = BeaconShireItemUtils.getShrineId(inv, SHRINE_CORE_ITEM_TYPE);
                    if (shrineId == -1) {
                        // new shulker box
                        int empty = inv.firstEmpty();
                        if (empty == -1) return;

                        ShrineCore shrine;
                        int id = shrineActivatorId(item);
                        if ((shrine = manager.updateShrine(id, shulker)) == null) {
                            shrine = manager.newShrine(shulker, null);
                        }
                        ev.setCancelled(true);
                        item.setAmount(item.getAmount() - 1);
                        shrine.putShrineItem();
                        return;
                    } else {
                        openGUI(p, shrineId, cs);
                        ev.setCancelled(true);
                    }
                }
            }
        }
        // TODO Implement similar logic as above for Shrine Shards

        ClickedShulker cs = getValidShulkerNear(ev.getClickedBlock(), 1, false); // o(n)

        if (cs == null) return;

        int id = BeaconShireItemUtils.getShrineId(cs.shulker.getInventory(), cs.isCore ? SHRINE_CORE_ITEM_TYPE : SHRINE_SHARD_ITEM_TYPE);
        if (id != -1) {
            openGUI(p, id, cs);
            p.swingMainHand();

            ev.setCancelled(true);
        }
    }

    private void openGUI(Player p, int id, ClickedShulker sh) {
        // FIXME Implement differentiation between Shrine Core and Shrine Shard
        if (!manager.openShrineGui(p, id, sh.isCore ? null : sh.shulker.getLocation()))
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, shrineInitFailText);
    }

    @EventHandler(ignoreCancelled = true)
    public void shireShulkerPlace(BlockPlaceEvent ev) {
        BlockState d = ev.getBlock().getState();
        if (d instanceof ShulkerBox sb) {
            BeaconShireItemUtils.ShrineIdResult res = BeaconShireItemUtils.getShrineId(sb.getInventory());
            if (res != null) {
                if (res.item().getType() == SHRINE_CORE_ITEM_TYPE)
                    manager.updateShrine(res.id(), sb);
                else ;// FIXME Implement shulker place with Shrine Shard item inside?
            }
        }
    }

    private ClickedShulker getValidShulkerNear(Block center, int tier, boolean onlyShard) {
        ClickedShulker ret = null;

        // Radius of straight directions
        int r = ShrineCore.RADIUS + 1; // TODO Move this to vars or somewhere else
        boolean centerIsLodestone = center.getType() == Material.LODESTONE;

        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST}) {
            Block ptr = center;
            boolean isLodestoneBefore = centerIsLodestone;

            for (int i = 0; i < r; i++) {
                ptr = ptr.getRelative(face);
                if (ptr.getType() == Material.LODESTONE) {
                    isLodestoneBefore = true;
                    continue;
                }
                BlockState state = ptr.getState();
                if (state instanceof ShulkerBox sb && sb.getCustomName() != null) { // TODO Do we want to move in shrine ID item detection logic here?
                    // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                    if (ret != null) {
                        return null;
                    }
                    if (isLodestoneBefore) {
                        ret = new ClickedShulker(sb, false);
                    }
                    if (BlockUtils.getBeaconBelow(ptr, tier) != null) {
                        ret = new ClickedShulker(sb, true);
                    }
                }
                isLodestoneBefore = false;
            }
        }

        if (ret != null || onlyShard) return ret;

        for (Block b : BlockUtils.getSurroundingStage2(center, ShrineCore.RADIUS)) {
            BlockState state = b.getState();
            if (state instanceof ShulkerBox sb
                    && sb.getCustomName() != null
                    && BlockUtils.getBeaconBelow(b, tier) != null
            ) {
                // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                if (ret != null) {
                    return null;
                }
                ret = new ClickedShulker(sb, true);

            }
        }
        return ret;
    }

    private record ClickedShulker(ShulkerBox shulker, boolean isCore) {}
}
