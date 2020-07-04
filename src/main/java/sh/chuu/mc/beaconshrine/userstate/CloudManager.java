package sh.chuu.mc.beaconshrine.userstate;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.ExperienceUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.userstate.CloudInventoryLores.*;

public class CloudManager implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final HashMap<Player, UserCloud> invs = new HashMap<>();
    private final Set<Player> viewing = new HashSet<>();

    public CloudManager() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(p);
        }
    }

    public void onDisable() {
        for (Player p : viewing) {
            p.closeInventory();
        }
        invs.values().forEach(UserCloud::saveInventory);
    }

    public boolean savePlayerState(Player p) {
        UserCloud is = invs.get(p);
        if (is == null) return false;

        int slot = 44;
        Inventory inv = BeaconShireItemUtils.getInventory(p, INVENTORY_NAME);
        ItemStack locRestore = createTeleportationScroll(p.getLocation());
        inv.setItem(slot--, locRestore);
        ItemStack expRestore = createExpItem(p);
        if (expRestore != null)
            inv.setItem(slot--, expRestore);
        // update below to slot-- when we need to add more items
        inv.setItem(slot, BeaconShireItemUtils.createEnderChestItem(p));
        is.setInventory(inv);
        p.getInventory().clear();
        p.getEnderChest().clear();
        p.setLevel(0);
        p.setExp(0);
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(100);
        return true;
    }

    public ItemStack[] getInventoryContents(Player p) {
        UserCloud is = invs.get(p);
        if (is == null)
            return null;
        return is.getInventory().getContents();
    }

    public boolean openInventory(Player p) {
        UserCloud is = invs.get(p);
        if (is == null)
            return false;
        viewing.add(p);
        p.openInventory(is.getInventory());
        return true;
    }

    private boolean applyExp(Player p, int exp) {
        UserCloud is = invs.get(p);
        if (is == null || exp == 0)
            return false;

        int currentExp = ExperienceUtils.getExp(p);
        ExperienceUtils.changeExp(p, currentExp + exp);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 1);
        return true;
    }

    private void loadPlayer(Player p) {
        try {
            invs.put(p, new UserCloud(p.getUniqueId()));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not load inventories data for " + p.getName(), ex);
        }
    }

    @EventHandler
    public void loadOnJoin(PlayerJoinEvent ev) {
        loadPlayer(ev.getPlayer());
    }

    @EventHandler
    public void saveOnLeave(PlayerQuitEvent ev) {
        UserCloud is = invs.remove(ev.getPlayer());
        if (is != null)
            is.saveInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent ev) {
        HumanEntity he = ev.getPlayer();
        if (he instanceof Player)
            viewing.remove(he);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCloudSpecialItemClick(InventoryClickEvent ev) {
        HumanEntity p = ev.getWhoClicked();
        Inventory inv = ev.getClickedInventory();
        if (inv != ev.getView().getTopInventory() || !viewing.contains(p))
            return;

        ItemStack item = ev.getCurrentItem();
        if (item == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return;

        if (item.getType() == EXP_ITEM_TYPE) {
            int exp = getExpItemValue(item);
            if (exp == -1) return;
            if (ev.isRightClick() && applyExp((Player) p, exp)) {
                inv.setItem(ev.getSlot(), null);
            }
            ev.setCancelled(true);
        } else if (item.getType() == TELEPORT_ITEM_TYPE) {
            Location loc = getTeleportLocation(item);
            if (loc == null) return;
            if (ev.isRightClick() && p.teleport(loc)) {
                inv.setItem(ev.getSlot(), null);
            }
            ev.setCancelled(true);
        }
    }
}
