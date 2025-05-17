package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 📜 QueueMessageModels
 * <p>
 * Defines all message paths for the queueMessages.yml config file.
 */
public enum QueueMessageModels {

    // ================== ⏳ QUEUE TIMEOUT ==================
    TIMEOUT_EXPIRED("timeout.expired"),

    // ================== ➞ QUEUE JOIN ==================
    QUEUE_ENTERED("queue.entered"),
    QUEUE_ALREADY_JOINED("queue.already-joined"),
    QUEUE_VOTE_REQUIRED("queue.vote-required"),
    QUEUE_NEEDING_PLAYERS_REMINDER("queue.reminder.waiting-for-players"),
    QUEUE_NEEDING_ARENA_REMINDER("queue.reminder.waiting-for-arena"),
    QUEUE_JOIN_USAGE_HINT("queue.join-usage-hint"),
    QUEUE_UNKNOWN_GAMEMODE("queue.unknown-gamemode"),
    QUEUE_UNKNOWN_ARENA("queue.unknown-arena"),
    QUEUE_ARENA_NOT_ENABLED("queue.arena-not-enabled"),
    QUEUE_ARENA_DOESNT_SUPPORT_GAMEMODE("queue.arena-doesnt-support-gamemode"),

    // ================== ❌ QUEUE LEAVE ==================
    LEAVE_REMOVED("leave.removed"),
    LEAVE_ALREADY_LEFT("leave.already-left"),

    // ================== ⚔ MATCH FOUND ==================
    MATCHMAKING_STARTING("matchmaking.starting"),
    MATCH_FOUND("matchmaking.starting"),

    // ================== 🚫 QUEUE KICKS ==================
    KICK_BLOCKED_PLAYER("kick.blocked-player"),
    KICK_NOT_ENOUGH_PLAYERS("kick.not-enough-players"),

    // ================== 🚫 QUEUE ABANDON ==================
    QUEUE_ABANDONED("queue.abandoned");

    private final String path;

    QueueMessageModels(String path) {
        this.path = path;
    }

    /**
     * Gets the config path for this message.
     *
     * @return The path key.
     */
    public String getPath() {
        return path;
    }
}
