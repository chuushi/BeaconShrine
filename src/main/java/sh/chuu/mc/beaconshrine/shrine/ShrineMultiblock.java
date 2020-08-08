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
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static sh.chuu.mc.beaconshrine.shrine.ShrineGuiLores.*;

public class ShrineMultiblock {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    private static final BaseComponent SAME_DIMENSION_REQUIRED = new TextComponent("Shrine is in another dimension");
    private static final BaseComponent NO_CLEARANCE = new TextComponent("Couldn't find any clearance for this shrine");
    private static final BaseComponent INVALID_SHRINE = new TextComponent("Unable to teleport to the broken shrine");
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    private static final int WARP_COOLDOWN = 300000;
    static final int RADIUS = 4;
    private final int id;
    private String name;
    private World w;
    private int x;
    private int z;
    private int shulkerY;
    private int beaconY;
    private DyeColor color;
    private ChatColor cc;
    private Material symbolItemType;
    private long firstTradeTime;
    private int scrollUses;
    private int scrollMax;
    private int scrollTotalPurchases;
    private Player trader = null;
    private Merchant merchant = null;

    /**
     * When creating this, assert that ShulkerBox shulker.getCustomName() is not null.
     * @param id
     * @param shulker
     * @param beacon
     */
    ShrineMultiblock(int id, ShulkerBox shulker, Beacon beacon, boolean dyed) {
        this.id = id;
        this.beaconY = beacon == null ? -1 : beacon.getY();
        this.firstTradeTime = 0;
        this.scrollMax = 3;
        this.scrollUses = 0;

        DyeColor color = dyed ? shulker.getColor() : null;
        setShulker(shulker.getWorld(), shulker.getX(), shulker.getZ(), shulker.getY(), shulker.getCustomName(), color);
        setSymbolItemType(shulker.getInventory());
    }

    ShrineMultiblock(int id, ConfigurationSection cs) {
        this.id = id;
        this.firstTradeTime = cs.getLong("scTime", 0);
        this.scrollMax = cs.getInt("scMax", 3);
        this.scrollUses = cs.getInt("scUses", 0);
        this.scrollTotalPurchases = cs.getInt("scPurch", 0);

        String name = cs.getString("name");
        String colorStr = cs.getString("color");
        DyeColor color = colorStr == null ? null : DyeColor.valueOf(colorStr);
        World w = Bukkit.getWorld(cs.getString("world"));
        Iterator<Integer> loc = cs.getIntegerList("loc").iterator();
        int x = loc.next();
        int z = loc.next();
        int shulkerY = loc.next();
        this.beaconY = loc.next();

        setShulker(w, x, z, shulkerY, name, color);
        String symIT = cs.getString("symIT");
        this.symbolItemType = symIT == null ? null : Material.getMaterial(symIT);
    }

    public ItemStack createShireActivatorItem() {
        return createShrineActivatorItem(name, cc, id, x, z);
    }

    int distanceSquaredXZ(int x, int z) {
        x -= this.x;
        z -= this.z;
        return x*x + z*z;
    }

    void updateShulker(ShulkerBox s, boolean dyed) {
        setShulker(s.getWorld(), s.getX(), s.getZ(), s.getY(), s.getCustomName(), dyed ? s.getColor() : null);
        this.beaconY = -1;
    }

    private void setShulker(World w, int x, int z, int shulkerY, String name, DyeColor color) {
        this.w = w;
        this.x = x;
        this.z = z;
        this.name = name;
        this.color = color;
        this.cc = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
        this.shulkerY = shulkerY;
    }

    public void updateSymbolItemType(Inventory inv) {
        setSymbolItemType(inv);
    }

