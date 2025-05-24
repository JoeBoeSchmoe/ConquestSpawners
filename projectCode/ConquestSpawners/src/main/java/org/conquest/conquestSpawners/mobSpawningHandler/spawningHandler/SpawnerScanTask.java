package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnerScanTask extends BukkitRunnable {

    private final MobManager mobManager;
    private final Map<String, Long> lastSpawnTimes = new HashMap<>();
    private final ConquestSpawners plugin = ConquestSpawners.getInstance();

    private static final String NO_COLLISION_TEAM = "no_collision";
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    public SpawnerScanTask(MobManager mobManager) {
        this.mobManager = mobManager;
        setupNoCollisionTeam();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        boolean debug = plugin.getConfig().getBoolean("debug.spawn-logging", false);

        for (World world : Bukkit.getWorlds()) {
            List<Player> nearbyPlayers = world.getPlayers().stream()
                    .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                    .toList();

            if (nearbyPlayers.isEmpty()) continue;

            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof TileState tile)) continue;

                    PersistentDataContainer data = tile.getPersistentDataContainer();
                    String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
                    Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);
                    if (mobKey == null || level == null) continue;

                    mobKey = mobKey.toLowerCase(Locale.ROOT);
                    MobDataModel mob = mobManager.getMob(mobKey);
                    if (mob == null || !mob.isSpawnerEnabled()) continue;

                    SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
                    if (levelData == null) continue;

                    Location origin = tile.getBlock().getLocation();
                    String locKey = buildLocationKey(origin);
                    int delayMillis = levelData.getSpawnerDelayResolved() * 1000;
                    if (now - lastSpawnTimes.getOrDefault(locKey, 0L) < delayMillis) continue;

                    Location spawnBase = origin.clone().add(0.5, 0, 0.5);
                    Block below = spawnBase.getBlock().getRelative(0, -1, 0);
                    for (int y = -1; y >= -3 && !below.getType().isSolid(); y--) {
                        below = below.getRelative(0, -1, 0);
                        spawnBase.add(0, -1, 0);
                    }

                    int activationRange = ConfigResolver.getInt(
                            mob.getPlayerActivationRange(),
                            "default-values.player-activation-range",
                            32
                    );
                    int activationRangeSq = activationRange * activationRange;

                    boolean playerNearby = false;
                    for (Player player : nearbyPlayers) {
                        if (player.getWorld() == world &&
                                player.getLocation().distanceSquared(spawnBase) <= activationRangeSq) {
                            playerNearby = true;
                            break;
                        }
                    }
                    if (!playerNearby) continue;

                    EntityType type;
                    try {
                        type = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        type = EntityType.PIG;
                    }

                    List<Location> validSpawns = SpawnLocationResolver.findValidSpawnLocations(
                            spawnBase, mob.getRequirements(), mob.getSpawnRadiusResolved(), type
                    );
                    if (validSpawns.isEmpty()) continue;

                    int mobCount = levelData.getMobCountResolved();
                    int xpDrop = levelData.getXpDropResolved();

                    for (int i = 0; i < mobCount; i++) {
                        Location spawnLoc = validSpawns.get(ThreadLocalRandom.current().nextInt(validSpawns.size()));
                        Entity entity = world.spawnEntity(spawnLoc, type);

                        if (!(entity instanceof LivingEntity mobEntity)) continue;

                        mobEntity.setMetadata("custom-spawner", new FixedMetadataValue(plugin, true));
                        mobEntity.setMetadata("conquest-spawner-drop", new FixedMetadataValue(plugin, true));
                        mobEntity.setMetadata("spawner-level", new FixedMetadataValue(plugin, level));
                        if (xpDrop > 0) {
                            mobEntity.setMetadata("custom-xp", new FixedMetadataValue(plugin, xpDrop));
                        }

                        sanitizeEntity(mobEntity);

                        if (mob.isDisableCollisionsResolved()) {
                            mobEntity.setCollidable(false);
                            addToNoCollisionTeam(mobEntity);
                        }

                        if (mob.isDisableMobAIResolved()) {
                            MobBehaviorSuppressorListener.tagDisableAI(mobEntity, plugin);
                        }

                        if (debug) {
                            plugin.getLogger().info("Spawned " + type + " at " + spawnLoc);
                        }
                    }

                    lastSpawnTimes.put(locKey, now);
                }
            }
        }
    }

    private void sanitizeEntity(LivingEntity entity) {
        // Force adult
        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
        }

        // Remove ride relationships
        if (entity.getVehicle() != null) {
            entity.getVehicle().remove();
        }

        entity.eject();
        entity.getPassengers().forEach(passenger -> {
            passenger.remove();
            entity.removePassenger(passenger);
        });

        // Clear gear
        if (entity.getEquipment() != null) {
            entity.getEquipment().clear();
            entity.getEquipment().setHelmetDropChance(0f);
            entity.getEquipment().setChestplateDropChance(0f);
            entity.getEquipment().setLeggingsDropChance(0f);
            entity.getEquipment().setBootsDropChance(0f);
            entity.getEquipment().setItemInMainHandDropChance(0f);
            entity.getEquipment().setItemInOffHandDropChance(0f);
        }

        // Special case for zombies
        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
            zombie.getPassengers().forEach(Entity::remove);
        }

        // Final pass: remove any passengers
        entity.getPassengers().forEach(Entity::remove);
    }


    private void setupNoCollisionTeam() {
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }
    }

    private void addToNoCollisionTeam(Entity entity) {
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team != null) {
            String uuid = entity.getUniqueId().toString();
            if (!team.hasEntry(uuid)) {
                team.addEntry(uuid);
            }
        }
    }

    private String buildLocationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
