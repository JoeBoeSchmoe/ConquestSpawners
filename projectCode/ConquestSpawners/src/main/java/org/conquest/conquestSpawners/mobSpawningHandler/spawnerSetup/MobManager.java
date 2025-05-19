package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Loads and manages all custom mob data from YAML config files.
 * Responsible for parsing and resolving dynamic mob spawning properties.
 */
public class MobManager {

    private final JavaPlugin plugin;
    private final Map<String, MobDataModel> mobData = new HashMap<>();

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all mob configuration files and parses their models.
     */
    public void loadAllMobs() {
        mobData.clear();
        Logger log = plugin.getLogger();

        File configFolder = new File(plugin.getDataFolder(), "mobLevelsConfiguration");
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            log.warning("⚠️  Failed to create mobLevelsConfiguration/ directory.");
            return;
        }

        extractDefaultsFromJar();

        File[] files = configFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("⚠️  No mob config files found in mobLevelsConfiguration/");
            return;
        }

        for (File file : files) {
            loadMobFile(file);
        }
    }

    private void loadMobFile(File file) {
        String mobKey = file.getName().replace(".yml", "");
        Logger log = plugin.getLogger();

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            MobDataModel model = new MobDataModel(
                    mobKey,
                    yaml.getBoolean("Spawner-Enabled", false),
                    yaml.getBoolean("Custom-Whitelisted-Worlds", false),
                    yaml.getStringList("Worlds"),
                    yaml.get("Player-Activation-Range"),
                    yaml.get("Disable-Mob-AI"),
                    yaml.get("Disable-Collisions"),
                    yaml.get("Allowed-Spawners-Per-Chunk"),
                    yaml.get("Spawn-Radius"),
                    yaml.getBoolean("OverrideDefaultDisplay", false),
                    yaml.getString("DisplayName"),
                    yaml.getStringList("DisplayLore"),
                    parseRequirements(yaml.getConfigurationSection("SpawnerRequirements")),
                    parseLevels(yaml.getConfigurationSection("Spawner-Levels"))
            );

            mobData.put(mobKey.toLowerCase(), model);
            log.info("✅  Loaded mob config: " + mobKey);

        } catch (Exception e) {
            log.warning("❌  Failed to load mob file: " + file.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Extracts bundled default configs on first launch if missing.
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
                        File target = new File(plugin.getDataFolder(), name);
                        if (!target.exists()) {
                            plugin.saveResource(name, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️  Failed to extract default mob config files: " + e.getMessage());
        }
    }

    private SpawnerRequirementsModel parseRequirements(ConfigurationSection section) {
        if (section == null) return new SpawnerRequirementsModel();

        SpawnerRequirementsModel req = new SpawnerRequirementsModel();
        req.air = section.getBoolean("Air", false);
        req.aboveSeaLevel = section.getBoolean("Above-Sea-Level", false);
        req.belowSeaLevel = section.getBoolean("Below-Sea-Level", false);
        req.aboveYAxis = section.getBoolean("Above-Y-Axis", false);
        req.belowYAxis = section.getBoolean("Below-Y-Axis", false);
        req.darkness = section.getBoolean("Darkness", false);
        req.totalDarkness = section.getBoolean("Total-Darkness", false);
        req.light = section.getBoolean("Light", false);
        req.maxEntitiesNearby = section.getBoolean("Max-Entities-Nearby", false);
        req.fluid = section.getBoolean("Fluid", false);
        req.inBiome = section.getBoolean("InBiome", false);
        req.allowedBiomes = section.getStringList("AllowedBiomes");
        req.onGround = section.getBoolean("On-Ground", false);
        req.onBlock = section.getBoolean("On-Block", false);
        req.allowedBlocks = section.getStringList("AllowedBlocks");

        return req;
    }

    private Map<Integer, SpawnerLevelModel> parseLevels(ConfigurationSection section) {
        Map<Integer, SpawnerLevelModel> levels = new LinkedHashMap<>();
        if (section == null) return levels;

        for (String levelKey : section.getKeys(false)) {
            try {
                ConfigurationSection levelSec = section.getConfigurationSection(levelKey);
                if (levelSec == null) continue;

                SpawnerLevelModel level = new SpawnerLevelModel();
                level.setSpawnerDelay(levelSec.get("SpawnerDelay"));
                level.setMobCount(levelSec.get("MobCount"));
                level.setXpDrop(levelSec.get("XPDrop"));
                level.setVanillaDrops(levelSec.getBoolean("VanillaDrops", false));
                level.setCostToUpgrade(levelSec.get("CostToUpgrade"));
                level.setCustomDrops(parseCustomDrops(levelSec.getConfigurationSection("CustomDrops")));

                levels.put(Integer.parseInt(levelKey), level);
            } catch (Exception e) {
                plugin.getLogger().warning("❌  Error loading spawner level " + levelKey + ": " + e.getMessage());
            }
        }

        return levels;
    }

    private List<CustomDropModel> parseCustomDrops(ConfigurationSection section) {
        List<CustomDropModel> drops = new ArrayList<>();
        if (section == null) return drops;

        for (String dropKey : section.getKeys(false)) {
            try {
                ConfigurationSection d = section.getConfigurationSection(dropKey);
                if (d == null) continue;

                String material = d.getString("Material");
                int amount = d.getInt("Amount", 1);
                double dropPercent = d.getDouble("Drop-Percent", 0.0);

                Map<String, Object> customData = Optional.ofNullable(d.getConfigurationSection("Custom-Data"))
                        .map(sec -> sec.getValues(false))
                        .orElse(new HashMap<>());

                drops.add(new CustomDropModel(material, amount, dropPercent, customData));
            } catch (Exception e) {
                plugin.getLogger().warning("⚠️  Failed to parse drop: " + dropKey + " - " + e.getMessage());
            }
        }

        return drops;
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
