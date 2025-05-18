package org.conquest.conquestSpawners.configurationHandler;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.AdminMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.UserMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.PlaceHolderAPIManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.VaultManager;

import java.util.logging.Logger;

/**
 * 🧩 ConfigurationManager
 * Handles loading config.yml and initializing external integrations (Vault, PlaceholderAPI, DecentHolograms).
 * Loads statically managed configuration files like ConfigFile and message files.
 */
public class ConfigurationManager {

    private final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private final Logger log = plugin.getLogger();
    private FileConfiguration config;

    public void initialize() {
        try {
            log.info("📦  Loading configuration...");

            // 🔃 Load all YAML files
            ConfigFile.load();
            AdminMessagesFile.load();
            UserMessagesFile.load();

            this.config = ConfigFile.getConfig();

            // ✅ Validate structure
            checkAll();

            // 🔌 Third-party integrations
            setupVault();
            setupPlaceholderAPI();
            setupDecentHolograms();

            log.info("✅  Configuration loading complete.");
        } catch (Exception e) {
            log.severe("❌  Failed to load configuration: " + e.getMessage());
        }
    }

    private void checkAll() {
        log.info("🔍  Validating config.yml structure...");
        check("chat-prefix");

        check("world-restrictions.whitelist-worlds");
        check("world-restrictions.allowed-worlds");

        check("command-aliases");
        check("placeholders.use-placeholderapi");
        check("holograms.use-decentholograms");

        check("cooldowns.command-delay-ms");
        check("cooldowns.gui-action-cooldown-ms");
        check("cooldowns.interaction-cooldown-ms");

        check("gui-settings.timeout-seconds");
    }

    private void check(String path) {
        if (!ConfigFile.contains(path)) {
            log.warning("⚠️ Missing config.yml key: '" + path + "'");
        }
    }

    private void setupVault() {
        VaultManager.initialize(true);
    }

    private void setupPlaceholderAPI() {
        boolean enabled = ConfigFile.getBoolean("placeholders.use-placeholderapi", true);
        PlaceHolderAPIManager.initialize(enabled);
    }

    private void setupDecentHolograms() {
        boolean enabled = ConfigFile.getBoolean("holograms.use-decentholograms", true);
        DecentHologramsManager.initialize(enabled);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
