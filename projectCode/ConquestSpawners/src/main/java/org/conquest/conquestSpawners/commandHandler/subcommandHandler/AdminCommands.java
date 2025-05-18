package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

/**
 * 🛠 AdminCommands
 * Handles all /spawners admin subcommands.
 */
public class AdminCommands {

    /**
     * Routes /spawners admin <sub> commands
     */
    public static boolean handle(Player player, String[] args) {
        String sub = args[1].toLowerCase();

        return switch (sub) {
            case "reload" -> handleReload(player);
            case "help" -> {
                MessageResponseManager.sendHelpPage(player, "admin-help", 1);
                yield true;
            }
            default -> {
                MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
                yield true;
            }
        };
    }

    private static boolean handleReload(Player player) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_RELOAD)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        ConquestSpawners.getInstance().reload();
        MessageResponseManager.send(player, AdminMessageModels.CONFIG_RELOADED);
        return true;
    }
}
