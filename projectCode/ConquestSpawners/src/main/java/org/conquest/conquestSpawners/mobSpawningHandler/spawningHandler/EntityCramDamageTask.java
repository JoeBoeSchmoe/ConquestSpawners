package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧱 EntityCramDamageTask
 * Periodically applies "cram damage" to clustered custom mobs when they exceed
 * the configurable cram limit within a small radius (~1 block).
 *
 * Controlled by:
 *   config.yml -> entity-cram-limit
 */
public class EntityCramDamageTask extends BukkitRunnable {

    private static final double CHECK_RADIUS = 0.5; // 1 block cube
    private static final double CRAM_DAMAGE = 1.0;   // Damage per tick when over limit

    private final int cramLimit;

    public EntityCramDamageTask() {
        this.cramLimit = ConquestSpawners.getInstance().getConfigurationManager().getEntityCramLimit();
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;

            for (LivingEntity entity : world.getLivingEntities()) {
                if (!isCustomMob(entity)) continue;

                Location loc = entity.getLocation();
                if (loc.getWorld() == null) continue;

                int count = countNearbyCustomMobs(loc, CHECK_RADIUS);

                if (count > cramLimit) {
                    entity.damage(CRAM_DAMAGE);
                }
            }
        }
    }

    private boolean isCustomMob(LivingEntity entity) {
        return entity.hasMetadata("conquest-spawner-drop");
    }

    private int countNearbyCustomMobs(Location loc, double radius) {
        int count = 0;
        for (LivingEntity nearby : loc.getWorld().getNearbyLivingEntities(loc, radius, radius, radius)) {
            if (isCustomMob(nearby)) {
                count++;
            }
        }
        return count;
    }
}
