package me.tonie.mrpbanished.commands.tribes;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TribeLeaveCommand implements CommandExecutor {

    private final MRPBanished plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> leaveRequests = new HashMap<>(); // Map to track players who requested to leave

    public TribeLeaveCommand(MRPBanished plugin) {
        this.plugin = plugin;
        this.playerDataManager = new PlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // First, check if the player has initiated a leave request before
        if (leaveRequests.containsKey(player.getUniqueId())) {
            long requestTime = leaveRequests.get(player.getUniqueId());
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - requestTime;

            // If it's within 30 seconds, proceed with the leave
            if (timeDiff <= 30000) { // 30 seconds window
                // Remove the player's tribe role
                removeTribeRole(player);

                // Set the player's tribe to "Lone Banished"
                String currentProfile = playerDataManager.getCurrentProfile(player);
                playerDataManager.savePlayerData(player, currentProfile, "Lone Banished");

                // Clear the leave request
                leaveRequests.remove(player.getUniqueId());

                player.sendMessage("You have successfully left your tribe and are now 'Lone Banished'.");
                return true;
            } else {
                player.sendMessage("Your leave request has expired. Please type /tribeleave again to confirm.");
                leaveRequests.remove(player.getUniqueId()); // Expired, so we remove the request
                return false;
            }
        }

        // If no leave request is pending, initiate one
        leaveRequests.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage("You have initiated the leave request. Type /tribeleave again within 30 seconds to confirm.");
        return true;
    }

    // Remove the player's current tribe role using LuckPerms
    private void removeTribeRole(Player player) {
        LuckPerms luckPerms = plugin.getLuckPerms();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user != null) {
            // Fetch the current roles and remove the tribe role
            for (Node node : user.getNodes()) {
                if (node instanceof InheritanceNode) {
                    String groupName = ((InheritanceNode) node).getGroupName();
                    if (groupName.endsWith("tribe")) { // Identifies tribe roles like northtribe, southtribe, etc.
                        user.data().remove(node);
                    }
                }
            }

            // Save the updated user data after removing the tribe role
            luckPerms.getUserManager().saveUser(user);
        }
    }
}
