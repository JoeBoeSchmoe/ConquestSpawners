package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 🎮 UserMessageModels
 * Enum keys for referencing structured userMessages.yml paths.
 */
public enum UserMessageModels {

    // ❓ Help & Usage
    HELP_HEADER("help.header"),
    HELP("help.header"), // ✅ alias if you reference just HELP
    USAGE_HINT("help.usage-hint"),
    UNKNOWN_COMMAND("help.unknown-command"),

    // 🔁 Cooldowns
    COMMAND_ON_COOLDOWN("cooldown.command"),

    // 🎁 Spawner Give
    SPAWNER_GIVE_SUCCESS("give.success"),
    SPAWNER_GIVE_FAIL("give.fail"),
    SPAWNER_GIVEN("give.success"), // ✅ alias for older reference

    // 📄 Info Display
    INFO_DISPLAY("info.display"),
    SPAWNER_INFO("info.display"); // ✅ alias for older reference

    private final String path;

    UserMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
