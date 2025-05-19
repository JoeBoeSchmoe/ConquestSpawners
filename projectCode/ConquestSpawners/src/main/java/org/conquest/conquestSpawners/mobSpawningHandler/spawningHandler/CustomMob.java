package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.CustomDropModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.List;
import java.util.UUID;

/**
 * Represents a mob queued for plugin-controlled spawning.
 */
public class CustomMob {

    private final UUID spawnerId;
    private final EntityType type;
    private final Location spawnLocation;
    private final int xpDrop;
    private final List<CustomDropModel> customDrops;
    private final SpawnerRequirementsModel requirements;

    public CustomMob(UUID spawnerId,
                     EntityType type,
                     Location spawnLocation,
                     int xpDrop,
                     List<CustomDropModel> customDrops,
                     SpawnerRequirementsModel requirements) {
        this.spawnerId = spawnerId;
        this.type = type;
        this.spawnLocation = spawnLocation;
        this.xpDrop = xpDrop;
        this.customDrops = customDrops;
        this.requirements = requirements;
    }

    public UUID getSpawnerId() {
        return spawnerId;
    }

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

    public SpawnerRequirementsModel getRequirements() {
        return requirements;
    }
}
