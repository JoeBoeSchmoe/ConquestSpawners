package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.Chunk;
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
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import java.util.*;

public class SpawnerPlaceListener implements Listener {

    private final MobManager mobManager;
    private final ConquestSpawners plugin;

    public SpawnerPlaceListener(MobManager mobManager) {
        this.mobManager = mobManager;
        this.plugin = ConquestSpawners.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) return;
        mobKey = mobKey.toLowerCase(Locale.ROOT);

        MobDataModel mob = mobManager.getMob(mobKey);
        if (mob == null) {
            reject(player, event, "Invalid spawner NBT (mob not found).");
            return;
        }

        SpawnerRequirementsModel req = mob.getRequirements();
        Location loc = event.getBlock().getLocation();
        int y = loc.getBlockY();

        // Biome restriction
        if (req.inBiome && !req.allowedBiomes.isEmpty()) {
            String biome = loc.getBlock().getBiome().getKey().getKey().toLowerCase(Locale.ROOT);
            Set<String> allowed = new HashSet<>();
            for (String b : req.allowedBiomes) allowed.add(b.toLowerCase(Locale.ROOT));

            if (!allowed.contains(biome)) {
                reject(player, event, "Cannot place this spawner in <gray>" + biome.replace("_", " ") + "</gray> biome.");
                return;
            }
        }

        // Y-axis checks
        if ((req.aboveSeaLevel && y <= 63) || (req.belowSeaLevel && y >= 63) ||
                (req.aboveYAxis && y <= 63) || (req.belowYAxis && y >= 63)) {
            reject(player, event, "Cannot place at Y=" + y + " due to vertical restrictions.");
            return;
        }

        // Chunk spawner limit checks
        Chunk chunk = loc.getChunk();
        int sameMobCount = 0;
        int totalSpawnerCount = 0;

        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof TileState tile)) continue;

            PersistentDataContainer existing = tile.getPersistentDataContainer();
            String existingKey = existing.get(ItemUtility.key("mob"), PersistentDataType.STRING);
            if (existingKey != null) {
                totalSpawnerCount++;
                if (existingKey.equalsIgnoreCase(mobKey)) {
                    sameMobCount++;
                }
            }
        }

        int globalLimit = plugin.getConfig().getInt("max-total-spawners-per-chunk", -1);
        if (globalLimit != -1 && totalSpawnerCount >= globalLimit) {
            reject(player, event, "Chunk has reached the global spawner limit (<gray>" + totalSpawnerCount + "/" + globalLimit + "</gray>).");
            return;
        }

        int mobLimit = mob.getAllowedSpawnersPerChunkResolved();
        if (sameMobCount >= mobLimit) {
            reject(player, event, "Too many <green>" + mobKey + "</green> spawners in this chunk (<gray>" + sameMobCount + "/" + mobLimit + "</gray>).");
            return;
        }

        // ✅ Set data on placed spawner + display entity
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            reject(player, event, "Block does not support persistent data.");
            return;
        }

        // Purely cosmetic display mob inside the spawner block
        try {
            EntityType type = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
            spawner.setSpawnedType(type);
        } catch (IllegalArgumentException e) {
            spawner.setSpawnedType(EntityType.PIG); // fallback visual
        }

        PersistentDataContainer placedData = spawner.getPersistentDataContainer();
        placedData.set(ItemUtility.key("mob"), PersistentDataType.STRING, mobKey);
        placedData.set(ItemUtility.key("level"), PersistentDataType.INTEGER, level);
        spawner.update(true);
    }

    private void reject(Player player, BlockPlaceEvent event, String reason) {
        MessageResponseManager.send(player, UserMessageModels.SPAWNER_PLACE_FAILED,
                Map.of("reason", reason));
        event.setCancelled(true);
    }
}
