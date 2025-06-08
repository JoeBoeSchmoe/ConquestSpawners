package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves valid spawn locations near a spawner block, based on obstruction-aware scanning.
 */
public class SpawnLocationResolver {

    private record Size(double width, double height) {}
    private record BlockXZ(int x, int z) {}

    private static final Map<EntityType, Size> ENTITY_SIZES = Map.of(
            EntityType.ALLAY, new Size(0.35, 0.6),
            EntityType.ZOMBIE, new Size(0.6, 1.95),
            EntityType.CREEPER, new Size(0.6, 1.7),
            EntityType.SPIDER, new Size(1.4, 0.9)
            // Add more as needed...
    );

    public static List<Location> resolveValidSpawnLocations(
            Location spawnerLoc,
            int maxRadius,
            EntityType entityType,
            SpawnerRequirementsModel req,
            int needed,
            Set<Block> globallyClaimedBlocks,
            int spawnerY // NEW param
    ) {
        List<Location> candidates = new ArrayList<>();
        World world = spawnerLoc.getWorld();
        if (world == null) return candidates;

        Map<BlockXZ, Boolean> solidBelowCache = new HashMap<>();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        int baseX = spawnerLoc.getBlockX();
        int baseY = spawnerLoc.getBlockY();
        int baseZ = spawnerLoc.getBlockZ();

        for (int yOffset = 1; yOffset >= -1; yOffset--) {
            int y = baseY + yOffset;

            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    int x = baseX + dx;
                    int z = baseZ + dz;

                    if (!isPathOpen(world, baseX, baseY, baseZ, x, y, z)) continue;

                    double fx = x + 0.5 + rand.nextDouble(-0.3, 0.3);
                    double fz = z + 0.5 + rand.nextDouble(-0.3, 0.3);
                    Location loc = new Location(world, fx, y, fz);
                    Block candidateBlock = loc.getBlock();

                    if (globallyClaimedBlocks != null && globallyClaimedBlocks.contains(candidateBlock)) continue;
                    if (isValidSpawnLocation(loc, req, entityType, solidBelowCache)) {
                        candidates.add(loc);
                        if (globallyClaimedBlocks != null && y <= spawnerY) {
                            globallyClaimedBlocks.add(candidateBlock);
                        }
                    }
                }
            }
        }

        Collections.shuffle(candidates, rand);
        List<Location> result = new ArrayList<>(needed);

        for (int i = 0; i < needed; i++) {
            if (i < candidates.size()) {
                result.add(candidates.get(i));
            } else if (!candidates.isEmpty()) {
                Location base = candidates.get(rand.nextInt(candidates.size()));
                result.add(base.clone().add(
                        rand.nextDouble(-0.4, 0.4),
                        0,
                        rand.nextDouble(-0.4, 0.4)
                ));
            }
        }

        return result;
    }

    private static boolean isPathOpen(World world, int sx, int sy, int sz, int tx, int ty, int tz) {
        int dy = Integer.compare(ty, sy);
        int dx = Integer.compare(tx, sx);
        int dz = Integer.compare(tz, sz);

        int cx = sx;
        int cy = sy;
        int cz = sz;

        while (cy != ty) {
            cy += dy;
            if (!world.getBlockAt(cx, cy, cz).isPassable()) return false;
        }

        while (cx != tx || cz != tz) {
            if (cx != tx) cx += dx;
            if (cz != tz) cz += dz;
            if (!world.getBlockAt(cx, cy, cz).isPassable()) return false;
        }

        return true;
    }

    private static boolean isValidSpawnLocation(Location loc, SpawnerRequirementsModel req, EntityType entityType, Map<BlockXZ, Boolean> cache) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block base = loc.getBlock();
        Block below = base.getRelative(0, -1, 0);
        BlockXZ key = new BlockXZ(loc.getBlockX(), loc.getBlockZ());

        int light = base.getLightLevel();
        boolean solidBelow = cache.computeIfAbsent(key, k -> below.getType().isSolid());

        if (req.air && !isBoundingBoxClear(world, loc, entityType)) return false;
        if (req.onGround && !solidBelow) return false;
        if (req.onBlock && (req.allowedBlocks == null || !req.allowedBlocks.contains(below.getType().name()))) return false;
        if (req.darkness && light > 7) return false;
        if (req.totalDarkness && light > 0) return false;
        if (req.light && light < 8) return false;
        if (req.fluid && !base.isLiquid()) return false;
        if (req.inBiome && req.allowedBiomes != null && !req.allowedBiomes.contains(base.getBiome().getKey().getKey().toLowerCase())) return false;

        return true;
    }

    private static boolean isBoundingBoxClear(World world, Location center, EntityType type) {
        Size size = ENTITY_SIZES.get(type);
        if (size == null) return false;

        double halfW = size.width / 2.0;
        double minX = center.getX() - halfW;
        double maxX = center.getX() + halfW;
        double minY = center.getY();
        double maxY = center.getY() + size.height;
        double minZ = center.getZ() - halfW;
        double maxZ = center.getZ() + halfW;

        for (int x = (int) Math.floor(minX); x <= Math.floor(maxX); x++) {
            for (int y = (int) Math.floor(minY); y <= Math.floor(maxY); y++) {
                for (int z = (int) Math.floor(minZ); z <= Math.floor(maxZ); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isPassable() || block.getType().isSolid()) return false;
                }
            }
        }
        return true;
    }
}
