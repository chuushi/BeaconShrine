package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

public class ShrineParticles {
    public static void spin() {
        
    }

    // Step: between 1 and 100
    public static void warpWarmUp(Location loc, Particle.DustOptions color, int step) {
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0.0, 0.0, 0.0, color);
    }
}
