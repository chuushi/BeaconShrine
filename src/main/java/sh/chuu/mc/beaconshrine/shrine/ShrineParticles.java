package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;

public class ShrineParticles {
    public static void spin() {
        
    }

    public static void attuning(Location person, Location shrine, int step) {
        // TODO optimize this thing
        Vector v = shrine.toVector().subtract(person.toVector()).divide(new Vector(20, 20, 20)); // TODO
        int m = step%20;
        double x = person.getX() + v.getX() * m;
        double y = person.getY() + v.getY() * m;
        double z = person.getZ() + v.getZ() * m;
        shrine.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 4, 0.25, 0.25, 0.25, new Particle.DustOptions(Color.WHITE, 1));
    }

    public static void warpWarmUp(Location loc, Particle.DustOptions color, int step) {
        double x = loc.getX() + Math.cos(step * 0.4);
        double y = loc.getY() + 1.0;
        double z = loc.getZ() + Math.sin(step * 0.4);
        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 1);
        loc.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 2, color);
    }

    public static void warpBoom(Location loc, Color color) {
        Firework boom = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta bm = boom.getFireworkMeta();
        bm.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(color).withFade(Color.GRAY).withFlicker().build());
        bm.setPower(0);
        boom.setFireworkMeta(bm);
        boom.setSilent(true);
        boom.setMetadata("noDamage", new FixedMetadataValue(BeaconShrine.getInstance(), true));
        boom.detonate();
    }
}
