package sh.chuu.mc.beaconshrine.shrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineManager;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static sh.chuu.mc.beaconshrine.Vars.*;
import static sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils.shrineActivatorId;

public abstract class AbstractShrine {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    protected final CloudManager cloudManager = plugin.getCloudManager();
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

    AbstractShrine(int id, ConfigurationSection cs, boolean xzy) {
        this.id = id;

        List<Integer> loc = cs.getIntegerList("loc");
        //noinspection ConstantConditions
        this.w = Bukkit.getWorld(cs.getString("world"));
        this.x = loc.get(0);
        this.y = loc.get(xzy ? 2 : 1);
        this.z = loc.get(xzy ? 1 : 2);
        this.name = cs.getString("name");

        String colorStr = cs.getString("color");
        this.color = colorStr == null ? null : DyeColor.valueOf(colorStr);
        this.chatColor = this.color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));

        this.midX = x + 0.5d;
        this.midY = y;
        this.midZ = z + 0.5d;
    }

    AbstractShrine(int id, Map<?, ?> cs) {
        this.id = id;

        @SuppressWarnings("unchecked")
        List<Integer> loc = (List<Integer>) cs.get("loc");
        this.w = Bukkit.getWorld((String) cs.get("world"));
        this.x = loc.get(0);
        this.y = loc.get(1);
        this.z = loc.get(2);
        this.name = (String) cs.get("name");

        String colorStr = (String) cs.get("color");
        this.color = colorStr == null ? null : DyeColor.valueOf(colorStr);
        this.chatColor = this.color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));

        this.midX = x + 0.5d;
        this.midY = y;
        this.midZ = z + 0.5d;
    }

    public abstract boolean isValid();
    public abstract Inventory getGui(Player p);
    public abstract ItemStack activatorItem();

    public int id() { return id; }
    public World world() { return w; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Location getShulkerLocation(boolean centerOnBlock) {
        double d = centerOnBlock ? 0.5 : 0.0;
        return new Location(w, x + d, y + d, z + d);
    }
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

    /**
     * Particles to show during idle
     * @param step
     */
    protected abstract void theParticles(int step);

    /**
     * Warp Teleportation Sequence
     * Warps teleport executes at 0 By this point, item has been consumed.
     * @param p Player that is teleporting
     * @return true if completed
     */
    protected abstract boolean warpSequence(int step, WarpSequence ws, Player p);


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
            if (item == null
                    || shrineActivatorId(item) != -1
                    || item.getType() == Material.COMPASS // TODO filter only Lodestone Compass
            ) continue;
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
                ParticleUtils.beam(from.getShulkerLocation(true), up, dustColor());
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

    /**
     * Applies save data *serialization* onto given ConfigurationSection object
     * @param cs ConfigurationSection to apply savedata to
     */
    public void save(ConfigurationSection cs) {
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, y, z});
        cs.set("symIT", symbolItemType == null ? null : symbolItemType.name());
    }

    public HashMap<String, Object> save() {
        HashMap<String, Object> ret = new HashMap<>(6);
        ret.put("name", name);
        ret.put("world", w.getName());
        ret.put("loc", new int[]{x, y, z});
        if (color != null) ret.put("color", color.toString());
        if (symbolItemType != null) ret.put("symIT", symbolItemType.name());
        return ret;
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
        protected int i = 100;
        private final Player p;
        protected final double initX;
        protected final double initY;
        protected final double initZ;
        protected final Color color = color();
        private final CompletableFuture<Boolean> result;

        protected final double newX = x + 0.5d;
        protected final double newY;
        protected final double newZ = z + 0.5d;

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

            if (i == 0) result.complete(true);
            if (warpSequence(i, this, p)) {
                if (i > 0) result.complete(false);
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
