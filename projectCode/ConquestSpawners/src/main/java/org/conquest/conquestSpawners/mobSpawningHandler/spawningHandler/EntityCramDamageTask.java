package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.*;

public class EntityCramDamageTask extends BukkitRunnable {

    private static final double CHECK_RADIUS = 1.0;
    private static final double CHECK_RADIUS_SQUARED = CHECK_RADIUS * CHECK_RADIUS;
    private static final double BASE_DAMAGE = 3.5;
    private static final double DAMAGE_STEP_MULTIPLIER = 2.0;
    private static final int GRID_SIZE = 2;

    private final int cramLimit;
    private final ConquestSpawners plugin;
    private int currentWorldIndex = 0;

    public EntityCramDamageTask() {
        this.plugin = ConquestSpawners.getInstance();
        this.cramLimit = plugin.getConfigurationManager().getEntityCramLimit();
    }

    @Override
    public void run() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return;

        World world = worlds.get(currentWorldIndex++ % worlds.size());

        List<LivingEntity> mobs = new ArrayList<>();
        for (LivingEntity entity : world.getLivingEntities()) {
            if (isCustomMob(entity) && !entity.isDead()) {
                mobs.add(entity);
            }
        }

        if (mobs.size() <= cramLimit) return;

        Map<GridKey, List<TrackedMob>> grid = new HashMap<>();
        for (LivingEntity mob : mobs) {
            Location loc = mob.getLocation();
            GridKey key = getGridKey(loc);
            grid.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new TrackedMob(mob, loc.getX(), loc.getY(), loc.getZ()));
        }

        Set<UUID> processed = new HashSet<>();

        for (List<TrackedMob> bucket : grid.values()) {
            if (bucket.size() <= cramLimit) continue;

            for (TrackedMob tracked : bucket) {
                if (processed.contains(tracked.entity.getUniqueId())) continue;

                int nearby = 0;

                for (int dx = -1; dx <= 1 && nearby <= cramLimit; dx++) {
                    for (int dz = -1; dz <= 1 && nearby <= cramLimit; dz++) {
                        GridKey neighborKey = new GridKey(tracked.gridX + dx, tracked.gridZ + dz);
                        List<TrackedMob> neighbors = grid.get(neighborKey);
                        if (neighbors == null) continue;

                        for (TrackedMob other : neighbors) {
                            if (other == tracked || processed.contains(other.entity.getUniqueId())) continue;
                            if (distanceSquared(tracked, other) <= CHECK_RADIUS_SQUARED) {
                                if (++nearby > cramLimit) break;
                            }
                        }
                    }
                }

                if (nearby <= cramLimit) continue;

                int overload = nearby - cramLimit;
                int stepSize = Math.max(1, cramLimit / 2);
                int stepCount = overload / stepSize;

                double scaledDamage = BASE_DAMAGE * Math.pow(DAMAGE_STEP_MULTIPLIER, stepCount);
                LivingEntity entity = tracked.entity;

                if (!entity.hasMetadata("allow-cram")) {
                    entity.setMetadata("allow-cram", new FixedMetadataValue(plugin, true));
                }

                entity.damage(scaledDamage);
                processed.add(entity.getUniqueId());
            }
        }
    }

    private boolean isCustomMob(LivingEntity entity) {
        return entity.hasMetadata("conquest-spawner-drop");
    }

    private GridKey getGridKey(Location loc) {
        return new GridKey(loc.getBlockX() / GRID_SIZE, loc.getBlockZ() / GRID_SIZE);
    }

    private double distanceSquared(TrackedMob a, TrackedMob b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static class GridKey {
        final int x, z;
        GridKey(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GridKey other && x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }

    private static class TrackedMob {
        final LivingEntity entity;
        final double x, y, z;
        final int gridX, gridZ;

        TrackedMob(LivingEntity entity, double x, double y, double z) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
            this.gridX = (int) x / GRID_SIZE;
            this.gridZ = (int) z / GRID_SIZE;
        }
    }
}
