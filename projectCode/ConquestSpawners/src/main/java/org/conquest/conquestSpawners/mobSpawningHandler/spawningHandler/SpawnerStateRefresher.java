package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.Locale;

public final class SpawnerStateRefresher {

    private SpawnerStateRefresher() {}

    /**
     * Re-applies all spawner timing + control values to *already-placed* spawners
     * that belong to ConquestSpawners (have our PDC keys).
     *
     * NOTE:
     * - Only touches LOADED chunks to avoid chunk loading thrash.
     * - Must run on main thread.
     */
    public static void refreshAllLoaded(ConquestSpawners plugin, MobManager mobManager) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshAllLoaded(plugin, mobManager));
            return;
        }

        int touched = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                BlockState[] states;
                try {
                    states = chunk.getTileEntities();
                } catch (Throwable t) {
                    continue;
                }

                for (BlockState st : states) {
                    if (!(st instanceof CreatureSpawner spawner)) continue;

                    PersistentDataContainer data = spawner.getPersistentDataContainer();
                    String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
                    Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

                    if (mobKey == null || mobKey.isEmpty() || level == null) continue;

                    mobKey = mobKey.toLowerCase(Locale.ROOT);
                    MobDataModel mob = mobManager.getMob(mobKey);
                    if (mob == null || !mob.isSpawnerEnabled()) continue;

                    SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
                    if (levelData == null) continue;

                    // -----------------------------------------------------------------
                    // Delay in config is SECONDS (per your SpawnerLevelModel docs).
                    // Apply to vanilla spawner in TICKS.
                    // -----------------------------------------------------------------
                    int delayTicks = levelData.getSpawnerDelayTicksResolved();

                    try {
                        spawner.setMinSpawnDelay(delayTicks);
                        spawner.setMaxSpawnDelay(delayTicks);

                        // Force current countdown to the new value so it takes effect immediately.
                        spawner.setDelay(delayTicks);
                    } catch (Throwable ignored) {}

                    // Activation range
                    int playerRange = clampInt(mob.getPlayerActivationRangeResolved(), 1, 128);
                    try {
                        spawner.setRequiredPlayerRange(playerRange);
                    } catch (Throwable ignored) {}

                    // Spawn range (not required for STRICT mode, but keep consistent)
                    int spawnRange = clampInt(mob.getSpawnRadiusResolved(), 0, 32);
                    try {
                        spawner.setSpawnRange(spawnRange);
                    } catch (Throwable ignored) {}

                    // STRICT mode: vanilla spawner should only "trigger" once per cycle
                    try {
                        spawner.setSpawnCount(1);
                    } catch (Throwable ignored) {}

                    spawner.update(true, false);
                    touched++;
                }
            }
        }

        plugin.getLogger().info("🔄  Refreshed " + touched + " loaded Conquest spawners (delay/range/state).");
    }

    private static int clampTicks(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
