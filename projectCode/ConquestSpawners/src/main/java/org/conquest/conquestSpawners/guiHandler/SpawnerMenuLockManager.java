package org.conquest.conquestSpawners.guiHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpawnerMenuLockManager
 * Ensures only ONE player can have a given spawner's upgrade menu open at a time.
 */
public final class SpawnerMenuLockManager {

    private static final Map<String, UUID> LOCKS = new ConcurrentHashMap<>();

    private SpawnerMenuLockManager() {}

    public static String key(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    public static boolean tryLock(Location spawnerLoc, Player player) {
        if (player == null) return false;

        String k = key(spawnerLoc);
        if (k == null) return false;

        UUID playerId = player.getUniqueId();

        // Fast path: same player already owns lock
        UUID current = LOCKS.get(k);
        if (playerId.equals(current)) return true;

        // If locked by someone else, validate owner still online
        if (current != null) {
            Player owner = Bukkit.getPlayer(current);
            if (owner != null && owner.isOnline()) {
                return false; // locked + owner still present
            }
            // stale lock -> clear it
            LOCKS.remove(k, current);
        }

        // Acquire lock
        return LOCKS.putIfAbsent(k, playerId) == null;
    }

    public static void unlock(Location spawnerLoc, UUID owner) {
        String k = key(spawnerLoc);
        if (k == null || owner == null) return;

        // Only unlock if the owner matches (prevents stealing unlocks)
        LOCKS.remove(k, owner);
    }

    public static UUID getOwner(Location spawnerLoc) {
        String k = key(spawnerLoc);
        if (k == null) return null;
        return LOCKS.get(k);
    }
}
