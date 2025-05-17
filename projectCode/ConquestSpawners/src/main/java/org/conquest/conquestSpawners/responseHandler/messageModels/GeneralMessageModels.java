package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 📌 GeneralMessageModels
 * Enum keys for accessing structured generalMessages.yml sections.
 */
public enum GeneralMessageModels {

    // 🌐 SYSTEM
    SYSTEM_NOT_PLAYER("system.not-player"),
    SYSTEM_NO_PERMISSION("system.no-permission"),
    DUEL_WORLD_NOT_ALLOWED("system.world-not-allowed"),

    SYSTEM_RELOADED("system.reloaded"),

    SYSTEM_ERROR_GENERIC("system.error.generic"),

    // ❓ HELP
    GENERAL_HELP_HEADER("help.header"),

    // 🛠 ADMIN
    ADMIN_USAGE_HINT("admin-usage-hint"),

    // ⚔️ DUEL
    DUEL_BLOCKING_DISABLED("duel.blocking-disabled"),
    DUEL_COUNTDOWN("duel.countdown"),
    DUEL_START("duel.start"),
    DUEL_ALREADY_IN_PROGRESS("duel.challenge.already-in-duel"),
    DUEL_CHALLENGE_YOU_IN_GAME("duel.challenge.you-in-game"),

    DUEL_CHALLENGE_DISABLED_IN_SETUP("duel.challenge.disabled-in-setup"),
    DUEL_CHALLENGE_TARGET_IN_SETUP("duel.challenge.target-in-setup"),
    DUEL_UNKNOWN_PLAYER("duel.unknown-player"),
    DUEL_CHALLENGE_SELF("duel.challenge-self"),
    DUEL_USAGE_HINT("duel.usage-hint"),
    DUEL_CANCEL_USAGE_HINT("duel.cancel-usage-hint"),
    DUEL_DENY_USAGE_HINT("duel.deny-usage-hint"),
    DUEL_ACCEPT_USAGE_HINT("duel.accept-usage-hint"),
    DUEL_CHALLENGE_SENT("duel.challenge.sent"),
    DUEL_CHALLENGE_RECEIVED("duel.challenge.received"),
    DUEL_CHALLENGE_ALREADY_SENT("duel.challenge.already-sent"),
    DUEL_CHALLENGE_TARGET_BLOCKED_YOU("duel.challenge.target-blocked-you"),
    DUEL_CHALLENGE_YOU_BLOCKED_TARGET("duel.challenge.you-blocked-target"),

    DUEL_RESPONSE_ACCEPT_RECEIVER("duel.response.accept-receiver"),
    DUEL_RESPONSE_ACCEPT_SENDER("duel.response.accept-receiving"),

    DUEL_RESPONSE_DENY_RECEIVER("duel.response.deny-receiver"),
    DUEL_RESPONSE_DENY_SENDER("duel.response.deny-receiving"),

    DUEL_RESPONSE_NO_PENDING("duel.response.no-pending"),
    DUEL_RESPONSE_NO_INVITE_FROM("duel.response.no-invite-from"),
    DUEL_NEED_SPECIFIC_ITEM("duel.challenge.need-specific-item"),

    DUEL_BLOCKED("duel.blocked"),
    DUEL_UNBLOCKED("duel.unblocked"),
    DUEL_BLOCKED_USAGE_HINT("duel.blocked-usage-hint"),
    DUEL_UNBLOCKED_USAGE_HINT("duel.unblocked-usage-hint"),
    DUEL_BLOCK_SELF("duel.block-self"),
    DUEL_UNBLOCK_SELF("duel.unblock-self"),
    DUEL_ALREADY_BLOCKED("duel.already-blocked"),
    DUEL_ALREADY_UNBLOCKED("duel.already-unblocked"),

    DUEL_BLOCKED_LIST_HEADER("duel.blocked-list.header"),
    DUEL_BLOCKED_LIST_ENTRY("duel.blocked-list.entry"),
    DUEL_BLOCKED_LIST_EMPTY("duel.blocked-list.empty"),
    DUEL_TARGET_LEFT("duel.challenge.target-left"),


    DUEL_CANCEL("duel.cancel"),
    DUEL_STATS("duel.stats"),

    // 🔁 COOLDOWNS
    COOLDOWN_INTERACTION("cooldown.interaction"),
    COOLDOWN_COMMAND("cooldown.command"),
    COOLDOWN_GUI("cooldown.gui"),

    // 🕒 GUI TIMEOUT
    GUI_SESSION_EXPIRED("gui.session-expired"),

    // 🔁 TELEPORTATION
    TELEPORTATION_COUNTDOWN("teleportation.countdown"),
    TELEPORTATION_COMPLETE("teleportation.complete"),
    TELEPORTATION_CANCELLED_BY_DEATH("teleportation.cancelled-by-death"),
    TELEPORTATION_CANCELLED_BY_DEAD_OPPONENT("teleportation.cancelled-by-dead-opponent"),
    TELEPORTATION_CANCELLED_BY_QUITTING_OPPONENT("teleportation.cancelled-by-quitting-opponent"),
    TELEPORTATION_CANCELLED_BY_INTERACTION_OR_MOVEMENT("teleportation.cancelled-by-interaction-or-movement"),
    TELEPORTATION_OPPONENT_CANCELLED_BY_INTERACTION_OR_MOVEMENT("teleportation.opponent-cancelled-by-interaction-or-movement"),
    TELEPORTATION_CANCELLED_BY_BEING_INTERRUPTED("teleportation.cancelled-by-being-interrupted"),
    TELEPORTATION_OPPONENT_CANCELLED_BY_BEING_INTERRUPTED("teleportation.opponent-cancelled-by-being-interrupted");


    private final String path;

    GeneralMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
