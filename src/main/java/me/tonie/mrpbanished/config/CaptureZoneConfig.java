package me.tonie.mrpbanished.config;

import me.tonie.mrpbanished.MRPBanished;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CaptureZoneConfig {
    private final MRPBanished plugin;
    private File file;
    private FileConfiguration config;

    private final Map<String, CaptureZone> captureZones = new HashMap<>();

    public CaptureZoneConfig(MRPBanished plugin) {
        this.plugin = plugin;
        reload(); // Load config on initialization
    }

    public void reload() {
        file = new File(plugin.getDataFolder(), "capturezones.yml");
        if (!file.exists()) {
            plugin.saveResource("capturezones.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadCaptureZones();
    }

    private void loadCaptureZones() {
        captureZones.clear();
        if (config.isConfigurationSection("capture-zones")) {
            for (String key : config.getConfigurationSection("capture-zones").getKeys(false)) {
                String captureRegion = config.getString("capture-zones." + key + ".capture-region");
                String mineRegion = config.getString("capture-zones." + key + ".mine-region");
                String displayName = config.getString("capture-zones." + key + ".display-name");
                int captureTime = config.getInt("capture-zones." + key + ".capture-time", 300);
                boolean captureEnabled = config.getBoolean("capture-zones." + key + ".capture-enabled", true);

                if (captureRegion != null && mineRegion != null && displayName != null) {
                    captureZones.put(key, new CaptureZone(captureRegion, mineRegion, displayName, captureTime, captureEnabled));
                }
            }
        }
    }

    public CaptureZone getCaptureZone(String key) {
        return captureZones.get(key);
    }

    public Map<String, CaptureZone> getAllCaptureZones() {
        return captureZones;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class CaptureZone {
        private final String captureRegion;
        private final String mineRegion;
        private final String displayName;
        private final int captureTime;
        private final boolean captureEnabled;

        public CaptureZone(String captureRegion, String mineRegion, String displayName, int captureTime, boolean captureEnabled) {
            this.captureRegion = captureRegion;
            this.mineRegion = mineRegion;
            this.displayName = displayName;
            this.captureTime = captureTime;
            this.captureEnabled = captureEnabled;
        }

        public String getCaptureRegion() {
            return captureRegion;
        }

        public String getMineRegion() {
            return mineRegion;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCaptureTime() {
            return captureTime;
        }

        public boolean isCaptureEnabled() {
            return captureEnabled;
        }
    }
}