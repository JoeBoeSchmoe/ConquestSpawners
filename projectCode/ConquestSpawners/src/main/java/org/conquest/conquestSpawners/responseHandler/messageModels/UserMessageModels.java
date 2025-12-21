package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 🎮 UserMessageModels
 * Enum keys for referencing structured userMessages.yml paths.
 */
public enum UserMessageModels {

    // ⛔ Not a player
    NOT_PLAYER("not-player"),

    // ❓ General command responses
    UNKNOWN_COMMAND("unknown-command"),
    USAGE_HINT("usage-hint"),

    // ⏱️ Cooldowns
    COMMAND_ON_COOLDOWN("command-on-cooldown"),

    // 🧱 Spawner messages
    SPAWNER_PLACE_FAILED("spawner-place-failed"),
    SPAWNER_PLACE_SUCCESS("spawner-place-success"),
    SPAWNER_PICKUP_SUCCESS("spawner-pickup-success"),
    SPAWNER_PICKUP_FAILED("spawner-pickup-failed"),
    SPAWNER_UPGRADE_INSUFFICIENT_FUNDS("spawner-upgrade-insufficient-funds"),

    // 🏰 Clan spawner limits
    CLAN_SPAWNER_CAPACITY_REACHED("clan-spawner-capacity-reached"),

    // 🔒 Spawner upgrade lock
    SPAWNER_UPGRADE_MENU_IN_USE("spawner-upgrade-menu-in-use");

    private final String path;

    UserMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
