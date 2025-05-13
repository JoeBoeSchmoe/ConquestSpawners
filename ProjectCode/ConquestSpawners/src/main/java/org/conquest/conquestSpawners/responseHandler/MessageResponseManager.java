package org.conquest.conquestSpawners.responseHandler;

import com.conquest.ConquestDuels;
import com.conquest.configurationHandler.ConfigurationFiles.messageFiles.ArenaMessagesFile;
import com.conquest.configurationHandler.ConfigurationFiles.messageFiles.GeneralMessagesFile;
import com.conquest.configurationHandler.ConfigurationFiles.messageFiles.KitMessagesFile;
import com.conquest.configurationHandler.ConfigurationFiles.messageFiles.QueueMessagesFile;
import com.conquest.configurationHandler.PlaceHolderAPIManager;
import com.conquest.responseHandler.effectHandler.*;
import com.conquest.responseHandler.messageModels.ArenaMessageModels;
import com.conquest.responseHandler.messageModels.GeneralMessageModels;
import com.conquest.responseHandler.messageModels.KitMessageModels;
import com.conquest.responseHandler.messageModels.QueueMessageModels;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * 📬 MessageResponseManager
 * Handles displaying messages from generalMessages.yml, with PAPI and effects.
 */
public class MessageResponseManager {

    private static final Logger log = ConquestDuels.getInstance().getLogger();

    private static String cachedPrefix = null;

    // --------------------- Main Message Sender -----------------------

    // Inside MessageResponseManager.java

    public static void send(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        if (player == null || section == null) return;

        // 1. Handle sending text + components
        send(player, section, placeholders);

        // 2. Handle sounds
        SoundResponseManager.play(player, section);

        // 3. Handle particles
        ParticleResponseManager.play(player, section);

        // 4. Handle bossbar
        BossBarResponseManager.send(player, section, placeholders);

        // 5. Handle actionbar
        ActionBarResponseManager.send(player, section, placeholders);

        // 6. Handle effects
        EffectResponseManager.send(player, section, placeholders);
    }

    // --- Queue Messages ---

