package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Map.entry;

/**
 * Resolves valid spawn locations near a spawner block, based on obstruction-aware scanning.
 */
public class SpawnLocationResolver {

    // --- Add your mob size map here! (should be static/final and cover all needed types) ---
    private static final Map<EntityType, Size> ENTITY_SIZES = Map.<EntityType, Size>ofEntries(
            // Passive Animals
            entry(EntityType.ALLAY,       new Size(0.35, 0.6)),
            entry(EntityType.BAT,         new Size(0.5, 0.9)),
            entry(EntityType.CAT,         new Size(0.6, 0.7)),
            entry(EntityType.CHICKEN,     new Size(0.4, 0.7)),
            entry(EntityType.COD,         new Size(0.5, 0.3)),
            entry(EntityType.DOLPHIN,     new Size(0.9, 0.6)),
            entry(EntityType.FOX,         new Size(0.6, 0.7)),
            entry(EntityType.PANDA,       new Size(1.3, 1.25)),
            entry(EntityType.PARROT,      new Size(0.5, 0.9)),
            entry(EntityType.PIG,         new Size(0.9, 0.9)),
            entry(EntityType.SHEEP,       new Size(0.9, 1.3)),
            entry(EntityType.SQUID,       new Size(0.8, 0.8)),
            entry(EntityType.TURTLE,      new Size(1.1, 0.4)),
            entry(EntityType.LLAMA,       new Size(0.9, 1.875)),
            entry(EntityType.MULE,        new Size(1.3965, 1.6)),
            entry(EntityType.DONKEY,      new Size(1.3965, 1.5)),
            entry(EntityType.HORSE,       new Size(1.3965, 1.6)),
            entry(EntityType.COW,         new Size(0.9, 1.4)),
            entry(EntityType.MOOSHROOM,   new Size(0.9, 1.4)),
            entry(EntityType.POLAR_BEAR,  new Size(1.3, 1.4)),
            entry(EntityType.RABBIT,      new Size(0.4, 0.5)),
            entry(EntityType.BEE,         new Size(0.7, 0.6)),
            entry(EntityType.GLOW_SQUID,  new Size(0.8, 0.8)),
            entry(EntityType.GOAT,        new Size(0.9, 1.3)),

            // Hostile Mobs
            entry(EntityType.BLAZE,               new Size(0.6, 1.8)),
            entry(EntityType.CAVE_SPIDER,        new Size(0.7, 0.5)),
            entry(EntityType.CREEPER,            new Size(0.6, 1.7)),
            entry(EntityType.DROWNED,            new Size(0.6, 1.95)),
            entry(EntityType.ELDER_GUARDIAN,     new Size(2.0, 2.0)),
            entry(EntityType.ENDERMAN,           new Size(0.6, 2.9)),
            entry(EntityType.ENDERMITE,          new Size(0.4, 0.3)),
            entry(EntityType.EVOKER,             new Size(0.6, 1.95)),
            entry(EntityType.GHAST,              new Size(4.0, 4.0)),
            entry(EntityType.GIANT,              new Size(3.6, 12.0)),
            entry(EntityType.HUSK,               new Size(0.6, 1.95)),
            entry(EntityType.ILLUSIONER,            new Size(0.6, 1.95)),
            entry(EntityType.RAVAGER,            new Size(1.95, 2.2)),
            entry(EntityType.SHULKER,            new Size(1.0, 1.0)),
            entry(EntityType.SKELETON,           new Size(0.6, 1.99)),
            entry(EntityType.SLIME,              new Size(2.04, 2.04)),
            entry(EntityType.MAGMA_CUBE,         new Size(2.04, 2.04)),
            entry(EntityType.SPIDER,             new Size(1.4, 0.9)),
            entry(EntityType.STRAY,              new Size(0.6, 1.99)),
            entry(EntityType.VEX,                new Size(0.4, 0.8)),
            entry(EntityType.VINDICATOR,         new Size(0.6, 1.95)),
            entry(EntityType.WITCH,              new Size(0.6, 1.95)),
            entry(EntityType.WITHER_SKELETON,    new Size(0.7, 2.4)),
            entry(EntityType.WITHER,             new Size(0.9, 3.5)),
            entry(EntityType.PHANTOM,            new Size(0.8, 0.5)),
            entry(EntityType.SILVERFISH,         new Size(0.4, 0.3)),
            entry(EntityType.SKELETON_HORSE,     new Size(1.3965, 1.6)),
            entry(EntityType.STRIDER,            new Size(0.9, 1.7)),
            entry(EntityType.PILLAGER,           new Size(0.6, 1.95)),
            entry(EntityType.PIGLIN,             new Size(0.6, 1.95)),
            entry(EntityType.PIGLIN_BRUTE,       new Size(0.6, 1.95)),
            entry(EntityType.ZOMBIE,             new Size(0.6, 1.95)),
            entry(EntityType.ZOMBIE_HORSE,       new Size(1.3965, 1.6)),
            entry(EntityType.ZOMBIE_VILLAGER,    new Size(0.6, 1.95)),
            entry(EntityType.ZOMBIFIED_PIGLIN,   new Size(0.6, 1.95)),

            // Utility & Passive Others
            entry(EntityType.ARMOR_STAND,        new Size(0.5, 1.975)),
            entry(EntityType.IRON_GOLEM,         new Size(1.4, 2.7)),
            entry(EntityType.SNOW_GOLEM,           new Size(0.7, 1.9)),
            entry(EntityType.MINECART,          new Size(0.98, 0.7))
    );

