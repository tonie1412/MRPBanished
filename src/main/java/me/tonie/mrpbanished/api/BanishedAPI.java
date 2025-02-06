package me.tonie.mrpbanished.api;

import me.tonie.mrpbanished.MRPBanished;
import java.util.UUID;

public class BanishedAPI {
    private static MRPBanished plugin;

    // Initialize API with plugin instance
    public BanishedAPI(MRPBanished instance) {
        plugin = instance;
    }

    /**
     * Get the active profile of a player.
     * @param playerUUID The UUID of the player.
     * @return The active profile name.
     */
    public static String getActiveProfile(UUID playerUUID) {
        return plugin.getPlayerDataManager().getActiveProfile(playerUUID);
    }

    /**
     * Get the player's current tribe.
     * @param playerUUID The UUID of the player.
     * @return The name of the player's tribe.
     */
    public static String getPlayerTribe(UUID playerUUID) {
        return plugin.getPlayerDataManager().getPlayerTribe(playerUUID);
    }
}
