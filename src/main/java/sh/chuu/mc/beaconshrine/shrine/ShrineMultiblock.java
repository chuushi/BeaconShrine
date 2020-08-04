package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.BeaconShrine;
import sh.chuu.mc.beaconshrine.utils.BeaconShireItemUtils;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;

import java.util.Iterator;
import java.util.List;

import static sh.chuu.mc.beaconshrine.shrine.ShrineGuiLores.*;

public class ShrineMultiblock {
    private final BeaconShrine plugin = BeaconShrine.getInstance();
    public static final Material BLOCK = Material.NETHERITE_BLOCK;
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
            if (item != null && item.getType() == INGOT_ITEM_TYPE && ShrineGuiLores.getShrineId(item) != -1)
                continue;
            this.symbolItemType = item.getType();
            break;
        }
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

    ItemStack createWarpScrollGuiItem() {
        return ShrineGuiLores.createWarpScrollGui(id, name, cc);
    }

    void save(ConfigurationSection cs) {
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, z, shulkerY, beaconY});
        cs.set("scTime", firstTradeTime);
        cs.set("scMax", scrollMax);
        cs.set("scUses", scrollUses);
        cs.set("scPurch", scrollTotalPurchases);
    }

    void putShrineItem() {
        getInventory().addItem(createShireActivatorItem());
    }
}
