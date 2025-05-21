package org.conquest.conquestSpawners.commandHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 🔄 AutoTabManager
 * Provides context-aware tab suggestions for all ConquestSpawners commands.
 */
public class AutoTabManager {

    private static final List<String> ROOT_COMMANDS = List.of(
            "admin"
    );

    private static final List<String> ADMIN_SUBCOMMANDS = List.of(
            "help",
            "reload",
            "give"
    );

    public static List<String> getSuggestions(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            return partialMatch(args[0], ROOT_COMMANDS);
        }

        if (!args[0].equalsIgnoreCase("admin")) {
            return Collections.emptyList();
        }

        // /spawners admin ...
        if (args.length == 2) {
            return partialMatch(args[1], ADMIN_SUBCOMMANDS);
        }

        // /spawners admin give ...
        if (args[1].equalsIgnoreCase("give")) {
            switch (args.length) {
                case 3 -> {
                    // <player>
                    return partialMatch(args[2], getOnlinePlayerNames());
                }
                case 4 -> {
                    // <mob>
                    return partialMatch(args[3], getMobKeys());
                }
                case 5 -> {
                    // <level>
                    String mobKey = args[3].toLowerCase();
                    MobManager manager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
                    MobDataModel mob = manager.getMob(mobKey);
                    if (mob != null) {
                        return partialMatch(args[4], getLevelStrings(mob));
                    }
                }
                case 6 -> {
                    // <quantity>
                    return partialMatch(args[5], List.of("1", "2", "3", "4", "5", "6", "7", "8", "16", "32","64"));
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    private static List<String> getMobKeys() {
        MobManager manager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
        return manager.getAllMobs().stream()
                .map(MobDataModel::getMobType)
                .sorted()
                .toList();
    }

    private static List<String> getLevelStrings(MobDataModel mob) {
        return mob.getSpawnerLevels().keySet().stream()
                .map(String::valueOf)
                .sorted()
                .toList();
    }

    private static List<String> partialMatch(String input, List<String> options) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
