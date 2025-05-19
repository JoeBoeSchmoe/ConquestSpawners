package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.*;

import java.util.*;

/**
 * Scans loaded chunks and queues mobs for spawning based on delay and player proximity.
 * Designed for high-performance even with large numbers of spawners.
 */
public class SpawnerScanTask extends BukkitRunnable {

    private final MobManager mobManager;
    private final MobSpawnQueue spawnQueue;
    private final Map<UUID, Long> lastSpawnTimes = new HashMap<>();

    private static final long TICK_DURATION_MS = 50L;

    public SpawnerScanTask(MobManager mobManager, MobSpawnQueue spawnQueue) {
        this.mobManager = mobManager;
        this.spawnQueue = spawnQueue;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (World world : Bukkit.getWorlds()) {
            // ✅ Skip unloaded or inactive worlds
            if (world.getPlayers().isEmpty()) continue;

            Chunk[] chunks = world.getLoadedChunks();
            if (chunks.length == 0) continue;

            for (Chunk chunk : chunks) {
                BlockState[] tileEntities = chunk.getTileEntities();
                if (tileEntities.length == 0) continue;

                for (BlockState state : tileEntities) {
                    if (!(state instanceof TileState tile)) continue;

                    PersistentDataContainer data = tile.getPersistentDataContainer();
                    String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
                    Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);
                    String idRaw = data.get(ItemUtility.key("id"), PersistentDataType.STRING);

                    if (mobKey == null || level == null || idRaw == null) continue;

                    UUID spawnerId;
                    try {
                        spawnerId = UUID.fromString(idRaw);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    MobDataModel mob = mobManager.getMob(mobKey.toLowerCase(Locale.ROOT));
                    if (mob == null) continue;

                    SpawnerLevelModel levelData = mob.getLevels().get(level);
                    if (levelData == null) continue;

                    long last = lastSpawnTimes.getOrDefault(spawnerId, 0L);
                    int delayTicks = levelData.getSpawnerDelayResolved();
                    if (now - last < delayTicks * TICK_DURATION_MS) continue;

                    Location spawnLoc = tile.getLocation().add(0.5, 1, 0.5);

                    int activationRange = ConfigResolver.getInt(
                            mob.getPlayerActivationRange(),
                            "default-values.player-activation-range",
                            32
                    );

                    // ✅ Only consider spawners with a player nearby
                    if (world.getNearbyPlayers(spawnLoc, activationRange).isEmpty()) continue;

                    EntityType type;
                    try {
                        type = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        type = EntityType.PIG;
                    }

                    int mobCount = levelData.getMobCountResolved();
                    int xpDrop = levelData.getXpDropResolved();

                    for (int i = 0; i < mobCount; i++) {
                        spawnQueue.add(new CustomMob(
                                spawnerId,
                                type,
                                spawnLoc,
                                xpDrop,
                                levelData.getCustomDrops(),
                                mob.getRequirements()
                        ));
                    }

                    // ✅ Cache spawn time for delay handling
                    lastSpawnTimes.put(spawnerId, now);
                }
            }
        }
    }
}
