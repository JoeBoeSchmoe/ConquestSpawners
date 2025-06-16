package org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuManagers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.EditorMenuManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUISession;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.GUISessionManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.EditingMenuHolder;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.ItemBuilder;

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
            FillerItemModel fillerItem = meta.getFillerItem();
            ItemStack filler = ItemBuilder.create(Map.of(
                    "material", fillerItem.getMaterial(),
                    "name", fillerItem.getName(),
                    "amount", fillerItem.getAmount(),
                    "lore", fillerItem.getLore(),
                    "enchanted", fillerItem.isEnchanted(),
                    "customData", fillerItem.getCustomData()
            ));
            ItemBuilder.setPlaceholderTag(filler);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        // 🧩 Build layout items
        List<Map<String, Object>> layoutItems = meta.getLayout();
        if (layoutItems != null) {
            for (Map<String, Object> layoutItem : layoutItems) {
                int slot = (int) layoutItem.getOrDefault("slot", -1);
                if (slot < 0 || slot >= size) continue;

                Map<String, Object> itemMap = new HashMap<>(layoutItem);
                ItemStack item = ItemBuilder.create(itemMap);
                ItemBuilder.setPlaceholderTag(item);
                inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
        session.markOpen();

        if (meta.getEffects().containsKey("open")) {
            meta.getEffects().get("open").play(player);
        }
    }
}
