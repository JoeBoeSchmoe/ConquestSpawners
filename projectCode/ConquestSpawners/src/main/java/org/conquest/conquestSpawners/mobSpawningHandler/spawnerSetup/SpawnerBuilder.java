package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.List;
import java.util.UUID;

/**
 * Builds the temporary visual spawner item using a placeholder material.
 * Plugin-controlled spawning — no vanilla logic.
 */
public class SpawnerBuilder {

    private static final MiniMessage mini = MiniMessage.miniMessage();

    public static ItemStack buildSpawner(MobDataModel mob, int level) {
        // 🧱 Placeholder block instead of SPANNER
        ItemStack item = new ItemStack(Material.SPAWNER); // <-- swapped from Material.SPAWNER
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 🏷 Name
        String rawName = mob.isOverrideDefaultDisplay()
                ? mob.getDisplayName()
                : ConquestSpawners.getInstance().getConfigurationManager().getConfig()
                .getString("default-values.default-display.display-name");

        if (rawName != null) {
            String resolved = rawName
                    .replace("{Spawner}", mob.getMobType())
                    .replace("{level}", String.valueOf(level));

            Component display = mini.deserialize(resolved);
            meta.displayName(defaultNonItalic(rawName, display));
        }


        // 📜 Lore
        List<String> loreLines = mob.isOverrideDefaultDisplay()
                ? mob.getDisplayLore()
                : ConquestSpawners.getInstance().getConfigurationManager().getConfig()
                .getStringList("default-values.default-display.display-lore");

        if (loreLines != null && !loreLines.isEmpty()) {
            List<Component> lore = loreLines.stream()
                    .map(line -> {
                        String resolved = line
                                .replace("{Spawner}", mob.getMobType())
                                .replace("{level}", String.valueOf(level));

                        Component c = mini.deserialize(resolved);
                        return defaultNonItalic(line, c);
                    })
                    .toList();

            meta.lore(lore);
        } else {
            meta.lore(null);
        }


        // 🧬 Metadata
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(ItemUtility.key("mob"), PersistentDataType.STRING, mob.getMobType().toLowerCase());
        data.set(ItemUtility.key("level"), PersistentDataType.INTEGER, level);


        // ✨ Enchantment glint (if enabled in config)
        boolean glint = ConquestSpawners.getInstance().getConfigurationManager().getConfig()
                .getBoolean("default-values.default-display.is-enchanted", false);

        if (glint) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // 🧼 Hide everything vanilla
        meta.addItemFlags(
                ItemFlag.HIDE_ITEM_SPECIFICS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_DYE
        );

        item.setItemMeta(meta);
        return item;
    }
    private static boolean explicitlyRequestsItalic(String raw) {
        if (raw == null) return false;
        String s = raw.toLowerCase();

        // MiniMessage common italics signals
        return s.contains("<i>") ||
                s.contains("</i>") ||
                s.contains("<italic>") ||
                s.contains("</italic>") ||
                s.contains("<em>") ||
                s.contains("</em>") ||
                s.contains("italic=true") ||
                s.contains("italic:false") == false && s.contains("italic"); // covers odd variants
    }

    private static Component defaultNonItalic(String raw, Component component) {
        // If user explicitly requested italics, don't override.
        if (explicitlyRequestsItalic(raw)) return component;

        // Otherwise force non-italic at the root so children inherit it unless they override.
        return component.decoration(TextDecoration.ITALIC, false);
    }

}
