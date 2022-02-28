package sh.chuu.mc.beaconshrine.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Beacon;
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

    @EventHandler(priority = EventPriority.NORMAL) // Keep this lower than LoreItemClickEvents's // TODO what is this comment
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

            if (ev.useItemInHand() == Event.Result.DENY) return;

            // New shrine detection - take away ingot
            boolean isCoreItem = item.getType() == SHRINE_CORE_ACTIVATOR_ITEM_TYPE;
            if (isCoreItem || item.getType() == SHRINE_SHARD_ACTIVATOR_ITEM_TYPE) {
                ClickedShulker cs = getValidShulkerNear(ev.getClickedBlock(), 4, false);
                if (cs != null) {
                    Inventory inv = cs.shulker.getInventory();
                    BeaconShireItemUtils.ShrineIdResult res = BeaconShireItemUtils.getShrineId(inv);

                    if (res == null) {
                        // new shulker box
                        int empty = inv.firstEmpty();
                        if (empty == -1) return;

                        int itemID = shrineActivatorId(item);
                        if (itemID != -1) {
                            if (isCoreItem) {
                                ShrineCore shrineCore;
                                if ((shrineCore = manager.updateShrine(itemID, cs.shulker, cs.beacon)) == null) {
                                    shrineCore = manager.newShrine(cs.shulker, cs.beacon);
                                }
                                ev.setCancelled(true);
                                inv.setItem(empty, shrineCore.activatorItem());
                                item.setAmount(item.getAmount() - 1);
                                return;
                            } else {
                                ShrineCore shrineCore = manager.getShrine(itemID);
                                // TODO make distance configurable
                                if (shrineCore.distanceSquaredXZ(cs.shulker.getX(), cs.shulker.getZ()) > 562500) {// 750^2
                                    // TODO move to Vars
                                    ev.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("The shrine is too far away"));
                                    return;
                                }
                                ev.setCancelled(true);
                                inv.setItem(empty, shrineCore.shardActivatorItem());
                                item.setAmount(item.getAmount() - 1);
                                shrineCore.updateShardList();
                                return;
                            }
                        }
                    } else {
                        openGUI(p, res.id(), cs);
                        ev.setCancelled(true);
                        return;
                    }
                }
            }
        }

        ClickedShulker cs = getValidShulkerNear(ev.getClickedBlock(), 1, false); // o(n)

        if (cs == null) return;

        int id = BeaconShireItemUtils.getShrineId(cs.shulker.getInventory(), cs.beacon == null ? SHRINE_SHARD_ACTIVATOR_ITEM_TYPE : SHRINE_CORE_ACTIVATOR_ITEM_TYPE);
        if (id != -1) {
            openGUI(p, id, cs);
            ev.setCancelled(true);
        }
    }

    private void openGUI(Player p, int id, ClickedShulker sh) {
        p.swingMainHand();
        if (!manager.openShrineGui(p, id, sh.shulker, sh.beacon != null))
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, shrineInitFailText);
    }

    @EventHandler(ignoreCancelled = true)
    public void shireShulkerPlace(BlockPlaceEvent ev) {
        BlockState d = ev.getBlock().getState();
        if (d instanceof ShulkerBox sb) {
            BeaconShireItemUtils.ShrineIdResult res = BeaconShireItemUtils.getShrineId(sb.getInventory());
            if (res != null) {
                if (res.item().getType() == SHRINE_CORE_ACTIVATOR_ITEM_TYPE)
                    manager.updateShrine(res.id(), sb, null);
                else if (res.item().getType() == SHRINE_SHARD_ACTIVATOR_ITEM_TYPE)
                    manager.getShrine(res.id()).updateShardList();
            }
        }
    }

    /**
     * Gets the valid Shulker box, whether it contains the necessary Shrine Activator item or not
     * @param center Center block to search from
     * @param tier Beacon tier to confirm for Core types, if any
     * @param onlyShard If it should only search for shards
     * @return Result, or null if no valid shulker box exists
     */
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
                if (state instanceof ShulkerBox sb && sb.getCustomName() != null) {
                    // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                    if (ret != null) {
                        return null;
                    }
                    if (isLodestoneBefore) {
                        ret = new ClickedShulker(sb, null);
                    }
                    Beacon beacon = BlockUtils.getBeaconBelow(ptr, tier);
                    if (!onlyShard && beacon != null) {
                        ret = new ClickedShulker(sb, beacon);
                    }
                }
                isLodestoneBefore = false;
            }
        }

        if (ret != null || onlyShard) return ret;

        for (Block b : BlockUtils.getSurroundingStage2(center, ShrineCore.RADIUS)) {
            BlockState state = b.getState();
            Beacon beacon = BlockUtils.getBeaconBelow(b, tier);
            if (state instanceof ShulkerBox sb
                    && sb.getCustomName() != null
                    && beacon != null
            ) {
                // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                if (ret != null) {
                    return null;
                }
                ret = new ClickedShulker(sb, beacon);

            }
        }
        return ret;
    }

    private record ClickedShulker(ShulkerBox shulker, Beacon beacon) {}
}
