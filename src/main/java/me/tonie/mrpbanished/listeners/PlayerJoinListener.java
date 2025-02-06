package me.tonie.mrpbanished.listeners;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final PlayerDataManager dataManager;

    public PlayerJoinListener(MRPBanished plugin) {
        this.dataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initialize both profiles for the player if not already created
        dataManager.initializeProfileData(event.getPlayer());
        // Load the player into the Glader profile
        dataManager.loadPlayerData(event.getPlayer(), "Glader");
    }
}
