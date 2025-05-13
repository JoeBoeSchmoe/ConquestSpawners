package org.conquest.conquestSpawners.responseHandler.effectHandler;

import org.bukkit.Sound;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 🎵 SoundCompatModel
 * Safely matches sounds across Minecraft versions and remap differences.
 * Supports runtime compatibility for older versions like 1.20 even when compiled on 1.21+.
 */
public class SoundCompatModel {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("ConquestDuels");

    /**
     * Aliases: remap legacy → modern enum format
     */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            // Legacy and shorthand
            Map.entry("click", "ui_button_click"),
            Map.entry("note_bass", "block_note_block_bass"),
            Map.entry("note_hat", "block_note_block_hat"),
            Map.entry("note_pling", "block_note_block_pling"),
            Map.entry("chest_close", "block_chest_close"),
            Map.entry("chest_open", "block_chest_open"),

            // Legacy Bukkit → Mojang enum remaps
            Map.entry("block.note_block.bass", "block_note_block_bass"),
            Map.entry("BLOCK_NOTE_BLOCK_BASS", "block_note_block_bass"),
            Map.entry("block_note_block_bass", "block_note_block_bass"),
            Map.entry("block_note_block_hat", "block_note_block_hat"),
            Map.entry("BLOCK_NOTE_BLOCK_HAT", "block_note_block_hat"),
            Map.entry("block_note_block_pling", "block_note_block_pling"),
            Map.entry("BLOCK_NOTE_BLOCK_PLING", "block_note_block_pling"),

            // Removed/renamed enum → valid alternative
            Map.entry("entity_experience_orb_pickup", "entity_player_levelup"),
            Map.entry("ENTITY_EXPERIENCE_ORB_PICKUP", "entity_player_levelup")
    );

    /**
     * Attempts to match a sound string safely across mappings and formats.
     *
     * @param input The sound name from config (case-insensitive)
     * @return Optional of matching Sound, or empty if not resolvable
     */
    public static Optional<Sound> match(String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        String raw = input.trim().toLowerCase(Locale.ROOT);
        String normalized = normalize(raw);

        // 1. Try exact normalized match using runtime-safe lookup
        Optional<Sound> direct = tryValueOf(normalized);
        if (direct.isPresent()) return direct;

        // 2. Alias → normalized → runtime-safe lookup
        String alias = ALIASES.get(raw);
        if (alias != null) {
            Optional<Sound> aliasMatch = tryValueOf(normalize(alias));
            if (aliasMatch.isPresent()) return aliasMatch;
        }

        // 3. Suffix fallback (e.g. user puts "pling", match "block_note_block_pling")
        for (Sound sound : Sound.values()) {
            if (normalize(sound.name()).endsWith(normalized)) {
                return Optional.of(sound);
            }
        }

        // 4. Failure
        log.warning("[ConquestDuels] ⚠️ Unknown or unsupported sound: '" + input + "'");
        return Optional.empty();
    }

    /**
     * Normalizes a sound string to a consistent lowercase underscore format.
     * E.g., "BLOCK.NOTE_BLOCK.BASS" → "block_note_block_bass"
     */
    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replace(".", "_")
                .replace(" ", "")
                .trim();
    }

    /**
     * Runtime-safe attempt to resolve a Sound enum.
     * Avoids NoSuchFieldError on older versions like Purpur 1.20.
     */
    @SuppressWarnings("unchecked")
    private static Optional<Sound> tryValueOf(String enumName) {
        try {
            @SuppressWarnings("rawtypes")
            Class enumClass = Sound.class;
            Sound sound = (Sound) Enum.valueOf(enumClass, enumName.toUpperCase(Locale.ROOT));
            return Optional.of(sound);
        } catch (IllegalArgumentException | NoSuchFieldError | ExceptionInInitializerError e) {
            return Optional.empty();
        }
    }

}
