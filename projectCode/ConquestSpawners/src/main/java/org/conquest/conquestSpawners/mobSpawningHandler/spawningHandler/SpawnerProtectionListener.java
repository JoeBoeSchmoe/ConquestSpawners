package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

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
     * Handles dropping a spawner item on explosion if allowed by config.
     * Always returns true to remove the spawner block visually.
     */
    private boolean handleSpawnerExplosion(Block block) {
        if (block.getType() != Material.SPAWNER) return false;

        boolean dropEnabled = plugin.getConfig().getBoolean("explosion-handling.drop-from-explosions", false);
        boolean allowVanillaConvert = plugin.getConfig().getBoolean("vanilla-spawner-conversion.enabled", false);

        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner spawner)) return true;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        // 📦 If this is a vanilla spawner, try to convert if config allows
        if (mobKey == null || level == null) {
            if (!allowVanillaConvert) return true;

            mobKey = Objects.requireNonNull(spawner.getSpawnedType()).name().toLowerCase(Locale.ROOT);
            level = 1;
        }

        MobDataModel mob = plugin.getConfigurationManager().getMobManager().getMob(mobKey.toLowerCase(Locale.ROOT));
        if (dropEnabled && mob != null) {
            ItemStack drop = SpawnerBuilder.buildSpawner(mob, level);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        block.setType(Material.AIR, false); // ✅ Actually remove spawner block

        return true;
    }

}
