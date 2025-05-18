package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class MobManager {

    private final JavaPlugin plugin;
    private final Map<String, MobDataModel> mobData = new HashMap<>();

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllMobs() {
        mobData.clear();
        Logger log = plugin.getLogger();

        File folder = new File(plugin.getDataFolder(), "mobLevelsConfiguration");
        if (!folder.exists() && !folder.mkdirs()) {
            log.warning("Could not create mobLevelsConfiguration/ directory.");
            return;
        }

        // 🔁 Dynamically copy missing .yml files from inside the JAR
        extractDefaultsFromJar();

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("No mob files found in mobLevelsConfiguration/");
            return;
        }

        for (File file : files) {
            String mobKey = file.getName().replace(".yml", "");
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                boolean enabled = yaml.getBoolean("Spawner-Enabled", false);
                boolean customWorlds = yaml.getBoolean("Custom-Whitelisted-Worlds", false);
                List<String> worlds = yaml.getStringList("Worlds");

                Object activationRange = yaml.get("Player-Activation-Range");
                Object disableAI = yaml.get("Disable-Mob-AI");
                Object spawnersPerChunk = yaml.get("Allowed-Spawners-Per-Chunk");

                boolean overrideDisplay = yaml.getBoolean("OverrideDefaultDisplay", false);
                String displayName = yaml.getString("DisplayName");
                List<String> displayLore = yaml.getStringList("DisplayLore");

                // 🧱 Requirements
                SpawnerRequirementsModel req = new SpawnerRequirementsModel();
                ConfigurationSection reqSection = yaml.getConfigurationSection("SpawnerRequirements");
                if (reqSection != null) {
                    req.air = reqSection.getBoolean("Air", false);
                    req.aboveSeaLevel = reqSection.getBoolean("Above-Sea-Level", false);
                    req.belowSeaLevel = reqSection.getBoolean("Below-Sea-Level", false);
                    req.aboveYAxis = reqSection.getBoolean("Above-Y-Axis", false);
                    req.belowYAxis = reqSection.getBoolean("Below-Y-Axis", false);
                    req.darkness = reqSection.getBoolean("Darkness", false);
                    req.totalDarkness = reqSection.getBoolean("Total-Darkness", false);
                    req.light = reqSection.getBoolean("Light", false);
                    req.maxEntitiesNearby = reqSection.getBoolean("Max-Entities-Nearby", false);
                    req.fluid = reqSection.getBoolean("Fluid", false);
                    req.inBiome = reqSection.getBoolean("InBiome", false);
                    req.allowedBiomes = reqSection.getStringList("AllowedBiomes");
                    req.onGround = reqSection.getBoolean("On-Ground", false);
                    req.onBlock = reqSection.getBoolean("On-Block", false);
                    req.allowedBlocks = reqSection.getStringList("AllowedBlocks");
                }

                // 🔁 Levels
                Map<Integer, SpawnerLevelModel> levels = new LinkedHashMap<>();
                ConfigurationSection levelsSection = yaml.getConfigurationSection("Spawner-Levels");
                if (levelsSection != null) {
                    for (String levelKey : levelsSection.getKeys(false)) {
                        ConfigurationSection levelSec = levelsSection.getConfigurationSection(levelKey);
                        if (levelSec == null) continue;

                        SpawnerLevelModel level = new SpawnerLevelModel();
                        level.spawnerDelay = levelSec.get("SpawnerDelay");
                        level.mobCount = levelSec.get("MobCount");
                        level.xpDrop = levelSec.get("XPDrop");
                        level.vanillaDrops = levelSec.getBoolean("VanillaDrops", false);
                        level.costToUpgrade = levelSec.get("CostToUpgrade");

                        List<CustomDropModel> drops = new ArrayList<>();
                        ConfigurationSection dropSec = levelSec.getConfigurationSection("CustomDrops");
                        if (dropSec != null) {
                            for (String dropKey : dropSec.getKeys(false)) {
                                ConfigurationSection d = dropSec.getConfigurationSection(dropKey);
                                if (d == null) continue;

                                CustomDropModel drop = new CustomDropModel();
                                drop.material = d.getString("Material");
                                drop.amount = d.getInt("Amount", 1);
                                drop.dropPercent = d.getDouble("Drop-Percent", 0.0);

                                ConfigurationSection customDataSec = d.getConfigurationSection("Custom-Data");
                                drop.customData = (customDataSec != null) ? customDataSec.getValues(false) : new HashMap<>();

                                drops.add(drop);
                            }
                        }

                        level.customDrops = drops;
                        levels.put(Integer.parseInt(levelKey), level);
                    }
                }

                MobDataModel model = new MobDataModel(
                        mobKey,
                        enabled,
                        customWorlds,
                        worlds,
                        activationRange,
                        disableAI,
                        spawnersPerChunk,
                        overrideDisplay,
                        displayName,
                        displayLore,
                        req,
                        levels
                );

                mobData.put(mobKey.toLowerCase(), model);
                log.info("✅ Loaded mob config: " + mobKey);

            } catch (Exception e) {
                log.warning("❌ Failed to load mob file: " + file.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Copies all .yml files from mobLevelsConfiguration/ in the JAR if they don't already exist on disk.
     */
    private void extractDefaultsFromJar() {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith("mobLevelsConfiguration/") && name.endsWith(".yml")) {
                        String fileName = name.substring("mobLevelsConfiguration/".length());
                        File target = new File(plugin.getDataFolder(), "mobLevelsConfiguration/" + fileName);

                        if (!target.exists()) {
                            plugin.saveResource(name, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Failed to extract default mob config files: " + e.getMessage());
        }
    }

    public MobDataModel getMob(String id) {
        return mobData.get(id.toLowerCase());
    }

    public Collection<MobDataModel> getAllMobs() {
        return mobData.values();
    }

    public void reload() {
        loadAllMobs();
    }
}
