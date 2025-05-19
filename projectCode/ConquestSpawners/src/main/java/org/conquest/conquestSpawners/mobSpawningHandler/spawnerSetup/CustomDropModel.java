package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import java.util.Map;

public class CustomDropModel {

    private final String material;
    private final int amount;
    private final double dropPercent;
    private final Map<String, Object> customData;

    public CustomDropModel(String material, int amount, double dropPercent, Map<String, Object> customData) {
        this.material = material;
        this.amount = amount;
        this.dropPercent = dropPercent;
        this.customData = customData;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public double getDropPercent() {
        return dropPercent;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }
}
