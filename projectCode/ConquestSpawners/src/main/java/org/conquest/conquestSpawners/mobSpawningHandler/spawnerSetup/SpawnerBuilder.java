package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
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
            Component display = mini.deserialize(
                    rawName.replace("{Spawner}", mob.getMobType()).replace("{level}", String.valueOf(level))
            );
            meta.displayName(display);
        }

        // 📜 Lore
        List<String> loreLines = mob.isOverrideDefaultDisplay()
                ? mob.getDisplayLore()
                : ConquestSpawners.getInstance().getConfigurationManager().getConfig()
                .getStringList("default-values.default-display.display-lore");

        if (loreLines != null && !loreLines.isEmpty()) {
            List<Component> lore = loreLines.stream()
                    .map(line -> mini.deserialize(
                            line.replace("{Spawner}", mob.getMobType())
                                    .replace("{level}", String.valueOf(level))
                    ))
                    .toList();
            meta.lore(lore);
        } else {
            meta.lore(null);
        }

        // 🧬 Metadata
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(ItemUtility.key("mob"), PersistentDataType.STRING, mob.getMobType().toLowerCase());
        data.set(ItemUtility.key("level"), PersistentDataType.INTEGER, level);
        data.set(ItemUtility.key("id"), PersistentDataType.STRING, UUID.randomUUID().toString());

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
}
