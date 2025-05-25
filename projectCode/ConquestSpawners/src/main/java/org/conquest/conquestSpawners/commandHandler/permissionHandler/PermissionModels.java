package org.conquest.conquestSpawners.commandHandler.permissionHandler;

/**
 * 🔐 PermissionModels
 * Enum of all permission nodes used in ConquestSpawners.
 */
public enum PermissionModels {

    // 🎮 User Permissions
    USER_BASE("conquestspawners.user.basecommand"),
    USER_HELP("conquestspawners.user.help"),
    USER_GIVE("conquestspawners.user.give"),
    USER_INFO("conquestspawners.user.info"),
    USER_PICKUP("conquestspawners.user.pickup"),
    USER_PICKUP_BYPASS("conquestspawners.user.pickup.bypass"),

    // 🛠 Admin Permissions
    ADMIN_BASE("conquestspawners.admin.basecommand"),
    ADMIN_RELOAD("conquestspawners.admin.reload"),
    ADMIN_GIVE("conquestspawners.admin.give"),
    ADMIN_ALL("conquestspawners.admin.*");

    private final String node;

    PermissionModels(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }

    @Override
    public String toString() {
        return node;
    }
}
