package sh.chuu.mc.beaconshrine.listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.shrine.ShrineCore;
import sh.chuu.mc.beaconshrine.shrine.ShrineGUI;
import sh.chuu.mc.beaconshrine.shrine.ShrineShard;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;

import java.util.List;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class GUIEvents implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final ShrineManager manager = plugin.getShrineManager();
    private final CloudManager cloudManager = plugin.getCloudManager();

    @EventHandler (priority = EventPriority.LOWEST)
    public void guiClick(InventoryClickEvent ev) {
        if (ev instanceof InventoryCreativeEvent) return;

        Player p = (Player) ev.getWhoClicked();
        ShrineManager.GuiView gui = manager.getGuiView(p);
        if (gui == null) return;

        Inventory inv = ev.getClickedInventory();
        ShrineManager.GuiType viewType = gui.type();

        //noinspection ConstantConditions "getGuiView() do NOT guarantee non-null
        if (gui == null || viewType == ShrineManager.GuiType.SHOP)
            return;

        if (ev.getClick().isShiftClick()) {
            ev.setCancelled(true);
            return;
        }

        ItemStack item = ev.getCurrentItem();
        boolean isTopInv = inv == ev.getView().getTopInventory();

        boolean coreWarpList = viewType == ShrineManager.GuiType.CORE_WARP_LIST;
        if (coreWarpList || viewType == ShrineManager.GuiType.SHARD_WARP_LIST) {
            if (ev.isLeftClick() && isTopInv) {
                ev.setCancelled(true);
                int clickId;
                if (coreWarpList) {
                    clickId = ShrineGUI.getWarpIdGui(item, true);
                    if (clickId != -1) {
                        manager.clickedWarpGui(p, manager.getShrine(clickId), gui.shrine());
                    }
                } else {
                    clickId = ShrineGUI.getWarpIdGui(item, false);
                    int id = gui.shrine().id();
                    ShrineCore shrine = manager.getShrine(id);
                    List<ShrineShard> shards = shrine.getShards();
                    if (clickId == -1) {
                        int coreCheck = ShrineGUI.getWarpIdGui(item, true);
                        if (coreCheck != -1) {
                            manager.clickedWarpGui(p, shrine, gui.shrine());
                        }
                    } else {
                        manager.clickedWarpGui(p, shards.get(clickId), gui.shrine());
                    }
                }
                return;
            }

            ItemStack cursor = ev.getView().getCursor();
            if (cursor != null && cursor.getType() == Material.AIR) cursor = null;
            if (coreWarpList) {
                if (cursor != null) {
                    boolean isWarpOnCursor = ShrineGUI.isWarpGui(cursor);
                    if (isTopInv ^ isWarpOnCursor) {
                        ev.setCancelled(true);
                    }
                }
            } else {
                if (isTopInv)
                    ev.setCancelled(true);
            }
            return;
        }

        if (!isTopInv) return;
        ev.setCancelled(true);
        if (item == null) return;

        if (viewType == ShrineManager.GuiType.HOME_CORE || viewType == ShrineManager.GuiType.HOME_SHARD) {
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

        if (gui.type() == ShrineManager.GuiType.CORE_WARP_LIST || gui.type() == ShrineManager.GuiType.SHARD_WARP_LIST) {
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
            boolean coreWarpList = gui.type() == ShrineManager.GuiType.CORE_WARP_LIST;
            if (coreWarpList || gui.type() == ShrineManager.GuiType.SHARD_WARP_LIST) {
                // account for the item that may have been on the hand
                ItemStack cursor = view.getCursor();
                if (cursor != null && ShrineGUI.isWarpGui(cursor)) {
                    inv.addItem(cursor);
                    view.setCursor(null);
                }

                if (coreWarpList) {
                    List<Integer> oldList = plugin.getCloudManager().getTunedShrineList(p);
                    List<Integer> newList = ShrineGUI.getWarpOrderGui(inv, gui.shrine().id());

                    if (oldList.equals(newList)) return;

                    oldList.clear();
                    oldList.addAll(newList);
                }
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
