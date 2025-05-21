package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.util.Vector;

/**
 * Suppresses all AI/pathfinding/movement/rotation for mobs tagged with disable-ai-logic.
 * Allows falling, head tracking, and liquid movement — but cancels hostile behaviors and grounded walking.
 */
public class MobBehaviorSuppressorListener implements Listener {

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTargetLiving(EntityTargetLivingEntityEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (hasAIDisabled(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        if (hasAIDisabled(event.getCombuster())) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels movement in X/Z while allowing natural Y-axis falling,
     * head rotation, and movement inside liquids (e.g., water/lava).
     */
    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (!hasAIDisabled(entity)) return;

        // Allow movement if the entity is currently in or moving into liquid
        Block toBlock = event.getTo().getBlock();
        if (isLiquid(toBlock)) return;

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        Vector delta = to.clone().subtract(from);

        if (Math.abs(delta.getX()) > 0.001 || Math.abs(delta.getZ()) > 0.001) {
            event.setTo(event.getFrom()); // Snap back
        }
    }

    private boolean hasAIDisabled(Entity entity) {
        return entity.hasMetadata("disable-ai-logic");
    }

    private boolean isLiquid(Block block) {
        Material type = block.getType();
        return type == Material.WATER || type == Material.LAVA
                || type == Material.BUBBLE_COLUMN
                || type == Material.KELP
                || type == Material.KELP_PLANT
                || type == Material.SEAGRASS
                || type == Material.TALL_SEAGRASS;
    }
}
