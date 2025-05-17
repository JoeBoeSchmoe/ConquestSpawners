package org.conquest.conquestSpawners.responseHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.PlaceHolderAPIManager;

import java.util.Map;

/**
 * 🧱 ComponentSerializerManager
 * Builds formatted, hoverable, and clickable components from YAML structures.
 */
public class ComponentSerializerManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Parses a raw MiniMessage string with optional placeholders for a player.
     *
     * @param player       Player for PlaceholderAPI, may be null
     * @param rawText      The MiniMessage formatted text
     * @param placeholders The map of custom placeholders {key}
     * @return The parsed Component
     */
    public static Component format(Player player, String rawText, Map<String, String> placeholders) {
        if (rawText == null || rawText.isEmpty()) return Component.empty();

        String parsed = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(rawText, placeholders);
        parsed = PlaceHolderAPIManager.parsePlaceholders(player, parsed);

        return MINI.deserialize(parsed);
    }

    /**
     * Deserializes a clickable and hoverable message component from a YAML map.
     *
     * @param raw          Raw map with "text", "hover", "click", "clickType"
     * @param player       Player for placeholder resolution
     * @param placeholders The map of custom placeholders
     * @return The fully built Component
     */
    public static Component deserializeComponent(Map<?, ?> raw, Player player, Map<String, String> placeholders) {
        String text = getString(raw, "text");
        String hover = getString(raw, "hover");
        String click = getString(raw, "click");
        String clickType = getString(raw, "clickType", "NONE");

        // 📜 Base text
        Component component = PlaceHolderAPIManager.parse(player, text, placeholders);

        // 🖱️ Hover event
        if (!hover.isEmpty()) {
            Component hoverComponent = PlaceHolderAPIManager.parse(player, hover, placeholders);
            component = component.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        // 🖱️ Click event
        if (!click.isEmpty() && !clickType.equalsIgnoreCase("NONE")) {
            String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);

            switch (clickType.toUpperCase()) {
                case "RUN_COMMAND" -> component = component.clickEvent(ClickEvent.runCommand(parsedClick));
                case "SUGGEST_COMMAND" -> component = component.clickEvent(ClickEvent.suggestCommand(parsedClick));
                case "OPEN_URL" -> component = component.clickEvent(ClickEvent.openUrl(parsedClick));
            }
        }

        return component;
    }

    // ==========================
    // 📦 Internal helpers
    // ==========================

    private static String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private static String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
