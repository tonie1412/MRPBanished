package me.tonie.mrpbanished.listeners;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import me.tonie.mrpbanished.MRPBanished;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.sk89q.worldguard.session.MoveType;

public class MineableFlagHandler extends FlagValueChangeHandler<StateFlag.State> {

    public static final Factory FACTORY = new Factory();

    public static class Factory extends Handler.Factory<MineableFlagHandler> {
        @Override
        public MineableFlagHandler create(Session session) {
            return new MineableFlagHandler(session);
        }
    }

    public MineableFlagHandler(Session session) {
        super(session, MRPBanished.getMineableFlag());
    }

    @Override
    protected void onInitialValue(LocalPlayer localPlayer, ApplicableRegionSet applicableRegionSet, StateFlag.State state) {

    }

    @Override
    protected boolean onSetValue(LocalPlayer localPlayer, Location from, Location to, ApplicableRegionSet regionSet, StateFlag.State oldValue, StateFlag.State newValue, MoveType moveType) {
        handleMineableFlag(localPlayer, newValue);
        return false;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer localPlayer, Location location, Location location1, ApplicableRegionSet applicableRegionSet, StateFlag.State state, MoveType moveType) {
        return false;
    }

    private void handleMineableFlag(LocalPlayer localPlayer, StateFlag.State state) {
        if (localPlayer != null) {
            Player player = Bukkit.getPlayer(localPlayer.getUniqueId()); // Convert LocalPlayer to Bukkit Player
                }
            }
        }
