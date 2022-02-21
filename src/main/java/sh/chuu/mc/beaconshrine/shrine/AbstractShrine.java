package sh.chuu.mc.beaconshrine.shrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.ShrineParticles;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static sh.chuu.mc.beaconshrine.Vars.*;

public abstract class AbstractShrine {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private final CloudManager cloudManager = plugin.getCloudManager();
    private final ShrineManager manager = plugin.getShrineManager();

    protected World w;
    protected int x;
    protected int y;
    protected int z;
    protected String name;
    protected DyeColor color;
    protected ChatColor chatColor;

    AbstractShrine(ShulkerBox shulker) {
        this.w = shulker.getWorld();
        this.x = shulker.getX();
        this.y = shulker.getY();
        this.z = shulker.getZ();
        this.name = shulker.getCustomName();
        this.color = shulker.getColor();
        this.chatColor = this.color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
    }

    AbstractShrine(ConfigurationSection cs) {
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
    }

    public abstract boolean isValid();
    public abstract Inventory getGui(Player p);
    public World world() { return w; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Location getShulkerLocation() { return new Location(w, x + 0.5, y + 0.5, z + 0.5); }
    public String name() { return name; }
    public Color color() { return color == null ? Color.WHITE : color.getColor(); }

    public Particle.DustOptions dustColor() { return new Particle.DustOptions(color(), 1); }

    public Inventory getInventory() {
        BlockState state = w.getBlockAt(x, y, z).getState();
        return state instanceof ShulkerBox ? ((ShulkerBox) state).getInventory() : null;
    }

    public int distanceSquaredXZ(int x, int z) {
        x -= this.x;
        z -= this.z;
        return x*x + z*z;
    }

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
                vector = ShrineParticles.getDiff(from.x, from.y, from.z, loc);
                ShrineParticles.shrineIgnitionSound(p);
                ShrineParticles.beam(from.getShulkerLocation(), up, dustColor());
                ShrineParticles.beam(loc, vector, dustColor());
            } else {
                ShrineParticles.beam(loc, up, dustColor());
                ShrineParticles.paperIgnitionSound(p);
            }

            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            manager.warpAdd(p);
            new WarpSequence(p, newY, ret).runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
            return ret;
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

                ShrineParticles.warpBoom(loc, c);
                cloudManager.setNextWarp(p, System.currentTimeMillis() + GLOBAL_WARP_COOLDOWN);
                p.teleport(to);
                p.removePotionEffect(PotionEffectType.LEVITATION);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, w.getEnvironment() == World.Environment.NETHER ? 100 : 200, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 0, false, false, false));
                result.complete(true);
            } else if (i == -1) {
                ShrineParticles.warpBoom(loc, c);
            } else if (i == -100) {
                this.cancel();
            }

            ShrineParticles.warpWarmUp(loc, dustOpt, i);
            i--;
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            manager.warpDone(p);
        }
    }
}
