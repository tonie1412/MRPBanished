package me.tonie.mrpbanished.listeners;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.config.CaptureZoneConfig;
import me.tonie.mrpbanished.managers.MineCaptureManager;
import me.tonie.mrpbanished.utils.ScoreboardUtils;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

public class MineCaptureListener implements Listener {
    private final MRPBanished plugin;
    private final MineCaptureManager captureManager;
    private final CaptureZoneConfig captureZoneConfig;

    public MineCaptureListener(MRPBanished plugin, MineCaptureManager captureManager, CaptureZoneConfig captureZoneConfig) {
        this.plugin = plugin;
        this.captureManager = captureManager;
        this.captureZoneConfig = captureZoneConfig;
    }

    @EventHandler
    public void onRegionEnter(RegionEnteredEvent event) {
        Player player = event.getPlayer();
        String region = event.getRegionName();

        // Check if this region is a registered mine region
        for (String key : captureZoneConfig.getAllCaptureZones().keySet()) {
            CaptureZoneConfig.CaptureZone zone = captureZoneConfig.getCaptureZone(key);
            if (zone.getMineRegion().equalsIgnoreCase(region)) {
                String owningTribe = getMineOwner(zone.getMineRegion());
                ScoreboardUtils.showMineScoreboard(player, zone, owningTribe);
            }

            if (zone.getCaptureRegion().equalsIgnoreCase(region)) {
                if (!zone.isCaptureEnabled()) {
                    player.sendMessage("§cThis mine cannot be captured right now.");
                    return;
                }
                captureManager.addPlayerToCaptureZone(player, zone);
            }
        }
    }

    @EventHandler
    public void onRegionLeave(RegionLeftEvent event) {
        Player player = event.getPlayer();
        String region = event.getRegionName();

        // Remove scoreboard if leaving a mine region
        for (String key : captureZoneConfig.getAllCaptureZones().keySet()) {
            CaptureZoneConfig.CaptureZone zone = captureZoneConfig.getCaptureZone(key);
            if (zone.getMineRegion().equalsIgnoreCase(region)) {
                ScoreboardUtils.removeMineScoreboard(player);
            }

            if (zone.getCaptureRegion().equalsIgnoreCase(region)) {
                captureManager.removePlayerFromCaptureZone(player, zone);
            }
        }
    }

    private String getMineOwner(String mineRegion) {
        File file = new File(plugin.getDataFolder(), "mineowners.yml");
        if (!file.exists()) return "None";

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("mines." + mineRegion, "None");
    }
}
