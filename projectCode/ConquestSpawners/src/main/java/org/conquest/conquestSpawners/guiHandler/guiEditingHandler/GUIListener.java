package org.conquest.conquestSpawners.guiHandler.guiEditingHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.guiHandler.SpawnerMenuLockManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.SpawnerMenuContext;
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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.SPAWNER_UPGRADE) return;

        event.setCancelled(true); // block movement always

        // Only handle clicks inside the top inventory (GUI), not player inventory
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int slot = event.getRawSlot();

        // Your YAML uses slot 16 for upgrade OR maxed
        if (slot == 16) {
            // If the menu is currently showing "maxed" in slot 16, do nothing.
            // (Barrier is your maxed icon.)
            if (clicked.getType() == Material.BARRIER) {
                return;
            }

            // Otherwise treat it as upgrade.
            SpawnerUpgradeService.tryUpgrade(player);
            return;
        }
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

        // ✅ delay unlock so "refresh by reopen" doesn't drop the lock mid-upgrade
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getOpenInventory();
            player.getOpenInventory().getTopInventory();
            boolean stillInSpawnerMenu =
                    player.getOpenInventory().getTopInventory().getHolder() instanceof EditingMenuHolder h && h.getMenuType() == GUIFileEnums.SPAWNER_UPGRADE;

            if (!stillInSpawnerMenu) {
                releaseSpawnerLock(player);
                GUISessionManager.get(player).ifPresent(GUISession::markClosed);
            }
        }, 1L);
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        releaseSpawnerLock(event.getPlayer());
    }

    private void releaseSpawnerLock(Player player) {
        GUISessionManager.get(player).ifPresent(session -> {
            Object raw = session.getEditingSpawnerContext();
            if (!(raw instanceof SpawnerMenuContext ctx)) return;
            if (ctx.getSpawnerLoc() == null) return;

            SpawnerMenuLockManager.unlock(ctx.getSpawnerLoc(), player.getUniqueId());

            // Optional: clear context so stale context can't unlock later
            session.setEditingSpawnerContext(null);
        });
    }
}
