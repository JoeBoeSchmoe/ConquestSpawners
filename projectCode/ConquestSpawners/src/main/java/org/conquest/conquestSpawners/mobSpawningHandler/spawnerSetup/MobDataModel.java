package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import java.util.List;
import java.util.Map;

/**
 * Represents all data loaded from a mob's YAML configuration.
 */
public class MobDataModel {

    private final String mobType;
    private final boolean spawnerEnabled;
    private final boolean customWhitelistedWorlds;
    private final List<String> allowedWorlds;

    private final Object playerActivationRange;
    private final Object disableMobAI;
    private final Object disableCollisions;
    private final Object allowedSpawnersPerChunk;
    private final Object spawnRadius;

    private final boolean overrideDefaultDisplay;
    private final String displayName;
    private final List<String> displayLore;

    private final SpawnerRequirementsModel requirements;
    private final Map<Integer, SpawnerLevelModel> levels;

    public MobDataModel(
            String mobType,
            boolean spawnerEnabled,
            boolean customWhitelistedWorlds,
            List<String> allowedWorlds,
            Object playerActivationRange,
            Object disableMobAI,
            Object disableCollisions,
            Object allowedSpawnersPerChunk,
            Object spawnRadius,
            boolean overrideDefaultDisplay,
            String displayName,
            List<String> displayLore,
            SpawnerRequirementsModel requirements,
            Map<Integer, SpawnerLevelModel> levels
    ) {
        this.mobType = mobType;
        this.spawnerEnabled = spawnerEnabled;
        this.customWhitelistedWorlds = customWhitelistedWorlds;
        this.allowedWorlds = allowedWorlds;
        this.playerActivationRange = playerActivationRange;
        this.disableMobAI = disableMobAI;
        this.disableCollisions = disableCollisions;
        this.allowedSpawnersPerChunk = allowedSpawnersPerChunk;
        this.spawnRadius = spawnRadius;
        this.overrideDefaultDisplay = overrideDefaultDisplay;
        this.displayName = displayName;
        this.displayLore = displayLore;
        this.requirements = requirements;
        this.levels = levels;
    }

    // === Raw Accessors ===
    public String getMobType() { return mobType; }
    public boolean isSpawnerEnabled() { return spawnerEnabled; }
    public boolean hasCustomWhitelistedWorlds() { return customWhitelistedWorlds; }
    public List<String> getAllowedWorlds() { return allowedWorlds; }
    public Object getPlayerActivationRange() { return playerActivationRange; }
    public Object getDisableMobAI() { return disableMobAI; }
    public Object getDisableCollisions() { return disableCollisions; }
    public Object getAllowedSpawnersPerChunk() { return allowedSpawnersPerChunk; }
    public Object getSpawnRadius() { return spawnRadius; }
    public boolean isOverrideDefaultDisplay() { return overrideDefaultDisplay; }
    public String getDisplayName() { return displayName; }
    public List<String> getDisplayLore() { return displayLore; }
    public SpawnerRequirementsModel getRequirements() { return requirements; }
    public Map<Integer, SpawnerLevelModel> getLevels() { return levels; }

    // === Resolved Accessors ===

    public int getPlayerActivationRangeResolved() {
        return ConfigResolver.getInt(playerActivationRange, "default-values.player-activation-range", 32);
    }

    public boolean isDisableMobAIResolved() {
        return ConfigResolver.getBoolean(disableMobAI, "default-values.disable-mob-ai", true);
    }

    public boolean isDisableCollisionsResolved() {
        return ConfigResolver.getBoolean(disableCollisions, "default-values.disable-collisions", true);
    }

    public int getAllowedSpawnersPerChunkResolved() {
        return ConfigResolver.getInt(allowedSpawnersPerChunk, "default-values.allowed-spawners-per-chunk", 8);
    }

    public int getSpawnRadiusResolved() {
        return ConfigResolver.getInt(spawnRadius, "default-values.spawn-radius", 4);
    }
}
