package org.conquest.conquestSpawners.responseHandler.messageModels;

/**
 * 🏟️ ArenaMessageModels
 * Enum keys for accessing structured arenaMessages.yml sections.
 */
public enum ArenaMessageModels {

    // ❓ HELP
    ARENA_HELP_HEADER("help.header"),

    // 📘 CREATE
    ARENA_IN_USE("state.setup-blocked-used"),
    ARENA_NOT_READY("state.not-ready"),
    CREATE_REGISTERED("create.registered"),
    CREATE_ALREADY_EXISTS("create.already-exists"),
    CREATE_USAGE_HINT("create.usage-hint"),

    // REGENERATE
    REGENERATE_USAGE_HINT("regenerate.usage-hint"),
    REGENERATE_STARTED("regenerate.started"),
    REGENERATE_ALREADY_RUNNING("regenerate.already-running"),
    REGENERATE_DISABLED("regenerate.disabled"),

    // ✅ SAVE
    SAVE_SUCCESS("save.success"),
    SAVE_ALREADY_IN_PROGRESS("save.already-in-progress"),
    SAVE_FAILED("save.fail"),
    SAVE_IN_PROGRESS_STARTED("save.started"),
    SAVE_BLOCKED_DUE_TO_REGEN("save.blocked-due-to-regen"),
    SAVE_MISSING_SPAWNS("save.missing-spawns"),
    TOGGLE_ALREADY_LOADING("toggle.already-loading"),

    // 🧭 SETUP
    SETUP_ENTERED_ENTER_FIRST("setup.enter-first"),
    SETUP_ENTERED_EDIT_MODE("setup.entered-edit-mode"),
    SETUP_EXITED_EDIT_MODE("setup.exited-edit-mode"),
    SETUP_SETSPAWN_SUCCESS("setup.setspawn.success"),
    SETUP_SETSPAWN_USAGE_HINT("setup.setspawn.usage-hint"),
    SETUP_SETSPAWN_INVALID_MODE("setup.setspawn.invalid-mode"),
    SETUP_SETSPAWN_UNSUPPORTED_MODE("setup.setspawn.unsupported-mode"),
    SETUP_SETSPAWN_INVALID_INDEX("setup.setspawn.invalid-index"),
    SETUP_REGION_WAND_GIVEN("setup.region.wand-given"),
    SETUP_REGION_SET_SUCCESS("setup.region.setregion.success"),
    SETUP_REGION_SET_NOT_SELECTED("setup.region.setregion.not-selected"),
    SETUP_REGION_SET_NOT_DEFINED("setup.region.setregion.not-defined"),
    SETUP_SETSPAWN_OUTSIDE_REGION("setup.setspawn.outside-region"),
    WAND_DROP_PREVENTED("setup.wand.drop-prevented"),
    WAND_CONTAINER_MOVE_PREVENTED("setup.wand.container-move-prevented"),
    SETUP_REGION_POSITION_1_SET("setup.region.position-1-set"),
    SETUP_REGION_POSITION_2_SET("setup.region.position-2-set"),
    SETUP_REGION_MISMATCHED_WORLDS("setup.region.mismatched-worlds"),
    SETUP_REGION_TOO_LARGE("setup.region.too-large"),
    SETUP_USAGE_HINT("setup.usage-hint"),

    // 🏗 ADD GAMEMODE
    SETUP_ADDGAMEMODE_SUCCESS("setup.addgamemode.success"),
    SETUP_ADDGAMEMODE_USAGE_HINT("setup.addgamemode.usage-hint"),
    SETUP_ADDGAMEMODE_ALREADY_EXISTS("setup.addgamemode.already-exists"),

    // 🧼 REMOVE GAMEMODE
    SETUP_REMOVEGAMEMODE_USAGE_HINT("setup.removegamemode.usage-hint"),
    SETUP_REMOVEGAMEMODE_NOT_PRESENT("setup.removegamemode.not-present"),
    SETUP_REMOVEGAMEMODE_SUCCESS("setup.removegamemode.success"),

    // 🏗 ADD KIT
    SETUP_ADDKIT_USAGE_HINT("setup.addkit.usage-hint"),
    SETUP_ADDKIT_SUCCESS("setup.addkit.success"),

    // 🧼 REMOVE KIT
    SETUP_REMOVEKIT_USAGE_HINT("setup.removekit.usage-hint"),
    SETUP_REMOVEKIT_SUCCESS("setup.removekit.success"),

    // 🏗 KIT ALREADY ASSIGNED
    KIT_ALREADY_ASSIGNED("setup.addkit.already-assigned"),

    // 🧼 KIT NOT ASSIGNED
    KIT_NOT_ASSIGNED("setup.removekit.not-assigned"),


    // ⚙️ LOAD METHOD
    SET_LOAD_METHOD_USAGE_HINT("setup.set-load-method.usage-hint"),
    SET_LOAD_METHOD_SUCCESS("setup.set-load-method.success"),
    SET_LOAD_METHOD_INVALID("setup.set-load-method.invalid-method"),

    // ADMIN SPAWN
    ADMINSPAWN_NOT_SET("arena.adminspawn.not-set"),
    SETUP_SETADMINSPAWN_USAGE_HINT("setup.setadminspawn.usage-hint"),
    SETUP_SETADMINSPAWN_SUCCESS("setup.setadminspawn.success"),

    // ⚙️ TOGGLE
    TOGGLE_ENABLED("toggle.enabled"),
    TOGGLE_DISABLED("toggle.disabled"),
    TOGGLE_ALREADY_ENABLED("toggle.already-enabled"),
    TOGGLE_ALREADY_DISABLED("toggle.already-disabled"),
    SETUP_BLOCKED("toggle.setup-blocked"),
    TOGGLE_USAGE_HINT("toggle.usage-hint"),

    // ♻️ REGEN
    REGEN_ENABLED("regen-toggle.enabled"),
    REGEN_DISABLED("regen-toggle.disabled"),
    REGEN_ALREADY_ENABLED("regen-toggle.already-enabled"),
    REGEN_ALREADY_DISABLED("regen-toggle.already-disabled"),
//    REGEN_TOO_LARGE("regen-toggle.too-large"),
    REGEN_TOO_LARGE_DISABLED("regen-toggle.setregion-too-big"),
//    REGEN_USAGE_HINT("regen-toggle.usage-hint"),

    // 📋 LIST
//    LIST_EMPTY("list.empty"),
//    LIST_HEADER("list.header"),
//    LIST_ENTRY("list.entry"),

    // ❌ DELETE
    DELETE_SUCCESS("delete.success"),
    ARENA_NOT_FOUND("arena.not-found"),
    DELETE_USAGE_HINT("delete.usage-hint"),

    // 🧾 INFO
//    INFO_DISPLAY("info.display"),
    INFO_NOT_FOUND("info.not-found"),
    INFO_USAGE_HINT("info.usage-hint"),

    // 🎮 DUEL GAME FLOW
    DUEL_GAME_START_ANNOUNCE("game.start"),
    DUEL_GAME_END_WINNER("game.end.winner"),
    DUEL_GAME_END_LOSER("game.end.loser"),
    DUEL_GAME_END_ANNOUNCE("game.announce"),
    DUEL_FORFEIT_NOTICE("game.forfeit-notice");


    private final String path;

    ArenaMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}