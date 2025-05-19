package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Finds dense, sub-block resolution spawn locations that meet requirement conditions.
 */
public class SpawnLocationResolver {

    public static List<Location> findValidSpawnLocations(Location center, SpawnerRequirementsModel req) {
        List<Location> valid = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return valid;

        int samples = 150; // Adjust based on density/performance
        double radius = 4.0;

        for (int i = 0; i < samples; i++) {
            double xOffset = randomOffset(radius);
            double yOffset = randomOffset(radius);
            double zOffset = randomOffset(radius);

            Location testLoc = center.clone().add(xOffset, yOffset, zOffset);

            if (testLoc.distanceSquared(center) < 1.25 * 1.25) continue;

            Block ground = testLoc.getBlock();
            Block below = ground.getRelative(0, -1, 0);
            Block above = ground.getRelative(0, 1, 0);

            // ✅ Base clearance
            if (!ground.isPassable()) continue;
            if (!above.isPassable()) continue;

            // ✅ Air-only?
            if (req.air && !ground.getType().isAir()) continue;

            // ✅ Ground surface
            if (req.onGround && !below.getType().isSolid()) continue;

            // ✅ Specific allowed blocks
            if (req.onBlock && (req.allowedBlocks == null ||
                    !req.allowedBlocks.contains(below.getType().name()))) continue;

            // ✅ Light check
            int light = ground.getLightLevel();
            if (req.darkness && light > 7) continue;
            if (req.totalDarkness && light > 0) continue;
            if (req.light && light < 8) continue;

            // ✅ Fluid check
            if (req.fluid && !ground.isLiquid()) continue;

            // ✅ Biome
            if (req.inBiome && (req.allowedBiomes == null ||
                    !req.allowedBiomes.contains(ground.getBiome().getKey().getKey().toLowerCase()))) continue;

            valid.add(testLoc);
        }

        return valid;
    }

    private static double randomOffset(double radius) {
        return ThreadLocalRandom.current().nextDouble(-radius, radius);
    }
}
