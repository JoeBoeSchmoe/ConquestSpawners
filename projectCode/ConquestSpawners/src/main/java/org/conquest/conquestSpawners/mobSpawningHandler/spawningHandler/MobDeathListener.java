package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 💀 Handles death logic, XP, and drops for custom-spawned mobs.
 */
public class MobDeathListener implements Listener {

    private final ConquestSpawners plugin;
    private final MobManager mobManager;
    private final Logger log;

    public MobDeathListener(ConquestSpawners plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.log = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomMobPreDeath(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!entity.hasMetadata("custom-spawner")) return;

        double finalHealth = entity.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0) {
            entity.setSilent(true);
            entity.setMetadata("suppress-death-particles", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata("custom-spawner")) return;

        event.setDroppedExp(0); // Always suppress vanilla XP

        // Read level metadata
        Optional<Integer> levelOpt = entity.getMetadata("spawner-level").stream()
                .filter(meta -> meta.getOwningPlugin() == plugin)
                .map(MetadataValue::asInt)
                .findFirst();
        if (levelOpt.isEmpty()) {
            log.warning("Missing spawner level for entity: " + entity.getType());
            return;
        }

        String mobKey = entity.getType().name().toLowerCase();
        MobDataModel mobData = mobManager.getMob(mobKey);
        if (mobData == null) {
            log.warning("Mob config not found: " + mobKey);
            return;
        }

        SpawnerLevelModel levelModel = mobData.getSpawnerLevels().get(levelOpt.get());
        if (levelModel == null) {
            log.warning("Missing level " + levelOpt.get() + " for mob " + mobKey);
            return;
        }

        // Custom drop logic
        if (!levelModel.isVanillaDrops()) {
            event.getDrops().clear();
        }

        List<CustomDropModel> customDrops = levelModel.getCustomDrops();
        if (customDrops != null && !customDrops.isEmpty()) {
            Location dropLoc = entity.getLocation();
            World world = entity.getWorld();

            for (CustomDropModel drop : customDrops) {
                if (ThreadLocalRandom.current().nextDouble(100) <= drop.getDropPercent()) {
                    Material material = Material.matchMaterial(drop.getMaterial().toUpperCase());
                    if (material == null) {
                        log.warning("❌ Invalid drop material: " + drop.getMaterial());
                        continue;
                    }
                    ItemStack item = new ItemStack(material, drop.getAmount());
                    world.dropItemNaturally(dropLoc, item);
                }
            }
        }

        // ✅ Spawn a single custom XP orb — only if killed by a player
        if (entity.getKiller() != null && entity.hasMetadata("custom-xp")) {
            int xp = entity.getMetadata("custom-xp").stream()
                    .filter(meta -> meta.getOwningPlugin() == plugin)
                    .map(MetadataValue::asInt)
                    .findFirst().orElse(0);

            if (xp > 0) {
                World world = entity.getWorld();
                Location loc = entity.getLocation().add(0, 0.25, 0);
                ExperienceOrb orb = world.spawn(loc, ExperienceOrb.class);
                orb.setExperience(xp);
                orb.setTicksLived(200); // 💡 Faster pickup
            }
        }

        // Despawn particle-suppressed mobs
        if (entity.hasMetadata("suppress-death-particles")) {
            Bukkit.getScheduler().runTask(plugin, entity::remove);
        }
    }
}
