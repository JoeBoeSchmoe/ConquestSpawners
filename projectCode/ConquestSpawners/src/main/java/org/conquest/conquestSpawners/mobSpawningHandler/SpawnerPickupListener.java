package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerBuilder;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpawnerPickupListener implements Listener {

    private final ConquestSpawners plugin;

    public SpawnerPickupListener(ConquestSpawners plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSpawnerBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();

        // 🛑 Require pickup permission
        if (!player.hasPermission(PermissionModels.USER_PICKUP.getNode())) {
            MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_FAILED,
                    Map.of("reason", "You are not allowed to pick up spawners."));
            event.setCancelled(true);
            return;
        }

        // ✅ Bypass tool checks if permission granted
        if (!player.hasPermission(PermissionModels.USER_PICKUP_BYPASS.getNode())) {
            ItemStack tool = player.getInventory().getItemInMainHand();

            // 🔧 Tool type check
            List<String> allowed = plugin.getConfigurationManager().getConfig()
                    .getStringList("pickup-requirements.allowed-tools");

            boolean validTool = allowed.stream()
                    .map(Material::valueOf)
                    .anyMatch(mat -> mat == tool.getType());

            if (!validTool) {
                MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_FAILED,
                        Map.of("reason", "You can't break this spawner with that tool."));
                event.setCancelled(true);
                return;
            }

            // 🪓 Silk Touch check
            boolean requireSilk = plugin.getConfigurationManager().getConfig()
                    .getBoolean("pickup-requirements.require-silk-touch");

            if (requireSilk && !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_FAILED,
                        Map.of("reason", "You need Silk Touch to collect this spawner."));
                event.setCancelled(true);
                return;
            }
        }

        // 🛑 Cancel vanilla drop behavior
        event.setExpToDrop(0);
        event.setDropItems(false);

        // 🧬 Retrieve mob + level from the spawner block
        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner spawner)) return;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        // If no NBT, optionally convert from vanilla spawner
        if (mobKey == null || level == null) {
            boolean allowConvert = plugin.getConfig().getBoolean("vanilla-spawner-conversion.enabled", false);

            if (!allowConvert) {
//                MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_FAILED,
//                        Map.of("reason", "This spawner has no custom data and cannot be collected."));
                return;
            }

            EntityType vanillaType = spawner.getSpawnedType();
            if(vanillaType == null) {
                return;
            }
            mobKey = vanillaType.name().toLowerCase(Locale.ROOT);
            level = 1;

            MobDataModel fallback = plugin.getConfigurationManager().getMobManager().getMob(mobKey);
            if (fallback == null) {
                MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_FAILED,
                        Map.of("reason", "Vanilla spawner type <red>" + mobKey + "</red> is not registered."));
                return;
            }

            ItemStack drop = SpawnerBuilder.buildSpawner(fallback, level);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            //player.sendMessage("§aVanilla spawner converted and collected as level 1 " + mobKey + "!");
            return;
        }

        // NBT-based custom spawner pickup
        MobDataModel mob = plugin.getConfigurationManager().getMobManager().getMob(mobKey.toLowerCase(Locale.ROOT));
        if (mob == null) {
            player.sendMessage("§cInvalid spawner type. Mob config not found.");
            event.setCancelled(true);
            return;
        }

        // 🎁 Drop custom item with correct mob + level
        ItemStack drop = SpawnerBuilder.buildSpawner(mob, level);
        block.getWorld().dropItemNaturally(block.getLocation(), drop);
        DecentHologramsManager.removeHologramIfExists(block.getLocation());

        MessageResponseManager.send(player, UserMessageModels.SPAWNER_PICKUP_SUCCESS);
    }
}
