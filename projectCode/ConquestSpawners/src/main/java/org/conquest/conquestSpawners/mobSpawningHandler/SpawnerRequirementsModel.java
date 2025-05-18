package org.conquest.conquestSpawners.mobSpawningHandler;

import java.util.List;

public class SpawnerRequirementsModel {
    public boolean air;
    public boolean aboveSeaLevel;
    public boolean belowSeaLevel;
    public boolean aboveYAxis;
    public boolean belowYAxis;
    public boolean darkness;
    public boolean totalDarkness;
    public boolean light;
    public boolean maxEntitiesNearby;
    public boolean fluid;
    public boolean inBiome;
    public List<String> allowedBiomes;
    public boolean onGround;
    public boolean onBlock;
    public List<String> allowedBlocks;
}
