package org.conquest.conquestSpawners.configurationHandler.configurationFiles;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public final class SpawnerDataFile {

    private static final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private static final Logger log = plugin.getLogger();

    private static File rootDir;

    private SpawnerDataFile() {}

    public static void load(SpawnerManager spawnerManager) {
        rootDir = new File(plugin.getDataFolder(), "SpawnerData");
        ensureDir(rootDir);

        if (spawnerManager == null) {
            log.warning("⚠️ SpawnerDataFile.load called with null SpawnerManager (directories ready, but nothing loaded).");
            return;
        }

        spawnerManager.clear();

        int loaded = 0;

        File[] mobFolders = rootDir.listFiles(File::isDirectory);
        if (mobFolders != null) {
            for (File mobFolder : mobFolders) {
                String mobKey = normalizeMobKey(mobFolder.getName());

                File[] levelFiles = mobFolder.listFiles((dir, name) -> {
                    String lower = name.toLowerCase(Locale.ROOT);
                    return lower.startsWith("level_") && lower.endsWith(".yml");
                });

                if (levelFiles == null) continue;

                for (File levelFile : levelFiles) {
                    Integer level = parseLevelFromFilename(levelFile.getName());
                    if (level == null) continue;

                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(levelFile);
                    List<String> encoded = cfg.getStringList("locations");
                    if (encoded == null || encoded.isEmpty()) continue;

                    for (String s : encoded) {
                        if (spawnerManager.addEncodedToMemoryOnly(mobKey, level, s)) {
                            loaded++;
                        }
                    }
                }
            }
        }

        log.info("🗂️  SpawnerData ready: " + rootDir.getPath()
                + " | Loaded " + loaded + " spawners into memory.");
    }

    // ---------------------------------------------------------------------
    // Disk mutation API (called by SpawnerManager)
    // ---------------------------------------------------------------------

    public static void addSpawner(String mobKey, int level, Location loc) {
        if (!isValid(loc, mobKey)) return;

        mobKey = normalizeMobKey(mobKey);

        YamlConfiguration cfg = loadLevelConfig(mobKey, level);
        List<String> list = new ArrayList<>(cfg.getStringList("locations"));

        String encoded = encode(loc);
        if (!list.contains(encoded)) {
            list.add(encoded);
            cfg.set("locations", list);
            saveLevelConfig(mobKey, level, cfg);
        }
    }

    public static void removeSpawner(String mobKey, int level, Location loc) {
        if (!isValid(loc, mobKey)) return;

        mobKey = normalizeMobKey(mobKey);

        YamlConfiguration cfg = loadLevelConfig(mobKey, level);
        List<String> list = new ArrayList<>(cfg.getStringList("locations"));

        if (list.remove(encode(loc))) {
            cfg.set("locations", list);
            saveLevelConfig(mobKey, level, cfg);
        }
    }

    /** ✅ Remove without needing a Bukkit World loaded. */
    public static void removeEncoded(String mobKey, int level, String encoded) {
        if (mobKey == null || mobKey.isEmpty() || encoded == null || encoded.isEmpty()) return;

        mobKey = normalizeMobKey(mobKey);

        YamlConfiguration cfg = loadLevelConfig(mobKey, level);
        List<String> list = new ArrayList<>(cfg.getStringList("locations"));
        if (list.isEmpty()) return;

        if (list.remove(encoded)) {
            cfg.set("locations", list);
            saveLevelConfig(mobKey, level, cfg);
        }
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private static File getLevelFile(String mobKey, int level) {
        mobKey = normalizeMobKey(mobKey);

        File mobFolder = new File(rootDir, mobKey);
        ensureDir(mobFolder);

        return new File(mobFolder, "Level_" + level + ".yml");
    }

    private static YamlConfiguration loadLevelConfig(String mobKey, int level) {
        return YamlConfiguration.loadConfiguration(getLevelFile(mobKey, level));
    }

    private static void saveLevelConfig(String mobKey, int level, YamlConfiguration cfg) {
        try {
            cfg.save(getLevelFile(mobKey, level));
        } catch (IOException e) {
            log.warning("❌ Failed to save spawner data (" + mobKey + " L" + level + "): " + e.getMessage());
        }
    }

    private static Integer parseLevelFromFilename(String filename) {
        // Level_<level>.yml
        try {
            return Integer.parseInt(
                    filename.substring("Level_".length(), filename.length() - ".yml".length())
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            log.warning("⚠️ Could not create directory: " + dir.getPath());
        }
    }

    private static boolean isValid(Location loc, String mobKey) {
        return mobKey != null && !mobKey.isEmpty()
                && loc != null
                && loc.getWorld() != null;
    }

    private static String normalizeMobKey(String mobKey) {
        return mobKey.trim().toLowerCase(Locale.ROOT);
    }

    /** Encode as: world:x:y:z */
    private static String encode(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }
}
