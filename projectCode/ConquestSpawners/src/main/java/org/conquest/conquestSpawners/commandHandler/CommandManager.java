package org.conquest.conquestSpawners.commandHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.commandHandler.subcommandHandler.AdminCommands;
import org.conquest.conquestSpawners.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestSpawners.cooldownHandler.CommandCooldownManager;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 🧭 CommandManager
 * Central executor and tab completer for /conquestspawners and its aliases.
 * Routes subcommands to AdminCommands and UserCommands based on alias map and permission level.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        registerAliases();
    }

    private static void registerAliases() {
        // Core commands
        ALIAS_MAP.put("help", "help");
        ALIAS_MAP.put("h", "help");

        ALIAS_MAP.put("give", "give");
        ALIAS_MAP.put("g", "give");

        ALIAS_MAP.put("info", "info");
        ALIAS_MAP.put("i", "info");

        // Admin group
        ALIAS_MAP.put("admin", "admin");

        // Utility
        ALIAS_MAP.put("reload", "reload");
        ALIAS_MAP.put("r", "reload");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            // If console runs /spawners with no args, just show usage/help
            if (sender instanceof Player p) {
                return UserCommands.sendUsageHint(p);
            }
            MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
            return true;
        }

        String input = args[0].toLowerCase();
        String subcommand = ALIAS_MAP.getOrDefault(input, null);

        if (subcommand == null) {
            if (sender instanceof Player p) {
                MessageResponseManager.send(p, AdminMessageModels.ADMIN_USAGE_HINT);
            } else {
                MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
            }
            return true;
        }

        // ✅ Console is allowed to run admin commands
        if (subcommand.equals("admin")) {
            return handleAdmin(sender, args);
        }

        // ❌ Everything else stays player-only
        if (!(sender instanceof Player player)) {
            return UserCommands.sendNotPlayer(sender);
        }

        // Cooldowns only apply to players
        if (CommandCooldownManager.isOnCooldown(player.getUniqueId())) {
            MessageResponseManager.send(player, UserMessageModels.COMMAND_ON_COOLDOWN);
            return true;
        }
        CommandCooldownManager.mark(player.getUniqueId());

        return UserCommands.handle(player, subcommand, args);
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
            return true;
        }
        return AdminCommands.handle(sender, args);
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return AutoTabManager.getSuggestions(sender, args);
    }
}
