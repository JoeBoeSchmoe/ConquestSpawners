package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * STRICT MODE:
 * - If spawner has our PDC (mob+level), we CANCEL vanilla spawning
 * - Then we spawn the full batch ourselves (exact mobCount best-effort)
 * - This prevents “vanilla mobs sneaking in” entirely for custom spawners.
 */
public final class CustomSpawnerSpawnListener implements Listener {

    private static final String NO_COLLISION_TEAM = "no_collision";

    private static final String META_CUSTOM_SPAWNER = "custom-spawner";
    private static final String META_SPAWNER_DROP_KEY = "conquest-spawner-drop";
    private static final String META_SPAWNER_LEVEL = "spawner-level";
    private static final String META_CUSTOM_XP = "custom-xp";
    private static final String META_SPAWNER_ORIGIN = "spawner-origin"; // world:x:y:z

    // Debounce: one batch per spawner per tick window
    private final Map<String, Long> lastBatchTickBySpawner = new ConcurrentHashMap<>();

    private final ConquestSpawners plugin;
    private final MobManager mobManager;
    private final Scoreboard scoreboard;

    public CustomSpawnerSpawnListener(ConquestSpawners plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.scoreboard = Bukkit.getScoreboardManager() != null ? Bukkit.getScoreboardManager().getMainScoreboard() : null;

        setupNoCollisionTeam();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        if (spawner == null) return;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        // Not ours -> ignore completely (vanilla spawner works normally)
        if (mobKey == null || mobKey.isEmpty() || level == null) return;

        mobKey = mobKey.toLowerCase(Locale.ROOT);

        MobDataModel mob = mobManager.getMob(mobKey);
        if (mob == null || !mob.isSpawnerEnabled()) {
            // This spawner has our PDC but config is missing/disabled -> block it.
            hardCancel(event);
            return;
        }

        SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
        if (levelData == null) {
            hardCancel(event);
            return;
        }

        // ---------------------------------------------------------------------
        // Keep vanilla spawner internals synced with config (covers reload + unloaded chunks).
        // Config delay is SECONDS, vanilla needs TICKS.
        // ---------------------------------------------------------------------
        try {
            int delayTicks = levelData.getSpawnerDelayTicksResolved();

            spawner.setMinSpawnDelay(delayTicks);
            spawner.setMaxSpawnDelay(delayTicks);
            spawner.setDelay(delayTicks);

            // STRICT mode: only trigger once per cycle
            spawner.setSpawnCount(1);

            // Keep these consistent too (optional but nice)
            spawner.setRequiredPlayerRange(Math.max(1, mob.getPlayerActivationRangeResolved()));
            spawner.setSpawnRange(Math.max(0, mob.getSpawnRadiusResolved()));

            spawner.update(true, false);
        } catch (Throwable ignored) {}

        // ---------------------------------------------------------------------
        // IMPORTANT: Stop vanilla spawner mobs from ever appearing.
        // ---------------------------------------------------------------------
        hardCancel(event);

        // If Paper already created the entity, remove it so no “vanilla AI mob” remains.
        Entity spawned = event.getEntity();
        if (spawned != null) spawned.remove();

        Location spawnerLoc = spawner.getLocation();
        if (spawnerLoc == null || spawnerLoc.getWorld() == null) return;

        String spawnerId = encodeSpawner(spawnerLoc);

        long nowTick = Bukkit.getCurrentTick();
        Long last = lastBatchTickBySpawner.get(spawnerId);

        // Debounce: many SpawnerSpawnEvent calls happen for the same spawner “cycle”
        if (last != null && nowTick == last) return;
        lastBatchTickBySpawner.put(spawnerId, nowTick);

        // Spawn our own full batch next tick (lets vanilla tick settle cleanly)
        String finalMobKey = mobKey;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnBatch(spawnerLoc, spawnerId, mob, finalMobKey, level, levelData);
        }, 1L);
    }

    private void spawnBatch(Location spawnerLoc, String spawnerId, MobDataModel mob, String mobKey, int level, SpawnerLevelModel levelData) {
        World w = spawnerLoc.getWorld();
        if (w == null) return;

        EntityType type = mob.getMobTypeEnum();
        if (type == null) return;

        int target = Math.max(1, levelData.getMobCountResolved());
        int range = Math.max(0, mob.getSpawnRadiusResolved());

        SpawnerRequirementsModel req = mob.getRequirements();

        // We only count our own mobs that are clearly from THIS spawner
        int existing = countOurOriginMobsNearby(w, spawnerLoc, spawnerId, range);
        int missing = target - existing;
        if (missing <= 0) return;

        // Try enough candidates to reliably find valid spots but avoid runaway loops
        int attempts = Math.min(160, Math.max(20, missing * 20));

        int spawned = 0;
        for (int i = 0; i < attempts && spawned < missing; i++) {
            Location cand = randomCandidate(spawnerLoc, range);
            if (cand == null) continue;

            // Must be in loaded area (avoid chunk thrash)
            if (!cand.getChunk().isLoaded()) continue;

            // Requirements at candidate location
            if (req != null && !meetsRequirements(req, cand)) continue;

            // Space check (feet+head)
            if (!isSpawnSpaceClear(cand)) continue;

            LivingEntity le = spawnLiving(w, type, cand);
            if (le == null) continue;

            // Tag + sanitize + AI/collision flags
            tag(le, spawnerId, mobKey, level, levelData);
            sanitizeEntity(le);

            if (mob.isDisableCollisionsResolved()) {
                le.setCollidable(false);
                addToNoCollisionTeam(le);
            }

            if (mob.isDisableMobAIResolved()) {
                MobBehaviorSuppressorListener.tagDisableAI(le, plugin);
                le.setSilent(true);
            }

            spawned++;
        }
    }

    private int countOurOriginMobsNearby(World w, Location spawnerLoc, String spawnerId, int range) {
        double r = Math.max(10, range + 8);
        double r2 = r * r;

        int count = 0;
        for (LivingEntity le : w.getLivingEntities()) {
            if (le.isDead()) continue;

            Location l = le.getLocation();
            if (l.getWorld() != w) continue;
            if (l.distanceSquared(spawnerLoc) > r2) continue;

            if (!le.hasMetadata(META_CUSTOM_SPAWNER)) continue;
            if (!le.hasMetadata(META_SPAWNER_ORIGIN)) continue;

            boolean match = le.getMetadata(META_SPAWNER_ORIGIN).stream()
                    .anyMatch(m -> m.getOwningPlugin() == plugin && spawnerId.equals(m.asString()));

            if (match) count++;
        }
        return count;
    }

    private void tag(LivingEntity le, String spawnerId, String mobKey, int level, SpawnerLevelModel levelData) {
        le.setMetadata(META_CUSTOM_SPAWNER, new FixedMetadataValue(plugin, true));
        le.setMetadata(META_SPAWNER_DROP_KEY, new FixedMetadataValue(plugin, mobKey));
        le.setMetadata(META_SPAWNER_LEVEL, new FixedMetadataValue(plugin, level));
        le.setMetadata(META_SPAWNER_ORIGIN, new FixedMetadataValue(plugin, spawnerId));

        int xpDrop = levelData.getXpDropResolved();
        if (xpDrop > 0) {
            le.setMetadata(META_CUSTOM_XP, new FixedMetadataValue(plugin, xpDrop));
        }
    }

    private String encodeSpawner(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    private Location randomCandidate(Location spawnerLoc, int range) {
        World w = spawnerLoc.getWorld();
        if (w == null) return null;

        int r = Math.max(1, range);

        int dx = ThreadLocalRandom.current().nextInt(-r, r + 1);
        int dz = ThreadLocalRandom.current().nextInt(-r, r + 1);

        int x = spawnerLoc.getBlockX() + dx;
        int z = spawnerLoc.getBlockZ() + dz;

        // keep near spawner Y with a little wiggle
        int baseY = spawnerLoc.getBlockY();
        int y = baseY + ThreadLocalRandom.current().nextInt(-1, 2);

        return new Location(w, x + 0.5, y, z + 0.5);
    }

    private boolean isSpawnSpaceClear(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        return feet.getType().isAir() && head.getType().isAir();
    }

    private LivingEntity spawnLiving(World w, EntityType type, Location loc) {
        try {
            Entity ent = w.spawnEntity(loc, type);
            return (ent instanceof LivingEntity le) ? le : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Requirements (your YAML rules)
    // ---------------------------------------------------------------------

    private boolean meetsRequirements(SpawnerRequirementsModel req, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        Block spawnBlock = loc.getBlock();

        int y = spawnBlock.getY();
        if (req.aboveSeaLevel && y <= 63) return false;
        if (req.belowSeaLevel && y >= 63) return false;
        if (req.aboveYAxis && y <= 63) return false;
        if (req.belowYAxis && y >= 63) return false;

        if (req.inBiome && req.allowedBiomes != null && !req.allowedBiomes.isEmpty()) {
            String biome = spawnBlock.getBiome().getKey().getKey().toLowerCase(Locale.ROOT);
            Set<String> allowed = new HashSet<>();
            for (String b : req.allowedBiomes) {
                if (b != null && !b.isEmpty()) allowed.add(b.toLowerCase(Locale.ROOT));
            }
            if (!allowed.contains(biome)) return false;
        }

        int combined = spawnBlock.getLightLevel();
        if (req.totalDarkness && combined > 0) return false;
        if (req.darkness && combined >= 8) return false;
        if (req.light && combined < 8) return false;

        if (!req.fluid) {
            if (spawnBlock.isLiquid()) return false;
            Block head = spawnBlock.getRelative(0, 1, 0);
            if (head.isLiquid()) return false;
        }

        if (req.air) {
            Block head = spawnBlock.getRelative(0, 1, 0);
            if (!head.getType().isAir()) return false;
        }

        Block below = spawnBlock.getRelative(0, -1, 0);

        if (req.onGround) {
            if (!below.getType().isSolid()) return false;
        }

        if (req.onBlock) {
            if (req.allowedBlocks == null || req.allowedBlocks.isEmpty()) return false;

            Material belowMat = below.getType();
            boolean ok = false;
            for (String s : req.allowedBlocks) {
                if (s == null || s.isEmpty()) continue;
                Material m = Material.matchMaterial(s.trim().toUpperCase(Locale.ROOT));
                if (m != null && m == belowMat) {
                    ok = true;
                    break;
                }
            }
            if (!ok) return false;
        }

        return true;
    }

    // ---------------------------------------------------------------------
    // Entity sanitization / collision team
    // ---------------------------------------------------------------------

    private void sanitizeEntity(LivingEntity entity) {
        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
        }

        if (entity.getVehicle() != null) {
            entity.getVehicle().remove();
        }

        entity.eject();
        entity.getPassengers().forEach(passenger -> {
            passenger.remove();
            entity.removePassenger(passenger);
        });

        if (entity.getEquipment() != null) {
            entity.getEquipment().clear();
            entity.getEquipment().setHelmetDropChance(0f);
            entity.getEquipment().setChestplateDropChance(0f);
            entity.getEquipment().setLeggingsDropChance(0f);
            entity.getEquipment().setBootsDropChance(0f);
            entity.getEquipment().setItemInMainHandDropChance(0f);
            entity.getEquipment().setItemInOffHandDropChance(0f);
        }

        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
            zombie.getPassengers().forEach(Entity::remove);
        }

        entity.getPassengers().forEach(Entity::remove);
    }

    private void setupNoCollisionTeam() {
        if (scoreboard == null) return;

        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }
    }

    private void addToNoCollisionTeam(Entity entity) {
        if (scoreboard == null || entity == null) return;

        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) return;

        String uuid = entity.getUniqueId().toString();
        if (!team.hasEntry(uuid)) team.addEntry(uuid);
    }

    private void hardCancel(SpawnerSpawnEvent event) {
        try {
            event.setCancelled(true);
        } catch (Throwable ignored) {}
    }
}
