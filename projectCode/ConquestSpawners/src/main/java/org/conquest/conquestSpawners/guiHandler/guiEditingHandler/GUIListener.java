package org.conquest.conquestSpawners.guiHandler.guiEditingHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiUtilites.EditingMenuHolder;

/**
 * 🧭 GUIListener
 * Handles input blocking and cleanup for the spawner upgrade menu.
 */
public class GUIListener implements Listener {

    private final ConquestSpawners plugin;

    public GUIListener(ConquestSpawners plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.SPAWNER_UPGRADE) return;

        event.setCancelled(true); // Block all item movement
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.SPAWNER_UPGRADE) return;

        event.setCancelled(true); // Prevent dragging items into GUI
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.SPAWNER_UPGRADE) return;

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof EditingMenuHolder)) {
                        GUISessionManager.get(player).ifPresent(GUISession::markClosed);
                    }
                },
                1L
        );
    }
}
