package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Polls the mob spawn queue and attempts to spawn each mob multiple times.
 * Gravity is preserved; AI behavior is suppressed via event-driven logic.
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

            MobDataModel data = mob.getMobDataModel();
            if (data == null || !data.isSpawnerEnabled()) continue;

            SpawnerLevelModel level = data.getLevels().get(mob.getSpawnerLevel());
            if (level == null) continue;

            SpawnerRequirementsModel reqs = data.getRequirements();
            int spawnRadius = data.getSpawnRadiusResolved();
            EntityType type = mob.getType();

            List<Location> validLocations = SpawnLocationResolver.findValidSpawnLocations(center, reqs, spawnRadius, type);
            if (validLocations.isEmpty()) continue;

            boolean spawned = false;
            for (int attempt = 0; attempt < 5 && !spawned; attempt++) {
                Location loc = validLocations.get(ThreadLocalRandom.current().nextInt(validLocations.size()));
                LivingEntity entity = (LivingEntity) world.spawnEntity(loc, type);
                if (entity.isDead()) continue;

                // 🏷️ Plugin metadata for XP drops and tracking
                entity.setMetadata("conquest-spawner-drop", new FixedMetadataValue(plugin, mob.getSpawnerId().toString()));
                if (mob.getXpDrop() > 0) {
                    entity.setMetadata("custom-xp", new FixedMetadataValue(plugin, mob.getXpDrop()));
                }

                // 📌 Entity behavior configuration
                entity.setGravity(true);
                entity.setFallDistance(0);
                entity.setSilent(true);
                entity.setRemoveWhenFarAway(false);

                if (data.isDisableCollisionsResolved()) {
                    entity.setCollidable(false);
                }

                if (data.isDisableMobAIResolved()) {
                    // External event handlers will suppress pathfinding, targeting, and AI ticks
                    entity.setMetadata("disable-ai-logic", new FixedMetadataValue(plugin, true));
                }

                spawned = true;
            }
        }
    }
}
