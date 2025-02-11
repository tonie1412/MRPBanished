package me.tonie.mrpbanished;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import me.tonie.mrpbanished.api.BanishedAPI;
import me.tonie.mrpbanished.commands.MRPBanishedReloadCommand;
import me.tonie.mrpbanished.commands.profiles.ProfileCommand;
import me.tonie.mrpbanished.commands.profiles.ProfileCommandTabCompleter;
import me.tonie.mrpbanished.commands.tribes.TribeAssignCommand;
import me.tonie.mrpbanished.commands.tribes.TribeLeaveCommand;
import me.tonie.mrpbanished.listeners.MineCaptureListener;
import me.tonie.mrpbanished.listeners.PlayerJoinListener;
import me.tonie.mrpbanished.listeners.MiningListener;
import me.tonie.mrpbanished.listeners.MineableFlagHandler;
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

    public static StateFlag MINEABLE_FLAG;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            getLogger().info("[MRPBanished] Registering 'mineable' flag with WorldGuard...");
            registerWorldGuardFlag();
        } else {
            getLogger().warning("[MRPBanished] WorldGuard is not installed! 'mineable' flag will not be registered.");
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            getLogger().warning("LuckPerms not found! Some features may not work.");
        }

        playerDataManager = new PlayerDataManager();
        captureZoneConfig = new CaptureZoneConfig(this);
        miningConfig = new MiningConfig(this);
        mineCaptureManager = new MineCaptureManager(this);
        miningManager = new MiningManager(this);

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            getLogger().info("WorldGuard detected! Registering flag handler...");
            Bukkit.getScheduler().runTaskLater(this, this::registerWorldGuardHandlers, 40L);
        } else {
            getLogger().warning("WorldGuard is not installed! Some features may not work.");
        }

        miningManager.reloadBedrockOres();

        Bukkit.getPluginCommand("profile").setExecutor(new ProfileCommand(playerDataManager, this));
        Bukkit.getPluginCommand("profile").setTabCompleter(new ProfileCommandTabCompleter());
        Bukkit.getPluginCommand("tribeassign").setExecutor(new TribeAssignCommand(this));
        Bukkit.getPluginCommand("tribeleave").setExecutor(new TribeLeaveCommand(this));
        Bukkit.getPluginCommand("mrpbanishedreload").setExecutor(new MRPBanishedReloadCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MineCaptureListener(this, mineCaptureManager, captureZoneConfig), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this, miningConfig, worldGuard), this);

        new BanishedAPI(this);
    }

    private void registerWorldGuardFlag() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            Flag<?> existing = registry.get("mineable");
            if (existing instanceof StateFlag) {
                MINEABLE_FLAG = (StateFlag) existing;
                getLogger().info("[MRPBanished] Using existing 'mineable' flag.");
            } else {
                StateFlag flag = new StateFlag("mineable", false);
                registry.register(flag);
                MINEABLE_FLAG = flag;
                getLogger().info("[MRPBanished] Successfully registered 'mineable' flag.");
            }
        } catch (FlagConflictException e) {
            getLogger().severe("[MRPBanished] Another plugin already registered the 'mineable' flag!");
            e.printStackTrace();
        } catch (Exception e) {
            getLogger().severe("[MRPBanished] Failed to register 'mineable' flag!");
            e.printStackTrace();
        }
    }

    private void registerWorldGuardHandlers() {
        try {
            SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
            sessionManager.registerHandler(MineableFlagHandler.FACTORY, null);
            getLogger().info("[MRPBanished] Registered WorldGuard 'mineable' flag handler.");
        } catch (Exception e) {
            getLogger().severe("[MRPBanished] Failed to register WorldGuard handler for 'mineable' flag!");
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
