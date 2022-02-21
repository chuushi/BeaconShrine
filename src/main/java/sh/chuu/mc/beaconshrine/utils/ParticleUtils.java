package sh.chuu.mc.beaconshrine.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;

public interface ParticleUtils {
    double ATTUNING_PARTICLE_RATE = Math.PI/10;

    static void shrineSpin(Location loc, Particle.DustOptions color, int radius, double radian) {
        double y = loc.getY() + 0.5;
        double dx = Math.cos(radian) * radius;
        double dz = Math.sin(radian) * radius;
        double x1 = loc.getX() + dx;
        double x2 = loc.getX() - dx;
        double z1 = loc.getZ() + dz;
        double z2 = loc.getZ() - dz;
        loc.getWorld().spawnParticle(Particle.REDSTONE, x1, y, z1, 3, color);
        loc.getWorld().spawnParticle(Particle.REDSTONE, x2, y, z2, 3, color);
    }

    static void beam(Location person, Vector v, Particle.DustOptions color) {
        final int interval = (int) v.length() * 4;
        Vector inc = v.clone().divide(new Vector(interval, interval, interval));
        Location loc = person.clone();
        for (int i = 0; i < interval; i++) {
            loc.add(inc);
            person.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0.1, 0.1, 0.1, color);
            person.getWorld().spawnParticle(Particle.CRIT, loc, 1);
        }
    }

    static void attuning(Location person, Vector v, Particle.DustOptions color, int step) {
        double m = (Math.sin(step * ATTUNING_PARTICLE_RATE) + 1)/2;
        double x = person.getX() + v.getX() * m;
        double y = person.getY() + v.getY() * m;
        double z = person.getZ() + v.getZ() * m;
        person.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 4, 0.1, 0.1, 0.1, color);
        if (step < 60)
            particleAroundPlayer(person, color, step);
    }

    static void attuneBoom(Location loc, Color color) {
        boom(loc, color, FireworkEffect.Type.BALL);
    }

    static void warpWarmUp(Location loc, Particle.DustOptions color, int step) {
        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 1);
        particleAroundPlayer(loc, color, step);
    }

    static void warpBoom(Location loc, Color color) {
        boom(loc, color, FireworkEffect.Type.BALL_LARGE);
    }

    static void boom(Location loc, Color color, FireworkEffect.Type type) {
        Firework boom = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta bm = boom.getFireworkMeta();
        bm.addEffect(FireworkEffect.builder().with(type).withColor(color).withFade(Color.GRAY).withFlicker().build());
        bm.setPower(0);
        boom.setFireworkMeta(bm);
        boom.setSilent(true);
        boom.setMetadata("noDamage", new FixedMetadataValue(BeaconShrine.getInstance(), true));
        boom.detonate();
    }

    static void shrineIgnitionSound(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1, 1);
    }

    static void paperIgnitionSound(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1, 1);
    }

    static Vector getDiff(int x, int y, int z, Location pl) {
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
