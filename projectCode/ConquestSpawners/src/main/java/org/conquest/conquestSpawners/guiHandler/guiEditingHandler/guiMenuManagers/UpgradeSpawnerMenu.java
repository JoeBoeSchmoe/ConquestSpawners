package org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuManagers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.EditorMenuManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUISession;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUISessionManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.EditingMenuHolder;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.ItemBuilder;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.*;

/**
 * 🧬 UpgradeSpawnerMenu
 * Builds and opens the spawner upgrade GUI for players.
 */
public class UpgradeSpawnerMenu {

    private static final GUIFileEnums MENU_TYPE = GUIFileEnums.SPAWNER_UPGRADE;

    public static void open(Player player) {
        DuelMenuMeta meta = EditorMenuManager.getMeta(MENU_TYPE);
        if (meta == null) return;

        GUISession session = GUISessionManager.getOrCreate(player);
        session.touch();

        int rows = meta.getRows();
        int size = rows * 9;
        Component title = MiniMessage.miniMessage().deserialize(meta.getTitleFormat());
        Inventory inv = Bukkit.createInventory(new EditingMenuHolder(MENU_TYPE), size, title);

        // 🧱 Fill background
        if (meta.isUsesFiller() && meta.getFillerItem() != null) {
            FillerItemModel filler = meta.getFillerItem();
            ItemStack fillerItem = ItemBuilder.create(Map.of(
                    "material", filler.getMaterial(),
                    "name", filler.getName(),
                    "amount", filler.getAmount(),
                    "lore", filler.getLore(),
                    "enchanted", filler.isEnchanted(),
                    "customData", filler.getCustomData()
            ));
            ItemBuilder.setPlaceholderTag(fillerItem);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        // 🧩 Context placeholders from mob config
        String spawnerId = "allay";
        int spawnerLevel = 2;

        MobManager mobManager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
        MobDataModel mobData = mobManager.getMob(spawnerId);

// Compute maxLevel BEFORE using it anywhere
        int maxLevel = 0;
        if (mobData != null && mobData.getSpawnerLevels() != null && !mobData.getSpawnerLevels().isEmpty()) {
            maxLevel = Collections.max(mobData.getSpawnerLevels().keySet());
        }
        boolean isMaxed = spawnerLevel >= maxLevel;

        SpawnerLevelModel levelData = (mobData != null && mobData.getSpawnerLevels() != null)
                ? mobData.getSpawnerLevels().get(spawnerLevel)
                : null;

        Map<String, String> placeholderMap = (levelData != null)
                ? Map.of(
                "spawn_rate", String.valueOf(levelData.getSpawnerDelayResolved()),
                "spawn_count", String.valueOf(levelData.getMobCountResolved()),
                "xp_bonus", String.valueOf(levelData.getXpDropResolved()),
                "upgrade_cost", String.valueOf(levelData.getCostToUpgradeResolved()),
                "upgrade_path", spawnerLevel + " → " + (isMaxed ? spawnerLevel : (spawnerLevel + 1))
        )
                : Collections.emptyMap();

        // 🛠 Choose appropriate layout icons
        if (meta.getLayout() != null) {
            List<Map<String, Object>> layout = meta.getLayout();

            Map<Integer, Map<String, Object>> uniqueSlots = new HashMap<>();
            for (Map<String, Object> itemData : layout) {
                int slotId = (int) itemData.getOrDefault("slot", -1);
                if (slotId < 0 || slotId >= size) continue;

                String action = ((String) itemData.getOrDefault("action", "")).toLowerCase(Locale.ROOT);
                if ("upgrade".equals(action) && isMaxed) continue;
                if ("maxed".equals(action) && !isMaxed) continue;

                uniqueSlots.putIfAbsent(slotId, itemData);
            }

            for (Map.Entry<Integer, Map<String, Object>> entry : uniqueSlots.entrySet()) {
                ItemStack menuItem = ItemBuilder.create(entry.getValue(), placeholderMap);
                ItemBuilder.setPlaceholderTag(menuItem);
                inv.setItem(entry.getKey(), menuItem);
            }
        }

        player.openInventory(inv);
        session.markOpen();

        if (meta.getEffects().containsKey("open")) {
            meta.getEffects().get("open").play(player);
        }
    }
}
