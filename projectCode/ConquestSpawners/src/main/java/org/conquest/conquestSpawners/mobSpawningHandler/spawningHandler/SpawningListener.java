package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns mobs from the queue and ensures each one gets multiple attempts before skipping.
 */
public class SpawningListener extends BukkitRunnable {

    private final MobSpawnQueue queue;
    private final ConquestSpawners plugin;

    public SpawningListener(MobSpawnQueue queue) {
        this.queue = queue;
        this.plugin = ConquestSpawners.getInstance();
    }

    @Override
    public void run() {
        while (!queue.isEmpty()) {
            CustomMob mob = queue.poll();
            if (mob == null) continue;

            Location center = mob.getSpawnLocation();
            World world = center.getWorld();
            if (world == null) continue;

            SpawnerRequirementsModel reqs = mob.getRequirements();
            List<Location> options = SpawnLocationResolver.findValidSpawnLocations(center, reqs);
            if (options.isEmpty()) continue;

            boolean spawned = false;
            for (int attempt = 0; attempt < 5 && !spawned; attempt++) {
                Location chosen = options.get(ThreadLocalRandom.current().nextInt(options.size()));
                LivingEntity entity = (LivingEntity) world.spawnEntity(chosen, mob.getType());
                if (entity.isDead()) continue;

                if (mob.getXpDrop() > 0) {
                    entity.setMetadata("custom-xp", new FixedMetadataValue(plugin, mob.getXpDrop()));
                }

                entity.setMetadata("conquest-spawner-drop", new FixedMetadataValue(plugin, mob.getSpawnerId().toString()));
                spawned = true;
            }

            // Optional: log if unable to spawn after retries
            if (!spawned) {
                plugin.getLogger().warning("⚠️ Failed to spawn " + mob.getType() + " from " + mob.getSpawnerId());
            }
        }
    }
}
