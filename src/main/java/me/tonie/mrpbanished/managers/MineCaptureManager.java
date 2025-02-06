package me.tonie.mrpbanished.managers;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.config.CaptureZoneConfig;
import me.tonie.mrpbanished.utils.ScoreboardUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MineCaptureManager {
    private final MRPBanished plugin;
    private final Map<String, Set<Player>> capturingPlayers = new HashMap<>();
    private final Map<String, Set<Player>> enemyPlayers = new HashMap<>();
    private final Map<String, Integer> captureTimers = new HashMap<>();
    private final Map<UUID, Float> storedXp = new HashMap<>();
    private final Map<String, String> mineOwnership = new HashMap<>();
    private File mineOwnersFile;
    private FileConfiguration mineOwnersConfig;

    public MineCaptureManager(MRPBanished plugin) {
        this.plugin = plugin;
        loadMineOwnership();
    }

    private void loadMineOwnership() {
        mineOwnersFile = new File(plugin.getDataFolder(), "mineowners.yml");
        if (!mineOwnersFile.exists()) {
            plugin.saveResource("mineowners.yml", false);
        }
        mineOwnersConfig = YamlConfiguration.loadConfiguration(mineOwnersFile);

        if (mineOwnersConfig.isConfigurationSection("mines")) {
            for (String mine : mineOwnersConfig.getConfigurationSection("mines").getKeys(false)) {
                mineOwnership.put(mine, mineOwnersConfig.getString("mines." + mine, "None"));
            }
        }
    }

    public void saveMineOwnership() {
        for (Map.Entry<String, String> entry : mineOwnership.entrySet()) {
            mineOwnersConfig.set("mines." + entry.getKey(), entry.getValue());
        }
        try {
            mineOwnersConfig.save(mineOwnersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMineOwner(String mineRegion, String tribe) {
        mineOwnership.put(mineRegion, tribe);
        saveMineOwnership();
    }

    public String getMineOwner(String mineRegion) {
        return mineOwnership.getOrDefault(mineRegion, "None");
    }

    public void addPlayerToCaptureZone(Player player, CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        String playerTribe = getPlayerTribe(player);

        if (playerTribe.equalsIgnoreCase("Lone Banished")) {
            player.sendMessage("§cYou cannot capture mines as a Lone Banished.");
            return;
        }

        capturingPlayers.putIfAbsent(captureRegion, new HashSet<>());
        capturingPlayers.get(captureRegion).add(player);
        storedXp.put(player.getUniqueId(), player.getExp());

        if (!captureTimers.containsKey(captureRegion)) {
            startCapture(zone);
        }
    }

    public void removePlayerFromCaptureZone(Player player, CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        if (capturingPlayers.containsKey(captureRegion)) {
            capturingPlayers.get(captureRegion).remove(player);
            if (capturingPlayers.get(captureRegion).isEmpty()) {
                stopCapture(zone);
            }
        }

        if (storedXp.containsKey(player.getUniqueId())) {
            player.setExp(storedXp.get(player.getUniqueId()));
            storedXp.remove(player.getUniqueId());
        }
    }

    public void addEnemyToCaptureZone(Player player, CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        enemyPlayers.putIfAbsent(captureRegion, new HashSet<>());
        enemyPlayers.get(captureRegion).add(player);
    }

    public void removeEnemyFromCaptureZone(Player player, CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        if (enemyPlayers.containsKey(captureRegion)) {
            enemyPlayers.get(captureRegion).remove(player);
        }
    }

    private void startCapture(CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        int baseCaptureTime = zone.getCaptureTime();
        captureTimers.put(captureRegion, baseCaptureTime);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!capturingPlayers.containsKey(captureRegion) || capturingPlayers.get(captureRegion).isEmpty()) {
                    stopCapture(zone);
                    cancel();
                    return;
                }

                if (enemyPlayers.containsKey(captureRegion) && !enemyPlayers.get(captureRegion).isEmpty()) {
                    return;
                }

                int remainingTime = captureTimers.get(captureRegion) - 1;
                captureTimers.put(captureRegion, remainingTime);

                int progress = 100 - (remainingTime * 100 / baseCaptureTime);

                for (Player player : capturingPlayers.get(captureRegion)) {
                    ScoreboardUtils.updateCaptureProgress(zone.getMineRegion(), progress, true, player);
                }

                for (Player player : enemyPlayers.getOrDefault(captureRegion, new HashSet<>())) {
                    ScoreboardUtils.updateCaptureProgress(zone.getMineRegion(), progress, false, player);
                }

                if (remainingTime <= 0) {
                    completeCapture(zone);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private void stopCapture(CaptureZoneConfig.CaptureZone zone) {
        String captureRegion = zone.getCaptureRegion();
        captureTimers.remove(captureRegion);
        capturingPlayers.remove(captureRegion);
        enemyPlayers.remove(captureRegion);
    }

    private void completeCapture(CaptureZoneConfig.CaptureZone zone) {
        String mineRegion = zone.getMineRegion();
        String displayName = zone.getDisplayName();
        String newOwner = getPlayerTribe(capturingPlayers.get(zone.getCaptureRegion()).iterator().next());

        setMineOwner(mineRegion, newOwner);
        Bukkit.broadcastMessage("§e" + displayName + " has been captured by " + newOwner + "!");
        ScoreboardUtils.updateMineOwnershipDisplay(mineRegion, newOwner);
        stopCapture(zone);
    }

    private String getPlayerTribe(Player player) {
        File file = new File(plugin.getDataFolder() + "/playerdata", player.getUniqueId().toString() + ".yml");
        if (!file.exists()) {
            return "Lone Banished";
        }

        FileConfiguration playerData = YamlConfiguration.loadConfiguration(file);
        return playerData.getString("tribe", "Lone Banished");
    }
}
