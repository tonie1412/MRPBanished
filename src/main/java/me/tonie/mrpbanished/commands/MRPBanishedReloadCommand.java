package me.tonie.mrpbanished.commands;

import me.tonie.mrpbanished.MRPBanished;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MRPBanishedReloadCommand implements CommandExecutor {
    private final MRPBanished plugin;

    public MRPBanishedReloadCommand(MRPBanished plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || sender.isOp() || sender.hasPermission("mrpbanished.reload")) {
            plugin.reloadConfig();
            plugin.getMiningConfig().reload();
            plugin.getCaptureZoneConfig().reload();
            plugin.getMineCaptureManager().saveMineOwnership();
            plugin.getMiningManager().reloadBedrockOres();

            sender.sendMessage(ChatColor.GREEN + "MRPBanished configuration files have been reloaded!");
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
    }
}
