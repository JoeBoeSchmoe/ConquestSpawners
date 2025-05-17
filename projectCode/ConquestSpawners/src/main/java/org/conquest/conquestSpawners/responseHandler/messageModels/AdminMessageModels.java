package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 👑 AdminMessageModels
 * Enum keys for referencing structured adminMessages.yml paths.
 */
public enum AdminMessageModels {

    // 📜 Help
    HELP_HEADER("help.header"),

    // 🔄 Reloading
    RELOAD_SUCCESS("reload.success"),
    RELOAD_FAIL("reload.fail"),

    // 🎁 Spawner Give
    GIVE_SUCCESS("give.success"),
    GIVE_FAIL("give.fail"),
    GIVE_USAGE_HINT("give.usage-hint"),

    // ❌ Permissions
    NO_PERMISSION("no-permission"),
    INVALID_COMMAND("invalid-command");

    private final String path;

    AdminMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
