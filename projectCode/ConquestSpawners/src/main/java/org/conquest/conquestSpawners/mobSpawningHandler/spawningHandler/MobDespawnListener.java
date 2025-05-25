package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.conquest.conquestSpawners.ConquestSpawners;

/**
 * 🧍 MobDespawnListener
 * Listens for player and chunk events that may require cleanup of custom mobs.
 */
public class MobDespawnListener implements Listener {

    private final ConquestSpawners plugin;
    private final MobDespawnTask despawnTask;

    public MobDespawnListener(ConquestSpawners plugin, MobDespawnTask despawnTask) {
        this.plugin = plugin;
        this.despawnTask = despawnTask;

        // 🔗 Self-register with the plugin manager
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (despawnTask.isCustomSpawnerMob(living)) {
                living.remove();
            }
        }
    }

    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            Bukkit.getScheduler().runTaskLater(plugin, despawnTask::runDirectEntityScan, 1L);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, despawnTask::runDirectEntityScan, 1L);
    }
}
