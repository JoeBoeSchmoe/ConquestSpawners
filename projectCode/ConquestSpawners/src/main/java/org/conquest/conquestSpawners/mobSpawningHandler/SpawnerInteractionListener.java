package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.guiHandler.GUIOpener;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;

public class SpawnerInteractionListener implements Listener {

    private final InteractionType hologramInteraction;
    private final InteractionType upgradeInteraction;

    public SpawnerInteractionListener(ConquestSpawners plugin) {
        this.hologramInteraction = InteractionType.from(
                plugin.getConfig().getString("interactions.hologram-display-interaction", "SHIFT_LEFT_CLICK")
        );
        this.upgradeInteraction = InteractionType.from(
                plugin.getConfig().getString("interactions.upgrade-interaction", "RIGHT_CLICK")
        );

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SPAWNER) return;

        InteractionType actual = resolveInteractionType(event);
        if (actual == null) return;

        BlockState state = clicked.getState();
        if (!(state instanceof CreatureSpawner)) return;

        // 🪧 Show hologram
        if (actual == hologramInteraction && DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.showTemporaryHologram(clicked.getLocation(), player);
            event.setCancelled(true);
            return;
        }

        // ⬆️ Open upgrade GUI
        if (actual == upgradeInteraction) {
            GUIOpener.open(player, GUIFileEnums.SPAWNER_UPGRADE);
            event.setCancelled(true);
        }
    }

    private InteractionType resolveInteractionType(PlayerInteractEvent event) {
        boolean shift = event.getPlayer().isSneaking();
        Action action = event.getAction();

        return switch (action) {
            case LEFT_CLICK_BLOCK -> shift ? InteractionType.SHIFT_LEFT_CLICK : InteractionType.LEFT_CLICK;
            case RIGHT_CLICK_BLOCK -> shift ? InteractionType.SHIFT_RIGHT_CLICK : InteractionType.RIGHT_CLICK;
            default -> null;
        };
    }

    private enum InteractionType {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_LEFT_CLICK,
        SHIFT_RIGHT_CLICK;

        public static InteractionType from(String raw) {
            try {
                return InteractionType.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
