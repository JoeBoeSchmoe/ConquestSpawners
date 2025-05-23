package org.conquest.conquestSpawners.configurationHandler;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.AdminMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.UserMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.PlaceHolderAPIManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.VaultManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;

import java.util.logging.Logger;

/**
 * 🧩 ConfigurationManager
 * Handles loading config.yml and initializing external integrations (Vault, PlaceholderAPI, DecentHolograms).
 * Loads statically managed configuration files like ConfigFile and message files.
 * Also initializes and loads mob configuration from /mobs.
 */
public class ConfigurationManager {

    private final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private final Logger log = plugin.getLogger();
    private FileConfiguration config;

    private final MobManager mobManager;

    public ConfigurationManager() {
        this.mobManager = new MobManager(plugin); // ✅ Instantiate ONCE here
    }

    public void initialize() {
        try {
            log.info("📦  Loading configuration...");

            // 🔃 Load all YAML files
            ConfigFile.load();
            AdminMessagesFile.load();
            UserMessagesFile.load();

            this.config = ConfigFile.getConfig();

            checkAll();

            // 🔌 Integrations
            setupVault();
            setupPlaceholderAPI();
            setupDecentHolograms();

            // ✅ Now we can safely reload the mobs
            mobManager.reload();

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

        // 🧱 Cram config validation
        check("entity-cram-limit");
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

    /**
     * 🌋 Gets the global cram limit for custom mobs from config.yml
     * Applies to all mobs tagged with conquest-spawner-drop.
     */
    public int getEntityCramLimit() {
        return getConfig().getInt("entity-cram-limit", 3);
    }

    public MobManager getMobManager() {
        return mobManager;
    }
}
