package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnLocationResolver {

    private static final class Size {
        final double width, height;
        Size(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final class BlockPosXZ {
        final int x, z;
        BlockPosXZ(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BlockPosXZ other && x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }

    private static final Map<EntityType, Size> ENTITY_SIZES = Map.ofEntries(
            Map.entry(EntityType.ZOMBIE, new Size(0.6, 1.95)),
            Map.entry(EntityType.CHICKEN, new Size(0.4, 0.7)),
            Map.entry(EntityType.SKELETON, new Size(0.6, 1.99)),
            Map.entry(EntityType.CREEPER, new Size(0.6, 1.7)),
            Map.entry(EntityType.SPIDER, new Size(1.4, 0.9)),
            Map.entry(EntityType.SLIME, new Size(0.6, 0.6))
    );

    public static List<Location> findValidSpawnLocations(Location center, SpawnerRequirementsModel req, int configRadius, EntityType entityType) {
        List<Location> valid = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return valid;

        if (req.inBiome && (req.allowedBiomes == null ||
                !req.allowedBiomes.contains(center.getBlock().getBiome().getKey().getKey().toLowerCase()))) {
            return valid;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int northRadius = scanRadius(world, cx, cy, cz, 0, -1, configRadius);
        int southRadius = scanRadius(world, cx, cy, cz, 0, 1, configRadius);
        int westRadius  = scanRadius(world, cx, cy, cz, -1, 0, configRadius);
        int eastRadius  = scanRadius(world, cx, cy, cz, 1, 0, configRadius);

        Map<BlockPosXZ, Boolean> solidBelowCache = new HashMap<>();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int dx = -westRadius; dx <= eastRadius; dx++) {
            for (int dz = -northRadius; dz <= southRadius; dz++) {
                int x = cx + dx;
                int z = cz + dz;

                for (int dy = 0; dy >= -2; dy--) {
                    double fx = x + rand.nextDouble(0.2, 0.8);
                    double fz = z + rand.nextDouble(0.2, 0.8);
                    Location loc = new Location(world, fx, cy + dy, fz);
                    if (tryAddValid(valid, loc, req, entityType, solidBelowCache)) break;
                }
            }
        }

        return valid;
    }

    private static int scanRadius(World world, int cx, int cy, int cz, int dx, int dz, int maxRadius) {
        for (int r = 1; r <= maxRadius; r++) {
            Block block = world.getBlockAt(cx + r * dx, cy, cz + r * dz);
            if (block.getType() != Material.AIR && block.getType() != Material.SPAWNER) {
                return r - 1;
            }
        }
        return maxRadius;
    }

    private static boolean tryAddValid(List<Location> valid, Location loc, SpawnerRequirementsModel req, EntityType entityType, Map<BlockPosXZ, Boolean> cache) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block base = loc.getBlock();
        Block below = base.getRelative(0, -1, 0);
        int light = base.getLightLevel();

        BlockPosXZ key = new BlockPosXZ(loc.getBlockX(), loc.getBlockZ());
        boolean belowSolid = cache.computeIfAbsent(key, k -> below.getType().isSolid());

        if (req.air && !isBoundingBoxSpaceClear(world, loc, entityType)) return false;
        if (!req.air && !belowSolid) return false;
        if (req.onGround && !belowSolid) return false;
        if (req.onBlock && (req.allowedBlocks == null || !req.allowedBlocks.contains(below.getType().name())))
            return false;
        if (req.darkness && light > 7) return false;
        if (req.totalDarkness && light > 0) return false;
        if (req.light && light < 8) return false;
        if (req.fluid && !base.isLiquid()) return false;

        valid.add(loc);
        return true;
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
