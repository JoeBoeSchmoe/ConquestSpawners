package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 📜 KitMessageModels
 *
 * Defines all message paths for the kitMessages.yml config file.
 */
public enum KitMessageModels {

    // ================== ❓ HELP ==================
    HELP_HEADER("help.header"),

    // ================== 🛠️ KIT CREATION ==================
    CREATE_SUCCESS("create.success"),
    CREATE_USAGE("create.usage"),
    CREATE_FAILED("create.failed"),

    // ================== 🗑️ KIT DELETION ==================
    DELETE_SUCCESS("delete.success"),
    DELETE_USAGE("delete.usage"),
    DELETE_FAILED("delete.failed"),

    // ================== 🎁 KIT CLAIMING ==================
    CLAIM_SUCCESS("claim.success"),
    CLAIM_USAGE("claim.usage"),
    CLAIM_FAILED("claim.failed"),

    // ================== 📜 KIT LISTING ==================
    LIST_HEADER("list.header"),
    LIST_ENTRY("list.entry"),

    // ================== 🗳️ VOTING SYSTEM ==================
    VOTE_VOTED_FOR_KIT("vote.voted-for-kit"),
    VOTE_CHANGED_VOTE("vote.changed-vote"),

    // ================== 🚫 ERRORS & PERMISSIONS ==================
    ERROR_KIT_NOT_FOUND("error.kit-not-found"),

    // ================== 💾 KIT SAVING ==================
    SAVE_SUCCESS("save.success"),
    SAVE_USAGE("save.usage");

    private final String path;

    KitMessageModels(String path) {
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
