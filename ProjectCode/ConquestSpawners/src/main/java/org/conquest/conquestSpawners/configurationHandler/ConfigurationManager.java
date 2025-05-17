package org.conquest.conquestSpawners.configurationHandler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.AdminMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.UserMessagesFile;

/**
 * 💾 ConfigurationManager — Central loader for all config/message files and integrations.
 */
public class ConfigurationManager {

    private final ConfigFile configFile = new ConfigFile();
    private final AdminMessagesFile adminMessagesFile = new AdminMessagesFile();
    private final UserMessagesFile userMessagesFile = new UserMessagesFile();

    private boolean vaultEnabled;
    private boolean papiEnabled;

    /**
     * Loads all YML files and integration systems.
     */
    public void loadAll() {
        configFile.load();
        adminMessagesFile.load();
        userMessagesFile.load();

        Bukkit.getScheduler().runTask(
                JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    loadVault();
                    loadPlaceholderAPI();
                }
        );
    }

    /**
     * Initializes Vault if enabled in config.
     */
    private void loadVault() {
        boolean enabledInConfig = configFile.getConfig().getBoolean("economy.use-vault", false);
        VaultManager.setup(enabledInConfig);
        vaultEnabled = VaultManager.isUsingVault();
    }

    /**
     * Initializes PlaceholderAPI if enabled in config.
     */
    private void loadPlaceholderAPI() {
        boolean enabledInConfig = configFile.getConfig().getBoolean("placeholders.use-placeholderapi", false);
        PlaceHolderAPIManager.initialize(enabledInConfig);
        papiEnabled = PlaceHolderAPIManager.isUsingPlaceholderAPI();
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return papiEnabled;
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public AdminMessagesFile getAdminMessagesFile() {
        return adminMessagesFile;
    }

    public UserMessagesFile getUserMessagesFile() {
        return userMessagesFile;
    }
}
