package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents all configuration data for a custom spawner mob,
 * loaded from its YAML definition file.
 */
public class MobDataModel {

    // === Core identifiers ===
    private final String mobType;
    private final boolean spawnerEnabled;

    // === World restrictions ===
    private final boolean customWhitelistedWorlds;
    private final List<String> allowedWorlds;

    // === Mob behavior & control ===
    private final Object playerActivationRange;
    private final Object disableMobAI;
    private final Object disableCollisions;
    private final Object allowedSpawnersPerChunk;
    private final Object spawnRadius;

    // === Display information ===
    private final boolean overrideDefaultDisplay;
    private final String displayName;
    private final List<String> displayLore;

    // === Hologram Display Override ===
    private final boolean overrideDefaultHologramDisplay;
    private final List<String> hologramDisplayLines;
    private final Double hologramVerticalOffset;
    private final Double hologramSpacing;
    private final Integer hologramDisplayTime;

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
            boolean overrideDefaultHologramDisplay,
            List<String> hologramDisplayLines,
            Double hologramVerticalOffset,
            Double hologramSpacing,
            Integer hologramDisplayTime,
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
        this.overrideDefaultHologramDisplay = overrideDefaultHologramDisplay;
        this.hologramDisplayLines = hologramDisplayLines;
        this.hologramVerticalOffset = hologramVerticalOffset;
        this.hologramSpacing = hologramSpacing;
        this.hologramDisplayTime = hologramDisplayTime;
        this.requirements = requirements;
        this.levels = levels;
    }

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
    public boolean isOverrideDefaultHologramDisplay() { return overrideDefaultHologramDisplay; }
    public List<String> getHologramDisplayLines() { return hologramDisplayLines; }
    public double getHologramVerticalOffsetResolved() {
        return hologramVerticalOffset != null ? hologramVerticalOffset : 1.5;
    }
    public double getHologramSpacingResolved() {
        return hologramSpacing != null ? hologramSpacing : 0.25;
    }
    public int getHologramDisplayTimeResolved() {
        return hologramDisplayTime != null ? hologramDisplayTime : 80;
    }
    public SpawnerRequirementsModel getRequirements() { return requirements; }
    public Map<Integer, SpawnerLevelModel> getSpawnerLevels() { return levels; }

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
    public String getDisplayNameResolved() {
        if (overrideDefaultDisplay && displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return capitalizeWords(mobType.replace("_", " ").toLowerCase(Locale.ROOT));
    }

    public EntityType getMobTypeEnum() {
        try {
            return EntityType.valueOf(mobType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return EntityType.PIG; // fallback, or null, or throw
        }
    }
    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                builder.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) builder.append(word.substring(1));
                builder.append(" ");
            }
        }
        return builder.toString().trim();
    }
}