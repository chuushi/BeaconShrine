package sh.chuu.mc.beaconshrine.listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;

import java.util.List;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class GUIEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final ShrineManager manager = plugin.getShrineManager();

    @EventHandler (priority = EventPriority.LOWEST)
    public void guiClick(InventoryClickEvent ev) {
        Player p = (Player) ev.getWhoClicked();
        Inventory inv = ev.getClickedInventory();
        ShrineManager.GuiView gui = manager.getGuiView(p);

        if (gui == null || gui.type() == ShrineManager.GuiType.SHOP)
            return;

        if (ev.getClick().isShiftClick()) {
            ev.setCancelled(true);
            return;
        }

        ItemStack item = ev.getCurrentItem();
        boolean isTopInv = inv == ev.getView().getTopInventory();

        if (gui.type() == ShrineManager.GuiType.WARP_LIST) {
            if (ev.isRightClick() && isTopInv) {
                ev.setCancelled(true);
                int clickId = ShrineGUI.getWarpIdGui(item);
                if (clickId != -1) {
                    manager.clickedWarpGui(p, clickId, gui.shrine());
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

        if (gui.type() == ShrineManager.GuiType.HOME_CORE || gui.type() == ShrineManager.GuiType.HOME_SHARD) {
            manager.clickedGui(gui.shrine(), item, p);
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void guiDrag(InventoryDragEvent ev) {
        HumanEntity he = ev.getWhoClicked();
        Inventory inv = ev.getView().getTopInventory();
        ShrineManager.GuiView gui = manager.getGuiView(he);
        if (gui == null || gui.type() == ShrineManager.GuiType.SHOP)
            return;

        if (gui.type() == ShrineManager.GuiType.WARP_LIST) {
            ev.setCancelled(true);
            return;
        }

        int topSize = inv.getSize();
        for (int slot : ev.getRawSlots()) {
            if (slot < topSize) {
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void guiClose(InventoryCloseEvent ev) {
        Player p = (Player) ev.getPlayer();
        ShrineManager.GuiView gui = manager.closedShrineGui(p);
        InventoryView view = ev.getView();
        Inventory inv = view.getTopInventory();

        if (gui != null) {
            if (gui.type() == ShrineManager.GuiType.WARP_LIST) {
                // account for the item that may have been on the hand
                ItemStack cursor = view.getCursor();
                if (cursor != null && ShrineGUI.isWarpGui(cursor)) {
                    inv.addItem(cursor);
                    view.setCursor(null);
                }

                List<Integer> oldList = plugin.getCloudManager().getTunedShrineList(p);
                List<Integer> newList = ShrineGUI.getWarpOrderGui(inv, gui.shrine().id());

                if (oldList.equals(newList)) return;

                oldList.clear();
                oldList.addAll(newList);
            }
            return;
        }

        if (inv.getType() == InventoryType.SHULKER_BOX) {
            BeaconShireItemUtils.ShrineIdResult res = BeaconShireItemUtils.getShrineId(inv);
            if (res == null) return;
            if (res.item().getType() == SHRINE_CORE_ACTIVATOR_ITEM_TYPE) {
                ShrineCore core = manager.getShrine(res.id()); // Costly operation to put it outside of if/else statement
                core.setSymbolItemType(inv);
                core.updateShardList();
            } else if (res.item().getType() == SHRINE_SHARD_ACTIVATOR_ITEM_TYPE) {
                ShrineCore core = manager.getShrine(res.id());
                core.updateShardList();
            }
        }
    }
}
