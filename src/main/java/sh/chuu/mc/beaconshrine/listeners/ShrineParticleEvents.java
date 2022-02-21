package sh.chuu.mc.beaconshrine.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;

public class ShrineParticleEvents implements Listener {
    private ShrineManager manager = BeaconShrine.getInstance().getShrineManager();

    @EventHandler(priority = EventPriority.MONITOR)
    public void startParticles(ChunkLoadEvent ev) {
        int x = ev.getChunk().getX();
        int z = ev.getChunk().getZ();
        manager.getShrines().values().forEach(s -> {
            if (ev.getWorld() == s.world() && x == s.x() >> 4 && z == s.z() >> 4 && s.isValid()) {
                s.startParticles();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopParticles(ChunkUnloadEvent ev) {
        int x = ev.getChunk().getX();
        int z = ev.getChunk().getZ();
        manager.getShrines().values().forEach(s -> {
            if (ev.getWorld() == s.world() && x == s.x() >> 4 && z == s.z() >> 4) {
                s.endParticles();
            }
        });
    }
}
