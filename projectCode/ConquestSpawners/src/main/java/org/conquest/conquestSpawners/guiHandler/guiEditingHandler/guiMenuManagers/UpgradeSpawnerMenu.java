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
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.SpawnerMenuContext;
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

        // ✅ Pull context from session (set by SpawnerInteractionListener -> GUIOpener.open(..., ctx))
        SpawnerMenuContext ctx = null;
        Object rawCtx = session.getEditingSpawnerContext();
        if (rawCtx instanceof SpawnerMenuContext cast) ctx = cast;

        if (ctx == null || !ctx.isValid()) {
            // No context => build menu with empty placeholders so players don't see wrong info
            renderLayout(inv, meta, Collections.emptyMap(), false);
            player.openInventory(inv);
            session.markOpen();
            playOpen(meta, player);
            return;
        }

        String mobKey = ctx.getMobKey();
        int spawnerLevel = ctx.getLevel();

        MobManager mobManager = ConquestSpawners.getInstance().getConfigurationManager().getMobManager();
        MobDataModel mobData = mobManager.getMob(mobKey);

        int maxLevel = 0;
        if (mobData != null && mobData.getSpawnerLevels() != null && !mobData.getSpawnerLevels().isEmpty()) {
            maxLevel = Collections.max(mobData.getSpawnerLevels().keySet());
        }

        boolean isMaxed = maxLevel > 0 && spawnerLevel >= maxLevel;

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
                : Map.of(
                "spawn_rate", "?",
                "spawn_count", "?",
                "xp_bonus", "?",
                "upgrade_cost", "?",
                "upgrade_path", spawnerLevel + " → " + spawnerLevel
        );

        renderLayout(inv, meta, placeholderMap, isMaxed);

        player.openInventory(inv);
        session.markOpen();
        playOpen(meta, player);
    }

    private static void renderLayout(Inventory inv, DuelMenuMeta meta, Map<String, String> placeholderMap, boolean isMaxed) {
        if (meta.getLayout() == null) return;

        int size = inv.getSize();
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

    private static void playOpen(DuelMenuMeta meta, Player player) {
        if (meta.getEffects() != null && meta.getEffects().containsKey("open")) {
            meta.getEffects().get("open").play(player);
        }
    }
}
