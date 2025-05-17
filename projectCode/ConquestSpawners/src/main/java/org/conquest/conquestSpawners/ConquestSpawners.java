package org.conquest.conquestSpawners;

import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;
import org.bukkit.Bukkit;

/**
 * 🧱 ConquestSpawners
 * Main plugin class. Handles lifecycle, configuration, and listener registration.
 */
public final class ConquestSpawners extends JavaPlugin {

    private static ConquestSpawners instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🔧  Initializing ConquestSpawners...");

        // 🔁 Load config and YAML files
        configurationManager = new ConfigurationManager();
        configurationManager.initialize();

        // 🎧 Register events
        registerListeners();

        getLogger().info("✅  ConquestSpawners enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("📴  ConquestSpawners has been disabled.");
    }

    /**
     * Reloads configuration and supporting files.
     */
    public void reload() {
        getLogger().info("🔄  Reloading ConquestSpawners...");
        configurationManager.initialize();
        // Optional: re-register tasks, clear caches, etc.
        getLogger().info("✅  Reload complete.");
    }

    /**
     * Registers all Bukkit event listeners.
     */
    private void registerListeners() {
        // Example: Bukkit.getPluginManager().registerEvents(new SpawnerInteractListener(), this);
    }

    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
