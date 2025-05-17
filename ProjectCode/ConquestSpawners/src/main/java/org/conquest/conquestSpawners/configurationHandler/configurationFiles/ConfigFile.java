package org.conquest.conquestSpawners.configurationHandler.configurationFiles;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * 📄 ConfigFile — Loads config.yml only
 */
public class ConfigFile {

    private final JavaPlugin plugin;
    private final Logger log;
    private final File file;
    private FileConfiguration config;

    public ConfigFile() {
        this.plugin = JavaPlugin.getProvidingPlugin(getClass());
        this.log = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                try (InputStream in = plugin.getResource("config.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default config.yml");
                    } else {
                        log.warning("⚠️ Missing config.yml in JAR");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded config.yml");

        } catch (Exception e) {
            log.severe("❌  Failed to load config.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
