package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

/**
 * 🎮 UserCommands
 * Handles all non-admin /spawners subcommands.
 * Currently redirects to admin usage hints only — non-admin commands not available yet.
 */
public class UserCommands {

    public static boolean handle(Player player, String subcommand, String[] args) {
        // Non-admin access is not supported yet
        MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
        return true;
    }

    /**
     * Sends a message to console or command blocks that this command is player-only.
     */
    public static boolean sendNotPlayer(CommandSender sender) {
        MessageResponseManager.send((Player) sender, UserMessageModels.NOT_PLAYER);
        return true;
    }

    /**
     * Sends the general usage hint (admin help) to players without arguments.
     */
    public static boolean sendUsageHint(Player player) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_BASE)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
        return true;
    }
}
