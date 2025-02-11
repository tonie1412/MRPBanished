package me.tonie.mrpbanished;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import me.tonie.mrpbanished.api.BanishedAPI;
import me.tonie.mrpbanished.commands.MRPBanishedReloadCommand;
import me.tonie.mrpbanished.commands.profiles.ProfileCommand;
import me.tonie.mrpbanished.commands.profiles.ProfileCommandTabCompleter;
import me.tonie.mrpbanished.commands.tribes.TribeAssignCommand;
import me.tonie.mrpbanished.commands.tribes.TribeLeaveCommand;
import me.tonie.mrpbanished.listeners.MineCaptureListener;
import me.tonie.mrpbanished.listeners.PlayerJoinListener;
import me.tonie.mrpbanished.listeners.MiningListener;
import me.tonie.mrpbanished.managers.MineCaptureManager;
import me.tonie.mrpbanished.managers.MiningManager;
import me.tonie.mrpbanished.playerdata.PlayerDataManager;
import me.tonie.mrpbanished.config.CaptureZoneConfig;
import me.tonie.mrpbanished.config.MiningConfig;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MRPBanished extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private LuckPerms luckPerms;
    private CaptureZoneConfig captureZoneConfig;
    private MiningConfig miningConfig;
    private MineCaptureManager mineCaptureManager;
    private MiningManager miningManager;
    private WorldGuardPlugin worldGuard;
    private static StateFlag MINEABLE_FLAG;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Load LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            getLogger().warning("LuckPerms not found! Some features may not work.");
        }

        // Initialize managers and configurations
        playerDataManager = new PlayerDataManager();
        captureZoneConfig = new CaptureZoneConfig(this);
        miningConfig = new MiningConfig(this);
        mineCaptureManager = new MineCaptureManager(this);
        miningManager = new MiningManager(this);

        // Load WorldGuard safely
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            getLogger().info("WorldGuard detected! Preparing custom flags...");
            // Delay flag registration to ensure WorldGuard is fully loaded
            Bukkit.getScheduler().runTaskLater(this, this::registerWorldGuardFlag, 40L); // 2-second delay
        } else {
            getLogger().warning("WorldGuard is not installed! 'mineable' flag will not be registered.");
        }

        miningManager.reloadBedrockOres();

        // Register commands
        Bukkit.getPluginCommand("profile").setExecutor(new ProfileCommand(playerDataManager, this));
        Bukkit.getPluginCommand("profile").setTabCompleter(new ProfileCommandTabCompleter());
        Bukkit.getPluginCommand("tribeassign").setExecutor(new TribeAssignCommand(this));
        Bukkit.getPluginCommand("tribeleave").setExecutor(new TribeLeaveCommand(this));
        Bukkit.getPluginCommand("mrpbanishedreload").setExecutor(new MRPBanishedReloadCommand(this));

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MineCaptureListener(this, mineCaptureManager, captureZoneConfig), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this, miningConfig, worldGuard), this);

        // Register API last to ensure all dependencies are initialized
        new BanishedAPI(this);
    }

    private void registerWorldGuardFlag() {
        try {
            FlagRegistry registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
            if (registry.get("mineable") == null) {
                MINEABLE_FLAG = new StateFlag("mineable", false);
                registry.register(MINEABLE_FLAG);
                getLogger().info("[MRPBanished] Successfully registered 'mineable' flag.");
            } else {
                getLogger().info("[MRPBanished] The 'mineable' flag is already registered.");
            }
        } catch (Exception e) {
            getLogger().severe("[MRPBanished] Failed to register 'mineable' flag!");
            e.printStackTrace();
        }
    }

    public static StateFlag getMineableFlag() {
        return MINEABLE_FLAG;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CaptureZoneConfig getCaptureZoneConfig() {
        return captureZoneConfig;
    }

    public MiningConfig getMiningConfig() {
        return miningConfig;
    }

    public MineCaptureManager getMineCaptureManager() {
        return mineCaptureManager;
    }

    public MiningManager getMiningManager() {
        return miningManager;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug-mode")) {
            getLogger().info("[MRPBanished DEBUG] " + message);
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
