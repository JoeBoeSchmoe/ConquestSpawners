package org.conquest.conquestSpawners;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;

public final class ConquestSpawners extends JavaPlugin {

    private static ConquestSpawners instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;

        // 🧩 Load all config files
        configurationManager = new ConfigurationManager();
        configurationManager.loadAll();

        getLogger().info("✅  ConquestSpawners enabled and configurations loaded.");

    }

    @Override
    public void onDisable() {
        getLogger().info("⛔  ConquestSpawners disabled.");
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
