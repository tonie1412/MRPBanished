package me.tonie.mrpbanished.config;

import me.tonie.mrpbanished.MRPBanished;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MiningConfig {
    private final MRPBanished plugin;
    private File miningFile;
    private FileConfiguration miningConfig;
    private final Map<Material, OreData> oreDataMap = new HashMap<>();

    public MiningConfig(MRPBanished plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        miningFile = new File(plugin.getDataFolder(), "mining.yml");
        if (!miningFile.exists()) {
            plugin.saveResource("mining.yml", false);
        }
        miningConfig = YamlConfiguration.loadConfiguration(miningFile);
        loadOres();
    }

    private void loadOres() {
        oreDataMap.clear();
        if (miningConfig.isConfigurationSection("ores")) {
            for (String key : miningConfig.getConfigurationSection("ores").getKeys(false)) {
                Material material = Material.matchMaterial(key.toUpperCase());
                if (material == null) continue;

                int miningTime = miningConfig.getInt("ores." + key + ".mining-time", 3);
                int regenTime = miningConfig.getInt("ores." + key + ".regen-time", 30);
                String dropRange = miningConfig.getString("ores." + key + ".drop-range", "1-2");
                List<String> pickaxes = miningConfig.getStringList("ores." + key + ".allowed-pickaxes");

                oreDataMap.put(material, new OreData(material, miningTime, regenTime, dropRange, pickaxes));
            }
        }
    }

    public OreData getOreData(Material material) {
        return oreDataMap.get(material);
    }

    public static class OreData {
        private final Material block;
        private final int miningTime;
        private final int regenTime;
        private final String dropRange;
        private final List<String> allowedPickaxes;

        public OreData(Material block, int miningTime, int regenTime, String dropRange, List<String> allowedPickaxes) {
            this.block = block;
            this.miningTime = miningTime;
            this.regenTime = regenTime;
            this.dropRange = dropRange;
            this.allowedPickaxes = allowedPickaxes;
        }

        public Material getBlock() {
            return block;
        }

        public int getMiningTime() {
            return miningTime;
        }

        public int getRegenTime() {
            return regenTime;
        }

        public String getDropRange() {
            return dropRange;
        }

        public List<String> getAllowedPickaxes() {
            return allowedPickaxes;
        }

        // ✅ Fix: Add missing method
        public List<String> getPickaxes() {
            return allowedPickaxes;
        }
    }
}
