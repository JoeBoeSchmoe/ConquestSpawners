package org.conquest.conquestSpawners.configurationHandler.configurationFiles;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Loads adminMessages.yml from /messagesConfiguration
 */
public class AdminMessagesFile {

    private final JavaPlugin plugin;
    private final Logger log;
    private final File file;
    private FileConfiguration config;

    public AdminMessagesFile() {
        this.plugin = JavaPlugin.getProvidingPlugin(getClass());
        this.log = plugin.getLogger();
        File dir = new File(plugin.getDataFolder(), "messagesConfiguration");
        this.file = new File(dir, "adminMessages.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try (InputStream in = plugin.getResource("messagesConfiguration/adminMessages.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created adminMessages.yml");
                    } else {
                        log.warning("⚠️  Missing adminMessages.yml in JAR");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded adminMessages.yml");

        } catch (Exception e) {
            log.severe("❌  Failed to load adminMessages.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
