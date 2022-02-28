package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;
import sh.chuu.mc.beaconshrine.utils.ParticleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class ShrineCore extends AbstractShrine {
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    public static final int RADIUS = 4;

    private final ArrayList<ShrineShard> shards = new ArrayList<>();

    private int beaconY;
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
    public ShrineCore(int id, ShulkerBox shulker, Beacon beacon) {
        super(id, shulker);
        this.beaconY = beacon == null ? -0xff : beacon.getY();
        this.firstTradeTime = 0;
        this.scrollMax = 3;
        this.scrollUses = 0;
        this.scrollTotalPurchases = 0;

        this.setSymbolItemType(shulker.getInventory());

        updateShardList();
    }

    public ShrineCore(int id, ConfigurationSection cs) {
        super(id, cs, true);
        this.firstTradeTime = cs.getLong("scTime", 0);
        this.scrollMax = cs.getInt("scMax", 3);
        this.scrollUses = cs.getInt("scUses", 0);
        this.scrollTotalPurchases = cs.getInt("scPurch", 0);

        this.beaconY = cs.getIntegerList("loc").get(3);

        String symIT = cs.getString("symIT");
        this.symbolItemType = symIT == null ? null : Material.getMaterial(symIT);

        List<Map<?, ?>> sh = cs.getMapList("sh");
        for (Map<?, ?> ss : sh)
            shards.add(new ShrineShard(id, this, ss));
    }

    @Override
    public ItemStack activatorItem() {
        return BeaconShireItemUtils.shrineActivatorItem(SHRINE_CORE_ACTIVATOR_ITEM_TYPE, true, name, chatColor, id, x, z);
    }

    public ItemStack shardActivatorItem() {
        return BeaconShireItemUtils.shrineActivatorItem(SHRINE_SHARD_ACTIVATOR_ITEM_TYPE, false, name, chatColor, id, x, z);
    }

    @Override
    public void setShulker(ShulkerBox s, boolean dyed) {
        super.setShulker(s, dyed);
        this.beaconY = -0xff;
    }

    public void setShulker(ShulkerBox s, boolean dyed, Beacon beacon) {
        super.setShulker(s, dyed);
        this.beaconY = beacon == null ? -0xff : beacon.getY();
    }

    /**
     * Is valid if
     * the shulker box contains a custom Netherite ingot,
     * the beacon below is full tier (Tier 4), and
     * the top blocks of beacon tower contains at least 4 Netherite blocks.
     * FIXME Blocked beacon beam is still valid!
     * TODO Require at least 2 blocks of free space within 7 blocks of shrine shulker directly under
     * @return true if this shrine is valid
     */
    @Override
    public boolean isValid() {
        Block shulkerBlock = w.getBlockAt(x, y, z);
        BlockState shulkerData = shulkerBlock.getState();
        if (!(shulkerData instanceof ShulkerBox sb) || this.id != BeaconShireItemUtils.getShrineId(sb.getInventory(), SHRINE_CORE_ACTIVATOR_ITEM_TYPE))
            return false;

        BlockState beaconState = beaconY == -0xff ? null : w.getBlockAt(x, beaconY, z).getState();
        Beacon beacon;
        if (beaconState instanceof Beacon) {
            beacon = (Beacon) beaconState;
            // TODO Implement beacon beam obstruction detection code
            if (beacon.getTier() < 4) return false;
        } else {
            beacon = BlockUtils.getBeaconBelow(shulkerBlock.getRelative(BlockFace.DOWN, 3), 4);
            if (beacon == null) {
                beaconY = -0xff;
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

    public Player trader() {
        return trader;
    }

    @Override
    public Inventory getGui(Player p) {
        ShulkerBox invState = (ShulkerBox) w.getBlockAt(x, y, z).getState();

        // Start making inventory
        Inventory gui = Bukkit.createInventory(null, InventoryType.DISPENSER, chatColor + name);
        gui.setItem(0, ShrineGUI.shulkerBox(invState, color));
        gui.setItem(1, ShrineGUI.WARP_LIST_ITEM);
        gui.setItem(4, ShrineGUI.SHARD_LIST_ITEM);
        // FIXME Shop Item amount desync (shows 0) after fully restocked

        // Add Ender Chest if it exists in Shulker inventory
        gui.setItem(6, ShrineGUI.CLOUD_CHEST_ITEM);
        gui.setItem(7, ShrineGUI.createShopItem(trader == null ? scrollMax - scrollUses : -1, firstTradeTime));
        if (invState.getInventory().contains(Material.ENDER_CHEST)) gui.setItem(8, ShrineGUI.ENDER_CHEST_ITEM);
        return gui;
    }

    @Override
    protected void theParticles(int step) {
        ParticleUtils.shrineSpin(new Location(w, midX, midY, midZ), dustColor(), 5, step * 0.1);
    }

    public void openMerchant(Player p) {
        if (scrollUses != 0 && System.currentTimeMillis() - firstTradeTime > ShrineGUI.RESTOCK_TIMER) {
            scrollUses = 0;
            firstTradeTime = 0;
            // Incrase max based on this curve
            scrollMax = 1 + ((int) Math.sqrt(scrollTotalPurchases + 1)) * 2;
        }

        merchant = Bukkit.createMerchant(name + " Scroll Shop");
        trader = p;
        MerchantRecipe warpScroll = new MerchantRecipe(createShireWarpItem(p), scrollUses, scrollMax, false);
        warpScroll.addIngredient(new ItemStack(Material.DIAMOND, 2));

        MerchantRecipe shrineShard = new MerchantRecipe(shardActivatorItem(), 0, 26, false);
        shrineShard.addIngredient(new ItemStack(SHRINE_SHARD_ACTIVATOR_ITEM_TYPE, 2));
        shrineShard.addIngredient(new ItemStack(Material.ENDER_PEARL, 1));

        merchant.setRecipes(ImmutableList.of(warpScroll, shrineShard));
        p.openMerchant(merchant, true);
    }

    public void closeMerchant(Player p) {
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

    @Override
    public ItemStack createWarpScrollGuiItem(boolean urHere) {
        return ShrineGUI.createCoreWarpGui(id, name, symbolItemType, chatColor, urHere);
    }

    public boolean containsShard(ShrineShard shard) {
        return shards.contains(shard);
    }

    public void updateShardList() {
        shards.clear();
        for (ItemStack item : getInventory()) {
            if (item == null || item.getType() != Material.COMPASS) continue;

            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof CompassMeta cm) || !cm.hasLodestone()) continue;

            Location lodestone = cm.getLodestone();
            if (lodestone == null) continue;

            ShardShulkerFromLodestone sbfl = getShulkerAttachedToLodestone(lodestone);
            if (sbfl != null) shards.add(new ShrineShard(id, this, sbfl.shulker, lodestone, sbfl.face.getOppositeFace()));
        }
    }

    public record ShardShulkerFromLodestone(ShulkerBox shulker, BlockFace face) {}
    /**
     * Find valid Shulker Box of this Shrine attached to lodestone
     * @param location Location of Lodestone
     * @return the ShulkerBox and Face record instance if valid, else null
     */
    public ShardShulkerFromLodestone getShulkerAttachedToLodestone(Location location) {
        Block block = location.getBlock();
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST}) {
            Block b = block.getRelative(face);
            BlockState blockState = b.getState();

            if (blockState instanceof ShulkerBox sb) {
                if (id == BeaconShireItemUtils.getShrineId(sb.getInventory(), SHRINE_SHARD_ACTIVATOR_ITEM_TYPE)) {
                    return new ShardShulkerFromLodestone(sb, face);
                }
            }
        }
        return null;
    }

    @Override
    protected WarpSequenceInit warpSequenceInit(Player p, AbstractShrine from) {
        if (from instanceof ShrineShard) return warpSequenceFromShrineInit(p, from);

        final double newY;
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
                return null;
            }
            newY = b.getY();
        } else {
            newY = w.getHighestBlockYAt(x, z) + 30;
        }

        Location loc = p.getLocation();
        Vector vector;
        Vector up = new Vector(0, 384 - loc.getY(), 0);
        if (from != null) {
            vector = ParticleUtils.getDiff(from.x, from.y, from.z, loc);
            ParticleUtils.shrineIgnitionSound(p);
            ParticleUtils.beam(from.getShulkerLocation(true), up, dustColor());
            ParticleUtils.beam(loc, vector, dustColor());
        } else {
            ParticleUtils.beam(loc, up, dustColor());
            ParticleUtils.paperIgnitionSound(p);
        }
        return new WarpSequenceInit(100, x + 0.5, newY, z + 0.5, false);
    }

    @Override
    protected boolean warpSequence(int step, WarpSequence ws, Player p) {
        if (ws.isShard) return warpSequenceFromShrine(step, ws, p);

        Location loc = p.getLocation();

        if (step > 30) {
            return loc.getX() != ws.initX || loc.getY() != ws.initY || loc.getZ() != ws.initZ;
        } else if (step == 30) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0, false, false, false));
        } else if (step == 10) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 63, false, false, false));
            //noinspection ConstantConditions
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.5f);
        } else if (step == 2) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, false, false, false));
        } else if (step == 0) {
            Location to = p.getLocation();
            to.setX(ws.newX);
            to.setY(ws.newY);
            to.setZ(ws.newZ);

            ParticleUtils.warpBoom(loc, ws.color);
            cloudManager.setNextWarp(p, System.currentTimeMillis() + GLOBAL_WARP_COOLDOWN);
            p.teleport(to);
            p.removePotionEffect(PotionEffectType.LEVITATION);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, w.getEnvironment() == World.Environment.NETHER ? 100 : 200, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 0, false, false, false));
        } else if (step == -1) {
            ParticleUtils.warpBoom(loc, ws.color);
        } else return step == -100;
        return false;
    }

    protected WarpSequenceInit warpSequenceFromShrineInit(Player p, AbstractShrine from) {
        // TODO Set destination as somewhere below or above the Shrine
//        ShrineShard.LodestoneBlock ls = getLodestone();
//        if (ls == null)
//            return null;
//
//        Block tpSpot = null;
//        for (int i = 0; i < ShrineCore.RADIUS; i++) {
//            if (ls.block.isPassable() && ls.block.getRelative(BlockFace.UP).isPassable()) {
//                tpSpot = ls.block;
//                break;
//            }
//        }
//        if (tpSpot == null)
//            return null;

//        return new WarpSequenceInit(100, tpSpot.getX() + 0.5, tpSpot.getY(), tpSpot.getZ() + 0.5, true);
        return null;
    }

    protected boolean warpSequenceFromShrine(int step, WarpSequence ws, Player p) {
        // FIXME Complete soon!
        return step == 0;
    }

    @Override
    public List<ShrineShard> getShards() {
        return shards;
    }

    @Override
    public void save(ConfigurationSection cs) {
        super.save(cs);
        cs.set("loc", new int[]{x, z, y, beaconY});
        cs.set("scTime", firstTradeTime);
        cs.set("scMax", scrollMax);
        cs.set("scUses", scrollUses);
        cs.set("scPurch", scrollTotalPurchases);

        List<Map<String, Object>> sh = new ArrayList<>(shards.size());
        for (ShrineShard ss : shards) { sh.add(ss.save()); }
        cs.set("sh", sh);
    }
}
