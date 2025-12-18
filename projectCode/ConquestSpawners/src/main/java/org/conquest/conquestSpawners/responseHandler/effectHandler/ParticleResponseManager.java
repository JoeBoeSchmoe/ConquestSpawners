package org.conquest.conquestSpawners.responseHandler.effectHandler;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.*;
import java.util.logging.Logger;

/**
 * 💨 ParticleResponseManager
 * Safely spawns particles defined in message config sections.
 *
 * Expected YAML shape (example):
 *
 * particles:
 *   - type: ENCHANTMENT_TABLE
 *     count: 8
 *     offset: [0.2, 0.4, 0.2]
 *     speed: 0.02
 *
 * Any particle that requires extra data (color, itemstack, blockdata, etc.)
 * is auto-skipped so we don't explode on send().
 */
public final class ParticleResponseManager {

    private static final Logger log = ConquestSpawners.getInstance().getLogger();

    // Particles that CANNOT be spawned with the "simple" spawnParticle(...) signature
    // (i.e. they require extra data like color info, blockstate, itemstack, etc.)
    private static final Set<Particle> DATA_REQUIRED_DENYLIST = EnumSet.of(
            // old "spell" family — unified in modern Paper
            Particle.EFFECT,
            Particle.INSTANT_EFFECT,
            Particle.WITCH,

            // color-based particles
            Particle.DUST,
            Particle.DUST_COLOR_TRANSITION,

            // block/item particles expect extra data
            Particle.BLOCK_MARKER,
            Particle.FALLING_DUST,
            Particle.ITEM
    );


    private ParticleResponseManager() {
        // utility
    }

    /**
     * Spawns all particles defined in a message section for the player (if any).
     *
     * @param player  Player to display particles to
     * @param section Section containing a `particles:` list
     */
    public static void play(Player player, ConfigurationSection section) {
        if (player == null || section == null) return;
        if (!section.isList("particles")) return;

        List<Map<?, ?>> particles = section.getMapList("particles");
        if (particles == null || particles.isEmpty()) return;

        for (Map<?, ?> particleData : particles) {
            safeSpawnParticle(player, particleData);
        }
    }

    /**
     * Attempts to spawn one particle entry from config, but never throws.
     */
    private static void safeSpawnParticle(Player player, Map<?, ?> data) {
        try {
            spawnParticle(player, data);
        } catch (Throwable t) {
            // We swallow here so messaging/rank logic etc. still completes
            log.warning("[ConquestClans] Particle spawn skipped: " + t.getMessage());
        }
    }

    /**
     * Core logic to parse a single map and spawn the particle if allowed.
     */
    private static void spawnParticle(Player player, Map<?, ?> data) {
        if (player == null || data == null) return;

        Object typeObj = data.get("type");
        if (!(typeObj instanceof String)) {
            log.warning("⚠️  Missing or invalid particle 'type' in config.");
            return;
        }

        final String typeString = typeObj.toString().trim().toUpperCase(Locale.ROOT);

        final Particle particle;
        try {
            particle = Particle.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            log.warning("⚠️  Invalid particle type in config: '" + typeString + "'");
            return;
        }

        // If this particle requires extra data we aren't providing, skip it safely.
        if (DATA_REQUIRED_DENYLIST.contains(particle)) {
            // Debug-level style log. We don't warn every time so console doesn't spam.
            log.fine("Skipping data-backed particle '" + particle.name()
                    + "' (needs extra data we don't provide)");
            return;
        }

        final int count = clampMin(parseInt(data.get("count"), 1));
        final double speed = parseDouble(data.get("speed"), 0.01D);
        final Vector offset = parseOffset(data.get("offset"));

        // Location slightly above head to look more "rewardy"
        final var loc = aboveHead(player);

        // This is the "simple" spawn signature that only works for data-free particles.
        player.spawnParticle(
                particle,
                loc,
                count,
                offset.getX(), offset.getY(), offset.getZ(),
                speed
        );
    }

    /* ------------------------- helpers ------------------------- */

    private static Vector parseOffset(Object raw) {
        if (raw instanceof List<?> list && list.size() == 3) {
            try {
                double x = Double.parseDouble(String.valueOf(list.get(0)));
                double y = Double.parseDouble(String.valueOf(list.get(1)));
                double z = Double.parseDouble(String.valueOf(list.get(2)));
                return new Vector(x, y, z);
            } catch (Exception ignored) {
                // fall through to default
            }
        }
        return new Vector(0, 0, 0);
    }

    private static int parseInt(Object value, int def) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static double parseDouble(Object value, double def) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int clampMin(int val) {
        return Math.max(val, 0);
    }

    private static org.bukkit.Location aboveHead(Player player) {
        return player.getLocation().add(0, 1.0, 0);
    }
}