    public static void send(Player player, QueueMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, QueueMessageModels model, String key, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key, value);
        send(player, model, placeholders);
    }

    public static void send(Player player, QueueMessageModels model, Map<String, String> placeholders) {
        if (player == null || model == null) return;

        ConfigurationSection section = getMessageSection(model);
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", false);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        List<String> lines = section.getStringList("text");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");

        ClickEvent.Action clickAction = null;
        try {
            clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        if (!lines.isEmpty()) {
            Component base = PlaceHolderAPIManager.parse(player, lines.getFirst(), placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }

            base = prefixComponent.append(base);

            if (section.isList("components")) {
                for (Map<?, ?> raw : section.getMapList("components")) {
                    Component comp = ComponentSerializerManager.deserializeComponent(raw, player, placeholders);
                    base = base.append(Component.space()).append(comp);
                }
            }

            player.sendMessage(base);
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) continue;

            Component message = prefixComponent.append(PlaceHolderAPIManager.parse(player, line, placeholders));
            if (!hover.isBlank()) {
                message = message.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                message = message.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }
            player.sendMessage(message);
        }

        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
    }

    public static void send(Player player, GeneralMessageModels model, Map<String, String> placeholders) {
        if (player == null || model == null) return;

        ConfigurationSection section = getMessageSection(model);
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", false);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            if (!lines.isEmpty()) {
                // Hover/click metadata for main lines
                String hover = section.getString("hover", null);
                String click = section.getString("click", null);
                String clickTypeRaw = section.getString("clickType", null);

                ClickEvent.Action clickAction = null;
                if (clickTypeRaw != null) {
                    try {
                        clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                // First line (may include buttons)
                String first = lines.getFirst();
                if (first != null && !first.trim().isEmpty()) {
                    Component base = PlaceHolderAPIManager.parse(player, first, placeholders);

                    if (hover != null) {
                        base = base.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
                    }
                    if (click != null && clickAction != null) {
                        String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                        base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
                    }

                    base = prefixComponent.append(base);

                    // Inline button components
                    if (section.isList("components")) {
                        List<Map<?, ?>> comps = section.getMapList("components");
                        for (Map<?, ?> raw : comps) {
                            Component comp = ComponentSerializerManager.deserializeComponent(raw, player, placeholders);
                            base = base.append(Component.space()).append(comp);
                        }
                    }

                    player.sendMessage(base);
                }

                // Remaining lines
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line == null || line.trim().isEmpty()) continue;

                    Component message = prefixComponent.append(PlaceHolderAPIManager.parse(player, line, placeholders));

                    if (hover != null) {
                        message = message.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
                    }
                    if (click != null && clickAction != null) {
                        String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                        message = message.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
                    }

                    player.sendMessage(message);
                }
            }
        }

        // 🎵 Sounds
        SoundResponseManager.play(player, section);

        // ✨ Particles
        ParticleResponseManager.play(player, section);

        // 🛡️ Bossbars
        BossBarResponseManager.send(player, section, placeholders);

        // 📢 Actionbars
        ActionBarResponseManager.send(player, section, placeholders);

        // 🧪 Effects
        EffectResponseManager.send(player, section, placeholders);
    }

    public static void send(Player player, GeneralMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, GeneralMessageModels model, String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        send(player, model, map);
    }

    public static void send(CommandSender sender, GeneralMessageModels model) {
        send(sender, model, Collections.emptyMap());
    }

    public static void send(CommandSender sender, GeneralMessageModels model, Map<String, String> placeholders) {
        if (sender == null || model == null) return;

        ConfigurationSection section = getMessageSection(model);
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", false);
        String plainPrefix = showPrefix ? getPrefixPlain() : "";

        if (section.isList("text")) {
            for (String line : section.getStringList("text")) {
                if (line == null || line.trim().isEmpty()) continue;
                String parsed = PlaceHolderAPIManager.parsePlain(null, plainPrefix + line, placeholders);
                sender.sendMessage(parsed);
            }
        }
    }

    // --- Arena Messages ---


    public static void send(Player player, ArenaMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, ArenaMessageModels model, String key, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key, value);
        send(player, model, placeholders);
    }

    public static void send(Player player, ArenaMessageModels model, Map<String, String> placeholders) {
        if (player == null || model == null) return;

        ConfigurationSection section = getMessageSection(model);
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", false);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        List<String> lines = section.getStringList("text");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");

        ClickEvent.Action clickAction = null;
        try {
            clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        // First line
        if (!lines.isEmpty()) {
            Component base = PlaceHolderAPIManager.parse(player, lines.getFirst(), placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick)); // ✅ use parsed
            }

            base = prefixComponent.append(base);

            // Optional inline components
            if (section.isList("components")) {
                for (Map<?, ?> raw : section.getMapList("components")) {
                    Component comp = ComponentSerializerManager.deserializeComponent(raw, player, placeholders);
                    base = base.append(Component.space()).append(comp);
                }
            }

            player.sendMessage(base);
        }

        // Other lines (if any)
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) continue;

            Component message = prefixComponent.append(PlaceHolderAPIManager.parse(player, line, placeholders));
            if (!hover.isBlank()) {
                message = message.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                message = message.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }
            player.sendMessage(message);
        }

        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
    }


    public static void sendArenaList(CommandSender sender, List<Map<String, String>> arenas) {
        String basePath = "list";

        if (arenas.isEmpty()) {
            ConfigurationSection emptySection = ArenaMessagesFile.getSection(basePath + ".empty");
            if (emptySection != null) {
                sendFormattedSection(sender, emptySection, Map.of());
            } else {
                sender.sendMessage("§c[ No arenas found — missing 'list.empty' format]");
            }
            return;
        }

        // Header section (static)
        ConfigurationSection header = ArenaMessagesFile.getSection(basePath + ".header");
        if (header != null) {
            sendFormattedSection(sender, header, Map.of());
        }

        // Entry section (repeats for each arena)
        ConfigurationSection entry = ArenaMessagesFile.getSection(basePath + ".entry");
        if (entry != null) {
            for (Map<String, String> placeholders : arenas) {
                sendFormattedSection(sender, entry, placeholders);
            }
        }
    }

    public static void sendArenaInfo(CommandSender sender, Map<String, String> placeholders) {
        ConfigurationSection section = ArenaMessagesFile.getSection("info.display");
        if (section == null) {
            sender.sendMessage("§c[ConquestDuels] Missing arena info message format (info.display).");
            return;
        }

        Player player = (sender instanceof Player p) ? p : null;

        // 🌟 Send each line of the arena info
        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;

                Component msg = PlaceHolderAPIManager.parse(player, line, placeholders);

                String hover = section.getString("hover", null);
                if (hover != null && !hover.isBlank()) {
                    msg = msg.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
                }

                String click = section.getString("click", null);
                String clickType = section.getString("clickType", "RUN_COMMAND");
                if (click != null && !click.isBlank()) {
                    try {
                        ClickEvent.Action action = ClickEvent.Action.valueOf(clickType.toUpperCase());
                        msg = msg.clickEvent(ClickEvent.clickEvent(action, click));
                    } catch (IllegalArgumentException ignored) {}
                }

                if (section.getBoolean("prefix", true)) {
                    msg = getPrefixComponent().append(msg);
                }

                sender.sendMessage(msg);
            }
        }

        // 🧱 Send UI components (with spawn line filtering)
        List<Map<?, ?>> components = section.getMapList("components");
        for (Map<?, ?> comp : components) {
            String rawText = (String) comp.get("text");

            // Skip if it's a spawn_line placeholder that wasn't filled
            if (rawText != null && rawText.startsWith("{spawn_line_")) {
                String parsed = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(rawText, placeholders);
                if (parsed == null || parsed.isBlank() || parsed.equals(rawText)) {
                    continue; // ❌ Unresolved or empty
                }
            }

            Component component = ComponentSerializerManager.deserializeComponent(comp, player, placeholders);
            sender.sendMessage(component);
        }

        // ⚠️ Show unsaved changes warning if flag is present
        String unsaved = placeholders.getOrDefault("unsaved", "false");
        if (Boolean.parseBoolean(unsaved)) {
            Component warning = MiniMessage.miniMessage().deserialize("<red>⚠ Unsaved Changes")
                    .hoverEvent(Component.text("You’ve made changes to this arena that aren’t saved yet."))
                    .clickEvent(ClickEvent.runCommand("/duel admin arena save"));
            sender.sendMessage(Component.empty()); // spacing
            sender.sendMessage(warning); // ⛔️ No prefix
        }

        // 🎵 Optional effects
        if (player != null) {
            SoundResponseManager.play(player, section);
            ParticleResponseManager.play(player, section);
            BossBarResponseManager.send(player, section, placeholders);
            ActionBarResponseManager.send(player, section, placeholders);
            EffectResponseManager.send(player, section, placeholders);
        }
    }

    /**
     * Sends a formatted message section with optional hover/click/sound/particles/effects.
     */
    private static void sendFormattedSection(CommandSender sender, ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) return;

        List<String> lines = section.getStringList("text");
        List<Map<?, ?>> components = section.getMapList("components");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");
        boolean prefix = section.getBoolean("prefix", true);

        Player player = (sender instanceof Player p) ? p : null;

        if (player != null) {
            SoundResponseManager.play(player, section);
            ParticleResponseManager.play(player, section);
            BossBarResponseManager.send(player, section, placeholders);
            ActionBarResponseManager.send(player, section, placeholders);
            EffectResponseManager.send(player, section, placeholders);
        }

        ClickEvent.Action clickAction = null;
        if (!clickTypeRaw.isBlank()) {
            try {
                clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;

            Component base = PlaceHolderAPIManager.parse(player, line, placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }

            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }

            if (prefix) {
                base = getPrefixComponent().append(base);
            }

            // Append components without space
            if (!components.isEmpty()) {
                for (Map<?, ?> raw : components) {
                    Component comp = ComponentSerializerManager.deserializeComponent(raw, player, placeholders);
                    base = base.append(comp);
                }
            }

            sender.sendMessage(base);
        }
    }

    // --------------------- Help Page Sender -----------------------

    public static void sendHelpPage(Player player, int page) {
        ConfigurationSection section = getMessageSection(GeneralMessageModels.GENERAL_HELP_HEADER);
        if (section == null) return;

        List<Map<?, ?>> allComponents = section.getMapList("components");
        List<Map<?, ?>> visibleComponents = new ArrayList<>();

        for (Map<?, ?> raw : allComponents) {
            String permission = (String) raw.get("permission");
            if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
                visibleComponents.add(raw);
            }
        }

        int perPage = 7;
        int maxPage = Math.max(1, (int) Math.ceil(visibleComponents.size() / (double) perPage));
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, visibleComponents.size());

        List<Map<?, ?>> pageComponents = visibleComponents.subList(start, end);

        // 📜 Header
        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == lines.size() - 2) { // 2nd to last line = show page counter
                    line += " <gray>(Page " + page + "/" + maxPage + ")";
                }
                player.sendMessage(PlaceHolderAPIManager.parse(player, line));
            }
        }

        // 🎛️ Command entries
        for (Map<?, ?> map : pageComponents) {
            Component comp = ComponentSerializerManager.deserializeComponent(map, player, Map.of(
                    "page", String.valueOf(page),
                    "max", String.valueOf(maxPage)
            ));
            player.sendMessage(comp);
        }
        player.sendMessage(Component.empty());

        // ✨ Effects
        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, Map.of(
                "page", String.valueOf(page),
                "max", String.valueOf(maxPage)
        ));
        ActionBarResponseManager.send(player, section, Map.of(
                "page", String.valueOf(page),
                "max", String.valueOf(maxPage)
        ));
        EffectResponseManager.send(player, section, Map.of(
                "page", String.valueOf(page),
                "max", String.valueOf(maxPage)
        ));
    }

    public static void sendAdminArenaHelpPage(Player player, int page) {
        ConfigurationSection section = getMessageSection(ArenaMessageModels.ARENA_HELP_HEADER);
        if (section == null) return;

        List<Map<?, ?>> allComponents = section.getMapList("components");
        List<Map<?, ?>> visibleComponents = new ArrayList<>();

        for (Map<?, ?> raw : allComponents) {
            String permission = (String) raw.get("permission");
            if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
                visibleComponents.add(raw);
            }
        }

        int perPage = 7;
        int maxPage = Math.max(1, (int) Math.ceil(visibleComponents.size() / (double) perPage));
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, visibleComponents.size());

        List<Map<?, ?>> pageComponents = visibleComponents.subList(start, end);

        // 📜 Header text
        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == lines.size() - 2) {
                    line += " <gray>(Page " + page + "/" + maxPage + ")";
                }
                player.sendMessage(PlaceHolderAPIManager.parse(player, line));
            }
        }

        // 🎛️ Commands for this page
        for (Map<?, ?> map : pageComponents) {
            Component comp = ComponentSerializerManager.deserializeComponent(map, player, Map.of(
                    "page", String.valueOf(page),
                    "max", String.valueOf(maxPage)
            ));
            player.sendMessage(comp);
        }
        player.sendMessage(Component.empty());

        // ✨ Effects (sound + particles + bossbar + actionbar + effects)
        Map<String, String> placeholders = Map.of(
                "page", String.valueOf(page),
                "max", String.valueOf(maxPage)
        );
        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
    }

    // --- Kit Messages ---

    public static void send(Player player, KitMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, KitMessageModels model, String key, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key, value);
        send(player, model, placeholders);
    }

    public static void send(Player player, KitMessageModels model, Map<String, String> placeholders) {
        if (player == null || model == null) return;

        ConfigurationSection section = getMessageSection(model);
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", false);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        List<String> lines = section.getStringList("text");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");

        ClickEvent.Action clickAction = null;
        try {
            clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        // First line
        if (!lines.isEmpty()) {
            Component base = PlaceHolderAPIManager.parse(player, lines.getFirst(), placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick)); // ✅ use parsed
            }


            base = prefixComponent.append(base);

            if (section.isList("components")) {
                for (Map<?, ?> raw : section.getMapList("components")) {
                    Component comp = ComponentSerializerManager.deserializeComponent(raw, player, placeholders);
                    base = base.append(Component.space()).append(comp);
                }
            }

            player.sendMessage(base);
        }

        // Other lines
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) continue;

            Component message = prefixComponent.append(PlaceHolderAPIManager.parse(player, line, placeholders));

            if (!hover.isBlank()) {
                message = message.hoverEvent(PlaceHolderAPIManager.parse(player, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                message = message.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }

            player.sendMessage(message);
        }

        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
    }

    /**
     * Sends the Kit Admin Help page (paginated).
     */
    public static void sendKitHelpPage(Player player, int page) {
        ConfigurationSection section = getMessageSection(KitMessageModels.HELP_HEADER);
        if (section == null) return;

        List<Map<?, ?>> allComponents = section.getMapList("components");
        List<Map<?, ?>> visibleComponents = new ArrayList<>();

        for (Map<?, ?> raw : allComponents) {
            String permission = (String) raw.get("permission");
            if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
                visibleComponents.add(raw);
            }
        }

        int perPage = 7;
        int maxPage = Math.max(1, (int) Math.ceil(visibleComponents.size() / (double) perPage));
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, visibleComponents.size());

        List<Map<?, ?>> pageComponents = visibleComponents.subList(start, end);

        // 📜 Header
        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == lines.size() - 2) { // 2nd to last line = show page counter
                    line += " <gray>(Page " + page + "/" + maxPage + ")";
                }
                player.sendMessage(PlaceHolderAPIManager.parse(player, line));
            }
        }

        // 🎛️ Command entries
        for (Map<?, ?> map : pageComponents) {
            Component comp = ComponentSerializerManager.deserializeComponent(map, player, Map.of(
                    "page", String.valueOf(page),
                    "max", String.valueOf(maxPage)
            ));
            player.sendMessage(comp);
        }
        player.sendMessage(Component.empty());

        // ✨ Effects
        Map<String, String> placeholders = Map.of(
                "page", String.valueOf(page),
                "max", String.valueOf(maxPage)
        );
        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
    }

    private static ConfigurationSection getMessageSection(KitMessageModels model) {
        ConfigurationSection section = KitMessagesFile.getSection(model.getPath());

        if (section == null) {
            log.warning("⚠️  Missing kit message section: " + model.getPath());
        }

        return section;
    }


    // --------------------- Utilities -----------------------

    private static ConfigurationSection getMessageSection(GeneralMessageModels generalMessageModels) {
        ConfigurationSection section = GeneralMessagesFile.getSection(generalMessageModels.getPath());

        if (section == null) {
            log.warning("⚠️  Missing message section: " + generalMessageModels.getPath());
        }

        return section;
    }

    private static ConfigurationSection getMessageSection(ArenaMessageModels arenaMessageModels) {
        ConfigurationSection section = ArenaMessagesFile.getSection(arenaMessageModels.getPath());

        if (section == null) {
            log.warning("⚠️  Missing message section: " + arenaMessageModels.getPath());
        }

        return section;
    }

    private static ConfigurationSection getMessageSection(QueueMessageModels queueMessageModels) {
        ConfigurationSection section = QueueMessagesFile.getSection(queueMessageModels.getPath());

        if (section == null) {
            log.warning("⚠️  Missing queue message section: " + queueMessageModels.getPath());
        }

        return section;
    }


    private static String getPrefix() {
        if (cachedPrefix == null) {
            cachedPrefix = ConquestDuels.getInstance()
                    .getConfigurationManager()
                    .getConfig()
                    .getString("chat-prefix", "<dark_gray>[<white>ConquestDuels<dark_gray>]<gray> ");
        }
        return cachedPrefix;
    }


    private static Component getPrefixComponent() {
        return PlaceHolderAPIManager.parse(null, getPrefix());
    }

    public static void resetPrefixCache() {
        cachedPrefix = null;
    }

    private static String getPrefixPlain() {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(getPrefixComponent());
    }
}