    private void setSymbolItemType(Inventory inv) {
        for (ItemStack item : inv) {
            if (item == null || item.getType() == INGOT_ITEM_TYPE && ShrineGuiLores.getShrineId(item) != -1)
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
        if (!(shulkerData instanceof ShulkerBox) || this.id != getShrineId(((ShulkerBox) shulkerData).getInventory()))
            return false;

        BlockState beaconState = beaconY == -1 ? null : w.getBlockAt(x, beaconY, z).getState();
        Beacon beacon;
        if (beaconState instanceof Beacon) {
            beacon = (Beacon) beaconState;
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

    public int getId() {
        return id;
    }

    Player getTrader() {
        return trader;
    }

    public World getWorld() {
        return w;
    }

    public int getX() {
        return x;
    }

    public int getShulkerY() {
        return shulkerY;
    }

    public int getZ() {
        return z;
    }

    public String getName() {
        return name;
    }

    public DyeColor getDyeColor() {
        return color;
    }

    Inventory getInventory() {
        BlockState state = w.getBlockAt(x, shulkerY, z).getState();
        return state instanceof ShulkerBox ? ((ShulkerBox) state).getInventory() : null;
    }

    Inventory getGui(Player p) {
        // TODO figure out if setting material is required (ItemMeta contains item info)
        ItemStack shulker = new ItemStack(BlockUtils.getShulkerBoxFromDyeColor(color));
        BlockStateMeta m = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox state = (ShulkerBox) w.getBlockAt(x, shulkerY, z).getState();
        boolean hasEnderChest = state.getInventory().contains(Material.ENDER_CHEST);

        m.setBlockState(state);
        m.setDisplayName(ChatColor.YELLOW + "Open Shrine Shulker Box");
        shulker.setItemMeta(m);

        Inventory gui = Bukkit.createInventory(null, InventoryType.DISPENSER, cc + name);
        gui.setItem(0, shulker);
        gui.setItem(2, CLOUD_CHEST_ITEM);
        gui.setItem(4, WARP_LIST_ITEM);
        gui.setItem(7, createShopItem(trader == null ? scrollMax - scrollUses : -1, firstTradeTime));
        if (hasEnderChest) gui.setItem(1, ENDER_CHEST_ITEM);
//        ItemStack[] c = plugin.getCloudManager().getInventoryContents(p);
//        if (c != null && c.length == 45) {
//            // set non-consuming teleportation
//            setQuickTeleportItem(gui, 3, p, c[42]);
//            setQuickTeleportItem(gui, 4, p, c[43]);
//            setQuickTeleportItem(gui, 5, p, c[44]);
//        }
        return gui;
    }

    private void setQuickTeleportItem(Inventory gui, int index, Player p, ItemStack scroll) {
        BeaconShireItemUtils.WarpScroll ws = BeaconShireItemUtils.getWarpScrollData(scroll);
        if (ws != null && ws.owner.equals(p.getUniqueId())) {
            ItemStack i = scroll.clone();
            ItemMeta im = i.getItemMeta();
            if (this.id == ws.id) {
                //noinspection ConstantConditions if ws exists, im also exists
                im.setLore(ImmutableList.of(ChatColor.GRAY + "You are here"));
            } else {
                //noinspection ConstantConditions if ws exists, im also exists
                List<String> l = im.getLore();
                //noinspection ConstantConditions if ws exists, l also exists
                l.set(2, ChatColor.YELLOW + "Right click to warp");
                im.setLore(l);
            }
            i.setItemMeta(im);
            gui.setItem(index, i);
        }
    }

    void openMerchant(Player p) {
        if (scrollUses != 0 && System.currentTimeMillis() - firstTradeTime > RESTOCK_TIMER) {
            scrollUses = 0;
            firstTradeTime = 0;
            // Incrase max based on this curve
            scrollMax = 1 + ((int) Math.sqrt(scrollTotalPurchases + 1)) * 2;
        }

        merchant = Bukkit.createMerchant(name + " Scroll Shop");
        trader = p;
        MerchantRecipe recipe = new MerchantRecipe(createShireWarpItem(p), scrollUses, scrollMax, false);
        recipe.addIngredient(new ItemStack(Material.DIAMOND, 2));
        if (true) {
            merchant.setRecipes(ImmutableList.of(recipe));
        } else {
            merchant.setRecipes(ImmutableList.of(recipe));
        }
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
        return BeaconShireItemUtils.createWarpScroll(id, name, cc, p);
    }

    ItemStack createWarpScrollGuiItem(boolean urHere) {
        return ShrineGuiLores.createWarpGui(id, name, symbolItemType, cc, urHere);
    }

    public CompletableFuture<Boolean> warpPlayer(Player p) {
        ShrineManager manager = plugin.getShrineManager();
        CloudManager cloudManager = plugin.getCloudManager();
        if (!manager.warpAdd(p)) {
            return CompletableFuture.completedFuture(false);
        } else if (!isValid()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, INVALID_SHRINE);
            return CompletableFuture.completedFuture(false);
        } else {
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1, 1);
            World w = getWorld();
            int x = getX();
            int z = getZ();
            Location tpLoc = p.getLocation();
            if (w != tpLoc.getWorld()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, SAME_DIMENSION_REQUIRED);
                return CompletableFuture.completedFuture(false);
            }
            tpLoc.setX(x + 0.5d);
            boolean isNether = w.getEnvironment() == World.Environment.NETHER;
            if (isNether) {
                Block b = w.getBlockAt(x, getShulkerY() + 2, z);
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
                tpLoc.setY(b.getY());
            } else {
                tpLoc.setY(w.getHighestBlockYAt(x, z) + 30);
            }
            tpLoc.setZ(z + 0.5d);
            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            Location pLoc = p.getLocation();
            new BukkitRunnable() {
                int i = 100;
                final double x = pLoc.getX();
                final double y = pLoc.getY();
                final double z = pLoc.getZ();
                final Color c = color == null ? Color.WHITE : color.getColor();

                @Override
                public void run() {
                    Location loc = p.getLocation();
                    final Particle.DustOptions dustOpt = new Particle.DustOptions(c, 1);

                    if (i > 30) {
                        if (loc.getX() != x || loc.getY() != y || loc.getZ() != z) {
                            ret.complete(false);
                            this.cancel();
                        }
                    } else if (i == 30) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0, false, false, false));
                    } else if (i == 10) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 63, false, false, false));
                        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.5f);
                    } else if (i == 0) {
                        ShrineParticles.warpBoom(loc, c);
                        cloudManager.setNextWarp(p, System.currentTimeMillis() + WARP_COOLDOWN);
                        p.teleport(tpLoc);
                        p.removePotionEffect(PotionEffectType.LEVITATION);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, isNether ? 100 : 200, 0, false, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 0, false, false, false));
                        ret.complete(true);
                    } else if (i == -1) {
                        ShrineParticles.warpBoom(tpLoc, c);
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

    void putShrineItem() {
        getInventory().addItem(createShireActivatorItem());
    }
}
