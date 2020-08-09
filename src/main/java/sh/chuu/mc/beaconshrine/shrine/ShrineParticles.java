package sh.chuu.mc.beaconshrine.shrine;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;

public class ShrineParticles {
    private static final double ATTUNING_PARTICLE_RATE = Math.PI/10;

    public static void spin() {
        
    }

    public static void beam(Location person, Vector v, Particle.DustOptions color) {
        int interval = 20;
        Vector inc = v.clone().divide(new Vector(interval, interval, interval));
        Location loc = person.clone();
        for (int i = 0; i < interval; i++) {
            loc.add(inc);
            person.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0.1, 0.1, 0.1, color);
        }
    }

    public static void attuning(Location person, Vector v, Particle.DustOptions color, int step) {
        double m = (Math.sin(step * ATTUNING_PARTICLE_RATE) + 1)/2;
        double x = person.getX() + v.getX() * m;
        double y = person.getY() + v.getY() * m;
        double z = person.getZ() + v.getZ() * m;
        person.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 4, 0.1, 0.1, 0.1, color);
        if (step < 60)
            particleAroundPlayer(person, color, step);
    }

    public static void attuneBoom(Location loc, Color color) {
        boom(loc, color, FireworkEffect.Type.BALL);
    }

    public static void warpWarmUp(Location loc, Particle.DustOptions color, int step) {
        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 1);
        particleAroundPlayer(loc, color, step);
    }

    public static void warpBoom(Location loc, Color color) {
        boom(loc, color, FireworkEffect.Type.BALL_LARGE);
    }

    public static void boom(Location loc, Color color, FireworkEffect.Type type) {
        Firework boom = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta bm = boom.getFireworkMeta();
        bm.addEffect(FireworkEffect.builder().with(type).withColor(color).withFade(Color.GRAY).withFlicker().build());
        bm.setPower(0);
        boom.setFireworkMeta(bm);
        boom.setSilent(true);
        boom.setMetadata("noDamage", new FixedMetadataValue(BeaconShrine.getInstance(), true));
        boom.detonate();
    }

    public static void ignitionSound(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1, 1);
    }

    public static Vector getDiff(int x, int y, int z, Location pl) {
        return new Vector(x + 0.5, y + 0.5, z + 0.5)
                .subtract(pl.toVector());
    }

    private static void particleAroundPlayer(Location loc, Particle.DustOptions color, int step) {
        double x = loc.getX() + Math.cos(step * 0.4);
        double y = loc.getY() + 1.0;
        double z = loc.getZ() + Math.sin(step * 0.4);
        loc.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 2, color);
    }
}
