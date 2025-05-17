package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

/**
 * 🎮 UserCommands
 * Handles all non-admin /spawner subcommands.
 */
public class UserCommands {

    public static boolean handle(Player player, String subcommand, String[] args) {
        switch (subcommand) {
            case "help" -> {
                // You can expand with usage details
                MessageResponseManager.send(player, UserMessageModels.HELP);
                return true;
            }
            case "give" -> {
                return handleGive(player, args);
            }
            case "info" -> {
                return handleInfo(player, args);
            }
            default -> {
                MessageResponseManager.send(player, UserMessageModels.UNKNOWN_COMMAND);
                return true;
            }
        }
    }

    private static boolean handleGive(Player player, String[] args) {
        // Your give logic here
        MessageResponseManager.send(player, UserMessageModels.SPAWNER_GIVEN);
        return true;
    }

    private static boolean handleInfo(Player player, String[] args) {
        // Your info logic here
        MessageResponseManager.send(player, UserMessageModels.SPAWNER_INFO);
        return true;
    }

    public static boolean sendNotPlayer(CommandSender sender) {
        sender.sendMessage("§cOnly players can use this command.");
        return true;
    }

    public static boolean sendUsageHint(Player player) {
        MessageResponseManager.send(player, UserMessageModels.USAGE_HINT);
        return true;
    }
}
