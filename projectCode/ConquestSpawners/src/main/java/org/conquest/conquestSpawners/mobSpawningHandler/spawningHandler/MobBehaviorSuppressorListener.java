package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class MobBehaviorSuppressorListener implements Listener {

    private static final long GRACE_PERIOD_MS = 1000;
    private static final boolean ANCHOR_MOB_ON_HIT = false;

    private final Map<UUID, Long> recentLiquidExit = new WeakHashMap<>();

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        if (hasAIDisabled(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityTargetLiving(EntityTargetLivingEntityEvent e) {
        if (hasAIDisabled(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (!hasAIDisabled(entity)) return;

        if (entity.hasMetadata("allow-cram")) {
            entity.removeMetadata("allow-cram", ConquestSpawners.getInstance());
            return;
        }

        // Suppress knockback and fire particles from passive damage, but allow animation
        switch (e.getCause()) {
            case FALL, FIRE, FIRE_TICK, LAVA, SUFFOCATION -> {
                Bukkit.getScheduler().runTask(ConquestSpawners.getInstance(), () -> {
                    entity.setVelocity(new Vector(0, 0, 0));
                    entity.setFallDistance(0f);
                    entity.setFireTicks(0);

                    if (entity instanceof LivingEntity living) {
                        living.setVisualFire(false);
                        // Allow animation as normal
                    }
                });
            }
            default -> {
                // Let all other damage apply normally
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (hasAIDisabled(e.getDamager())) {
            e.setCancelled(true);
            return;
        }

        if (!hasAIDisabled(e.getEntity())) return;

        // Cancel knockback but allow red flash + animation
        Bukkit.getScheduler().runTask(ConquestSpawners.getInstance(), () -> {
            e.getEntity().setVelocity(new Vector(0, 0, 0));
            if (ANCHOR_MOB_ON_HIT) {
                Location base = e.getEntity().getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                e.getEntity().teleport(base);
            }
        });
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (hasAIDisabled(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent e) {
        if (hasAIDisabled(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityCombustByEntity(EntityCombustByEntityEvent e) {
        if (hasAIDisabled(e.getCombuster())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (!hasAIDisabled(entity)) return;
        if (sameBlock(event.getFrom(), event.getTo())) return;

        UUID id = entity.getUniqueId();
        Vector velocity = entity.getVelocity();

        if (velocity.getY() < -0.25 && entity instanceof LivingEntity living) {
            living.setFallDistance(0f);
        }

        if (velocity.getY() < -0.01) return;

        if (isInLiquid(entity)) {
            recentLiquidExit.put(id, System.currentTimeMillis());
            return;
        }

        long sinceWet = System.currentTimeMillis() - recentLiquidExit.getOrDefault(id, 0L);
        if (sinceWet < GRACE_PERIOD_MS) {
            gentlyPushOutOfWater(entity, velocity, event);
            return;
        }

        if (velocity.getY() > 0.01 && !isInLiquid(entity)) {
            entity.setVelocity(new Vector(velocity.getX(), 0, velocity.getZ()));
        }

        if (velocity.getX() != 0 || velocity.getZ() != 0) {
            Vector damped = new Vector(0, velocity.getY(), 0);
            entity.setVelocity(damped);
        }

        if (event.getFrom().distanceSquared(event.getTo()) > 0.0001) {
            event.setTo(event.getFrom());
        }
    }

    private void gentlyPushOutOfWater(Entity entity, Vector velocity, EntityMoveEvent event) {
        Vector forward = velocity.clone().setY(0);
        if (forward.lengthSquared() < 0.0001) {
            forward = event.getFrom().toVector().subtract(entity.getLocation().toVector()).setY(0);
        }
        if (forward.lengthSquared() > 0.0001) {
            forward.normalize().multiply(0.03);
            entity.setVelocity(velocity.add(forward.setY(0.01)));
        }
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private boolean isInLiquid(Entity entity) {
        Location loc = entity.getLocation();
        Block main = loc.getBlock();
        Block below = loc.clone().subtract(0, 0.1, 0).getBlock();
        return isLiquid(main) || isLiquid(below) || isLiquidNearby(entity);
    }

    private boolean isLiquidNearby(Entity entity) {
        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (isLiquid(world.getBlockAt(x + dx, y, z + dz))) return true;
            }
        }
        return false;
    }

    private boolean isLiquid(Block block) {
        return switch (block.getType()) {
            case WATER, LAVA, BUBBLE_COLUMN, KELP, KELP_PLANT, SEAGRASS, TALL_SEAGRASS -> true;
            default -> false;
        };
    }

    private boolean hasAIDisabled(Entity entity) {
        return entity.hasMetadata("disable-ai-logic");
    }

    public static void tagDisableAI(Entity entity, Plugin plugin) {
        entity.setMetadata("disable-ai-logic", new FixedMetadataValue(plugin, true));
        if (entity instanceof Mob mob) {
            mob.setAware(false);
        }
    }
}
