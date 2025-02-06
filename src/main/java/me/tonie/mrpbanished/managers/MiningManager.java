package me.tonie.mrpbanished.managers;

import me.tonie.mrpbanished.MRPBanished;
import me.tonie.mrpbanished.config.MiningConfig;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MiningManager {
    private final MRPBanished plugin;
    private final Random random = new Random();
    private final File bedrockFile;
    private final FileConfiguration bedrockConfig;
    private final Map<UUID, MiningTask> activeMiners = new HashMap<>();

    public MiningManager(MRPBanished plugin) {
        this.plugin = plugin;
        bedrockFile = new File(plugin.getDataFolder(), "bedrockores.yml");
        if (!bedrockFile.exists()) {
            plugin.saveResource("bedrockores.yml", false);
        }
        bedrockConfig = YamlConfiguration.loadConfiguration(bedrockFile);
    }

    public void startMining(Player player, Block block, MiningConfig.OreData oreData) {
        int miningTime = oreData.getMiningTime() * 20; // Convert seconds to ticks
        UUID playerUUID = player.getUniqueId();

        // Cancel existing mining task if player starts a new one
        if (activeMiners.containsKey(playerUUID)) {
            activeMiners.get(playerUUID).cancelMining();
        }

        MiningTask task = new MiningTask(player, block, oreData, miningTime);
        activeMiners.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 5); // Runs every 5 ticks (0.25s)
    }

    private class MiningTask extends BukkitRunnable {
        private final Player player;
        private final Block block;
        private final MiningConfig.OreData oreData;
        private final int totalTime;
        private int ticksElapsed = 0;
        private final Location initialLocation;

        public MiningTask(Player player, Block block, MiningConfig.OreData oreData, int totalTime) {
            this.player = player;
            this.block = block;
            this.oreData = oreData;
            this.totalTime = totalTime;
            this.initialLocation = player.getLocation().clone();

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, totalTime, 1));
        }

        @Override
        public void run() {
            if (ticksElapsed >= totalTime) {
                completeMining();
                cancel();
                return;
            }

            // Check if player moved or looked away
            if (hasPlayerMoved() || hasPlayerLookedAway()) {
                cancelMining();
                return;
            }

            // Update XP bar progress
            player.setExp((float) ticksElapsed / totalTime);

            // Play mining sound effect every second
            if (ticksElapsed % 20 == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.8f, 1.0f);
            }

            // Play mining particle effect
            player.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 10, block.getBlockData());

            ticksElapsed += 5;
        }

        private boolean hasPlayerMoved() {
            return !player.getLocation().getBlock().equals(initialLocation.getBlock());
        }

        private boolean hasPlayerLookedAway() {
            Location eyeLocation = player.getEyeLocation();
            double dotProduct = eyeLocation.getDirection().normalize()
                    .dot(block.getLocation().toVector().subtract(eyeLocation.toVector()).normalize());

            return dotProduct <= 0.98;
        }

        private void completeMining() {
            block.setType(Material.BEDROCK);
            saveBedrockOre(block);

            int dropAmount = random.nextInt(oreData.getDropRange().equals("1-2") ? 2 : 1) + 1;
            player.getInventory().addItem(new ItemStack(oreData.getBlock(), dropAmount));

            // Play success sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

            // Regenerate the ore after a set time
            new BukkitRunnable() {
                @Override
                public void run() {
                    regenerateOre(block, oreData.getBlock());
                }
            }.runTaskLater(plugin, oreData.getRegenTime() * 20);

            activeMiners.remove(player.getUniqueId());
        }

        public void cancelMining() {
            player.sendMessage("§cMining interrupted!");
            player.setExp(0);
            player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            activeMiners.remove(player.getUniqueId());
            cancel();
        }
    }

    private void saveBedrockOre(Block block) {
        String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        bedrockConfig.set("bedrock_ores." + locKey, block.getType().name());

        try {
            bedrockConfig.save(bedrockFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeBedrockOre(Block block) {
        String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        bedrockConfig.set("bedrock_ores." + locKey, null);

        try {
            bedrockConfig.save(bedrockFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void regenerateOre(Block block, Material originalOre) {
        block.setType(originalOre);
        removeBedrockOre(block);
    }

    public void reloadBedrockOres() {
        if (bedrockConfig.isConfigurationSection("bedrock_ores")) {
            for (String locKey : bedrockConfig.getConfigurationSection("bedrock_ores").getKeys(false)) {
                String[] parts = locKey.split(",");
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;

                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Material oreType = Material.matchMaterial(bedrockConfig.getString("bedrock_ores." + locKey));

                if (oreType != null) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(oreType);
                    removeBedrockOre(block);
                }
            }
        }
    }
}
