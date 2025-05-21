package org.conquest.conquestSpawners;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.commandHandler.CommandManager;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerPlaceListener;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.MobBehaviorSuppressorListener;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.MobSpawnQueue;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.SpawnerScanTask;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.SpawningListener;

import java.util.List;

/**
 * 🧱 ConquestSpawners
 * Main plugin class. Handles lifecycle, configuration, and listener registration.
 */
public final class ConquestSpawners extends JavaPlugin {

    private static ConquestSpawners instance;
    private ConfigurationManager configurationManager;
    private final MobSpawnQueue spawnQueue = new MobSpawnQueue();

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🔧  Initializing ConquestSpawners...");

        // 🔁 Load config and YAML files
        configurationManager = new ConfigurationManager();
        configurationManager.initialize();

        // 📜 Register commands
        setupCommands();

        // 🎧 Register listeners and periodic tasks
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

        PluginCommand baseCommand = getCommand("conquestspawners");
        if (baseCommand == null) {
            getLogger().severe("❌  Command 'conquestspawners' not registered in plugin.yml.");
            return;
        }

        baseCommand.setExecutor(commandManager);
        baseCommand.setTabCompleter(commandManager);

        List<String> aliases = getConfig().getStringList("command-aliases");
        if (!aliases.isEmpty()) {
            getLogger().info("🔗  Registered aliases from config: " + String.join(", ", aliases));
        }
    }

    /**
     * Registers all Bukkit event listeners and background tasks.
     */
    private void registerListeners() {
        // 📦 Handles placement restrictions (biome, y-axis, etc.)
        new SpawnerPlaceListener(configurationManager.getMobManager());

        // 🎧 Suppresses targeting, pathfinding, and attack AI while preserving gravity
        getServer().getPluginManager().registerEvents(new MobBehaviorSuppressorListener(), this);

        // 🕒 Start mob queue processing task
        new SpawningListener(spawnQueue).runTaskTimer(this, 20L, 20L); // every 1 second
        new SpawnerScanTask(configurationManager.getMobManager(), spawnQueue)
                .runTaskTimer(this, 20L, 20L); // every 1 second

        getLogger().info("🎧  Listeners and spawning task registered.");
    }


    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public MobSpawnQueue getSpawnQueue() {
        return spawnQueue;
    }
}
