package sh.chuu.mc.beaconshrine.shrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static sh.chuu.mc.beaconshrine.Vars.*;

public abstract class AbstractShrine {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final CloudManager cloudManager = plugin.getCloudManager();
    private final ShrineManager manager = plugin.getShrineManager();

    protected final int id;
    protected World w;
    protected int x;
    protected int y;
    protected int z;
    protected double midX;
    protected double midY;
    protected double midZ;
    protected String name;
    protected DyeColor color;
    protected ChatColor chatColor;
    protected Material symbolItemType;

    private ShrineParticleRunnable particles;

    AbstractShrine(int id, ShulkerBox shulker) {
        this.id = id;

        this.w = shulker.getWorld();
        this.x = shulker.getX();
        this.y = shulker.getY();
        this.z = shulker.getZ();
        this.name = shulker.getCustomName();
        this.color = shulker.getColor();
        this.chatColor = this.color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));

        this.midX = x + 0.5d;
        this.midY = y;
        this.midZ = z + 0.5d;
    }

    AbstractShrine(int id, ConfigurationSection cs) {
        this.id = id;

        List<Integer> loc = cs.getIntegerList("loc");
        //noinspection ConstantConditions
        this.w = Bukkit.getWorld(cs.getString("world"));
        this.x = loc.get(0);
        this.z = loc.get(1);
        this.y = loc.get(2);
        this.name = cs.getString("name");

        String colorStr = cs.getString("color");
        this.color = colorStr == null ? null : DyeColor.valueOf(colorStr);
        this.chatColor = this.color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));

        this.midX = x + 0.5d;
        this.midY = y;
        this.midZ = z + 0.5d;
    }

    public abstract boolean isValid();
    public abstract Inventory getGui(Player p);
    public int id() { return id; }
    public World world() { return w; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Location getShulkerLocation() { return new Location(w, x + 0.5, y + 0.5, z + 0.5); }
    public String name() { return name; }
    public Color color() { return color == null ? Color.WHITE : color.getColor(); }
    public Particle.DustOptions dustColor() { return new Particle.DustOptions(color(), 1); }

    public final void startParticles() {
        if (particles != null)
            particles.cancel();

        particles = new ShrineParticleRunnable();

        particles.runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
    }

    public final void endParticles() {
        if (particles != null) {
            particles.cancel();
            particles = null;
        }
    }

    public final boolean hasParticles() {
        return particles != null;
    }

    protected abstract void theParticles(int step);

    public Inventory getInventory() {
        BlockState state = w.getBlockAt(x, y, z).getState();
        return state instanceof ShulkerBox ? ((ShulkerBox) state).getInventory() : null;
    }

    public void setShulker(ShulkerBox s, boolean dyed) {
        this.w = s.getWorld();
        this.x = s.getX();
        this.z = s.getZ();
        this.y = s.getY();
        this.name = s.getCustomName();
        this.color = dyed ? s.getColor() : null;
        this.chatColor = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
    }

    public void setSymbolItemType(Inventory inv) {
        for (ItemStack item : inv) {
            if (item == null || item.getType() == INGOT_ITEM_TYPE && ShrineGUI.getShrineId(item) != -1)
                continue;
            this.symbolItemType = item.getType();
            return;
        }
        this.symbolItemType = null;
    }

    public int distanceSquaredXZ(int x, int z) {
        x -= this.x;
        z -= this.z;
        return x*x + z*z;
    }

    // FIXME make this detect and work with ShrineShard as well. This code is only for teleportation to "ShrineCore".
    public CompletableFuture<Boolean> warp(Player p, AbstractShrine from) {
        if (manager.warpContains(p)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_WARPING);
            return CompletableFuture.completedFuture(false);
        } else if (!isValid()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return CompletableFuture.completedFuture(false);
        } else {
            final double newY;
            if (w != p.getWorld()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, SAME_DIMENSION_REQUIRED);
                return CompletableFuture.completedFuture(false);
            }
            if (w.getEnvironment() == World.Environment.NETHER) {
                Block b = w.getBlockAt(x, y + 2, z);
                int air = 8;
                while (b.getY() < 124 && air != 0) {
                    if (b.isPassable()) air--;
                    else air = Math.max(air, 3);
                    b = b.getRelative(BlockFace.UP);
                }
                if (b.getY() == 124) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, NO_CLEARANCE);
                    return CompletableFuture.completedFuture(false);
                }
                newY = b.getY();
            } else {
                newY = w.getHighestBlockYAt(x, z) + 30;
            }

            Location loc = p.getLocation();
            Vector vector;
            Vector up = new Vector(0, 255 - loc.getY(), 0);
            if (from != null) {
                vector = ParticleUtils.getDiff(from.x, from.y, from.z, loc);
                ParticleUtils.shrineIgnitionSound(p);
                ParticleUtils.beam(from.getShulkerLocation(), up, dustColor());
                ParticleUtils.beam(loc, vector, dustColor());
            } else {
                ParticleUtils.beam(loc, up, dustColor());
                ParticleUtils.paperIgnitionSound(p);
            }

            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            manager.warpAdd(p);
            new WarpSequence(p, newY, ret).runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
            return ret;
        }
    }

    private class ShrineParticleRunnable extends BukkitRunnable {
        private int step = 0;

        @Override
        public final void run() {
            theParticles(step++);
            if (step == Integer.MAX_VALUE) step = 0;
        }
    }

    protected class WarpSequence extends BukkitRunnable {
        private int i = 100;
        private final Player p;
        private final double initX;
        private final double initY;
        private final double initZ;
        private final Color c = color();
        private final CompletableFuture<Boolean> result;

        private final double newX = x + 0.5d;
        private final double newY;
        private final double newZ = z + 0.5d;

        private WarpSequence(Player player, double newY, CompletableFuture<Boolean> result) {
            super();
            Location pLoc = player.getLocation();
            this.p = player;
            this.initX = pLoc.getX();
            this.initY = pLoc.getY();
            this.initZ = pLoc.getZ();
            this.newY = newY;
            this.result = result;
        }

        @Override
        public void run() {
            Location loc = p.getLocation();
            final Particle.DustOptions dustOpt = dustColor();

            if (i > 30) {
                if (loc.getX() != initX || loc.getY() != initY || loc.getZ() != initZ) {
                    result.complete(false);
                    this.cancel();
                }
            } else if (i == 30) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0, false, false, false));
            } else if (i == 10) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 63, false, false, false));
                //noinspection ConstantConditions
                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.5f);
            } else if (i == 2) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, false, false, false));
            } else if (i == 0) {
                Location to = p.getLocation();
                to.setX(newX);
                to.setY(newY);
                to.setZ(newZ);

                ParticleUtils.warpBoom(loc, c);
                cloudManager.setNextWarp(p, System.currentTimeMillis() + GLOBAL_WARP_COOLDOWN);
                p.teleport(to);
                p.removePotionEffect(PotionEffectType.LEVITATION);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, w.getEnvironment() == World.Environment.NETHER ? 100 : 200, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 0, false, false, false));
                result.complete(true);
            } else if (i == -1) {
                ParticleUtils.warpBoom(loc, c);
            } else if (i == -100) {
                this.cancel();
            }

            ParticleUtils.warpWarmUp(loc, dustOpt, i);
            i--;
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            manager.warpDone(p);
        }
    }
}
