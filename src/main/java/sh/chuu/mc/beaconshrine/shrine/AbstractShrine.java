package sh.chuu.mc.beaconshrine.shrine;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
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
    public abstract Inventory getGui(Player p); // TODO Change unclear naming - this does NOT get a GUI. It opens a GUI to a player.
    public abstract ItemStack activatorItem();
    public abstract ItemStack createWarpScrollGuiItem(boolean urHere, Player p);
    public abstract List<ShrineShard> getShards();

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
     * @param step tick/timing
     */
    protected abstract void theParticles(int step);

    /**
     * Initial Warp Teleportation Sequence
     * @param p Player warping
     * @param from The origin shrine of teleportation
     * @return Init record of just xyz coords
     */
    protected abstract WarpSequenceInit preWarpSequence(Player p, AbstractShrine from);

    /**
     * Warp Teleportation Sequence
     * Warps teleport executes at 0 By this point, item has been consumed.
     * @param p Player that is teleporting
     * @return true if completed
     */
    protected abstract boolean warpSequence(int step, WarpSequence ws, Player p);

    protected boolean warpSequenceFromShrine(int step, WarpSequence ws, Player p) {
        Location loc = p.getLocation();

        ParticleUtils.beam(loc,ParticleUtils.getDiff(x, y, z, loc), dustColor());

        if (step > 20) {
            return loc.getX() != ws.initX || loc.getY() != ws.initY || loc.getZ() != ws.initZ;
        } else if (step == 20) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0, false, false, false));
            return false;
        } else if (step == 0) {
            Location to = loc.clone();
            to.setX(ws.newX);
            to.setY(ws.newY);
            to.setZ(ws.newZ);

            ParticleUtils.warpBoom(loc, ws.color);
            p.teleport(to);
            p.removePotionEffect(PotionEffectType.LEVITATION);
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
            return false;
        } else if (step == -1) {
            ParticleUtils.warpBoom(loc, ws.color);
            return true;
        } else return step == -20;
    }

    public CompletableFuture<Boolean> doAttuneAnimation(Player p) {
        ParticleUtils.shrineIgnitionSound(p);
        final Location initLoc = p.getLocation();
        final double initX = initLoc.getX();
        final double initY = initLoc.getY();
        final double initZ = initLoc.getZ();
        CompletableFuture<Boolean> complete = new CompletableFuture<>();

        new BukkitRunnable() {
            final Vector vector = ParticleUtils.getDiff(x, y, z, p.getLocation());
            private int step = 100;

            @Override
            public void run() {
                Location newLoc = p.getLocation();
                if (initX != newLoc.getX() || initY != newLoc.getY() || initZ != newLoc.getZ() || !isValid()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuning cancelled due to movement or invalid shrine").create());
                    complete.complete(false);
                    this.cancel();
                } else if (step == 0) {
                    complete.complete(true);
                    this.cancel();
                    attuneComplete(p, initLoc);
                } else if (step%20 == 0) {
                    int secs = step / 20;
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("Attuning with " + name + ", please wait " + secs + (secs == 1 ? " second" : " seconds")).create()
                    );
                }
                ParticleUtils.attuning(initLoc, vector, dustColor(), step--);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return complete;
    }

    private void attuneComplete(Player p, Location initLoc) {
        plugin.getCloudManager().attuneShrine(p, this);
        ParticleUtils.attuneBoom(initLoc, color());
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new ComponentBuilder("Attuned with " + name).create()
        );
        startParticles();
    }

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

    public CompletableFuture<Boolean> warp(Player p, AbstractShrine from) {
        if (manager.warpContains(p)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_WARPING);
            return CompletableFuture.completedFuture(false);
        }
        if (!isValid()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return CompletableFuture.completedFuture(false);
        }
        if (w != p.getWorld()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, SAME_DIMENSION_REQUIRED);
            return CompletableFuture.completedFuture(false);
        }
        if (!w.getWorldBorder().isInside(getShulkerLocation(false))) {
            // TODO Move to Vars
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Shrine is outside of world border"));
            return CompletableFuture.completedFuture(false);
        }

        WarpSequenceInit warpSequenceInit = preWarpSequence(p, from);
        if (warpSequenceInit == null)
            return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        manager.warpAdd(p);
        new WarpSequence(p, warpSequenceInit, ret).runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
        return ret;
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

    protected record WarpSequenceInit(int step, double newX, double newY, double newZ, boolean isShard) {}

    protected class WarpSequence extends BukkitRunnable {
        protected int i;
        private final Player p;
        protected final double initX;
        protected final double initY;
        protected final double initZ;
        protected final Color color = color();
        private final CompletableFuture<Boolean> result;

        protected final boolean isShard;
        protected final double newX;
        protected final double newY;
        protected final double newZ;

        private WarpSequence(Player player, WarpSequenceInit init, CompletableFuture<Boolean> result) {
            super();
            this.i = init.step;
            this.newX = init.newX;
            this.newY = init.newY;
            this.newZ = init.newZ;
            this.isShard = init.isShard;
            Location pLoc = player.getLocation();
            this.p = player;
            this.initX = pLoc.getX();
            this.initY = pLoc.getY();
            this.initZ = pLoc.getZ();
            this.result = result;
        }

        @Override
        public void run() {
            if (i == 0) result.complete(true);
            if (warpSequence(i, this, p)) {
                if (i > 0) result.complete(false);
                this.cancel();
            }
            i--;
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            manager.warpDone(p);
        }
    }
}