    public static List<Location> resolveValidSpawnLocations(
            Location spawnerLoc,
            MobDataModel mob,
            SpawnerLevelModel levelData,
            World world
    ) {
        int radius = resolveRadius(mob, levelData);
        int baseY = spawnerLoc.getBlockY();
        int[] ySlabs = {baseY + 1, baseY, baseY - 1};

        List<Location> candidates = new ArrayList<>();
        Set<String> used = new HashSet<>();
        EntityType entityType = mob.getMobTypeEnum();
        SpawnerRequirementsModel req = mob.getRequirements();

        for (int slabY : ySlabs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > radius || (dx == 0 && dz == 0)) continue;

                    int steps = Math.max(Math.abs(dx), Math.abs(dz));
                    double stepX = dx / (double) steps;
                    double stepZ = dz / (double) steps;

                    for (int step = 1; step <= steps; step++) {
                        int x = spawnerLoc.getBlockX() + (int) Math.round(stepX * step);
                        int z = spawnerLoc.getBlockZ() + (int) Math.round(stepZ * step);

                        Block block = world.getBlockAt(x, slabY, z);
                        if (block.getType().isSolid() && !isSpawner(block)) {
                            // Abort scanning this direction once a solid block is hit
                            break;
                        }

                        double rx = x + ThreadLocalRandom.current().nextDouble();
                        double rz = z + ThreadLocalRandom.current().nextDouble();
                        Location candidate = new Location(world, rx, slabY, rz);

                        if (!isBoundingBoxClear(world, candidate, entityType)) continue;
                        if (!isValidSpawnLocation(candidate, req, entityType, new HashMap<>())) continue;

                        String key = String.format("%.2f,%.0f,%.2f", rx, (double) slabY, rz);
                        if (used.add(key)) {
                            candidates.add(candidate);
                        }

                        // We *continue scanning this direction* unless blocked
                    }
                }
            }
        }

        int mobCount = levelData.getMobCountResolved();
        List<Location> result = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        int tries = mobCount * 3;
        for (int i = 0; i < tries && !candidates.isEmpty() && result.size() < mobCount; i++) {
            Location pick = candidates.get(random.nextInt(candidates.size()));
            if (!result.contains(pick)) result.add(pick);
        }

        while (result.size() < mobCount && !candidates.isEmpty()) {
            result.add(candidates.get(random.nextInt(candidates.size())));
        }

        return result;
    }

    /** Determines radius (int) for this mob, from config or mob data. */
    private static int resolveRadius(MobDataModel mob, SpawnerLevelModel levelData) {
        int r = mob.getSpawnRadiusResolved();
        return Math.max(0, r);
    }

    /** Checks if a block is a custom spawner. You may want a more advanced check here! */
    private static boolean isSpawner(Block block) {
        // Adapt this if you use custom block types, NBT, or persistent data
        return block.getType() == Material.SPAWNER;
    }

    /**
     * Checks if the mob's hitbox will fit at this location.
     * This uses the mob's bounding box size from ENTITY_SIZES or (0.6, 1.8) as default.
     */
    private static boolean isBoundingBoxClear(World world, Location loc, EntityType type) {
        Size size = ENTITY_SIZES.getOrDefault(type, new Size(0.6, 1.8));
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        // Bounding box is centered at (x, y, z), but extends width/2 to each side, and height up
        double minX = x - size.width / 2, maxX = x + size.width / 2;
        double minY = y, maxY = y + size.height;
        double minZ = z - size.width / 2, maxZ = z + size.width / 2;

        // Sample every 0.3 blocks (finer = more accurate, coarser = faster)
        for (double xx = minX; xx <= maxX; xx += 0.3) {
            for (double yy = minY; yy <= maxY; yy += 0.3) {
                for (double zz = minZ; zz <= maxZ; zz += 0.3) {
                    Block block = world.getBlockAt((int) Math.floor(xx), (int) Math.floor(yy), (int) Math.floor(zz));
                    if (block.getType().isSolid() && !isSpawner(block)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the location is valid for spawning based on mob requirements.
     * @param cache a cache for below-block solid checks, key = (x, z)
     */
    private static boolean isValidSpawnLocation(Location loc, SpawnerRequirementsModel req, EntityType entityType, Map<BlockXZ, Boolean> cache) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block base = loc.getBlock();
        Block below = base.getRelative(0, -1, 0);
        BlockXZ key = new BlockXZ(loc.getBlockX(), loc.getBlockZ());

        int light = base.getLightLevel();
        boolean solidBelow = cache.computeIfAbsent(key, k -> below.getType().isSolid());

        if (req.air && !isBoundingBoxClear(world, loc, entityType)) return false;
        if (req.onGround) {
            if (!solidBelow) return false;

            // Only apply onBlock check *if* onGround is also true
            if (req.onBlock && (req.allowedBlocks == null || !req.allowedBlocks.contains(below.getType().name())))
                return false;
        }

        if (req.darkness && light > 7) return false;
        if (req.totalDarkness && light > 0) return false;
        if (req.light && light < 8) return false;
        if (req.fluid && !base.isLiquid()) return false;
        if (req.inBiome && req.allowedBiomes != null && !req.allowedBiomes.contains(base.getBiome().getKey().getKey().toLowerCase())) return false;

        return true;
    }

    // --- Size record for hitbox ---
    public static class Size {
        public final double width;
        public final double height;

        public Size(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    /** Simple value class for XZ keys, used in caching. */
    private static class BlockXZ {
        private final int x, z;
        BlockXZ(int x, int z) { this.x = x; this.z = z; }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockXZ b)) return false;
            return x == b.x && z == b.z;
        }
        public int hashCode() { return Objects.hash(x, z); }
    }
}
