package org.conquest.conquestSpawners.commandHandler.subcommandHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerBuilder;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

import java.util.HashMap;
import java.util.Map;

public class AdminCommands {

    /**
     * Routes /spawners admin <sub> commands.
     * ✅ Console is allowed for admin commands.
     */
    public static boolean handle(CommandSender sender, String[] args) {

        if (args.length < 2) {
            MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "help" -> {
                if (sender instanceof Player player) {
                    MessageResponseManager.sendHelpPage(player, "admin-help", 1);
                } else {
                    // Console can't view GUI help pages; just show usage hint
                    MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
                }
                yield true;
            }
            case "give" -> handleGive(sender, args);
            default -> {
                MessageResponseManager.send(sender, AdminMessageModels.ADMIN_USAGE_HINT, Map.of());
                yield true;
            }
        };
    }

    private static boolean handleReload(CommandSender sender) {
        if (!PermissionManager.has(sender, PermissionModels.ADMIN_RELOAD)) {
            MessageResponseManager.send(sender, AdminMessageModels.NO_PERMISSION, Map.of());
            return true;
        }

        ConquestSpawners.getInstance().reload();
        MessageResponseManager.send(sender, AdminMessageModels.CONFIG_RELOADED, Map.of());
        return true;
    }

    private static boolean handleGive(CommandSender sender, String[] args) {
        if (!PermissionManager.has(sender, PermissionModels.ADMIN_GIVE)) {
            MessageResponseManager.send(sender, AdminMessageModels.NO_PERMISSION, Map.of());
            return true;
        }

        // /spawners admin give <player> <mob> <level> <quantity>
        if (args.length < 6) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_USAGE, Map.of());
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Player not found."));
            return true;
        }

        String mobKey = args[3].toLowerCase();
        MobManager mobManager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
        MobDataModel mob = mobManager.getMob(mobKey);

        if (mob == null) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Invalid mob."));
            return true;
        }

        int level;
        int quantity;
        try {
            level = Integer.parseInt(args[4]);
            quantity = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Level and quantity must be numbers."));
            return true;
        }

        if (quantity <= 0) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Quantity must be at least 1."));
            return true;
        }

        if (level <= 0) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Level must be at least 1."));
            return true;
        }

        if (!mob.getSpawnerLevels().containsKey(level)) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Level not defined for this mob."));
            return true;
        }

        // ✅ Give spawner items
        // (Strict inventory rule preserved, matching your existing behavior)
        if (target.getInventory().firstEmpty() == -1) {
            MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_FAILED,
                    Map.of("reason", "Target player's inventory is full."));
            return true;
        }

        for (int i = 0; i < quantity; i++) {
            target.getInventory().addItem(SpawnerBuilder.buildSpawner(mob, level));
        }
        target.updateInventory();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("mob", mobKey);
        placeholders.put("level", String.valueOf(level));
        placeholders.put("quantity", String.valueOf(quantity));

        MessageResponseManager.send(sender, AdminMessageModels.SPAWNER_GIVE_SUCCESS, placeholders);
        return true;
    }
}
