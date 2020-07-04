package sh.chuu.mc.beaconshrine.shrine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import static sh.chuu.mc.beaconshrine.shrine.ShireGuiLores.INGOT;
import static sh.chuu.mc.beaconshrine.shrine.ShireGuiLores.getShrineId;
import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.useWarpScroll;

public class ShrineEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final ShrineManager manager = plugin.getShrineManager();
    private final BaseComponent shrineInitFailText = new TextComponent("Shrine is not set up properly; run /shrinehelp");

    @EventHandler(priority = EventPriority.NORMAL) // Keep this lower than LoreItemClickEvents's
    public void shrineClick(PlayerInteractEvent ev) {
        if (ev.getPlayer().isSneaking() || ev.useInteractedBlock() == Event.Result.DENY
                || ev.getHand() != EquipmentSlot.HAND || ev.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        // New shrine detection - take away ingot
        ItemStack item = ev.getItem();
        if (item != null) {
            if (ev.useItemInHand() == Event.Result.DENY) return;
            if (item.getType() == INGOT) {
                ShulkerBox shulker = getValidShulkerNear(ev.getClickedBlock(), 4);
                if (shulker != null) {
                    Inventory inv = shulker.getInventory();
                    if (getShrineId(inv) == -1) {
                        // new shulker box
                        int empty = inv.firstEmpty();
                        if (empty == -1) return;

                        ShrineMultiblock shrine;
                        int id = getShrineId(item);
                        if (id == -1) {
                            shrine = manager.newShrine(shulker, null);
                        } else {
                            shrine = manager.updateShrine(id, shulker);
                        }
                        item.setAmount(item.getAmount() - 1);
                        shrine.putShrineItem();
                        ev.setCancelled(true);
                        return;
                    }
                }
            }
        }

        Block b = ev.getClickedBlock();
        if (b != null && BlockUtils.hasInteraction(b.getType())) return;

        ShulkerBox shulker = getValidShulkerNear(ev.getClickedBlock(), 1);
        if (shulker == null) return;

        int id = getShrineId(shulker.getInventory());
        if (id != -1) {
            Player p = ev.getPlayer();
            if (!manager.openShrineGui(p, id))
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, shrineInitFailText);
            ev.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void shireShulkerPlace(BlockPlaceEvent ev) {
        BlockState d = ev.getBlock().getState();
        if (d instanceof ShulkerBox) {
            ShulkerBox sb = (ShulkerBox) d;
            int id = getShrineId(sb.getInventory());
            if (id != -1)
                manager.updateShrine(id, sb);
        }
    }

    @EventHandler
    public void guiClick(InventoryClickEvent ev) {
        HumanEntity he = ev.getWhoClicked();
        Inventory inv = ev.getClickedInventory();
        int id = manager.getGuiViewingId(he);
        if (id == -1 || inv != ev.getView().getTopInventory() || inv.getType() == InventoryType.MERCHANT)
            return;

        ev.setCancelled(true);
        ItemStack item = ev.getCurrentItem();
        if (item == null)
            return;

        if (item.getType() == BeaconShireItemUtils.WARP_SCROLL_MATERIAL) {
            if (ev.isRightClick()) {
                BeaconShireItemUtils.WarpScroll ws = BeaconShireItemUtils.getWarpScrollData(item);
                if (ws != null) {
                    useWarpScroll((Player) he, ws);
                }
            }
            return;
        }

        manager.clickedGui(id, item, (Player) he);
    }

    @EventHandler
    public void guiClose(InventoryCloseEvent ev) {
        manager.closeShrineGui(ev.getPlayer());
    }

    private ShulkerBox getValidShulkerNear(Block center, int tier) {
        ShulkerBox ret = null;
        for (Block b : BlockUtils.getSurroundingInBeaconBeam(center, ShrineMultiblock.RADIUS, tier)) {
            BlockState state = b.getState();
            if (state instanceof ShulkerBox && ((ShulkerBox) state).getCustomName() != null) {
                // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                if (ret != null)
                    return null;
                ret = (ShulkerBox) state;
            }
        }
        return ret;
    }
}
