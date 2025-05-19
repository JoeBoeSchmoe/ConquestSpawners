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
    private final Object allowedSpawnersPerChunk;

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
            Object allowedSpawnersPerChunk,
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
        this.allowedSpawnersPerChunk = allowedSpawnersPerChunk;
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
    public Object getAllowedSpawnersPerChunk() { return allowedSpawnersPerChunk; }
    public boolean isOverrideDefaultDisplay() { return overrideDefaultDisplay; }
    public String getDisplayName() { return displayName; }
    public List<String> getDisplayLore() { return displayLore; }
    public SpawnerRequirementsModel getRequirements() { return requirements; }
    public Map<Integer, SpawnerLevelModel> getLevels() { return levels; }

    // === Resolved Accessors ===

    /**
     * Gets the player activation range, resolved through config.yml if 'default' is set.
     */
    public int getPlayerActivationRangeResolved() {
        return ConfigResolver.getInt(playerActivationRange, "default-values.player-activation-range", 32);
    }

    /**
     * Returns true if AI should be disabled for spawned mobs, resolved through config.yml.
     */
    public boolean isDisableMobAIResolved() {
        return ConfigResolver.getBoolean(disableMobAI, "default-values.disable-mob-ai", true);
    }

    /**
     * Gets the allowed spawners per chunk, resolved through config.yml if 'default' is set.
     */
    public int getAllowedSpawnersPerChunkResolved() {
        return ConfigResolver.getInt(allowedSpawnersPerChunk, "default-values.allowed-spawners-per-chunk", 8);
    }
}
