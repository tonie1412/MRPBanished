package me.tonie.mrpbanished.utils;

import me.tonie.mrpbanished.config.CaptureZoneConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardUtils {
    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private static final Map<String, Integer> captureProgress = new HashMap<>();

    public static void showMineScoreboard(Player player, CaptureZoneConfig.CaptureZone zone, String owningTribe) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("MineInfo", "dummy", "§e" + zone.getDisplayName());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score mineName = objective.getScore("§6Mine: §f" + zone.getDisplayName());
        mineName.setScore(3);

        Score owner = objective.getScore("§bOwned by: §f" + owningTribe);
        owner.setScore(2);

        Score captureStatus = objective.getScore("§aCapturable: §f" + (zone.isCaptureEnabled() ? "Yes" : "No"));
        captureStatus.setScore(1);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
    }

    public static void updateCaptureProgress(String mineRegion, int progress, boolean isCapturing, Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("MineInfo");

        if (objective == null) return;

        scoreboard.resetScores("§cCapture Progress: §f" + progress + "%");
        scoreboard.resetScores("§cTime Left: §f" + (100 - progress) + "%");

        if (isCapturing) {
            Score progressScore = objective.getScore("§cCapture Progress: §f" + progress + "%");
            progressScore.setScore(1);
        } else {
            Score defenseScore = objective.getScore("§cTime Left: §f" + (100 - progress) + "%");
            defenseScore.setScore(1);
        }
    }

    public static void removeMineScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        playerScoreboards.remove(player.getUniqueId());
    }

    public static void updateMineOwnershipDisplay(String mineRegion, String newOwner) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerScoreboards.containsKey(player.getUniqueId())) {
                Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
                Objective objective = scoreboard.getObjective("MineInfo");

                if (objective != null) {
                    // Reset old owner score and update it
                    scoreboard.resetScores("§bOwned by: §f" + newOwner);
                    Score newOwnerScore = objective.getScore("§bOwned by: §f" + newOwner);
                    newOwnerScore.setScore(3);
                }
            }
        }
    }

}
