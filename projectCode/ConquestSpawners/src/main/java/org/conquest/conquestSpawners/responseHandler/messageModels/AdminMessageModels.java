package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 👑 AdminMessageModels
 * Enum keys for referencing structured adminMessages.yml paths.
 */
public enum AdminMessageModels {

    // 📜 Help
    HELP_HEADER("help.header"),
    ADMIN_HELP("help.header"),
    ADMIN_USAGE_HINT("help.usage-hint"),

    // 🔄 Reloading
    RELOAD_SUCCESS("reload.success"),
    RELOAD_FAIL("reload.fail"),
    RELOADED("reload.success"), // ✅ alias for RELOAD_SUCCESS

    // 🎁 Spawner Give
    GIVE_SUCCESS("give.success"),
    GIVE_FAIL("give.fail"),
    GIVE_USAGE_HINT("give.usage-hint"),
    SPAWNER_GIVEN_ADMIN("give.success"), // ✅ alias for GIVE_SUCCESS

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
