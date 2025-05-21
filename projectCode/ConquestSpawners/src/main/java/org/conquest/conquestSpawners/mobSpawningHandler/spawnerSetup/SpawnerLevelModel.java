package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import java.util.List;

/**
 * Represents the configuration for a single spawner level.
 * Includes customizable values for spawn timing, XP, mob count, and drops.
 * Supports "default" values which fallback to global config defaults.
 */
public class SpawnerLevelModel {

    // === Configurable Fields ===
    private Object spawnerDelay;
    private Object mobCount;
    private Object xpDrop;
    private boolean vanillaDrops;
    private Object costToUpgrade;
    private List<CustomDropModel> customDrops;

    // === Raw Accessors ===

    public Object getSpawnerDelay() {
        return spawnerDelay;
    }

    public Object getMobCount() {
        return mobCount;
    }

    public Object getXpDrop() {
        return xpDrop;
    }

    public boolean isVanillaDrops() {
        return vanillaDrops;
    }

    public Object getCostToUpgrade() {
        return costToUpgrade;
    }

    public List<CustomDropModel> getCustomDrops() {
        return customDrops;
    }

    // === Setters ===

    public void setSpawnerDelay(Object spawnerDelay) {
        this.spawnerDelay = spawnerDelay;
    }

    public void setMobCount(Object mobCount) {
        this.mobCount = mobCount;
    }

    public void setXpDrop(Object xpDrop) {
        this.xpDrop = xpDrop;
    }

    public void setVanillaDrops(boolean vanillaDrops) {
        this.vanillaDrops = vanillaDrops;
    }

    public void setCostToUpgrade(Object costToUpgrade) {
        this.costToUpgrade = costToUpgrade;
    }

    public void setCustomDrops(List<CustomDropModel> customDrops) {
        this.customDrops = customDrops;
    }

    // === Resolved Getters with Config Fallback ===

    /**
     * Gets the mob count for this spawner level, falling back to global default if needed.
     */
    public int getMobCountResolved() {
        return ConfigResolver.getInt(
                mobCount != null ? mobCount : "default",
                "default-values.mob-count",
                1
        );
    }

    /**
     * Gets the amount of XP this spawner level will award on spawn.
     */
    public int getXpDropResolved() {
        return ConfigResolver.getInt(
                xpDrop != null ? xpDrop : "default",
                "default-values.xp-drop",
                0
        );
    }

    /**
     * Gets the spawn delay in seconds for this spawner level.
     */
    public int getSpawnerDelayResolved() {
        return ConfigResolver.getInt(
                spawnerDelay != null ? spawnerDelay : "default",
                "default-values.spawn-delay",
                8
        );
    }

    /**
     * Gets the cost to upgrade to this spawner level.
     */
    public int getCostToUpgradeResolved() {
        return ConfigResolver.getInt(
                costToUpgrade != null ? costToUpgrade : "default",
                "default-values.cost-to-upgrade",
                0
        );
    }
}
