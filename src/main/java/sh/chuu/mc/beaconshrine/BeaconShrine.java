package sh.chuu.mc.beaconshrine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import sh.chuu.mc.beaconshrine.shrine.ShrineEvents;
import sh.chuu.mc.beaconshrine.shrine.ShrineManager;
import sh.chuu.mc.beaconshrine.userstate.CloudManager;
import sh.chuu.mc.beaconshrine.utils.CloudLoreUtils;

import java.io.IOException;
import java.util.logging.Level;

import static sh.chuu.mc.beaconshrine.utils.CloudLoreUtils.INVENTORY_NAME;

public class BeaconShrine extends JavaPlugin {
    private static BeaconShrine instance = null;
    private CloudManager cloudManager = null;
    private ShrineManager shrineManager = null;

    public static BeaconShrine getInstance() {
        return BeaconShrine.instance;
    }

    public CloudManager getCloudManager() {
        return cloudManager;
    }

    public ShrineManager getShrineManager() {
        return shrineManager;
    }

    @Override
    public void onEnable() {
        BeaconShrine.instance = this;
        saveDefaultConfig();
        cloudManager = new CloudManager();
        getServer().getPluginManager().registerEvents(cloudManager, this);
        try {
            shrineManager = new ShrineManager();
            getServer().getPluginManager().registerEvents(new ShrineEvents(), this);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load Shrine storage", e);
        }
    }

    @Override
    public void onDisable() {
        cloudManager.onDisable();
        shrineManager.onDisable();
        cloudManager = null;
        shrineManager = null;
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0 && args[0].equalsIgnoreCase("fetch")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (!cloudManager.openInventory(p))
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent[]{new TextComponent("You do not have an extra inventory!")});
            }
            return true;
        }

        if (sender instanceof Player) {
            Player p = (Player) sender;
            Inventory inv = CloudLoreUtils.getInventory(p, INVENTORY_NAME);
            ItemStack expRestore = CloudLoreUtils.createExpItem(p);
            if (expRestore != null)
                inv.setItem(44, expRestore);
            inv.setItem(43, CloudLoreUtils.createEnderChestItem(p));
            cloudManager.setInventory(p, inv);
            cloudManager.saveExp(p);
            cloudManager.openInventory(p);
        }

        return true;
    }
}
