package me.tonie.mrpbanished.commands.tribes;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.profiles.OnlineProfile;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TribeBetrayCommand implements CommandExecutor {

    private final MRPBanished plugin;
    private final PlayerDataManager playerDataManager;
    private final BetonQuest betonQuestAPI;
    private final Map<UUID, UUID> betrayalRequests = new HashMap<>(); // Map to store betrayal requests

    public TribeBetrayCommand(MRPBanished plugin) {
        this.plugin = plugin;
        this.playerDataManager = new PlayerDataManager();
        // Get the BetonQuest plugin instance
        this.betonQuestAPI = plugin.getServer().getPluginManager().getPlugin("BetonQuest") instanceof BetonQuest ? (BetonQuest) plugin.getServer().getPluginManager().getPlugin("BetonQuest") : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mrpbanished.tribebetray")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /tribebetray <playerName>");
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = plugin.getServer().getPlayer(targetName);

        if (targetPlayer == null) {
            player.sendMessage("Player " + targetName + " is not online.");
            return true;
        }

        String currentTribe = playerDataManager.getCurrentProfile(player);
        String targetTribe = playerDataManager.getCurrentProfile(targetPlayer);

        // Ensure players are in different tribes
        if (currentTribe.equals(targetTribe)) {
            player.sendMessage("You cannot betray a player from the same tribe.");
            return true;
        }

        // Send the betrayal request
        betrayalRequests.put(targetPlayer.getUniqueId(), player.getUniqueId());
        targetPlayer.sendMessage(player.getName() + " has sent you a betrayal request. Type /betrayaccept to accept.");

        return true;
    }

    // Command to accept the betrayal
    public boolean onBetrayAcceptCommand(Player player) {
        UUID targetUuid = betrayalRequests.get(player.getUniqueId());

        if (targetUuid == null) {
            player.sendMessage("You have no pending betrayal requests.");
            return false;
        }

        Player targetPlayer = plugin.getServer().getPlayer(targetUuid);
        if (targetPlayer == null) {
            player.sendMessage("The player who sent you the request is no longer online.");
            return false;
        }

        // Start tracking the betrayal with the BETRAYING<TRIBE> tag
        String currentTribe = playerDataManager.getCurrentProfile(player);
        sendBetrayalTag(player, currentTribe); // Assign BETRAYING<TRIBE> tag based on the player's current tribe

        player.sendMessage("You have accepted the betrayal request. You must complete the betrayal dungeon.");

        return true;
    }

    // Method to assign the BETRAYING<TRIBE> tag to the player using BetonQuest API
    private void sendBetrayalTag(Player player, String currentTribe) {
        if (betonQuestAPI != null) {
            String tag = "BETRAYING" + currentTribe.toUpperCase(); // BETRAYING<TRIBE> (e.g., BETRAYINGWEST, BETRAYINGNORTH)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Assign the tag to the player via BetonQuest API
                betonQuestAPI.getPlayerData((OnlineProfile) player).addTag(tag);
                plugin.getLogger().info("[MRPBanished] Player " + player.getName() + " has been assigned the tag: " + tag);
            });
        } else {
            plugin.getLogger().warning("BetonQuest plugin not found, unable to assign betrayal tag.");
        }
    }

    // /tribebetraycomplete command that only console can run
    public boolean onCompleteBetrayalCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !sender.hasPermission("mrpbanished.tribebetray.complete")) {
            sender.sendMessage("You do not have permission to run this command.");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /tribebetraycomplete <playerName>");
            return false;
        }

        String playerName = args[0];
        Player player = plugin.getServer().getPlayer(playerName);

        if (player == null) {
            sender.sendMessage("Player " + playerName + " is not online.");
            return false;
        }

        // Complete the betrayal and assign them to the betrayer's tribe
        UUID betrayerUuid = betrayalRequests.get(player.getUniqueId());
        if (betrayerUuid == null) {
            sender.sendMessage("Player " + playerName + " has no active betrayal request.");
            return false;
        }

        Player betrayer = plugin.getServer().getPlayer(betrayerUuid);
        if (betrayer == null) {
            sender.sendMessage("The betrayer is no longer online.");
            return false;
        }

        String betrayerTribe = playerDataManager.getCurrentProfile(betrayer);

        // Assign player to the betrayer's tribe
        playerDataManager.savePlayerData(player, playerDataManager.getCurrentProfile(player), betrayerTribe); // Set the player's new tribe
        betrayalRequests.remove(player.getUniqueId()); // Remove betrayal request

        // Remove the BETRAYING<TRIBE> tag
        removeBetrayalTag(player);

        player.sendMessage("You have successfully betrayed your tribe and joined " + betrayerTribe + " tribe.");
        sender.sendMessage("Betrayal for " + playerName + " has been completed. They are now in the " + betrayerTribe + " tribe.");

        return true;
    }

    // Remove the betrayal tag from the player
    private void removeBetrayalTag(Player player) {
        if (betonQuestAPI != null) {
            String currentTribe = playerDataManager.getCurrentProfile(player);
            String tag = "BETRAYING" + currentTribe.toUpperCase(); // BETRAYING<TRIBE>
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Remove the betrayal tag from the player using BetonQuest API
                betonQuestAPI.getPlayerData((OnlineProfile) player).removeTag(tag);
                plugin.getLogger().info("[MRPBanished] Player " + player.getName() + " has had their betrayal tag removed.");
            });
        }
    }
}
