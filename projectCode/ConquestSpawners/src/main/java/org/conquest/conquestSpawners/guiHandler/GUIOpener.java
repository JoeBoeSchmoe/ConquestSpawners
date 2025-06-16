package org.conquest.conquestSpawners.guiHandler;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUISessionManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.EditorMenuManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuManagers.UpgradeSpawnerMenu;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.EffectModel;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.EffectModelParser;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.FillerItemParser;

import java.util.*;

/**
 * 📂 GUIOpener
 * Opens spawner upgrade GUI and builds meta dynamically if needed.
 */
public class GUIOpener {

    public static void open(Player player, GUIFileEnums type) {
        ensureMetaBuilt(type);
        GUISessionManager.getOrCreate(player).touch();

        switch (type) {
            case SPAWNER_UPGRADE -> UpgradeSpawnerMenu.open(player);
        }
    }

    private static void ensureMetaBuilt(GUIFileEnums type) {
        if (EditorMenuManager.hasMeta(type)) return;

        FileConfiguration config = type.getConfig();

        String title = config.getString("menu.title", "<gray>Menu");
        boolean usesFiller = config.getBoolean("menu.filler", true);
        int rows = Math.min(6, Math.max(1, config.getInt("menu.rows-per-page", 1)));

        List<Map<String, Object>> layout = new ArrayList<>();
        if (config.isList("menu.layout")) {
            for (Map<?, ?> raw : config.getMapList("menu.layout")) {
                Map<String, Object> mapped = new HashMap<>();
                raw.forEach((k, v) -> {
                    if (k instanceof String key) {
                        mapped.put(key, v);
                    }
                });
                layout.add(mapped);
            }
        }

        FillerItemModel fillerItem = null;
        ConfigurationSection fillerSection = config.getConfigurationSection("menu.filler-item");
        if (fillerSection != null) {
            fillerItem = FillerItemParser.parse(fillerSection);
        }

        Map<String, EffectModel> effects = new HashMap<>();
        ConfigurationSection soundsSection = config.getConfigurationSection("menu.sounds");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                ConfigurationSection sfx = soundsSection.getConfigurationSection(key);
                if (sfx != null) {
                    EffectModel model = EffectModelParser.parseEffect(sfx);
                    if (model != null) {
                        effects.put(key.toLowerCase(Locale.ROOT), model);
                    }
                }
            }
        }

        Map<String, Object> emptyItem = null;
        ConfigurationSection emptySection = config.getConfigurationSection("menu.empty-item");
        if (emptySection != null) {
            emptyItem = DuelMenuMeta.extractSection(emptySection);
        }

        Map<String, Object> playerItem = null;
        ConfigurationSection itemSection = config.getConfigurationSection("item");
        if (itemSection != null) {
            playerItem = DuelMenuMeta.extractSection(itemSection);
        }

        DuelMenuMeta meta = new DuelMenuMeta(type, title, rows, usesFiller, layout, fillerItem, effects, emptyItem, playerItem);
        EditorMenuManager.putMeta(type, meta);
    }
}
