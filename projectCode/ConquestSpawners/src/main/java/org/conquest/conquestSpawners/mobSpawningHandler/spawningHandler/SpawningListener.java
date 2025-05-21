package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Polls the mob spawn queue and attempts to spawn each mob multiple times.
 * Gravity is preserved; AI behavior is suppressed via event-driven logic.
 * Mobs are optionally set to non-collidable so players can walk through them.
 */
public class SpawningListener extends BukkitRunnable {

    private static final String NO_COLLISION_TEAM = "no_collision";

    private final MobSpawnQueue queue;
    private final ConquestSpawners plugin;

    public SpawningListener(MobSpawnQueue queue) {
        this.queue = queue;
        this.plugin = ConquestSpawners.getInstance();
        setupNoCollisionTeam();
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

            SpawnerLevelModel level = data.getSpawnerLevels().get(mob.getSpawnerLevel());
            if (level == null) continue;

            SpawnerRequirementsModel reqs = data.getRequirements();
            int spawnRadius = data.getSpawnRadiusResolved();
            EntityType type = mob.getType();

            List<Location> validLocations = SpawnLocationResolver.findValidSpawnLocations(center, reqs, spawnRadius, type);
            if (validLocations.isEmpty()) continue;

            boolean spawned = false;
            for (int attempt = 0; attempt < 5 && !spawned; attempt++) {
                Location loc = validLocations.get(ThreadLocalRandom.current().nextInt(validLocations.size()));
                Entity spawnedEntity = world.spawnEntity(loc, type);

                if (!(spawnedEntity instanceof LivingEntity entity) || entity.isDead()) continue;

                // Plugin metadata for XP drops and tracking
                entity.setMetadata("conquest-spawner-drop", new FixedMetadataValue(plugin, mob.getSpawnerId().toString()));
                if (mob.getXpDrop() > 0) {
                    entity.setMetadata("custom-xp", new FixedMetadataValue(plugin, mob.getXpDrop()));
                }

                // Base behavior configuration
                entity.setGravity(true);
                entity.setFallDistance(0);
                entity.setSilent(true);
                entity.setRemoveWhenFarAway(false);
                entity.setAI(true); // Allow AI so listeners can optionally suppress

                if (data.isDisableCollisionsResolved()) {
                    entity.setCollidable(false);
                    addToNoCollisionTeam(entity);
                }

                if (data.isDisableMobAIResolved()) {
                    entity.setMetadata("disable-ai-logic", new FixedMetadataValue(plugin, true));
                }

                spawned = true;
            }
        }
    }

    private void setupNoCollisionTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }
    }

    private void addToNoCollisionTeam(Entity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team != null) {
            team.addEntry(entity.getUniqueId().toString());
        }
    }
}
