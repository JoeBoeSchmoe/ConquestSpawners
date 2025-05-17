package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 🙋 UserMessageModels
 * Enum keys for referencing structured userMessages.yml paths.
 */
public enum UserMessageModels {

    // 🧱 Basic interactions
    PLACE_SUCCESS("spawner.place-success"),
    BREAK_SUCCESS("spawner.break-success"),
    UPGRADE_SUCCESS("spawner.upgrade-success"),
    UPGRADE_FAIL("spawner.upgrade-fail"),
    MAX_LEVEL_REACHED("spawner.max-level"),

    // ❌ Deny feedback
    WORLD_NOT_ALLOWED("denied.world"),
    NO_PERMISSION("denied.permission"),
    COOLDOWN_ACTIVE("denied.cooldown"),
    INVALID_ACTION("denied.invalid-action");

    private final String path;

    UserMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
