package org.conquest.conquestSpawners;

import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;

public final class ConquestSpawners extends JavaPlugin {

    private static ConquestSpawners instance;
    private ConfigurationManager configurationManager;
    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
