package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import java.util.List;

/**
 * Represents the configuration for a single spawner level.
 */
public class SpawnerLevelModel {

    private Object spawnerDelay;
    private Object mobCount;
    private Object xpDrop;
    private boolean vanillaDrops;
    private Object costToUpgrade;
    private List<CustomDropModel> customDrops;

    // === Raw Getters ===
    public Object getSpawnerDelay() { return spawnerDelay; }
    public Object getMobCount() { return mobCount; }
    public Object getXpDrop() { return xpDrop; }
    public boolean isVanillaDrops() { return vanillaDrops; }
    public Object getCostToUpgrade() { return costToUpgrade; }
    public List<CustomDropModel> getCustomDrops() { return customDrops; }

    // === Setters ===
    public void setSpawnerDelay(Object spawnerDelay) { this.spawnerDelay = spawnerDelay; }
    public void setMobCount(Object mobCount) { this.mobCount = mobCount; }
    public void setXpDrop(Object xpDrop) { this.xpDrop = xpDrop; }
    public void setVanillaDrops(boolean vanillaDrops) { this.vanillaDrops = vanillaDrops; }
    public void setCostToUpgrade(Object costToUpgrade) { this.costToUpgrade = costToUpgrade; }
    public void setCustomDrops(List<CustomDropModel> customDrops) { this.customDrops = customDrops; }

    // === Resolved Accessors using ConfigResolver ===

    public int getMobCountResolved() {
        return ConfigResolver.getInt(mobCount, "default-values.mob-count", 1);
    }

    public int getXpDropResolved() {
        return ConfigResolver.getInt(xpDrop, "default-values.xp-drop", 0);
    }

    public int getSpawnerDelayResolved() {
        return ConfigResolver.getInt(spawnerDelay, "default-values.spawn-delay", 20);
    }

    public int getCostToUpgradeResolved() {
        return ConfigResolver.getInt(costToUpgrade, "default-values.cost-to-upgrade", 0);
    }
}
