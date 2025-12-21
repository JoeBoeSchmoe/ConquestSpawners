package org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels;

import org.bukkit.Location;

import java.util.Locale;

/**
 * SpawnerMenuContext
 * Carries the clicked spawner's identity so menus show correct stats.
 */
public final class SpawnerMenuContext {

    private final String mobKey;          // normalized lower-case key stored in PDC
    private final int level;              // spawner level stored in PDC
    private final Location spawnerLoc;    // optional, useful for upgrades later

    public SpawnerMenuContext(String mobKey, int level, Location spawnerLoc) {
        this.mobKey = mobKey == null ? null : mobKey.toLowerCase(Locale.ROOT);
        this.level = level;
        this.spawnerLoc = spawnerLoc;
    }

    public String getMobKey() {
        return mobKey;
    }

    public int getLevel() {
        return level;
    }

    public Location getSpawnerLoc() {
        return spawnerLoc;
    }

    public boolean isValid() {
        return mobKey != null && !mobKey.isEmpty() && level > 0 && spawnerLoc != null && spawnerLoc.getWorld() != null;
    }
}
