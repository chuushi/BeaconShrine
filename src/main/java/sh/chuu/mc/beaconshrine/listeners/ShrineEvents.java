package sh.chuu.mc.beaconshrine.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.shrine.ShrineManager;
import sh.chuu.mc.beaconshrine.shrine.ShrineMultiblock;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import java.util.List;

import static sh.chuu.mc.beaconshrine.Vars.INGOT_ITEM_TYPE;

public class ShrineEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final ShrineManager manager = plugin.getShrineManager();
    private final BaseComponent shrineInitFailText = new TextComponent("Shrine is not set up properly; run /shrinehelp");

    @EventHandler
    public void shrineFirework(EntityDamageByEntityEvent ev) {
        Entity damager = ev.getDamager();
        if (damager instanceof Firework && damager.hasMetadata("noDamage"))
            ev.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL) // Keep this lower than LoreItemClickEvents's
    public void shrineClick(PlayerInteractEvent ev) { // FIXME Excessive Event Calls
        if (ev.getPlayer().isSneaking() || ev.useInteractedBlock() == Event.Result.DENY
                || ev.getHand() != EquipmentSlot.HAND || ev.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        // New shrine detection - take away ingot
        ItemStack item = ev.getItem();
        if (item != null) {
            if (ev.useItemInHand() == Event.Result.DENY) return;
            if (item.getType() == INGOT_ITEM_TYPE) {
                ShulkerBox shulker = getValidShulkerNear(ev.getClickedBlock(), 4);
                if (shulker != null) {
                    Inventory inv = shulker.getInventory();
                    if (ShrineGUI.getShrineId(inv) == -1) {
                        // new shulker box
                        int empty = inv.firstEmpty();
                        if (empty == -1) return;

                        ShrineMultiblock shrine;
                        int id = ShrineGUI.getShrineId(item);
                        if ((shrine = manager.updateShrine(id, shulker)) == null) {
                            shrine = manager.newShrine(shulker, null);
                        }
                        ev.setCancelled(true);
                        item.setAmount(item.getAmount() - 1);
                        shrine.putShrineItem();
                        return;
                    }
                }
            }
        }

        Block b = ev.getClickedBlock();
        if (b != null && BlockUtils.hasInteraction(b.getType())) return;

        ShulkerBox shulker = getValidShulkerNear(ev.getClickedBlock(), 1);
        if (shulker == null) return;

        int id = ShrineGUI.getShrineId(shulker.getInventory());
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
        if (d instanceof ShulkerBox sb) {
            int id = ShrineGUI.getShrineId(sb.getInventory());
            if (id != -1)
                manager.updateShrine(id, sb);
        }
    }

    @EventHandler
    public void guiClick(InventoryClickEvent ev) {
        Player p = (Player) ev.getWhoClicked();
        Inventory inv = ev.getClickedInventory();
        ShrineManager.GuiView gui = manager.getGuiView(p);
        if (gui == null || gui.type == ShrineManager.GuiType.SHOP)
            return;

        if (ev.getClick().isShiftClick()) {
            ev.setCancelled(true);
            return;
        }

        ItemStack item = ev.getCurrentItem();
        boolean isTopInv = inv == ev.getView().getTopInventory();

        if (gui.type == ShrineManager.GuiType.WARP_LIST) {
            if (ev.isRightClick() && isTopInv) {
                ev.setCancelled(true);
                int clickId = ShrineGUI.getWarpIdGui(item);
                if (clickId != -1) {
                    manager.clickedWarpGui(p, clickId, gui.shrine);
                }
                return;
            }

            ItemStack cursor = ev.getView().getCursor();
            if (cursor != null && cursor.getType() == Material.AIR) cursor = null;
            if (cursor != null) {
                boolean isWarpOnCursor = ShrineGUI.isWarpGui(cursor);
                if (isTopInv ^ isWarpOnCursor) {
                    ev.setCancelled(true);
                }
            }
            return;
        }

        if (!isTopInv) return;
        ev.setCancelled(true);
        if (item == null) return;

        if (gui.type == ShrineManager.GuiType.HOME) {
            manager.clickedGui(gui.shrine.id(), item, p);
        }
    }

    @EventHandler
    public void guiDrag(InventoryDragEvent ev) {
        HumanEntity he = ev.getWhoClicked();
        Inventory inv = ev.getView().getTopInventory();
        ShrineManager.GuiView gui = manager.getGuiView(he);
        if (gui == null || gui.type == ShrineManager.GuiType.SHOP)
            return;

        if (gui.type == ShrineManager.GuiType.WARP_LIST) {
            ev.setCancelled(true);
            return;
        }

        int topSize = inv.getSize();
        for (int slot : ev.getRawSlots()) {
            if (slot < topSize) {
                ev.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void guiClose(InventoryCloseEvent ev) {
        Player p = (Player) ev.getPlayer();
        ShrineManager.GuiView gui = manager.closeShrineGui(p);
        InventoryView view = ev.getView();
        Inventory inv = view.getTopInventory();

        if (gui != null) {
            if (gui.type == ShrineManager.GuiType.WARP_LIST) {
                // account for the item that may have been on the hand
                ItemStack cursor = view.getCursor();
                if (cursor != null && ShrineGUI.isWarpGui(cursor)) {
                    inv.addItem(cursor);
                    view.setCursor(null);
                }

                List<Integer> oldList = plugin.getCloudManager().getTunedShrineList(p);
                List<Integer> newList = ShrineGUI.getWarpOrderGui(inv, gui.shrine.id());

                if (oldList.equals(newList)) return;

                oldList.clear();
                oldList.addAll(newList);
            }
            return;
        }

        if (inv.getType() == InventoryType.SHULKER_BOX) {
            int id = ShrineGUI.getShrineId(inv);
            if (id == -1) return;

            manager.getShrine(id).setSymbolItemType(inv);
        }
    }

    private ShulkerBox getValidShulkerNear(Block center, int tier) {
        ShulkerBox ret = null;
        for (Block b : BlockUtils.getSurrounding(center, ShrineMultiblock.RADIUS)) {
            BlockState state = b.getState();
            if (state instanceof ShulkerBox sb
                    && sb.getCustomName() != null
                    && BlockUtils.getBeaconBelow(b, tier) != null
            ) {
                // prevent counting in when there's more than two shulker boxes on the beacon beam(s)
                if (ret != null) {
                    return null;
                }
                ret = sb;

            }
        }
        return ret;
    }
}