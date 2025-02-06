package me.tonie.mrpbanished.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public class PlayerProfileChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID playerUUID;
    private final String oldProfile;
    private final String newProfile;

    public PlayerProfileChangeEvent(UUID playerUUID, String oldProfile, String newProfile) {
        this.playerUUID = playerUUID;
        this.oldProfile = oldProfile;
        this.newProfile = newProfile;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getOldProfile() { return oldProfile; }
    public String getNewProfile() { return newProfile; }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public HandlerList getHandlers() { return handlers; }
}
