package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.Locale;

public class SpawnerListener implements Listener {

    private final MobManager mobManager;

    public SpawnerListener(MobManager mobManager) {
        this.mobManager = mobManager;
        Bukkit.getPluginManager().registerEvents(this, ConquestSpawners.getInstance());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != SpawnReason.SPAWNER) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        Location loc = entity.getLocation();
        Block spawnerBlock = loc.clone().subtract(0, 1, 0).getBlock();

        if (spawnerBlock.getType() != Material.SPAWNER) return;

        BlockState state = spawnerBlock.getState();
        if (!(state instanceof CreatureSpawner spawner)) return;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("conquestspawners", "mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("conquestspawners", "level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) {
            event.setCancelled(true);
            return;
        }

        MobDataModel mob = mobManager.getMob(mobKey.toLowerCase(Locale.ROOT));
        if (mob == null || !mob.getLevels().containsKey(level)) {
            event.setCancelled(true);
            return;
        }

        SpawnerRequirementsModel req = mob.getRequirements();

        // --- Light Level Checks ---
        @SuppressWarnings("deprecation")
        int light = loc.getBlock().getLightLevel();

        if (req.darkness && light >= 8) {
            event.setCancelled(true);
            return;
        }
        if (req.totalDarkness && light > 0) {
            event.setCancelled(true);
            return;
        }
        if (req.light && light <= 8) {
            event.setCancelled(true);
            return;
        }

        // --- Nearby Entity Cap ---
        if (req.maxEntitiesNearby) {
            long count = loc.getWorld().getNearbyEntities(loc, 8, 4, 8).stream()
                    .filter(e -> e.getType() == entity.getType())
                    .count();

            if (count > 8) {
                event.setCancelled(true);
                return;
            }
        }

        // --- On-Ground Solid Block Check ---
        Block below = loc.clone().subtract(0, 1, 0).getBlock();
        if (req.onGround && !below.getType().isSolid()) {
            event.setCancelled(true);
            return;
        }

        // --- Specific Block Restriction ---
        if (req.onBlock && !req.allowedBlocks.isEmpty()) {
            String type = below.getType().name();
            if (req.allowedBlocks.stream().noneMatch(b -> b.equalsIgnoreCase(type))) {
                event.setCancelled(true);
                return;
            }
        }

        // --- Air Clearance Above ---
        if (req.air) {
            Block above = loc.clone().add(0, 2, 0).getBlock();
            if (!above.getType().isAir()) {
                event.setCancelled(true);
            }
        }
    }
}
