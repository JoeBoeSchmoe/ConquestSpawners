package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.CustomDropModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.List;

/**
 * Represents a mob queued for plugin-controlled spawning.
 * Includes type, location, level, and custom drop/data configuration.
 */
public class CustomMob {

    /** Bukkit entity type (e.g. ZOMBIE, ALLAY, etc.) */
    private final EntityType type;

    /** Location where the mob will attempt to spawn */
    private final Location spawnLocation;

    /** Amount of XP this mob should drop */
    private final int xpDrop;

    /** Custom drops for this mob instance */
    private final List<CustomDropModel> customDrops;

    /** Full mob configuration model from YAML */
    private final MobDataModel mobDataModel;

    /** Spawner level index (e.g. 1–5) */
    private final int spawnerLevel;

    public CustomMob(EntityType type,
                     Location spawnLocation,
                     int xpDrop,
                     List<CustomDropModel> customDrops,
                     MobDataModel mobDataModel,
                     int spawnerLevel) {
        this.type = type;
        this.spawnLocation = spawnLocation;
        this.xpDrop = xpDrop;
        this.customDrops = customDrops;
        this.mobDataModel = mobDataModel;
        this.spawnerLevel = spawnerLevel;
    }

    // === Getters ===

    public EntityType getType() {
        return type;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public int getXpDrop() {
        return xpDrop;
    }

    public List<CustomDropModel> getCustomDrops() {
        return customDrops;
    }

    public MobDataModel getMobDataModel() {
        return mobDataModel;
    }

    public int getSpawnerLevel() {
        return spawnerLevel;
    }

    /**
     * Gets the spawner level model from the mob’s data config.
     * @return SpawnerLevelModel or null if invalid level
     */
    public SpawnerLevelModel getSpawnerLevelModel() {
        return mobDataModel.getSpawnerLevels().get(spawnerLevel);
    }

    @Override
    public String toString() {
        return "CustomMob{" +
                "type=" + type +
                ", level=" + spawnerLevel +
                ", spawnLocation=" + spawnLocation +
                '}';
    }
}
