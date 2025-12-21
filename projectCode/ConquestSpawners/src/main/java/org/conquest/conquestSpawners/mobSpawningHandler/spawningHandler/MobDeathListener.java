package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.CustomDropModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 💀 Handles death logic, XP, and drops for custom-spawned mobs.
 *
 * Also:
 * - Fire Aspect auto-cooks/smelts drops
 * - Slime/MagmaCube split children inherit custom spawner metadata (+ disable-ai)
 * - Slime/MagmaCube with disable-ai cannot deal contact damage
 */
public class MobDeathListener implements Listener {

    private final ConquestSpawners plugin;
    private final MobManager mobManager;
    private final Logger log;

    /**
     * Cache: input material -> cooked/smelted result material (or null if none)
     */
    private final Map<Material, Material> cookResultCache = new EnumMap<>(Material.class);

    /**
     * Pending slime split records so we can apply metadata to children in CreatureSpawnEvent.
     */
    private final Queue<PendingSplit> pendingSplits = new ConcurrentLinkedQueue<>();

    public MobDeathListener(ConquestSpawners plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.log = plugin.getLogger();

        // Cleanup expired split records
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingSplits.removeIf(p -> p.remaining <= 0 || now >= p.expiresAtMillis);
        }, 20L, 20L);
    }

    // ========================================================================
    // Split inheritance: Slime / Magma Cube
    // ========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent event) {
        Entity parent = event.getEntity();

        // Only inherit if parent is ours
        if (!parent.hasMetadata("custom-spawner")) return;

        int childCount = event.getCount();
        if (childCount <= 0) return;

        int parentSize = getSplitSize(parent);
        if (parentSize <= 1) return; // size 1 doesn't split further

        int childSize = Math.max(1, parentSize / 2);

        Integer spawnerLevel = readIntMeta(parent, "spawner-level");
        String mobKey = readStringMeta(parent);
        Integer customXp = readIntMeta(parent, "custom-xp");

        if (spawnerLevel == null || mobKey == null || mobKey.isBlank()) {
            log.warning("[SplitInherit] Missing spawner-level or mobKey on splitting " + parent.getType());
            return;
        }

        // If parent had AI disabled, children must inherit it too.
        boolean aiDisabled = parent.hasMetadata("disable-ai-logic");

        pendingSplits.add(new PendingSplit(
                parent.getLocation(),
                childSize,
                childCount,
                spawnerLevel,
                mobKey,
                customXp,
                aiDisabled,
                System.currentTimeMillis() + 1500 // 1.5s matching window
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSplitChildSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return;

        Entity child = event.getEntity();
        if (!(child instanceof Slime) && !(child instanceof MagmaCube)) return;

        int childSize = getSplitSize(child);
        Location childLoc = child.getLocation();

        PendingSplit match = findBestSplitMatch(childLoc, childSize);
        if (match == null) return;

        child.setMetadata("custom-spawner", new FixedMetadataValue(plugin, true));
        child.setMetadata("spawner-level", new FixedMetadataValue(plugin, match.spawnerLevel));
        child.setMetadata("conquest-spawner-drop", new FixedMetadataValue(plugin, match.mobKey));

        if (match.customXp != null) {
            child.setMetadata("custom-xp", new FixedMetadataValue(plugin, match.customXp));
        }

        // ✅ Inherit disable-ai flag so children are harmless + fully suppressed
        if (match.aiDisabled) {
            child.setMetadata("disable-ai-logic", new FixedMetadataValue(plugin, true));
        }

        match.remaining--;
    }

    /**
     * Slimes/magma cubes can still deal contact damage even if they aren't moving.
     * If "disable-ai-logic" is set, we force them to be harmless.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisabledSlimeDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Slime) && !(damager instanceof MagmaCube)) return;
        if (!damager.hasMetadata("disable-ai-logic")) return;

        event.setCancelled(true);
    }

    private PendingSplit findBestSplitMatch(Location childLoc, int childSize) {
        PendingSplit best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (PendingSplit p : pendingSplits) {
            if (p.remaining <= 0) continue;
            if (p.childSize != childSize) continue;

            if (p.origin.getWorld() == null || childLoc.getWorld() == null) continue;
            if (!p.origin.getWorld().equals(childLoc.getWorld())) continue;

            double distSq = p.origin.distanceSquared(childLoc);
            if (distSq > 9.0) continue; // within 3 blocks

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = p;
            }
        }

        return best;
    }

    private int getSplitSize(Entity e) {
        if (e instanceof Slime s) return s.getSize();
        if (e instanceof MagmaCube m) return m.getSize();
        return -1;
    }

    // ========================================================================
    // Death particle suppression (pre-death)
    // ========================================================================

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

    // ========================================================================
    // Main death logic
    // ========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata("custom-spawner")) return;

        event.setDroppedExp(0); // Always suppress vanilla XP

        // Fire Aspect auto-cook toggle
        final boolean autoCookDrops = shouldAutoCook(entity);

        // Read level metadata
        Optional<Integer> levelOpt = entity.getMetadata("spawner-level").stream()
                .filter(meta -> meta.getOwningPlugin() == plugin)
                .map(MetadataValue::asInt)
                .findFirst();
        if (levelOpt.isEmpty()) {
            log.warning("Missing spawner level for entity: " + entity.getType());
            return;
        }

        String mobKey = entity.getMetadata("conquest-spawner-drop").stream()
                .filter(meta -> meta.getOwningPlugin() == plugin)
                .map(MetadataValue::asString)
                .findFirst()
                .orElse(null);

        if (mobKey == null || mobKey.isEmpty()) {
            log.warning("Missing mobKey metadata for entity: " + entity.getType());
            return;
        }

        mobKey = mobKey.toLowerCase(Locale.ROOT);

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

        // Vanilla drops (if enabled)
        if (!levelModel.isVanillaDrops()) {
            event.getDrops().clear();
        } else if (autoCookDrops) {
            List<ItemStack> drops = event.getDrops();
            for (int i = 0; i < drops.size(); i++) {
                ItemStack cooked = toCookedIfPossible(drops.get(i));
                if (cooked != null) drops.set(i, cooked);
            }
        }

        // Custom drops
        List<CustomDropModel> customDrops = levelModel.getCustomDrops();
        if (customDrops != null && !customDrops.isEmpty()) {
            Location dropLoc = entity.getLocation();
            World world = entity.getWorld();

            for (CustomDropModel drop : customDrops) {
                if (ThreadLocalRandom.current().nextDouble(100) <= drop.getDropPercent()) {
                    Material material = Material.matchMaterial(drop.getMaterial().toUpperCase(Locale.ROOT));
                    if (material == null) {
                        log.warning("❌ Invalid drop material: " + drop.getMaterial());
                        continue;
                    }

                    ItemStack item = new ItemStack(material, drop.getAmount());

                    if (autoCookDrops) {
                        ItemStack cooked = toCookedIfPossible(item);
                        if (cooked != null) item = cooked;
                    }

                    world.dropItemNaturally(dropLoc, item);
                }
            }
        }

        // Custom XP orb (only if killed by player)
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
                orb.setTicksLived(200);
            }
        }

        // Despawn particle-suppressed mobs
        if (entity.hasMetadata("suppress-death-particles")) {
            Bukkit.getScheduler().runTask(plugin, entity::remove);
        }
    }

    // ========================================================================
    // Fire Aspect -> auto cook/smelt
    // ========================================================================

    private boolean shouldAutoCook(LivingEntity entity) {
        Player killer = entity.getKiller();
        if (killer == null) return false;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return false;

        return weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 0;
    }

    private ItemStack toCookedIfPossible(ItemStack stack) {
        if (stack == null) return null;

        Material type = stack.getType();
        if (type.isAir()) return null;

        Material result = getCookResult(type);
        if (result == null) return null;

        return new ItemStack(result, stack.getAmount());
    }

    private Material getCookResult(Material input) {
        if (cookResultCache.containsKey(input)) {
            return cookResultCache.get(input);
        }

        Material found = null;
        ItemStack probe = new ItemStack(input);

        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();

            if (recipe instanceof FurnaceRecipe furnace) {
                if (matchesCookingInput(furnace, probe)) {
                    found = safeResultType(furnace.getResult());
                    break;
                }
                continue;
            }

            if (recipe instanceof SmokingRecipe smoker) {
                if (matchesCookingInput(smoker, probe)) {
                    found = safeResultType(smoker.getResult());
                    break;
                }
                continue;
            }

            if (recipe instanceof BlastingRecipe blast) {
                if (matchesCookingInput(blast, probe)) {
                    found = safeResultType(blast.getResult());
                    break;
                }
                continue;
            }

            if (recipe instanceof CampfireRecipe campfire) {
                if (matchesCookingInput(campfire, probe)) {
                    found = safeResultType(campfire.getResult());
                    break;
                }
            }
        }

        cookResultCache.put(input, found);
        return found;
    }

    private boolean matchesCookingInput(FurnaceRecipe recipe, ItemStack probe) {
        try {
            RecipeChoice choice = recipe.getInputChoice();
            return choice != null && choice.test(probe);
        } catch (NoSuchMethodError ignored) {
            ItemStack in = recipe.getInput();
            return in != null && in.getType() == probe.getType();
        }
    }

    private boolean matchesCookingInput(SmokingRecipe recipe, ItemStack probe) {
        try {
            RecipeChoice choice = recipe.getInputChoice();
            return choice != null && choice.test(probe);
        } catch (NoSuchMethodError ignored) {
            ItemStack in = recipe.getInput();
            return in != null && in.getType() == probe.getType();
        }
    }

    private boolean matchesCookingInput(BlastingRecipe recipe, ItemStack probe) {
        try {
            RecipeChoice choice = recipe.getInputChoice();
            return choice != null && choice.test(probe);
        } catch (NoSuchMethodError ignored) {
            ItemStack in = recipe.getInput();
            return in != null && in.getType() == probe.getType();
        }
    }

    private boolean matchesCookingInput(CampfireRecipe recipe, ItemStack probe) {
        try {
            RecipeChoice choice = recipe.getInputChoice();
            return choice != null && choice.test(probe);
        } catch (NoSuchMethodError ignored) {
            ItemStack in = recipe.getInput();
            return in != null && in.getType() == probe.getType();
        }
    }

    private Material safeResultType(ItemStack result) {
        if (result == null) return null;
        Material type = result.getType();
        return (type == null || type.isAir()) ? null : type;
    }

    // ========================================================================
    // Metadata helpers + split record model
    // ========================================================================

    private Integer readIntMeta(Entity entity, String key) {
        return entity.getMetadata(key).stream()
                .filter(m -> m.getOwningPlugin() == plugin)
                .findFirst()
                .map(MetadataValue::asInt)
                .orElse(null);
    }

    private String readStringMeta(Entity entity) {
        return entity.getMetadata("conquest-spawner-drop").stream()
                .filter(m -> m.getOwningPlugin() == plugin)
                .findFirst()
                .map(MetadataValue::asString)
                .orElse(null);
    }

    private static final class PendingSplit {
        private final Location origin;
        private final int childSize;
        private int remaining;

        private final int spawnerLevel;
        private final String mobKey;
        private final Integer customXp;

        private final boolean aiDisabled;
        private final long expiresAtMillis;

        private PendingSplit(Location origin,
                             int childSize,
                             int remaining,
                             int spawnerLevel,
                             String mobKey,
                             Integer customXp,
                             boolean aiDisabled,
                             long expiresAtMillis) {
            this.origin = origin;
            this.childSize = childSize;
            this.remaining = remaining;
            this.spawnerLevel = spawnerLevel;
            this.mobKey = mobKey;
            this.customXp = customXp;
            this.aiDisabled = aiDisabled;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
