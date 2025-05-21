package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import java.util.List;
import java.util.Map;

/**
 * Represents all configuration data for a custom spawner mob,
 * loaded from its YAML definition file.
 */
public class MobDataModel {

    // === Core identifiers ===
    private final String mobType;                    // Internal Bukkit EntityType string (e.g., "ZOMBIE")
    private final boolean spawnerEnabled;            // Whether the spawner is usable or disabled globally

    // === World restrictions ===
    private final boolean customWhitelistedWorlds;   // If true, uses per-mob world whitelist
    private final List<String> allowedWorlds;        // Explicit world names (if enabled)

    // === Mob behavior & control ===
    private final Object playerActivationRange;      // Player proximity required to activate spawner
    private final Object disableMobAI;               // AI toggle per mob
    private final Object disableCollisions;          // Collision toggle per mob
    private final Object allowedSpawnersPerChunk;    // Cap per chunk
    private final Object spawnRadius;                // Spawn search radius in blocks

    // === Display information ===
    private final boolean overrideDefaultDisplay;    // If true, uses per-mob name/lore
    private final String displayName;
    private final List<String> displayLore;

    // === Spawn conditions & leveling ===
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

    // === Raw accessors ===

    public String getMobType() {
        return mobType;
    }

    public boolean isSpawnerEnabled() {
        return spawnerEnabled;
    }

    public boolean hasCustomWhitelistedWorlds() {
        return customWhitelistedWorlds;
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public Object getPlayerActivationRange() {
        return playerActivationRange;
    }

    public Object getDisableMobAI() {
        return disableMobAI;
    }

    public Object getDisableCollisions() {
        return disableCollisions;
    }

    public Object getAllowedSpawnersPerChunk() {
        return allowedSpawnersPerChunk;
    }

    public Object getSpawnRadius() {
        return spawnRadius;
    }

    public boolean isOverrideDefaultDisplay() {
        return overrideDefaultDisplay;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDisplayLore() {
        return displayLore;
    }

    public SpawnerRequirementsModel getRequirements() {
        return requirements;
    }

    public Map<Integer, SpawnerLevelModel> getLevels() {
        return levels;
    }

    // === Resolved accessors with config fallback ===

    /**
     * @return Player activation distance in blocks, resolved from config if set to "default".
     */
    public int getPlayerActivationRangeResolved() {
        return ConfigResolver.getInt(playerActivationRange, "default-values.player-activation-range", 32);
    }

    /**
     * @return Whether spawned mobs should have AI disabled.
     */
    public boolean isDisableMobAIResolved() {
        return ConfigResolver.getBoolean(disableMobAI, "default-values.disable-mob-ai", true);
    }

    /**
     * @return Whether spawned mobs should have collisions disabled.
     */
    public boolean isDisableCollisionsResolved() {
        return ConfigResolver.getBoolean(disableCollisions, "default-values.disable-collisions", true);
    }

    /**
     * @return Maximum allowed spawners of this type per chunk.
     */
    public int getAllowedSpawnersPerChunkResolved() {
        return ConfigResolver.getInt(allowedSpawnersPerChunk, "default-values.allowed-spawners-per-chunk", 8);
    }

    /**
     * @return Search radius around spawner for valid spawn points.
     */
    public int getSpawnRadiusResolved() {
        return ConfigResolver.getInt(spawnRadius, "default-values.spawn-radius", 4);
    }
}
