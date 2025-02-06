package me.tonie.mrpbanished.commands.profiles;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.api.events.PlayerProfileChangeEvent;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileCommand implements CommandExecutor {

    private final PlayerDataManager dataManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownTime;

    public ProfileCommand(PlayerDataManager dataManager, MRPBanished plugin) {
        this.dataManager = dataManager;
        this.cooldownTime = plugin.getConfig().getLong("cooldown-time") * 1000; // Convert seconds to milliseconds
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(translate("&6[&aMazeRP&6] &cUsage: /profile <player> <Glader|Banished|resetcooldown|setage|setdesc|rpname>"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            sender.sendMessage(translate("&6[&aMazeRP&6] &cPlayer '" + targetName + "' is not online."));
            return true;
        }

        String subCommand = args[1].toLowerCase();

        // Handle resetting cooldowns
        if (subCommand.equals("resetcooldown")) {
            return handleResetCooldown(sender, targetPlayer);
        }

        // Handle setting age, description, or RP name
        if (subCommand.equals("setage") || subCommand.equals("setdesc") || subCommand.equals("rpname")) {
            return handleProfileSubcommands(targetPlayer, subCommand, args);
        }

        // Handle profile switching
        if (subCommand.equals("glader") || subCommand.equals("banished")) {
            return handleProfileSwitch(sender, targetPlayer, subCommand);
        }

        sender.sendMessage(translate("&6[&aMazeRP&6] &cInvalid subcommand."));
        return true;
    }

    private boolean handleProfileSwitch(CommandSender sender, Player player, String targetProfile) {
        UUID uuid = player.getUniqueId();
        final String finalTargetProfile = capitalize(targetProfile);

        final String currentProfile = dataManager.getCurrentProfile(player);
        if (currentProfile.equals(finalTargetProfile)) {
            sender.sendMessage(translate("&6[&aMazeRP&6] &e" + player.getName() + " is already in the " + finalTargetProfile + " profile."));
            return true;
        }

        // Cooldown check
        if (cooldowns.containsKey(uuid) && (System.currentTimeMillis() - cooldowns.get(uuid)) < cooldownTime) {
            if (!sender.hasPermission("mrpbanished.admin")) { // Allow admins to bypass cooldown
                long timeLeft = (cooldownTime - (System.currentTimeMillis() - cooldowns.get(uuid))) / 1000;
                sender.sendMessage(translate("&6[&aMazeRP&6] &c" + player.getName() + " must wait " + timeLeft + " seconds before switching profiles again."));
                return true;
            }
        }

        // Apply blindness effect and sound
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);

        // Delay to simulate transition
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MRPBanished"), () -> {
            // Save current profile data
            dataManager.savePlayerData(player, currentProfile, "Lone Banished");

            // Clear player state
            player.getInventory().clear();
            player.getEnderChest().clear();
            player.setLevel(0);
            player.setTotalExperience(0);

            // **Trigger PlayerProfileChangeEvent BEFORE changing profile**
            Bukkit.getPluginManager().callEvent(new PlayerProfileChangeEvent(uuid, currentProfile, finalTargetProfile));

            // Load new profile
            dataManager.setCurrentProfile(player, finalTargetProfile);
            dataManager.loadPlayerData(player, finalTargetProfile);

            // Apply roles
            dataManager.applyProfileRoles(player, finalTargetProfile);

            // Restore Roleplay Attributes
            restoreRoleplaySettings(player, finalTargetProfile);

            // Teleport player based on profile
            teleportToProfileSpawn(player, finalTargetProfile);

            // Play completion sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Update cooldown
            cooldowns.put(uuid, System.currentTimeMillis());

            sender.sendMessage(translate("&6[&aMazeRP&6] &a" + player.getName() + " switched to the " + finalTargetProfile + " profile."));
            player.sendMessage(translate("&6[&aMazeRP&6] &aYou have switched to the " + finalTargetProfile + " profile."));
            Bukkit.getLogger().info("[MRPBanished] Player " + player.getName() + " switched to profile: " + finalTargetProfile);
        }, 40L);

        return true;
    }


    private void teleportToProfileSpawn(Player player, String profile) {
        Location spawnLocation = dataManager.getSpawnLocationForProfile(player, profile);
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
            player.sendMessage(translate("&6[&aMazeRP&6] &aYou have been teleported to your profile's spawn location."));
        } else {
            player.sendMessage(translate("&6[&aMazeRP&6] &cNo spawn point found for this profile! Contact an admin."));
        }
    }

    private boolean handleProfileSubcommands(Player player, String subCommand, String[] args) {
        if (args.length < 3) {
            player.sendMessage(translate("&6[&aMazeRP&6] &cUsage: /profile <player> <Glader|Banished> " + subCommand + " <value>"));
            return true;
        }

        String value = String.join(" ", args).substring(args[0].length() + args[1].length() + subCommand.length() + 3);

        String profile = dataManager.getCurrentProfile(player);
        dataManager.setRoleplayAttribute(player, profile, subCommand, value);

        player.sendMessage(translate("&6[&aMazeRP&6] &aYour " + subCommand + " has been updated for the " + profile + " profile!"));
        return true;
    }

    private void restoreRoleplaySettings(Player player, String profile) {
        String age = dataManager.getRoleplayAttribute(player, profile, "setage");
        String desc = dataManager.getRoleplayAttribute(player, profile, "setdesc");
        String rpname = dataManager.getRoleplayAttribute(player, profile, "rpname");

        if (!age.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setage " + player.getName() + " " + age);
        }
        if (!desc.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setdesc " + player.getName() + " " + desc);
        }
        if (!rpname.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rpname " + player.getName() + " " + rpname);
        }
    }

    private boolean handleResetCooldown(CommandSender sender, Player targetPlayer) {
        if (!sender.hasPermission("mrpbanished.admin")) {
            sender.sendMessage(translate("&6[&aMazeRP&6] &cYou do not have permission to reset cooldowns."));
            return true;
        }

        cooldowns.remove(targetPlayer.getUniqueId());
        sender.sendMessage(translate("&6[&aMazeRP&6] &aCooldown for " + targetPlayer.getName() + " has been reset."));
        return true;
    }

    private String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
