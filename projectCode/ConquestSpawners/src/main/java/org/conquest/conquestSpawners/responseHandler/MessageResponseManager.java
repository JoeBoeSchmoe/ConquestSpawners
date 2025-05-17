package org.conquest.conquestSpawners.responseHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.AdminMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.UserMessagesFile;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.PlaceHolderAPIManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import java.util.*;
import java.util.logging.Logger;

public class MessageResponseManager {

    private static final Logger log = ConquestSpawners.getInstance().getLogger();
    private static String cachedPrefix = null;

    public static void send(Player player, AdminMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, AdminMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (player == null || section == null) return;
        sendFormatted(player, section, placeholders);
    }

    public static void send(Player player, UserMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, UserMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (player == null || section == null) return;
        sendFormatted(player, section, placeholders);
    }

    public static void send(CommandSender sender, AdminMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (sender == null || section == null) return;
        sendText(sender, section, placeholders);
    }

    public static void send(CommandSender sender, UserMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (sender == null || section == null) return;
        sendText(sender, section, placeholders);
    }

    private static void sendFormatted(CommandSender sender, ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) return;

        List<String> lines = section.getStringList("text");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");
        boolean showPrefix = section.getBoolean("prefix", true);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        ClickEvent.Action clickAction = null;
        try {
            clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            Component base = PlaceHolderAPIManager.parse(sender instanceof Player ? (Player) sender : null, line, placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(sender instanceof Player ? (Player) sender : null, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }

            base = prefixComponent.append(base);
            sender.sendMessage(base);
        }
    }

    private static void sendText(CommandSender sender, ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) return;

        boolean showPrefix = section.getBoolean("prefix", true);
        String prefix = showPrefix ? getPrefixPlain() : "";

        for (String line : section.getStringList("text")) {
            if (line == null || line.trim().isEmpty()) continue;
            String parsed = PlaceHolderAPIManager.parsePlain(null, prefix + line, placeholders);
            sender.sendMessage(parsed);
        }
    }

    private static ConfigurationSection getMessageSection(AdminMessageModels model) {
        ConfigurationSection section = AdminMessagesFile.getSection(model.getPath());
        if (section == null) log.warning("⚠️ Missing admin message section: " + model.getPath());
        return section;
    }

    private static ConfigurationSection getMessageSection(UserMessageModels model) {
        ConfigurationSection section = UserMessagesFile.getSection(model.getPath());
        if (section == null) log.warning("⚠️ Missing user message section: " + model.getPath());
        return section;
    }

    private static String getPrefix() {
        if (cachedPrefix == null) {
            cachedPrefix = ConquestSpawners.getInstance()
                    .getConfigurationManager()
                    .getConfig()
                    .getString("chat-prefix", "<gray>[ConquestSpawners]</gray> ");
        }
        return cachedPrefix;
    }

    private static Component getPrefixComponent() {
        return PlaceHolderAPIManager.parse(null, getPrefix());
    }

    private static String getPrefixPlain() {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(getPrefixComponent());
    }

    public static void resetPrefixCache() {
        cachedPrefix = null;
    }
}