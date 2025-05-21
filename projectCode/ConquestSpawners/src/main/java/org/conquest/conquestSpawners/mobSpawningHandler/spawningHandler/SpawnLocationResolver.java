package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves valid spawn locations within a defined radius around a spawner.
 * Takes into account entity size, spawner requirements, and vertical space.
 */
public class SpawnLocationResolver {

    /**
     * Attempts to find all valid spawn locations near a spawner.
     *
     * @param center     Spawner origin (usually center of the block)
     * @param req        Requirement rules for that mob
     * @param radius     Horizontal radius to check
     * @param entityType EntityType to check dimensions for
     * @return List of viable spawn locations
     */
    public static List<Location> findValidSpawnLocations(Location center, SpawnerRequirementsModel req, int radius, EntityType entityType) {
        List<Location> valid = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return valid;

        int attempts = 150;

        for (int i = 0; i < attempts; i++) {
            double x = center.getX() + ThreadLocalRandom.current().nextDouble(-radius, radius);
            double y = center.getY() - ThreadLocalRandom.current().nextInt(0, 4); // up to 3 blocks below
            double z = center.getZ() + ThreadLocalRandom.current().nextDouble(-radius, radius);
            Location testLoc = new Location(world, x, y, z);

            Block base = testLoc.getBlock();
            Block above = base.getRelative(0, 1, 0);
            Block below = base.getRelative(0, -1, 0);

            // ✅ Check basic clearance
            if (!base.isPassable() || !above.isPassable()) continue;

            // ✅ Ground required unless Air is allowed
            if (!req.air && !below.getType().isSolid()) continue;

            // ✅ Air-only spawns
            if (req.air && !base.getType().isAir()) continue;

            // ✅ On-ground flag
            if (req.onGround && !below.getType().isSolid()) continue;

            // ✅ Specific block types
            if (req.onBlock && (req.allowedBlocks == null ||
                    !req.allowedBlocks.contains(below.getType().name()))) continue;

            // ✅ Light level checks
            int light = base.getLightLevel();
            if (req.darkness && light > 7) continue;
            if (req.totalDarkness && light > 0) continue;
            if (req.light && light < 8) continue;

            // ✅ Fluid check
            if (req.fluid && !base.isLiquid()) continue;

            // ✅ Biome restriction
            if (req.inBiome && (req.allowedBiomes == null ||
                    !req.allowedBiomes.contains(base.getBiome().getKey().getKey().toLowerCase()))) continue;

            // ✅ Add to list
            valid.add(testLoc.clone().add(0.5, 0, 0.5));
        }

        return valid;
    }
}
