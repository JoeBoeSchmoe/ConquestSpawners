package org.conquest.conquestSpawners.configurationHandler.configurationFiles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * 🛡️ adminMessagesFile
 * Loads and manages adminMessages.yml in the MessagesConfiguration folder.
 */
public class AdminMessagesFile {

    private static final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private AdminMessagesFile() {
        // Utility class
    }

    public static void load() {
        try {
            File messagesFolder = new File(plugin.getDataFolder(), "messagesConfiguration");
            if (!messagesFolder.exists() && !messagesFolder.mkdirs()) {
                log.warning("⚠️  Failed to create messagesConfiguration folder");
            }

            File file = new File(messagesFolder, "adminMessages.yml");
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("messagesConfiguration/adminMessages.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default adminMessages.yml");
                    } else {
                        log.warning("⚠️  Missing embedded adminMessages.yml resource!");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded adminMessages.yml successfully.");
        } catch (Exception e) {
            log.severe("❌  Failed to load adminMessages.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection("messages." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains("messages." + path);
    }

    public static String get(String path) {
        return config != null ? config.getString("messages." + path) : null;
    }
}
