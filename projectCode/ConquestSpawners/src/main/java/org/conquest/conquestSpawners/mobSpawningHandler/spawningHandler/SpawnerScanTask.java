package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
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

    public SpawnerScanTask(MobManager mobManager, MobSpawnQueue spawnQueue) {
        this.mobManager = mobManager;
        this.spawnQueue = spawnQueue;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().isEmpty()) continue;

            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
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
                    if (mob == null || !mob.isSpawnerEnabled()) continue;

                    SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
                    if (levelData == null) continue;

                    long last = lastSpawnTimes.getOrDefault(spawnerId, 0L);
                    int delaySeconds = levelData.getSpawnerDelayResolved();
                    int delayMillis = delaySeconds * 1000;
                    if (now - last < delayMillis) continue;

                    Location spawnLoc = tile.getLocation().add(0.5, 0, 0.5);

                    // Adjust Y if above air
                    Block below = spawnLoc.getBlock().getRelative(0, -1, 0);
                    if (!below.getType().isSolid()) {
                        for (int y = -1; y >= -3; y--) {
                            Block candidate = spawnLoc.getBlock().getRelative(0, y, 0);
                            if (candidate.getType().isSolid()) {
                                spawnLoc.add(0, y, 0);
                                break;
                            }
                        }
                    }

                    int activationRange = ConfigResolver.getInt(
                            mob.getPlayerActivationRange(),
                            "default-values.player-activation-range",
                            32
                    );

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
                        boolean added = spawnQueue.add(new CustomMob(
                                spawnerId,
                                type,
                                spawnLoc,
                                xpDrop,
                                levelData.getCustomDrops(),
                                mob,
                                level
                        ));
                        if (!added) break;
                    }

                    lastSpawnTimes.put(spawnerId, now);
                }
            }
        }
    }
}
