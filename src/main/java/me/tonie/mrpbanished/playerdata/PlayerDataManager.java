package me.tonie.mrpbanished.playerdata;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Random;

public class PlayerDataManager {

    private final File playerDataFolder;

    public PlayerDataManager() {
        playerDataFolder = new File(Bukkit.getServer().getPluginManager().getPlugin("MRPBanished").getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public void savePlayerData(Player player, String profile, String tribe) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("current-profile", profile);
        config.set("profiles." + profile + ".inventory", player.getInventory().getContents());
        config.set("profiles." + profile + ".armor", player.getInventory().getArmorContents());
        config.set("profiles." + profile + ".enderchest", player.getEnderChest().getContents());
        config.set("profiles." + profile + ".xp-levels", player.getLevel());
        config.set("profiles." + profile + ".xp-points", player.getTotalExperience());
        config.set("profiles." + profile + ".luckperms-roles", getLuckPermsRoles(player));
        config.set("profiles." + profile + ".tribe", tribe);

        // Save RP attributes (age, description, RP name)
        config.set("profiles." + profile + ".setage", getRoleplayAttribute(player, profile, "setage"));
        config.set("profiles." + profile + ".setdesc", getRoleplayAttribute(player, profile, "setdesc"));
        config.set("profiles." + profile + ".rpname", getRoleplayAttribute(player, profile, "rpname"));

        try {
            config.save(file);
            Bukkit.getLogger().info("[MRPBanished] Saved data for profile '" + profile + "' of player: " + player.getName());
        } catch (IOException e) {
            Bukkit.getLogger().severe("[MRPBanished] Failed to save data for profile '" + profile + "' of player: " + player.getName());
            e.printStackTrace();
        }
    }

    public void loadPlayerData(Player player, String profile) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");

        if (!file.exists()) {
            Bukkit.getLogger().warning("[MRPBanished] No profile data found for " + player.getName() + ". Creating default profiles.");
            savePlayerData(player, "Glader", ""); // Create default
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("profiles." + profile)) {
            Bukkit.getLogger().warning("[MRPBanished] Profile '" + profile + "' missing for " + player.getName() + ". Defaulting to Glader.");
            profile = "Glader";
        }

        player.getInventory().setContents(config.getList("profiles." + profile + ".inventory").toArray(new ItemStack[0]));
        player.getInventory().setArmorContents(config.getList("profiles." + profile + ".armor").toArray(new ItemStack[0]));
        player.getEnderChest().setContents(config.getList("profiles." + profile + ".enderchest").toArray(new ItemStack[0]));
        player.setLevel(config.getInt("profiles." + profile + ".xp-levels"));
        player.setTotalExperience(config.getInt("profiles." + profile + ".xp-points"));
        setLuckPermsRoles(player, config.getStringList("profiles." + profile + ".luckperms-roles"));

        String tribe = config.getString("profiles." + profile + ".tribe", "none");
        Bukkit.getLogger().info("[MRPBanished] Loaded tribe '" + tribe + "' for player: " + player.getName());

        Bukkit.getLogger().info("[MRPBanished] Loaded data for profile '" + profile + "' of player: " + player.getName());
    }


    public String getCurrentProfile(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");
        if (!file.exists()) {
            return "Glader";
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("current-profile", "Glader");
    }

    public void setCurrentProfile(Player player, String profile) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("current-profile", profile);

        try {
            config.save(file);
            Bukkit.getLogger().info("[MRPBanished] Updated current profile for player: " + player.getName() + " to '" + profile + "'");
        } catch (IOException e) {
            Bukkit.getLogger().severe("[MRPBanished] Failed to update current profile for player: " + player.getName());
            e.printStackTrace();
        }
    }

    public void setRoleplayAttribute(Player player, String profile, String key, String value) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("profiles." + profile + "." + key, value);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRoleplayAttribute(Player player, String profile, String key) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");

        if (!file.exists()) {
            return "";
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("profiles." + profile + "." + key, "");
    }

    public Location getSpawnLocationForProfile(Player player, String profile) {
        FileConfiguration config = Bukkit.getPluginManager().getPlugin("MRPBanished").getConfig();
        String path = "spawns." + profile;

        // Handle Banished separately since it has multiple spawn options
        if (profile.equalsIgnoreCase("Banished")) {
            List<String> banishedSpawns = config.getStringList("spawns.Banished");
            if (!banishedSpawns.isEmpty()) {
                String randomSpawn = banishedSpawns.get(new Random().nextInt(banishedSpawns.size()));
                return parseLocationFromString(randomSpawn);
            }
        }

        // Check if the profile has a direct spawn (Glader or Tribes)
        if (config.contains(path + ".world")) {
            return parseLocation(
                    config.getString(path + ".world"),
                    config.getDouble(path + ".x"),
                    config.getDouble(path + ".y"),
                    config.getDouble(path + ".z")
            );
        }

        // Check if the player has a tribe and use the tribe spawn
        String tribe = config.getString("profiles." + profile + ".tribe", "none");
        if (!tribe.equals("none") && config.contains("spawns.Banished.tribes." + tribe + ".world")) {
            return parseLocation(
                    config.getString("spawns.Banished.tribes." + tribe + ".world"),
                    config.getDouble("spawns.Banished.tribes." + tribe + ".x"),
                    config.getDouble("spawns.Banished.tribes." + tribe + ".y"),
                    config.getDouble("spawns.Banished.tribes." + tribe + ".z")
            );
        }

        // Fallback to world spawn if no location is found
        Bukkit.getLogger().warning("[MRPBanished] No spawn locations found for profile '" + profile + "'. Using world spawn.");
        return player.getWorld().getSpawnLocation();
    }


