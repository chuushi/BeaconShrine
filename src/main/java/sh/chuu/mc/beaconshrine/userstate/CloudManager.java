package sh.chuu.mc.beaconshrine.userstate;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.utils.ExperienceUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.utils.CloudLoreUtils.INVENTORY_NAME;

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
        invs.values().forEach(UserCloud::saveInventory);
        for (Player p : viewing) {
            p.closeInventory();
        }
    }

    public boolean setInventory(Player p, Inventory inv) {
        UserCloud is = invs.get(p);
        if (is == null)
            return false;
        is.setInventory(inv);
        return true;
    }

    public boolean openInventory(Player p) {
        UserCloud is = invs.get(p);
        if (is == null)
            return false;
        viewing.add(p);
        p.openInventory(is.getInventory());
        return true;
    }

    public void saveExp(Player p) {
        UserCloud is = invs.get(p);
        if (is != null)
            is.setExp(ExperienceUtils.getExp(p));
    }

    private boolean applyExp(Player p) {
        UserCloud is = invs.get(p);
        if (is == null || is.getExp() == 0)
            return false;

        int currentExp = ExperienceUtils.getExp(p);
        ExperienceUtils.changeExp(p, currentExp + is.getExp());
        is.setExp(0);
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

    @EventHandler
    public void onCloudSpecialItemClick(InventoryClickEvent ev) {
        HumanEntity he = ev.getWhoClicked();
        Inventory inv = ev.getClickedInventory();
        if (!(he instanceof Player) || inv != ev.getView().getTopInventory())
            return;

        Player p = (Player) he;
        if (!viewing.contains(p))
            return;

        ItemStack item = ev.getCurrentItem();
        if (item == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return;

        // 1. If EXP bottles, apply EXPs
        if (item.getType() == Material.EXPERIENCE_BOTTLE) {
            if (ev.isRightClick() && applyExp(p))
                inv.setItem(ev.getSlot(), null);
            ev.setCancelled(true);
        }
        // 2. If paper (ticket), teleport to the location mentioned in lore
        // TODO Teleportation stuffs??
    }
}
