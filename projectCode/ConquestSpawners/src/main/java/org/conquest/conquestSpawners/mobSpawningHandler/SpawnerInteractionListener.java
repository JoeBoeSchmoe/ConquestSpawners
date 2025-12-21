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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.guiHandler.GUIOpener;
import org.conquest.conquestSpawners.guiHandler.SpawnerMenuLockManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.SpawnerMenuContext;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerInteractionListener implements Listener {

    private final InteractionType hologramInteraction;
    private final InteractionType upgradeInteraction;

    // ✅ rate-limit: "menu in use" message (per player)
    private static final long IN_USE_MSG_COOLDOWN_MS = 1000L;
    private static final Map<UUID, Long> lastInUseMsgMs = new ConcurrentHashMap<>();

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
        if (!(state instanceof CreatureSpawner spawner)) return;

        // 🪧 Show hologram
        if (actual == hologramInteraction && DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.showTemporaryHologram(clicked.getLocation(), player);
            event.setCancelled(true);
            return;
        }

        // ⬆️ Open upgrade GUI (WITH CONTEXT + LOCK)
        if (actual == upgradeInteraction) {
            SpawnerMenuContext ctx = buildContextFromSpawner(spawner);
            if (ctx == null || !ctx.isValid()) return;

            boolean locked = SpawnerMenuLockManager.tryLock(ctx.getSpawnerLoc(), player);
            if (!locked) {
                // ✅ enforce 1 msg / second
                if (shouldSendInUseMessage(player)) {
                    String ownerName = "someone";
                    UUID ownerId = SpawnerMenuLockManager.getOwner(ctx.getSpawnerLoc());
                    if (ownerId != null) {
                        Player owner = Bukkit.getPlayer(ownerId);
                        if (owner != null) ownerName = owner.getName();
                    }

                    MessageResponseManager.send(
                            player,
                            UserMessageModels.SPAWNER_UPGRADE_MENU_IN_USE,
                            Map.of("player", ownerName)
                    );
                }

                event.setCancelled(true);
                return;
            }

            GUIOpener.open(player, GUIFileEnums.SPAWNER_UPGRADE, ctx);
            event.setCancelled(true);
        }
    }

    private static boolean shouldSendInUseMessage(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();

        Long last = lastInUseMsgMs.get(id);
        if (last != null && (now - last) < IN_USE_MSG_COOLDOWN_MS) return false;

        lastInUseMsgMs.put(id, now);
        return true;
    }

    private SpawnerMenuContext buildContextFromSpawner(CreatureSpawner spawner) {
        if (spawner == null) return null;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        if (mobKey == null || mobKey.isEmpty() || level == null) return null;

        return new SpawnerMenuContext(mobKey.toLowerCase(Locale.ROOT), level, spawner.getLocation());
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
