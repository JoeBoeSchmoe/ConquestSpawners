package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

/**
 * 🛠 AdminCommands
 * Handles all /spawner admin subcommands.
 */
public class AdminCommands {

    public static boolean handle(Player player, String[] args) {
        String sub = args[1].toLowerCase();

        return switch (sub) {
            case "reload" -> handleReload(player);
            case "give" -> handleAdminGive(player, args);
            case "help" -> {
                MessageResponseManager.send(player, AdminMessageModels.ADMIN_HELP);
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

        // Perform config reload here
        MessageResponseManager.send(player, AdminMessageModels.RELOADED);
        return true;
    }

    private static boolean handleAdminGive(Player player, String[] args) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_GIVE)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        // Admin-level spawner giving logic here
        MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVEN_ADMIN);
        return true;
    }
}
