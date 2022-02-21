package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.scheduler.BukkitRunnable;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.ShrineItemStack;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;
import sh.chuu.mc.beaconshrine.utils.ShrineParticles;

import static sh.chuu.mc.beaconshrine.Vars.*;

public class ShrineCore extends AbstractShrine {
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
    public static final int RADIUS = 4;

    private final int id;
    private int beaconY;
    private Material symbolItemType;
    private long firstTradeTime;
    private int scrollUses;
    private int scrollMax;
    private int scrollTotalPurchases;
    private Player trader = null;
    private Merchant merchant = null;
    private BukkitRunnable particles;

    /**
     * When creating this, assert that ShulkerBox shulker.getCustomName() is not null.
     * @param id ID of the point
     * @param shulker The Shulker box
     * @param beacon The beacon block
     */
    public ShrineCore(int id, ShulkerBox shulker, Beacon beacon) {
        super(shulker);
        this.id = id;
        this.beaconY = beacon == null ? -1 : beacon.getY();
        this.firstTradeTime = 0;
        this.scrollMax = 3;
        this.scrollUses = 0;
        this.scrollTotalPurchases = 0;

        this.setSymbolItemType(shulker.getInventory());
    }

    public ShrineCore(int id, ConfigurationSection cs) {
        super(cs);
        this.id = id;
        this.firstTradeTime = cs.getLong("scTime", 0);
        this.scrollMax = cs.getInt("scMax", 3);
        this.scrollUses = cs.getInt("scUses", 0);
        this.scrollTotalPurchases = cs.getInt("scPurch", 0);

        this.beaconY = cs.getIntegerList("loc").get(3);

        String symIT = cs.getString("symIT");
        this.symbolItemType = symIT == null ? null : Material.getMaterial(symIT);
    }

    public void startParticles() {
        if (particles != null)
            particles.cancel();

        particles = new BukkitRunnable() {
            private int step = 0;
            private final double px = x + 0.5d;
            private final double py = y;
            private final double pz = z + 0.5d;
            @Override
            public void run() {
                ShrineParticles.shrineSpin(new Location(w, px, py, pz), dustColor(), 5, step++);
                if (step == Integer.MAX_VALUE)
                    step = 0;
            }
        };

        particles.runTaskTimer(BeaconShrine.getInstance(), 0L, 1L);
    }

    public void endParticles() {
        if (particles != null) {
            particles.cancel();
            particles = null;
        }
    }

    public boolean hasParticles() {
        return particles != null;
    }

    public ItemStack createShireActivatorItem() {
        return ShrineItemStack.createShrineActivatorItem(name, chatColor, id, x, z);
    }

    public void setShulker(ShulkerBox s, boolean dyed) {
        this.w = s.getWorld();
        this.x = s.getX();
        this.z = s.getZ();
        this.y = s.getY();
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
    @Override
    public boolean isValid() {
        Block shulkerBlock = w.getBlockAt(x, y, z);
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

    public Player trader() {
        return trader;
    }

    @Override
    public Inventory getGui(Player p) {
        ShulkerBox invState = (ShulkerBox) w.getBlockAt(x, y, z).getState();

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

    public void openMerchant(Player p) {
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

    public ItemStack createWarpScrollGuiItem(boolean urHere) {
        return ShrineGUI.createWarpGui(id, name, symbolItemType, chatColor, urHere);
    }

    public void save(ConfigurationSection cs) {
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, z, y, beaconY});
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
