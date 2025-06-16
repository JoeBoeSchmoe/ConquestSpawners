package org.conquest.conquestSpawners.guiHandler.guiEditingHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.*;

/**
 * 🧠 GUISessionManager
 * Manages active GUI sessions for players using the Spawner Upgrade system.
 */
public class GUISessionManager {

    private static final Map<UUID, GUISession> sessions = new HashMap<>();
    private static final long SESSION_TIMEOUT = 60_000L; // 60 seconds

    // ─────────────────────────────────────────
    // 🎮 Session Lifecycle
    // ─────────────────────────────────────────

    public static GUISession getOrCreate(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), GUISession::new);
    }

    public static Optional<GUISession> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public static void remove(UUID playerId) {
        sessions.remove(playerId);
    }

    public static void clear() {
        sessions.clear();
    }

    public static Map<UUID, GUISession> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public static boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ─────────────────────────────────────────
    // ⏱️ Timeout & Cleanup
    // ─────────────────────────────────────────

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired(SESSION_TIMEOUT));
    }

    /**
     * 🔒 Closes all inventories of players in GUI sessions.
     */
    public static void closeAll() {
        for (UUID uuid : sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(ConquestSpawners.getInstance(), () -> {
                    player.closeInventory(); // ✅ Explicit no-arg method
                });
            }
        }
    }

    public static void closeAllSync() {
        for (UUID uuid : sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.closeInventory(); // ✅ Do this immediately
            }
        }
        sessions.clear(); // 🧹 Optional: clean up session map
    }

    public static void expireInactiveSessions(long timeoutMillis) {
        long now = System.currentTimeMillis();
        Set<UUID> expired = new HashSet<>();

        for (Map.Entry<UUID, GUISession> entry : sessions.entrySet()) {
            GUISession session = entry.getValue();
            if (session.wasClosed()) continue;
            if (now - session.getLastInteraction() > timeoutMillis) {
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(ConquestSpawners.getInstance(), () -> {
                    player.closeInventory(); // ✅ Disambiguated
                });
            }
            sessions.remove(uuid);
        }
    }
}
