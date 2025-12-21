package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.ConfigFile;

/**
 * Utility class for resolving YAML-loaded Object fields to typed values,
 * supporting "default" keywords and plugin-defined fallbacks.
 *
 * IMPORTANT:
 * Never cache FileConfiguration in a static field. Config can be reloaded.
 */
public final class ConfigResolver {

    private ConfigResolver() {}

    private static FileConfiguration cfg() {
        // Always fetch the current config instance
        return ConfigFile.getConfig();
    }

    public static int getInt(Object value, String configPath, int fallback) {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue(); // covers Long, Double, etc.
        if (isDefaultKeyword(value)) return cfg().getInt(configPath, fallback);
        return fallback;
    }

    public static boolean getBoolean(Object value, String configPath, boolean fallback) {
        if (value instanceof Boolean b) return b;
        if (isDefaultKeyword(value)) return cfg().getBoolean(configPath, fallback);
        return fallback;
    }

    public static double getDouble(Object value, String configPath, double fallback) {
        if (value instanceof Number n) return n.doubleValue(); // covers ints too
        if (isDefaultKeyword(value)) return cfg().getDouble(configPath, fallback);
        return fallback;
    }

    public static String getString(Object value, String configPath, String fallback) {
        if (value instanceof String s && !isDefaultKeyword(s)) return s;
        return cfg().getString(configPath, fallback);
    }

    public static boolean isDefaultKeyword(Object value) {
        return value instanceof String s && s.equalsIgnoreCase("default");
    }
}
