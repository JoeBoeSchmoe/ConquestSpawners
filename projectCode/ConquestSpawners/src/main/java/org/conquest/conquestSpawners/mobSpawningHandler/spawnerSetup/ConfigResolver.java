package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;

/**
 * Utility class for resolving YAML-loaded Object fields to typed values,
 * supporting "default" keywords and plugin-defined fallbacks.
 */
public class ConfigResolver {

    private static final FileConfiguration config = ConquestSpawners.getInstance().getConfig();

    public static int getInt(Object value, String configPath, int fallback) {
        if (value instanceof Integer) return (Integer) value;
        if (isDefaultKeyword(value)) return config.getInt(configPath, fallback);
        return fallback;
    }

    public static boolean getBoolean(Object value, String configPath, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (isDefaultKeyword(value)) return config.getBoolean(configPath, fallback);
        return fallback;
    }

    public static double getDouble(Object value, String configPath, double fallback) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();  // covers ints too
        if (isDefaultKeyword(value)) return config.getDouble(configPath, fallback);
        return fallback;
    }

    public static String getString(Object value, String configPath, String fallback) {
        if (value instanceof String s && !isDefaultKeyword(s)) return s;
        return config.getString(configPath, fallback);
    }

    public static boolean isDefaultKeyword(Object value) {
        return value instanceof String && ((String) value).equalsIgnoreCase("default");
    }
}
