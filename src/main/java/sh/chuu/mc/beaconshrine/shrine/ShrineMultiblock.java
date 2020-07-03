package sh.chuu.mc.beaconshrine.shrine;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sh.chuu.mc.beaconshrine.utils.BlockUtils;
import sh.chuu.mc.beaconshrine.utils.CloudLoreUtils;

import static sh.chuu.mc.beaconshrine.utils.CloudLoreUtils.BLOCK;

class ShrineMultiblock {
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

    /**
     * When creating this, assert that ShulkerBox shulker.getCustomName() is not null.
     * @param id
     * @param shulker
     * @param beacon
     */
    ShrineMultiblock(int id, ShulkerBox shulker, Beacon beacon, boolean dyed) {
        this(id,
                shulker.getWorld(),
                shulker.getX(),
                shulker.getZ(),
                shulker.getY(),
                beacon == null ? -1 : beacon.getY(),
                shulker.getCustomName(),
                dyed ? shulker.getColor() : null);
    }

    ShrineMultiblock(int id, World w, int x, int z, int shulkerY, int beaconY, String name, DyeColor color) {
        this.id = id;
        this.beaconY = beaconY;
        setShulker(w, x, z, shulkerY, name, color);
    }

    Location getLocation() {
        return new Location(w, x + 0.5, shulkerY + 0.5, z + 0.5);
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

    void setShulker(World w, int x, int z, int shulkerY, String name, DyeColor color) {
        this.w = w;
        this.x = x;
        this.z = z;
        this.name = name;
        this.color = color;
        this.cc = color == null ? ChatColor.RESET : ChatColor.of("#" + Integer.toString(color.getColor().asRGB(), 0x10));
        this.shulkerY = shulkerY;
    }

    /**
     * Is valid if
     * the shulker box contains a Netherite ingot,
     * the beacon below is full tier (Tier 4), and
     * the first row of beacon contains at least 4 Netherite blocks.
     *
     * @return true if this shrine is valid
     */
    boolean isValid() {
        Block shulkerBlock = w.getBlockAt(x, shulkerY, z);
        BlockState shulkerData = shulkerBlock.getState();
        if (!(shulkerData instanceof ShulkerBox) || this.id != CloudLoreUtils.getShrineId(((ShulkerBox) shulkerData).getInventory()))
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

    Inventory getInventory() {
        BlockState state = w.getBlockAt(x, shulkerY, z).getState();
        return state instanceof ShulkerBox ? ((ShulkerBox) state).getInventory() : null;
    }

    Inventory getGui(Player p) {
        ItemStack shulker = new ItemStack(BlockUtils.getShulkerBoxFromDyeColor(color));
        ItemMeta m = shulker.getItemMeta();
        if (m instanceof BlockStateMeta) {
            ((BlockStateMeta) m).setBlockState(w.getBlockAt(x, shulkerY, z).getState());
            m.setDisplayName(ChatColor.YELLOW + "Open Shrine Shulker Box");
            shulker.setItemMeta(m);
        }
        Inventory gui = Bukkit.createInventory(null, InventoryType.DISPENSER, cc + name);
        gui.setItem(1, CloudLoreUtils.CLOUD_CHEST);
        gui.setItem(4, shulker);
        gui.setItem(7, CloudLoreUtils.createShopItem(3));
        return gui;
    }

    Merchant getMerchant() {
        Merchant merchant = Bukkit.createMerchant(cc + name + " Scroll Shop");
        MerchantRecipe netRecipe = new MerchantRecipe(CloudLoreUtils.createWarpScroll(id, name, cc), 5);
        netRecipe.addIngredient(new ItemStack(Material.NETHERITE_INGOT));
        MerchantRecipe diaRecipe = new MerchantRecipe(CloudLoreUtils.createWarpScroll(id, name, cc), 5);
        diaRecipe.addIngredient(new ItemStack(Material.DIAMOND, 4));
        merchant.setRecipes(ImmutableList.of(netRecipe, diaRecipe));
        return merchant;
    }

    void save(ConfigurationSection cs) {
        cs.set("name", name);
        cs.set("color", color == null ? null : color.toString());
        cs.set("world", w.getName());
        cs.set("loc", new int[]{x, z, shulkerY, beaconY});
    }

    ItemStack createShrineItem() {
        return CloudLoreUtils.createShrineItem(cc + name, id, x, z);
    }

    // TODO Possibly open up beacon inventory??
//    public Inventory getBeaconInventory() {
//    }
}
