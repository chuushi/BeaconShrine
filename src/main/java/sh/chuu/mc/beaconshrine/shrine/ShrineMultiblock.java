package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineItemStack;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;
import sh.chuu.mc.beaconshrine.utils.ShrineParticles;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class ShrineMultiblock {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private static final BaseComponent SAME_DIMENSION_REQUIRED = new TextComponent("Shrine is in another dimension");
    private static final BaseComponent NO_CLEARANCE = new TextComponent("Couldn't find any clearance for this shrine");
    private static final BaseComponent INVALID_SHRINE = new TextComponent("Unable to teleport to the broken shrine");
    private static final BaseComponent INVALID_WARPING = new TextComponent("Move to cancel the current warp first");
    private static final int WARP_COOLDOWN = 300000;
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    public static final int RADIUS = 4;

    private final int id;
    private String name;
    private World w;
    private int x;
    private int z;
    private int shulkerY;
    private int beaconY;
    private DyeColor color;
    private ChatColor chatColor;
    private Material symbolItemType;
    private long firstTradeTime;
    private int scrollUses;
    private int scrollMax;
    private int scrollTotalPurchases;
    private Player trader = null;
    private Merchant merchant = null;

    /**
     * When creating this, assert that ShulkerBox shulker.getCustomName() is not null.
     * @param id ID of the point
     * @param shulker The Shulker box
     * @param beacon The beacon block
     */
    public ShrineMultiblock(int id, ShulkerBox shulker, Beacon beacon) {
        this.id = id;
        this.beaconY = beacon == null ? -1 : beacon.getY();
        this.firstTradeTime = 0;
        this.scrollMax = 3;
        this.scrollUses = 0;
        this.scrollTotalPurchases = 0;

        DyeColor color = shulker.getColor();
        this.w = shulker.getWorld();
        this.x = shulker.getX();
        this.z = shulker.getZ();
        this.shulkerY = shulker.getY();
        this.name = shulker.getCustomName();
        this.color = color;
        this.chatColor = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
        this.setSymbolItemType(shulker.getInventory());
    }

    public ShrineMultiblock(int id, ConfigurationSection cs) {
        this.id = id;
        this.firstTradeTime = cs.getLong("scTime", 0);
        this.scrollMax = cs.getInt("scMax", 3);
        this.scrollUses = cs.getInt("scUses", 0);
        this.scrollTotalPurchases = cs.getInt("scPurch", 0);
        //noinspection ConstantConditions
        this.w = Bukkit.getWorld(cs.getString("world"));

        Iterator<Integer> loc = cs.getIntegerList("loc").iterator();
        this.x = loc.next();
        this.z = loc.next();
        this.shulkerY = loc.next();
        this.beaconY = loc.next();

        this.name = cs.getString("name");

        String colorStr = cs.getString("color");
        DyeColor color = colorStr == null ? null : DyeColor.valueOf(colorStr);
        this.color = color;
        this.chatColor = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));

        String symIT = cs.getString("symIT");
        this.symbolItemType = symIT == null ? null : Material.getMaterial(symIT);
    }

    public ItemStack createShireActivatorItem() {
        return ShrineItemStack.createShrineActivatorItem(name, chatColor, id, x, z);
    }

    public int distanceSquaredXZ(int x, int z) {
        x -= this.x;
        z -= this.z;
        return x*x + z*z;
    }

    public void setShulker(ShulkerBox s, boolean dyed) {
        this.w = s.getWorld();
        this.x = s.getX();
        this.z = s.getZ();
        this.shulkerY = s.getY();
        this.name = s.getCustomName();
        this.color = dyed ? s.getColor() : null;
        this.chatColor = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
        this.beaconY = -1;
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

    /**
     * Is valid if
     * the shulker box contains a Netherite ingot,
     * the beacon below is full tier (Tier 4), and
     * the first row of beacon contains at least 4 Netherite blocks.
     *
     * @return true if this shrine is valid
     */
    public boolean isValid() {
        Block shulkerBlock = w.getBlockAt(x, shulkerY, z);
        BlockState shulkerData = shulkerBlock.getState();
        if (!(shulkerData instanceof ShulkerBox) || this.id != ShrineGUI.getShrineId(((ShulkerBox) shulkerData).getInventory()))
            return false;

        BlockState beaconState = beaconY == -1 ? null : w.getBlockAt(x, beaconY, z).getState();
        Beacon beacon;
        if (beaconState instanceof Beacon) {
            beacon = (Beacon) beaconState;
            // TODO Implement beacon beam obstruction detection code
            if (beacon.getTier() < 4) return false;
        } else {
            beacon = BlockUtils.getBeaconBelow(shulkerBlock.getRelative(BlockFace.DOWN, 3), 4);
            if (beacon == null) {
                beaconY = -1;
                return false;
            } else {
                beaconY = beacon.getY();
            }
        }

        Block below = beacon.getBlock().getRelative(BlockFace.DOWN);
        int count = 4;
        if (below.getType() == BLOCK) count--;
        for (Block b : BlockUtils.getSurrounding8(below))
            if (b.getType() == BLOCK && --count == 0) return true;
        return false;
    }

    public int id() {
        return id;
    }

    Player trader() {
        return trader;
    }

    public World getWorld() {
        return w;
    }

    public int x() {
        return x;
    }

    public int shulkerY() {
        return shulkerY;
    }

    public int z() {
        return z;
    }

    public String name() {
        return name;
    }

    public Color color() {
        return color == null ? Color.WHITE : color.getColor();
    }

    public Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(color(), 1);
    }

    Inventory getInventory() {
        BlockState state = w.getBlockAt(x, shulkerY, z).getState();
        return state instanceof ShulkerBox ? ((ShulkerBox) state).getInventory() : null;
    }

    Inventory getGui(Player p) {
        ShulkerBox invState = (ShulkerBox) w.getBlockAt(x, shulkerY, z).getState();

        // Start making inventory
        Inventory gui = Bukkit.createInventory(null, InventoryType.DISPENSER, chatColor + name);
        gui.setItem(0, ShrineGUI.shulkerBox(invState, color));
        gui.setItem(2, ShrineGUI.CLOUD_CHEST_ITEM);
        gui.setItem(4, ShrineGUI.WARP_LIST_ITEM);
        // FIXME Shop Item amount desync (shows 0) after fully restocked
        gui.setItem(7, ShrineGUI.createShopItem(trader == null ? scrollMax - scrollUses : -1, firstTradeTime));

        // Add Ender Chest if it exists in Shulker inventory
        if (invState.getInventory().contains(Material.ENDER_CHEST)) gui.setItem(1, ShrineGUI.ENDER_CHEST_ITEM);
        return gui;
    }

    void openMerchant(Player p) {
        if (scrollUses != 0 && System.currentTimeMillis() - firstTradeTime > ShrineGUI.RESTOCK_TIMER) {
            scrollUses = 0;
            firstTradeTime = 0;
            // Incrase max based on this curve
            scrollMax = 1 + ((int) Math.sqrt(scrollTotalPurchases + 1)) * 2;
        }

        merchant = Bukkit.createMerchant(name + " Scroll Shop");
        trader = p;
        MerchantRecipe recipe = new MerchantRecipe(createShireWarpItem(p), scrollUses, scrollMax, false);
        recipe.addIngredient(new ItemStack(Material.DIAMOND, 2));
        merchant.setRecipes(ImmutableList.of(recipe));
        p.openMerchant(merchant, true);
    }

    void closeMerchant(Player p) {
        if (trader != p) return;
        int uses = merchant.getRecipe(0).getUses();
        if (uses != scrollUses) {
            if (firstTradeTime == 0) firstTradeTime = System.currentTimeMillis();
            scrollTotalPurchases += uses - scrollUses;
            scrollUses = uses;
        }
        trader = null;
        merchant = null;
    }

    private ItemStack createShireWarpItem(Player p) {
        return BeaconShireItemUtils.createWarpScroll(id, name, chatColor, p);
    }

    ItemStack createWarpScrollGuiItem(boolean urHere) {
        return ShrineGUI.createWarpGui(id, name, symbolItemType, chatColor, urHere);
    }

    public CompletableFuture<Boolean> warpPlayer(Player p, ShrineMultiblock from) {
        ShrineManager manager = plugin.getShrineManager();
        CloudManager cloudManager = plugin.getCloudManager();
        if (manager.warpContains(p)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_WARPING);
            return CompletableFuture.completedFuture(false);
        } else if (!isValid()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return CompletableFuture.completedFuture(false);
        } else {
            World w = getWorld();
            final double newX = x + 0.5d;
            final double newY;
            final double newZ = z + 0.5d;
            if (w != p.getWorld()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, SAME_DIMENSION_REQUIRED);
                return CompletableFuture.completedFuture(false);
            }
            boolean isNether = w.getEnvironment() == World.Environment.NETHER;
            if (isNether) {
                Block b = w.getBlockAt(x, shulkerY() + 2, z);
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
                vector = ShrineParticles.getDiff(from.x, from.shulkerY, from.z, loc);
                ShrineParticles.shrineIgnitionSound(p);
                ShrineParticles.beam(from.getShulkerLocation(), up, getDustOptions());
                ShrineParticles.beam(loc, vector, getDustOptions());
            } else {
                ShrineParticles.beam(loc, up, getDustOptions());
                ShrineParticles.paperIgnitionSound(p);
            }

            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            Location pLoc = p.getLocation();
            manager.warpAdd(p);
            new BukkitRunnable() {
                int i = 100;
                final double x = pLoc.getX();
                final double y = pLoc.getY();
                final double z = pLoc.getZ();
                final Color c = color();

                @Override
                public void run() {
                    Location loc = p.getLocation();
                    final Particle.DustOptions dustOpt = getDustOptions();

                    if (i > 30) {
                        if (loc.getX() != x || loc.getY() != y || loc.getZ() != z) {
                            ret.complete(false);
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
                        cloudManager.setNextWarp(p, System.currentTimeMillis() + WARP_COOLDOWN);
                        p.teleport(to);
                        p.removePotionEffect(PotionEffectType.LEVITATION);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, isNether ? 100 : 200, 0, false, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 0, false, false, false));
                        ret.complete(true);
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
            }.runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
            return ret;
        }
    }

    private Location getShulkerLocation() {
        return new Location(w, x + 0.5, shulkerY + 0.5, z + 0.5);
    }

    void save(ConfigurationSection cs) {
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, z, shulkerY, beaconY});
        cs.set("scTime", firstTradeTime);
        cs.set("symIT", symbolItemType == null ? null : symbolItemType.name());
        cs.set("scMax", scrollMax);
        cs.set("scUses", scrollUses);
        cs.set("scPurch", scrollTotalPurchases);
    }

    /**
     * This assumes that a Shulker box exists as an inventory
     */
    public void putShrineItem() {
        getInventory().addItem(createShireActivatorItem());
    }
}
