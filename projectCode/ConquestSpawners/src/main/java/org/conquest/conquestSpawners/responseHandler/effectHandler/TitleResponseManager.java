package org.conquest.conquestSpawners.responseHandler.effectHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.PlaceHolderAPIManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 🎬 TitleResponseManager
 * Sends title/subtitle messages with customizable fade-in, stay, and fade-out durations.
 * Supports YAML format:
 * - title.timings as key-value block
 * - OR title.timings as inline list (e.g. [ fadeIn: 10, stay: 40, fadeOut: 0 ])
 */
public class TitleResponseManager {

    public static void send(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        if (player == null || section == null) return;

        ConfigurationSection titleSection = section.getConfigurationSection("title");
        if (titleSection == null) return;

        String rawTitle = titleSection.getString("text", "").trim();
        String rawSubtitle = titleSection.getString("subtitle", "").trim();

        if (rawTitle.isEmpty() && rawSubtitle.isEmpty()) return;

        Component title = rawTitle.isEmpty() ? Component.empty()
                : PlaceHolderAPIManager.parse(player, rawTitle, placeholders);
        Component subtitle = rawSubtitle.isEmpty() ? Component.empty()
                : PlaceHolderAPIManager.parse(player, rawSubtitle, placeholders);

        int fadeIn = 10, stay = 40, fadeOut = 20;

        Object timingsObj = titleSection.get("timings");

        // ✅ Support YAML block map (fadeIn: 10)
        if (timingsObj instanceof MemorySection memory) {
            fadeIn = memory.getInt("fadeIn", fadeIn);
            stay = memory.getInt("stay", stay);
            fadeOut = memory.getInt("fadeOut", fadeOut);
        }

        // ✅ Support YAML inline list (e.g. [ fadeIn: 10, ... ])
        else if (timingsObj instanceof List<?> timingList) {
            for (Object line : timingList) {
                if (!(line instanceof String str)) continue;
                String[] parts = str.split(":", 2);
                if (parts.length != 2) continue;
                String key = parts[0].trim().toLowerCase();
                String val = parts[1].trim();
                try {
                    int parsed = Integer.parseInt(val);
                    switch (key) {
                        case "fadein" -> fadeIn = parsed;
                        case "stay" -> stay = parsed;
                        case "fadeout" -> fadeOut = parsed;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (!isEmpty(title) || !isEmpty(subtitle)) {
            player.clearTitle();
            player.showTitle(Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                            Duration.ofMillis(fadeIn * 50L),
                            Duration.ofMillis(stay * 50L),
                            Duration.ofMillis(fadeOut * 50L)
                    )
            ));

            ConquestSpawners.getInstance().getLogger().info(
                    "[Title] Sent to " + player.getName() +
                            " | Title: " + rawTitle +
                            " | Subtitle: " + rawSubtitle +
                            " | Timings: fadeIn=" + fadeIn + ", stay=" + stay + ", fadeOut=" + fadeOut
            );
        }
    }

    private static boolean isEmpty(Component component) {
        return component == null
                || Component.empty().equals(component)
                || net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component).isBlank();
    }
}
