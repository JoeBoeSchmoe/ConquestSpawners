package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerBuilder;

import java.util.Locale;
import java.util.Objects;

public class SpawnerProtectionListener implements Listener {

    private final ConquestSpawners plugin;

    public SpawnerProtectionListener(ConquestSpawners plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::handleSpawnerExplosion);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::handleSpawnerExplosion);
    }

    @EventHandler
    public void onEndermanMove(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.ENDERMAN &&
                event.getBlock().getType() == Material.SPAWNER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.SPAWNER) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.SPAWNER) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.getBlock().getType() == Material.SPAWNER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.SPAWNER) {
            event.setCancelled(true);
        }
    }

    /**
     * Explosion policy:
     * - Always prevent vanilla explosion handling for spawners (remove from blockList)
     * - Optionally drop a spawner item
     * - If we are actually deleting it (setting AIR), ALSO remove from SpawnerManager/disk
     *
     * Returns true to remove from explosion blockList.
     */
    private boolean handleSpawnerExplosion(Block block) {
        if (block.getType() != Material.SPAWNER) return false;

        boolean dropEnabled = plugin.getConfig().getBoolean("explosion-handling.drop-from-explosions", false);
        boolean allowVanillaConvert = plugin.getConfig().getBoolean("vanilla-spawner-conversion.enabled", false);

        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner spawner)) {
            // Still remove it from the explosion list; if you want it actually deleted, we delete it anyway.
            block.setType(Material.AIR, false);
            return true;
        }

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        final boolean isCustomSpawner = (mobKey != null && level != null);

        // If vanilla and conversion disabled: just delete block (or keep it protected if you prefer).
        if (!isCustomSpawner && !allowVanillaConvert) {
            block.setType(Material.AIR, false);
            return true;
        }

        // If vanilla conversion path: fabricate mobKey/level for drop ONLY (not stored in SpawnerManager)
        String resolvedMobKey = mobKey;
        int resolvedLevel = (level == null ? 1 : level);

        if (!isCustomSpawner) {
            EntityType spawned = spawner.getSpawnedType();
            if (spawned == null) {
                block.setType(Material.AIR, false);
                return true;
            }
            resolvedMobKey = Objects.requireNonNull(spawned).name().toLowerCase(Locale.ROOT);
            resolvedLevel = 1;
        }

        // Drop item if enabled and mob exists
        MobDataModel mob = plugin.getConfigurationManager().getMobManager()
                .getMob(resolvedMobKey.toLowerCase(Locale.ROOT));

        if (dropEnabled && mob != null) {
            ItemStack drop = SpawnerBuilder.buildSpawner(mob, resolvedLevel);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        // ✅ Layer 1 storage cleanup: only for real custom spawners (PDC-backed)
        if (isCustomSpawner) {
            plugin.getConfigurationManager()
                    .getSpawnerManager()
                    .removeSpawner(mobKey.toLowerCase(Locale.ROOT), resolvedLevel, block.getLocation());
        }

        // ✅ Actually remove spawner block
        block.setType(Material.AIR, false);

        // ✅ Prevent vanilla explosion handling for this block
        return true;
    }
}