    private Location parseLocationFromString(String locString) {
        String[] parts = locString.split(", ");
        if (parts.length == 4) {
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            );
        }
        return null;
    }


    private Location parseLocation(String world, double x, double y, double z) {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }


    private List<String> getLuckPermsRoles(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            return List.of();
        }

        return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());
    }

    private void setLuckPermsRoles(Player player, List<String> roles) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        user.data().clear();

        for (String role : roles) {
            user.data().add(InheritanceNode.builder(role).build());
        }

        luckPerms.getUserManager().saveUser(user);
    }

    public void applyProfileRoles(Player player, String profile) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        // Fetch roles specific to the profile
        List<String> profileRoles = Bukkit.getPluginManager().getPlugin("MRPBanished")
                .getConfig().getStringList("default-roles." + profile);

        // Fetch carry-over roles from the configuration
        List<String> carryOverRoles = Bukkit.getPluginManager().getPlugin("MRPBanished")
                .getConfig().getStringList("carry-over-roles");

        // Fetch restricted roles for the current profile
        List<String> restrictedRoles = Bukkit.getPluginManager().getPlugin("MRPBanished")
                .getConfig().getStringList("restricted-roles." + profile);

        // Fetch all current roles assigned to the player
        Set<String> currentRoles = getLuckPermsRoles(player).stream().collect(Collectors.toSet());

        // Add only the carry-over roles that the player already has
        List<String> validCarryOverRoles = carryOverRoles.stream()
                .filter(currentRoles::contains)
                .collect(Collectors.toList());

        // Determine roles to keep (profile roles + valid carry-over roles)
        List<String> rolesToKeep = profileRoles.stream().collect(Collectors.toList());
        rolesToKeep.addAll(validCarryOverRoles);

        // Remove restricted roles and roles not in rolesToKeep
        for (String role : currentRoles) {
            if (restrictedRoles.contains(role) || !rolesToKeep.contains(role)) {
                user.data().remove(InheritanceNode.builder(role).build());
            }
        }

        // Add roles from rolesToKeep that the player doesn't have yet
        for (String role : rolesToKeep) {
            if (!currentRoles.contains(role)) {
                user.data().add(InheritanceNode.builder(role).build());
            }
        }

        // Save changes to the LuckPerms user
        luckPerms.getUserManager().saveUser(user);
        Bukkit.getLogger().info("[MRPBanished] Applied roles for profile '" + profile + "' to player: " + player.getName());
    }

    public void initializeProfileData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(playerDataFolder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        boolean profilesCreated = false;

        if (!config.contains("profiles.Glader")) {
            config.set("profiles.Glader.inventory", player.getInventory().getContents());
            config.set("profiles.Glader.armor", player.getInventory().getArmorContents());
            config.set("profiles.Glader.enderchest", player.getEnderChest().getContents());
            config.set("profiles.Glader.xp-levels", player.getLevel());
            config.set("profiles.Glader.xp-points", player.getTotalExperience());
            config.set("profiles.Glader.luckperms-roles", getLuckPermsRoles(player));
            profilesCreated = true;
        }

        if (!config.contains("profiles.Banished")) {
            config.set("profiles.Banished.inventory", new ItemStack[0]);
            config.set("profiles.Banished.armor", new ItemStack[0]);
            config.set("profiles.Banished.enderchest", new ItemStack[0]);
            config.set("profiles.Banished.xp-levels", 0);
            config.set("profiles.Banished.xp-points", 0);
            config.set("profiles.Banished.luckperms-roles", List.of());
            profilesCreated = true;
        }

        // Ensure current profile is set to Glader if missing
        if (!config.contains("current-profile")) {
            config.set("current-profile", "Glader");
        }

        if (profilesCreated) {
            try {
                config.save(file);
                Bukkit.getLogger().info("[MRPBanished] Initialized profiles for player: " + player.getName());
            } catch (IOException e) {
                Bukkit.getLogger().severe("[MRPBanished] Failed to initialize profiles for player: " + player.getName());
                e.printStackTrace();
            }
        }
    }

    public String getActiveProfile(UUID playerUUID) {
        File file = new File(playerDataFolder, playerUUID + ".yml");

        if (!file.exists()) {
            return "Glader"; // Default profile
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("current-profile", "Glader");
    }

    public String getPlayerTribe(UUID playerUUID) {
        File file = new File(playerDataFolder, playerUUID + ".yml");

        if (!file.exists()) {
            return "none"; // Default to no tribe
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String profile = getActiveProfile(playerUUID);
        return config.getString("profiles." + profile + ".tribe", "none");
    }

}