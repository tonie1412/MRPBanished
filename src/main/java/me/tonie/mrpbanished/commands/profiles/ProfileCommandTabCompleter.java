package me.tonie.mrpbanished.commands.profiles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest online player names for the first argument (username)
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // Suggest available profile options and commands for the second argument
            return Arrays.asList("Glader", "Banished", "resetcooldown", "setage", "setdesc", "rpname");
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("resetcooldown"))) {
            // Suggest online player names for the resetcooldown subcommand
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }

        return null; // No further suggestions
    }
}
