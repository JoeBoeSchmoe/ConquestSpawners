package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves valid spawn locations within a defined radius around a spawner.
 * Uses bounding box math and wall-aware centering to avoid clipping mobs into blocks.
 */
public class SpawnLocationResolver {

    private static final class Size {
        final double width, height;

        Size(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<EntityType, Size> ENTITY_SIZES = Map.ofEntries(
            Map.entry(EntityType.ZOMBIE, new Size(0.6, 1.95)),
            Map.entry(EntityType.CHICKEN, new Size(0.4, 0.7)),
            Map.entry(EntityType.SKELETON, new Size(0.6, 1.99)),
            Map.entry(EntityType.CREEPER, new Size(0.6, 1.7)),
            Map.entry(EntityType.SPIDER, new Size(1.4, 0.9)),
            Map.entry(EntityType.SLIME, new Size(0.6, 0.6))
            // Extend as needed
    );

    public static List<Location> findValidSpawnLocations(Location center, SpawnerRequirementsModel req, int radius, EntityType entityType) {
        List<Location> valid = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return valid;

        int maxAttempts = 150;

        int xNeg = getClearDistance(world, center, -1, 0, radius);
        int xPos = getClearDistance(world, center, 1, 0, radius);
        int zNeg = getClearDistance(world, center, 0, -1, radius);
        int zPos = getClearDistance(world, center, 0, 1, radius);

        int yRange = 3;

        for (int i = 0; i < maxAttempts; i++) {
            double xOffset = ThreadLocalRandom.current().nextDouble(-xNeg, xPos);
            double zOffset = ThreadLocalRandom.current().nextDouble(-zNeg, zPos);

            double x = center.getX() + xOffset;
            double y = Math.floor(center.getY() - ThreadLocalRandom.current().nextInt(0, yRange)) + 0.01;
            double z = center.getZ() + zOffset;

            Location candidate = new Location(world, x, y, z);
            Location adjusted = snapBackIfNearWall(candidate, center);

            Block base = adjusted.getBlock();
            Block below = base.getRelative(0, -1, 0);

            if (req.air && !isBoundingBoxSpaceClear(world, adjusted, entityType)) continue;
            if (!req.air && !below.getType().isSolid()) continue;
            if (req.onGround && !below.getType().isSolid()) continue;
            if (req.onBlock && (req.allowedBlocks == null || !req.allowedBlocks.contains(below.getType().name()))) continue;

            int light = base.getLightLevel();
            if (req.darkness && light > 7) continue;
            if (req.totalDarkness && light > 0) continue;
            if (req.light && light < 8) continue;
            if (req.fluid && !base.isLiquid()) continue;
            if (req.inBiome && (req.allowedBiomes == null ||
                    !req.allowedBiomes.contains(base.getBiome().getKey().getKey().toLowerCase()))) continue;

            valid.add(adjusted.add(0.5, 0, 0.5));
        }

        return valid;
    }

    private static Location snapBackIfNearWall(Location spawn, Location center) {
        World world = spawn.getWorld();
        if (world == null) return spawn;

        double dx = spawn.getX() - center.getX();
        double dz = spawn.getZ() - center.getZ();

        Location adjusted = spawn.clone();

        // Check if block directly in front of direction is solid
        if (dx > 0) {
            if (world.getBlockAt(spawn.clone().add(1, 0, 0)).getType().isSolid()) {
                adjusted.setX(center.getX());
            }
        } else if (dx < 0) {
            if (world.getBlockAt(spawn.clone().add(-1, 0, 0)).getType().isSolid()) {
                adjusted.setX(center.getX());
            }
        }

        if (dz > 0) {
            if (world.getBlockAt(spawn.clone().add(0, 0, 1)).getType().isSolid()) {
                adjusted.setZ(center.getZ());
            }
        } else if (dz < 0) {
            if (world.getBlockAt(spawn.clone().add(0, 0, -1)).getType().isSolid()) {
                adjusted.setZ(center.getZ());
            }
        }

        return adjusted;
    }

    private static int getClearDistance(World world, Location center, int dx, int dz, int max) {
        for (int i = 1; i <= max; i++) {
            Location check = center.clone().add(i * dx, 0, i * dz);
            if (!check.getBlock().isPassable()) return i - 1;
        }
        return max;
    }

    public static boolean isBoundingBoxSpaceClear(World world, Location center, EntityType type) {
        Size size = ENTITY_SIZES.get(type);
        if (size == null) return false;

        double halfWidth = size.width / 2.0;
        double height = size.height;

        double minX = center.getX() - halfWidth;
        double maxX = center.getX() + halfWidth;
        double minY = center.getY();
        double maxY = center.getY() + height;
        double minZ = center.getZ() - halfWidth;
        double maxZ = center.getZ() + halfWidth;

        for (int x = (int) Math.floor(minX); x <= Math.floor(maxX); x++) {
            for (int y = (int) Math.floor(minY); y <= Math.floor(maxY); y++) {
                for (int z = (int) Math.floor(minZ); z <= Math.floor(maxZ); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isPassable() || block.getType().isSolid()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
