package me.tonie.mrpbanished.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.config.MiningConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;

import java.io.File;

public class MiningListener implements Listener {
    private final MRPBanished plugin;
    private final MiningConfig miningConfig;
    private final WorldGuardPlugin worldGuard;

    public MiningListener(MRPBanished plugin, MiningConfig miningConfig, WorldGuardPlugin worldGuard) {
        this.plugin = plugin;
        this.miningConfig = miningConfig;
        this.worldGuard = worldGuard;
    }

    @EventHandler
    public void onRightClickOre(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material blockType = block.getType();
        MiningConfig.OreData oreData = miningConfig.getOreData(blockType);
        if (oreData == null) return; // Ignore non-configured ores

        // Check if mining is allowed in this WorldGuard region
        if (!isMiningAllowed(player, block.getLocation())) {
            player.sendMessage("§cYou cannot mine in this region!");
            return;
        }

        // Check mine ownership
        String mineRegion = getMineRegion(block);
        if (!canMine(player, mineRegion)) {
            player.sendMessage("§cYour tribe does not own this mine. Only the controlling tribe can mine here!");
            return;
        }

        // Check if the player has a valid pickaxe
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !oreData.getPickaxes().contains(item.getItemMeta().getDisplayName())) {
            player.sendMessage("§cYou need a valid pickaxe to mine this ore!");
            return;
        }

        // Start mining process
        plugin.getMiningManager().startMining(player, block, oreData);
    }

    private boolean isMiningAllowed(Player player, Location location) {
        RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regions == null) return false;

        ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        for (ProtectedRegion region : set) {
            if (region.getFlag(MRPBanished.getMineableFlag()) == StateFlag.State.ALLOW) {
                return true;
            }
        }
        return false;
    }

    private String getMineRegion(Block block) {
        // Logic to determine which mine region the block is in
        return "northeastmine"; // Placeholder, needs WorldGuard region check
    }

    private boolean canMine(Player player, String mineRegion) {
        File file = new File(plugin.getDataFolder(), "mineowners.yml");
        if (!file.exists()) return true; // If no owner, anyone can mine

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String ownerTribe = config.getString("mines." + mineRegion, "None");
        String playerTribe = getPlayerTribe(player);

        return ownerTribe.equals("None") || ownerTribe.equalsIgnoreCase(playerTribe);
    }

    private String getPlayerTribe(Player player) {
        File file = new File(plugin.getDataFolder() + "/playerdata", player.getUniqueId().toString() + ".yml");
        if (!file.exists()) {
            return "Lone Banished"; // Default if no data exists
        }

        FileConfiguration playerData = YamlConfiguration.loadConfiguration(file);
        return playerData.getString("tribe", "Lone Banished");
    }
}
