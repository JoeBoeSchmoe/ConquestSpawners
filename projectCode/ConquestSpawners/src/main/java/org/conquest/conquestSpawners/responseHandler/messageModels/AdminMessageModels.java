package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 👑 AdminMessageModels
 * Enum keys for referencing structured adminMessages.yml paths.
 * All keys correspond to entries in messages.adminMessages.yml.
 */
public enum AdminMessageModels {

    // ✅ Basic usage and fallback
    ADMIN_USAGE_HINT("admin-usage-hint"),
    UNKNOWN_ADMIN_COMMAND("unknown-admin-command"),

    // 🔄 Reload
    CONFIG_RELOADED("config-reloaded"),

    // 🆘 Help menu
    ADMIN_HELP("admin-help"),

    // ❌ Permissions
    NO_PERMISSION("no-permission"),
    CONSOLE_BLOCKED("console-blocked"),

    // 🎁 Spawner Give Command
    SPAWNER_GIVE_SUCCESS("spawner-give-success"),
    SPAWNER_GIVE_FAILED("spawner-give-failed"),
    SPAWNER_GIVE_USAGE("spawner-give-usage");

    private final String path;

    AdminMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
