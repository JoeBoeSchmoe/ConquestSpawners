package org.conquest.conquestSpawners.configurationHandler;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.configFile;

import java.util.logging.Logger;

/**
 * 🧩 ConfigurationManager
 * Handles loading config.yml and initializing external integrations (Vault, PlaceholderAPI).
 * Loads statically managed configuration files like configFile.
 */
public class ConfigurationManager {

    private final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private final Logger log = plugin.getLogger();
    private FileConfiguration config;

    /**
     * Initializes core config file and third-party integrations.
     */
    public void initialize() {
        try {
            log.info("📦  Loading configuration...");
            configFile.load();
            this.config = configFile.getConfig();

            // ✅ Validate config keys
            check("chat-prefix");
            check("economy.use-vault");
            check("placeholders.use-placeholderapi");

            // 🔗 Initialize integrations
            setupVault();
            setupPlaceholderAPI();

            log.info("✅  Configuration loading complete.");
        } catch (Exception e) {
            log.severe("❌  Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Checks if the given config path exists and logs a warning if missing.
     *
     * @param path config.yml path
     */
    private void check(String path) {
        if (!configFile.contains(path)) {
            log.warning("⚠️ Missing config.yml key: '" + path + "'");
        }
    }

    private void setupVault() {
        boolean enabled = configFile.getBoolean("economy.use-vault", false);
        //VaultManager.initialize(enabled);
    }

    private void setupPlaceholderAPI() {
        boolean enabled = configFile.getBoolean("placeholders.use-placeholderapi", true);
        //PlaceholderAPIManager.initialize(enabled);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
