package org.conquest.conquestSpawners;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestSpawners.commandHandler.CommandManager;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUIListener;
import org.conquest.conquestSpawners.mobSpawningHandler.*;
import org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler.*;
import org.conquest.conquestSpawners.responseHandler.effectHandler.BossBarResponseManager;

import java.util.List;

public final class ConquestSpawners extends JavaPlugin {

    private static ConquestSpawners instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🔧  Initializing ConquestSpawners...");

        configurationManager = new ConfigurationManager();
        configurationManager.initialize();

        setupCommands();
        registerListeners();

        getLogger().info("✅  ConquestSpawners enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("📴  Shutting down ConquestSpawners...");

        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        MobDespawnTask.forceDespawnAllCustomMobs();

        if (DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.clearAllHolograms();
            getLogger().info("🪧  Cleared remaining active holograms.");
        }

        instance = null;

        System.gc();
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        getLogger().info("🧹  Memory usage at shutdown: " + usedMem + " MB");

        getLogger().info("✅  ConquestSpawners disabled successfully.");
    }

    public void reload() {
        getLogger().info("🔄  Reloading ConquestSpawners...");

        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        MobDespawnTask.forceDespawnAllCustomMobs();

        if (DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.clearAllHolograms();
            getLogger().info("🧹  Cleared DecentHolograms temporary displays.");
        }

        configurationManager.initialize();

        registerListeners();
        BossBarResponseManager.clearAll();

        getLogger().info("✅  Reload complete.");
    }

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

    private void registerListeners() {
        new SpawnerPlaceListener(configurationManager.getMobManager());

        getServer().getPluginManager().registerEvents(new MobBehaviorSuppressorListener(), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this, configurationManager.getMobManager()), this);

        getServer().getPluginManager().registerEvents(
                new CustomSpawnerSpawnListener(this, configurationManager.getMobManager()),
                this
        );

        // ✅ IMPORTANT: register fire/lava protection listener
        getServer().getPluginManager().registerEvents(new SpawnerFireLavaProtectionListener(), this);

        new EntityCramDamageTask().runTaskTimer(this, 100L, 5L);

        MobDespawnTask despawnTask = new MobDespawnTask(this);
        despawnTask.runTaskTimer(this, 20L * 300, 20L * 60);
        new MobDespawnListener(this, despawnTask);

        new SpawnerPickupListener(this);
        new SpawnerProtectionListener(this);
        new SpawnerInteractionListener(this);

        new GUIListener(this);

        getLogger().info("🎧  Listeners and spawning tasks registered (vanilla spawner mode).");
    }


    public static ConquestSpawners getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
