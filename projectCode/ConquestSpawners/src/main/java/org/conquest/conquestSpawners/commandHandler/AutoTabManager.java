package org.conquest.conquestSpawners.commandHandler;

import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * ⌨️ AutoTabManager
 * Handles tab suggestions for all spawner commands.
 */
public class AutoTabManager {

    private static final List<String> ROOT_COMMANDS = Arrays.asList(
            "help", "give", "info", "admin"
    );

    public static List<String> getSuggestions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String root : ROOT_COMMANDS) {
                if (root.startsWith(input)) {
                    matches.add(root);
                }
            }
            return matches;
        }

        // Admin subcommands, nested tabs etc.
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return Arrays.asList("reload", "give", "help");
        }

        return Collections.emptyList();
    }
}
