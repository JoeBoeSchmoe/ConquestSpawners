package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

import java.util.HashMap;
import java.util.Map;

public class AdminCommands {

    /**
     * Routes /spawners admin <sub> commands.
     */
    public static boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageResponseManager.send(sender, AdminMessageModels.CONSOLE_BLOCKED, Map.of());
            return true;
        }

        if (args.length < 2) {
            MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "reload" -> handleReload(player);
            case "help" -> {
                MessageResponseManager.sendHelpPage(player, "admin-help", 1);
                yield true;
            }
            case "give" -> handleGive(player, args);
            default -> {
                MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
                yield true;
            }
        };
    }

    private static boolean handleReload(Player player) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_RELOAD)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION, Map.of());
            return true;
        }

        ConquestSpawners.getInstance().reload();
        MessageResponseManager.send(player, AdminMessageModels.CONFIG_RELOADED, Map.of());
        return true;
    }

    private static boolean handleGive(Player player, String[] args) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_GIVE)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION, Map.of());
            return true;
        }

        if (args.length < 6) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_USAGE, Map.of());
            return true;
        }

        Player target = player.getServer().getPlayerExact(args[2]);
        if (target == null) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Player not found."));
            return true;
        }

        String mobKey = args[3].toLowerCase();
        MobManager mobManager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
        MobDataModel mob = mobManager.getMob(mobKey);

        if (mob == null) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Invalid mob."));
            return true;
        }

        int level, quantity;
        try {
            level = Integer.parseInt(args[4]);
            quantity = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Level and quantity must be numbers."));
            return true;
        }

        if (!mob.getLevels().containsKey(level)) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Level not defined for this mob."));
            return true;
        }

        // Check for inventory space
        if (target.getInventory().firstEmpty() == -1) {
            MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Target player's inventory is full."));
            return true;
        }

        // 🧱 Give spawner items
        for (int i = 0; i < quantity; i++) {
            target.getInventory().addItem(
                    org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerBuilder.buildSpawner(mob, level)
            );
        }
        target.updateInventory();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("mob", mobKey);
        placeholders.put("level", String.valueOf(level));
        placeholders.put("quantity", String.valueOf(quantity));

        MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_SUCCESS, placeholders);
        return true;
    }

}
