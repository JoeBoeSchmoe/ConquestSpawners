package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.List;

/**
 * 🧹 MobDespawnTask
 * Periodically removes custom-spawned mobs if no nearby players (within activation range) exist.
 */
public class MobDespawnTask extends BukkitRunnable {

    private final ConquestSpawners plugin;
    private final MobManager mobManager;
    private final int defaultActivationRange;
    private int worldIndex = 0;

    public MobDespawnTask(ConquestSpawners plugin) {
        this.plugin = plugin;
        this.mobManager = plugin.getConfigurationManager().getMobManager();
        this.defaultActivationRange = plugin.getConfig().getInt("default-values.player-activation-range", 32);
    }

    @Override
    public void run() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return;

        World world = worlds.get(worldIndex++ % worlds.size());
        if (world.getPlayers().isEmpty()) return;

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isCustomSpawnerMob(living)) continue;

            int activationRange = getMobActivationRange(living);
            double rangeSquared = activationRange * activationRange;

            boolean hasNearby = world.getNearbyPlayers(living.getLocation(), activationRange).stream()
                    .anyMatch(p -> {
                        GameMode mode = p.getGameMode();
                        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE || mode == GameMode.CREATIVE;
                    });

            if (!hasNearby) {
                living.remove();
            }
        }
    }

    private boolean isCustomSpawnerMob(LivingEntity entity) {
        if (!entity.hasMetadata("custom-spawner")) return false;
        for (MetadataValue value : entity.getMetadata("custom-spawner")) {
            if (value.getOwningPlugin() == plugin && value.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    private int getMobActivationRange(LivingEntity entity) {
        String mobKey = null;
        Integer level = null;

        if (entity.hasMetadata("conquest-spawner-drop")) {
            for (MetadataValue meta : entity.getMetadata("conquest-spawner-drop")) {
                if (meta.getOwningPlugin() == plugin) {
                    mobKey = meta.asString().toLowerCase();
                    break;
                }
            }
        }

        if (entity.hasMetadata("spawner-level")) {
            for (MetadataValue meta : entity.getMetadata("spawner-level")) {
                if (meta.getOwningPlugin() == plugin) {
                    level = meta.asInt();
                    break;
                }
            }
        }

        if (mobKey != null) {
            MobDataModel mob = mobManager.getMob(mobKey);
            if (mob != null && level != null) {
                SpawnerLevelModel lvl = mob.getSpawnerLevels().get(level);
                if (lvl != null && mob.getPlayerActivationRange() != null) {
                    return (int) mob.getPlayerActivationRange();
                }
            }
        }

        return defaultActivationRange;
    }
}
