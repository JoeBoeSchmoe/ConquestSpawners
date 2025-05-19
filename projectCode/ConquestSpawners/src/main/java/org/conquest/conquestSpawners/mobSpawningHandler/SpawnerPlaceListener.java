package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.*;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SpawnerPlaceListener implements Listener {

    private final MobManager mobManager;

    public SpawnerPlaceListener(MobManager mobManager) {
        this.mobManager = mobManager;
        Bukkit.getPluginManager().registerEvents(this, ConquestSpawners.getInstance());
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!item.hasItemMeta()) return;

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("conquestspawners", "mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("conquestspawners", "level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) return;

        MobDataModel mob = mobManager.getMob(mobKey.toLowerCase(Locale.ROOT));
        if (mob == null) {
            reject(player, event, "Invalid spawner NBT (mob not found).");
            return;
        }

        // === Validate placement ===
        SpawnerRequirementsModel req = mob.getRequirements();
        Location loc = event.getBlock().getLocation();
        int y = loc.getBlockY();

        if (req.inBiome && !req.allowedBiomes.isEmpty()) {
            String biome = loc.getBlock().getBiome().getKey().getKey().toLowerCase(Locale.ROOT);
            List<String> allowed = req.allowedBiomes.stream().map(String::toLowerCase).toList();
            if (!allowed.contains(biome)) {
                reject(player, event, "Cannot place this spawner in <gray>" + biome.replace("_", " ") + "</gray> biome.");
                return;
            }
        }

        if ((req.aboveSeaLevel && y <= 63) || (req.belowSeaLevel && y >= 63) ||
                (req.aboveYAxis && y <= 63) || (req.belowYAxis && y >= 63)) {
            reject(player, event, "Cannot place at Y=" + y + " due to vertical restrictions.");
            return;
        }

        // === Store persistent data ===
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof TileState tile)) {
            reject(player, event, "Block does not support persistent data.");
            return;
        }

        PersistentDataContainer placedData = tile.getPersistentDataContainer();
        placedData.set(ItemUtility.key("mob"), PersistentDataType.STRING, mobKey.toLowerCase());
        placedData.set(ItemUtility.key("level"), PersistentDataType.INTEGER, level);
        placedData.set(ItemUtility.key("id"), PersistentDataType.STRING,
                Objects.requireNonNull(data.get(ItemUtility.key("id"), PersistentDataType.STRING)));
        tile.update(true);

        // === Visual spinning mob setup (no spawn logic) ===
        Bukkit.getScheduler().runTask(ConquestSpawners.getInstance(), () -> {
            BlockState bs = block.getState();
            if (bs instanceof CreatureSpawner spawner) {
                try {
                    EntityType visual = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
                    spawner.setSpawnedType(visual);
                } catch (IllegalArgumentException e) {
                    spawner.setSpawnedType(EntityType.PIG);
                }

                spawner.setDelay(20);                 // Short delay to trigger animation
                spawner.setMinSpawnDelay(120);
                spawner.setMaxSpawnDelay(240);
                spawner.setSpawnCount(0);             // Prevent actual spawning
                spawner.setRequiredPlayerRange(0);    // No player needed
                spawner.setSpawnRange(0);             // No spawn range
                spawner.update(true);
            }
        });
    }

    private void reject(Player player, BlockPlaceEvent event, String reason) {
        MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED, Map.of("reason", reason));
        event.setCancelled(true);
    }
}
