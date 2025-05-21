package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.util.Vector;

/**
 * Suppresses all AI/pathfinding/movement/rotation for mobs tagged with disable-ai-logic.
 * Allows falling and taking damage, but cancels targeting, attacking, moving, rotating.
 */
public class MobBehaviorSuppressorListener implements Listener {

    /**
     * Blocks all targeting logic for AI-disabled mobs.
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks targeting players or entities.
     */
    @EventHandler
    public void onEntityTargetLiving(EntityTargetLivingEntityEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents AI-disabled mobs from attacking other entities.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (hasAIDisabled(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent mobs from griefing blocks (e.g., Endermen, Wither).
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents AI-disabled mobs from combusting in daylight.
     */
    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (hasAIDisabled(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents AI-disabled mobs from igniting others.
     */
    @EventHandler
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        if (hasAIDisabled(event.getCombuster())) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels movement in X/Z while allowing natural Y-axis falling.
     */
    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (!hasAIDisabled(entity)) return;

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        Vector delta = to.clone().subtract(from);

        // Allow only vertical motion (falling)
        if (Math.abs(delta.getX()) > 0.001 || Math.abs(delta.getZ()) > 0.001) {
            event.setTo(event.getFrom()); // Snap back to original X/Z
        }

        // Optionally freeze yaw/pitch (no head turns)
        event.getTo().setYaw(event.getFrom().getYaw());
        event.getTo().setPitch(event.getFrom().getPitch());
    }

    /**
     * Check if entity has AI disabled via metadata.
     */
    private boolean hasAIDisabled(Entity entity) {
        return entity.hasMetadata("disable-ai-logic");
    }
}
