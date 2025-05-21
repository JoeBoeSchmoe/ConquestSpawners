package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.CustomDropModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 💀 Handles death drops and death particle suppression for custom-spawned mobs.
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

    /**
     * Handles custom drop logic after a mob dies.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.hasMetadata("custom-spawner")) return;

        Optional<Integer> levelOpt = getSpawnerLevelFromMetadata(entity);
        if (levelOpt.isEmpty()) {
            log.warning("Spawner level metadata missing for entity: " + entity.getType());
            return;
        }

        String mobKey = entity.getType().name().toLowerCase();
        MobDataModel mobData = mobManager.getMob(mobKey);
        if (mobData == null) {
            log.warning("Mob type not found in config: " + mobKey);
            return;
        }

        SpawnerLevelModel levelModel = mobData.getSpawnerLevels().get(levelOpt.get());
        if (levelModel == null) {
            log.warning("Level " + levelOpt.get() + " not found for mob " + mobKey);
            return;
        }

        log.info("⚰️ Handling custom drops for " + mobKey + " at level " + levelOpt.get());

        if (!levelModel.isVanillaDrops()) {
            event.getDrops().clear();
        }

        List<CustomDropModel> customDrops = levelModel.getCustomDrops();
        if (customDrops != null) {
            for (CustomDropModel drop : customDrops) {
                double roll = ThreadLocalRandom.current().nextDouble(0, 100);
                if (roll <= drop.getDropPercent()) {
                    Material material = Material.matchMaterial(drop.getMaterial().toUpperCase());
                    if (material == null) {
                        log.warning("❌ Invalid drop material: " + drop.getMaterial());
                        continue;
                    }

                    ItemStack item = new ItemStack(material, drop.getAmount());
                    // TODO: Use drop.getCustomData() to apply lore or tags
                    entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                }
            }
        }

        // Sound-only suppression happens here; real particle suppression is in damage hook
        entity.setSilent(true);
    }

    /**
     * Pre-death hook: intercept damage and remove entity before it triggers death animation.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomMobPreDeath(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        if (!entity.hasMetadata("custom-spawner")) return;

        double finalHealth = entity.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0) {
            log.info("🫥 Suppressing death animation for: " + entity.getType());
            Bukkit.getScheduler().runTask(plugin, entity::remove);
        }
    }

    private Optional<Integer> getSpawnerLevelFromMetadata(LivingEntity entity) {
        return entity.getMetadata("spawner-level").stream()
                .filter(meta -> meta.getOwningPlugin() == plugin)
                .map(MetadataValue::asInt)
                .findFirst();
    }
}
