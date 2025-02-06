package me.tonie.mrpbanished.commands.tribes;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TribeAssignCommand implements CommandExecutor {

    private MRPBanished plugin;
    private PlayerDataManager playerDataManager;

    public TribeAssignCommand(MRPBanished plugin) {
        this.plugin = plugin;
        this.playerDataManager = new PlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("mrpbanished.tribeassign"))) {
            sender.sendMessage("You do not have permission to use this command.");
            return false;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /tribeassign <playerName> <tribeName>");
            return false;
        }

        String playerName = args[0];
        String tribeName = args[1].toLowerCase();

        // Validate tribe name
        if (!tribeName.equals("north") && !tribeName.equals("south") && !tribeName.equals("east") && !tribeName.equals("west")) {
            sender.sendMessage("Invalid tribe name! Valid tribes are: north, south, east, west.");
            return false;
        }

        Player player = plugin.getServer().getPlayer(playerName);

        if (player == null) {
            sender.sendMessage("Player " + playerName + " is not online.");
            return false;
        }

        // Assign the correct tribe role
        String role = tribeName + "tribe";
        assignTribeRole(player, role);

        // Save the player's tribe in playerdata.yml (include tribe and profile name)
        playerDataManager.savePlayerData(player, tribeName, tribeName);

        return true;
    }

    // Assign the player to the correct tribe role using LuckPerms
    private void assignTribeRole(Player player, String role) {
        LuckPerms luckPerms = plugin.getLuckPerms();
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            // Use InheritanceNode for adding group roles
            Node groupNode = InheritanceNode.builder(role).build();
            user.data().add(groupNode);  // Add the group node (role) to the user
        });
    }
}
