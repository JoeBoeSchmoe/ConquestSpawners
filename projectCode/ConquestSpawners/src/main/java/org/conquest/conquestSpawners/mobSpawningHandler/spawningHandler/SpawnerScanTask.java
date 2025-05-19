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
 * Scans all loaded chunks and queues mobs for spawning, respecting delays and range rules.
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
                    if (mob == null || !mob.getLevels().containsKey(level)) continue;

                    SpawnerLevelModel levelData = mob.getLevels().get(level);
                    Location spawnLoc = tile.getLocation().add(0.5, 1, 0.5);

                    // ⏱ Check delay
                    int delayTicks = levelData.getSpawnerDelayResolved();
                    long last = lastSpawnTimes.getOrDefault(spawnerId, 0L);
                    if (now - last < delayTicks * 50L) continue;

                    // 🎯 Check for nearby players
                    int activationRange = ConfigResolver.getInt(
                            mob.getPlayerActivationRange(), "default-values.player-activation-range", 32
                    );
                    if (!spawnLoc.getWorld().getNearbyPlayers(spawnLoc, activationRange).isEmpty()) {
                        int count = levelData.getMobCountResolved();
                        int xp = levelData.getXpDropResolved();

                        EntityType type;
                        try {
                            type = EntityType.valueOf(mob.getMobType().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            type = EntityType.PIG;
                        }

                        for (int i = 0; i < count; i++) {
                            spawnQueue.add(new CustomMob(
                                    spawnerId,
                                    type,
                                    spawnLoc,
                                    xp,
                                    levelData.getCustomDrops(),
                                    mob.getRequirements()
                            ));
                        }

                        lastSpawnTimes.put(spawnerId, now); // ⏱ Update delay time
                    }
                }
            }
        }
    }
}
