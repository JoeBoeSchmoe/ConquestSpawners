package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        if (item.getType() != Material.SPAWNER || !item.hasItemMeta()) return;

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("conquestspawners", "mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("conquestspawners", "level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) return;

        MobDataModel mob = mobManager.getMob(mobKey.toLowerCase(Locale.ROOT));
        if (mob == null) {
            reject(player, event, "Invalid spawner NBT (mob not found).");
            return;
        }

        SpawnerRequirementsModel req = mob.getRequirements();
        Location loc = event.getBlock().getLocation();
        int y = loc.getBlockY();

        // --- Biome Check ---
        if (req.inBiome && !req.allowedBiomes.isEmpty()) {
            String biome = loc.getBlock().getBiome().getKey().getKey().toLowerCase(Locale.ROOT);
            List<String> allowed = req.allowedBiomes.stream().map(String::toLowerCase).toList();
            if (!allowed.contains(biome)) {
                reject(player, event, "Cannot place this spawner in <gray>" + biome.replace("_", " ") + "</gray> biome.");
                return;
            }
        }

        // --- Y-Axis Checks ---
        if (req.aboveSeaLevel && y <= 63) {
            reject(player, event, "Must be placed above sea level (Y > 63). Current Y=" + y);
            return;
        }
        if (req.belowSeaLevel && y >= 63) {
            reject(player, event, "Must be placed below sea level (Y < 63). Current Y=" + y);
            return;
        }
        if (req.aboveYAxis && y <= 63) {
            reject(player, event, "Must be placed above Y=63. Current Y=" + y);
            return;
        }
        if (req.belowYAxis && y >= 63) {
            reject(player, event, "Must be placed below Y=63. Current Y=" + y);
            return;
        }

        // ✅ Passed all checks — update block metadata
        Block block = event.getBlockPlaced();
        BlockState state = block.getState();
        if (state instanceof CreatureSpawner spawner) {
            try {
                EntityType type = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
                spawner.setSpawnedType(type);
            } catch (IllegalArgumentException e) {
                spawner.setSpawnedType(EntityType.PIG); // fallback
            }
            spawner.update(true, false); // update spawner block
        }
    }

    private void reject(Player player, BlockPlaceEvent event, String reason) {
        MessageResponseManager.send(player, AdminMessageModels.SPAWNER_GIVE_FAILED, Map.of("reason", reason));
        event.setCancelled(true);
    }
}
