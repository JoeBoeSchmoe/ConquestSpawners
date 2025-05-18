package org.conquest.conquestSpawners;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.commandHandler.CommandManager;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerListener;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerPlaceListener;

import java.util.List;

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

        // 📜 Register commands
        setupCommands();

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
        getLogger().info("✅  Reload complete.");
    }

    /**
     * Registers main command and aliases as defined in plugin.yml and config.yml
     */
    private void setupCommands() {
        CommandManager commandManager = new CommandManager();

        // Main command must match plugin.yml
        PluginCommand baseCommand = getCommand("conquestspawners");
        if (baseCommand == null) {
            getLogger().severe("❌  Command 'conquestspawners' not registered in plugin.yml.");
            return;
        }

        baseCommand.setExecutor(commandManager);
        baseCommand.setTabCompleter(commandManager);

        // Log aliases defined in config (not dynamically registered but good for debugging)
        List<String> aliases = getConfig().getStringList("command-aliases");
        if (!aliases.isEmpty()) {
            getLogger().info("🔗  Registered aliases from config: " + String.join(", ", aliases));
        }
    }

    /**
     * Registers all Bukkit event listeners.
     */
    private void registerListeners() {
        // 📦 Handles placement restrictions (biome, y-axis, etc.)
        new SpawnerPlaceListener(configurationManager.getMobManager());

        // ⚙️ Handles spawning logic and runtime checks (air, light, etc.)
        new SpawnerListener(configurationManager.getMobManager());

        getLogger().info("🎧  Event listeners registered.");
    }


    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
