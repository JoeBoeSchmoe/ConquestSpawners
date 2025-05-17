package org.conquest.conquestSpawners.commandHandler;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.commandHandler.subcommandHandler.AdminCommands;
import org.conquest.conquestSpawners.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestSpawners.cooldownHandler.CommandCooldownManager;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 🧭 CommandManager
 * Central executor and tab completer for /spawner and subcommands.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        registerAliases();
    }

    private static void registerAliases() {
        ALIAS_MAP.put("help", "help");
        ALIAS_MAP.put("h", "help");

        ALIAS_MAP.put("give", "give");
        ALIAS_MAP.put("g", "give");

        ALIAS_MAP.put("info", "info");
        ALIAS_MAP.put("i", "info");

        ALIAS_MAP.put("admin", "admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return UserCommands.sendNotPlayer(sender);
        }

        if (CommandCooldownManager.isOnCooldown(player.getUniqueId())) {
            MessageResponseManager.send(player, UserMessageModels.COMMAND_ON_COOLDOWN);
            return true;
        }
        CommandCooldownManager.mark(player.getUniqueId());

        if (args.length == 0) {
            return UserCommands.sendUsageHint(player);
        }

        String input = args[0].toLowerCase();
        String subcommand = ALIAS_MAP.getOrDefault(input, null);

        if (subcommand == null) {
            MessageResponseManager.send(player, UserMessageModels.UNKNOWN_COMMAND);
            return true;
        }

        if (subcommand.equals("admin")) {
            return handleAdmin(player, args);
        }

        return UserCommands.handle(player, subcommand, args);
    }

    private boolean handleAdmin(Player player, String[] args) {
        if (args.length < 2) {
            MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
            return true;
        }

        return AdminCommands.handle(player, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return AutoTabManager.getSuggestions(sender, args);
    }
}
