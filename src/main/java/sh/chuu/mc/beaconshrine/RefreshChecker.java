package sh.chuu.mc.beaconshrine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static sh.chuu.mc.beaconshrine.BeaconShrine.REFRESH_TIME_NODE;

public class RefreshChecker implements Listener {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private long time = plugin.getConfig().getLong(REFRESH_TIME_NODE, 0);

    void setTime() {
        time = System.currentTimeMillis();
        plugin.getConfig().set(REFRESH_TIME_NODE, time);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinDuringRefresh(PlayerJoinEvent ev) {
        Player p = ev.getPlayer();
        if (p.getLastPlayed() > time)
            return;
        plugin.getCloudManager().savePlayerState(p);
        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()); // Imagine the spawn location is in the nether >w>
    }
}
