package org.conquest.conquestSpawners;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.commandHandler.CommandManager;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerInteractionListener;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerPickupListener;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerPlaceListener;
import org.conquest.conquestSpawners.mobSpawningHandler.SpawnerProtectionListener;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.*;
import org.conquest.conquestSpawners.responseHandler.effectHandler.BossBarResponseManager;

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

        // 🎧 Register listeners and periodic tasks
        registerListeners();

        getLogger().info("✅  ConquestSpawners enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("📴 Shutting down ConquestSpawners...");

        // 1. Cancel all scheduled tasks
        Bukkit.getScheduler().cancelTasks(this);

        // 2. Unregister all event listeners
        HandlerList.unregisterAll(this);

        // 3. Despawn all custom mobs spawned by this plugin
        MobDespawnTask.forceDespawnAllCustomMobs();

        // 4. Remove all remaining holograms if integration is enabled
        if (DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.clearAllHolograms();
            getLogger().info("🪧 Cleared remaining active holograms.");
        }

        // 5. Null static references if applicable
        instance = null;

        // 6. Log final memory footprint
        System.gc(); // Optional: suggest GC run (safe here)
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        getLogger().info("🧹  Memory usage at shutdown: " + usedMem + " MB");

        getLogger().info("✅  ConquestSpawners disabled successfully.");
    }


    /**
     * Reloads plugin configuration, despawns mobs, clears tasks/listeners, and re-registers everything.
     */
    public void reload() {
        getLogger().info("🔄  Reloading ConquestSpawners...");

        // 1. Cancel all scheduled tasks and unregister listeners
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        // 2. Despawn all custom mobs
        MobDespawnTask.forceDespawnAllCustomMobs();

        // 3. Clear active holograms (if enabled)
        if (DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.clearAllHolograms();
            getLogger().info("🧹  Cleared DecentHolograms temporary displays.");
        }

        // 4. Reload all configs and mob YAMLs
        configurationManager.initialize();

        // 5. Re-register tasks, listeners, and command bindings
        registerListeners();
        BossBarResponseManager.clearAll();

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

    private void unregisterTasksAndListeners() {
        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Unregister all listeners
        HandlerList.unregisterAll(this);
    }

    /**
     * Registers all Bukkit event listeners and background tasks.
     */
    private void registerListeners() {
        // 📦 Handles placement restrictions (biome, y-axis, etc.)
        new SpawnerPlaceListener(configurationManager.getMobManager());

        // 🎧 Suppresses targeting, pathfinding, and attack AI while preserving gravity
        getServer().getPluginManager().registerEvents(new MobBehaviorSuppressorListener(), this);

        // 💀 Handles death drops and particle suppression for custom mobs
        getServer().getPluginManager().registerEvents(
                new MobDeathListener(this, configurationManager.getMobManager()), this
        );

        // 🔍 Scans for nearby eligible spawners and queues spawnable mobs
        new SpawnerScanTask(configurationManager.getMobManager())
                .runTaskTimer(this, 20L, 20L); // every 1 second

        // 💥 Cram detection for custom mobs
        new EntityCramDamageTask().runTaskTimer(this, 100L, 5L); // every 5 ticks (0.25s)

        // 🧹 Despawn handler (both scheduled and event-driven)
        MobDespawnTask despawnTask = new MobDespawnTask(this);
        despawnTask.runTaskTimer(this, 20L * 300, 20L * 60); // every minute

        new MobDespawnListener(this, despawnTask);

        new SpawnerPickupListener(this);
        new SpawnerProtectionListener(this);

        new SpawnerInteractionListener(this);
        getLogger().info("🎧  Listeners and spawning tasks registered.");
    }

    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
