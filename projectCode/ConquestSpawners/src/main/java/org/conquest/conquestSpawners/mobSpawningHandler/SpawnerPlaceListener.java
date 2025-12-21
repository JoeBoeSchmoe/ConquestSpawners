package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.ConquestClansManager;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.DecentHologramsManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerRequirementsModel;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpawnerPlaceListener implements Listener {

    private final ConquestSpawners plugin;
    private final MobManager mobManager;
    private final SpawnerManager spawnerManager;

    public SpawnerPlaceListener(MobManager mobManager) {
        this.plugin = ConquestSpawners.getInstance();
        this.mobManager = mobManager;
        this.spawnerManager = plugin.getConfigurationManager().getSpawnerManager();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        PersistentDataContainer itemData = item.getItemMeta().getPersistentDataContainer();
        String mobKey = itemData.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = itemData.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) return;
        mobKey = mobKey.toLowerCase(Locale.ROOT);

        MobDataModel mob = mobManager.getMob(mobKey);
        if (mob == null) {
            reject(player, event, "Invalid spawner NBT (mob not found).");
            return;
        }

        Location loc = event.getBlock().getLocation();
        if (loc.getWorld() == null) {
            reject(player, event, "Invalid world (cannot place spawner here).");
            return;
        }

        SpawnerRequirementsModel req = mob.getRequirements();
        int y = loc.getBlockY();

        // ---------------------------------------------------------------------
        // Biome restriction
        // ---------------------------------------------------------------------
        if (req != null && req.inBiome && req.allowedBiomes != null && !req.allowedBiomes.isEmpty()) {
            String biome = loc.getBlock().getBiome().getKey().getKey().toLowerCase(Locale.ROOT);

            Set<String> allowed = new HashSet<>();
            for (String b : req.allowedBiomes) {
                if (b != null && !b.isEmpty()) allowed.add(b.toLowerCase(Locale.ROOT));
            }

            if (!allowed.contains(biome)) {
                reject(player, event, "Cannot place this spawner in <gray>" + biome.replace("_", " ") + "</gray> biome.");
                return;
            }
        }

        // ---------------------------------------------------------------------
        // Y-axis checks (keeping your same logic)
        // ---------------------------------------------------------------------
        if (req != null) {
            if ((req.aboveSeaLevel && y <= 63) || (req.belowSeaLevel && y >= 63)
                    || (req.aboveYAxis && y <= 63) || (req.belowYAxis && y >= 63)) {
                reject(player, event, "Cannot place at Y=" + y + " due to vertical restrictions.");
                return;
            }
        }

        // ---------------------------------------------------------------------
        // Chunk spawner limit checks (tile entity scan)
        // ---------------------------------------------------------------------
        Chunk chunk = loc.getChunk();
        int sameMobCount = 0;
        int totalSpawnerCount = 0;

        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof TileState tile)) continue;

            PersistentDataContainer existing = tile.getPersistentDataContainer();
            String existingKey = existing.get(ItemUtility.key("mob"), PersistentDataType.STRING);
            if (existingKey != null) {
                totalSpawnerCount++;
                if (existingKey.equalsIgnoreCase(mobKey)) sameMobCount++;
            }
        }

        int globalLimit = plugin.getConfig().getInt("max-total-spawners-per-chunk", -1);
        if (globalLimit != -1 && totalSpawnerCount >= globalLimit) {
            reject(player, event, "Chunk has reached the global spawner limit (<gray>"
                    + totalSpawnerCount + "/" + globalLimit + "</gray>).");
            return;
        }

        int mobLimit = mob.getAllowedSpawnersPerChunkResolved();
        if (mobLimit > 0 && sameMobCount >= mobLimit) {
            reject(player, event, "Too many <green>" + mobKey + "</green> spawners in this chunk (<gray>"
                    + sameMobCount + "/" + mobLimit + "</gray>).");
            return;
        }

        // ---------------------------------------------------------------------
        // 🏰 ConquestClans integration checks (claim-only + per-clan cap)
        // ---------------------------------------------------------------------
        if (ConquestClansManager.mustBeInClaims()) {

            // Must be inside a claimed chunk owned by the player’s clan
            if (!ConquestClansManager.isPlayersOwnClaim(player, loc)) {
                reject(player, event, "Spawners must be placed inside <yellow>your clan claim</yellow>.");
                return;
            }

            // Optional per-clan spawner cap
            int max = ConquestClansManager.getMaxSpawnerCountPerClan();
            if (max > -1) {
                String clanId = ConquestClansManager.getPlayerClanId(player).orElse(null);
                if (clanId == null) {
                    reject(player, event, "You must be in a clan to place spawners in claims.");
                    return;
                }

                // ✅ Best-effort count (uses your SpawnerManager storage as source of truth)
                int current = spawnerManager.getSpawnerCountForClan(clanId);

                if (current + 1 > max) {
                    MessageResponseManager.send(
                            player,
                            UserMessageModels.CLAN_SPAWNER_CAPACITY_REACHED,
                            Map.of(
                                    "current", String.valueOf(current),
                                    "max", String.valueOf(max)
                            )
                    );
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // ---------------------------------------------------------------------
        // Write spawner PDC + apply vanilla settings immediately + store location
        // ---------------------------------------------------------------------
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            reject(player, event, "Block does not support spawner data.");
            return;
        }

        // Visual spin type
        try {
            EntityType type = EntityType.valueOf(mob.getMobType().toUpperCase(Locale.ROOT));
            spawner.setSpawnedType(type);
        } catch (IllegalArgumentException ignored) {
            spawner.setSpawnedType(EntityType.PIG);
        }

        // Persist identity keys
        PersistentDataContainer placedData = spawner.getPersistentDataContainer();
        placedData.set(ItemUtility.key("mob"), PersistentDataType.STRING, mobKey);
        placedData.set(ItemUtility.key("level"), PersistentDataType.INTEGER, level);

        // Apply vanilla spawner settings NOW (do not wait for SpawnerSpawnEvent)
        SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
        if (levelData != null) {
            applySpawnerSettings(spawner, mob, levelData);
        }

        // Save tile state
        spawner.update(true, false);

        // Storage handshake (memory + disk)
        spawnerManager.addSpawner(mobKey, level, loc);

        // Hologram feedback
        if (DecentHologramsManager.isEnabled()) {
            DecentHologramsManager.showTemporaryHologram(block.getLocation(), player);
        }

        MessageResponseManager.send(player, UserMessageModels.SPAWNER_PLACE_SUCCESS);
    }

    private void applySpawnerSettings(CreatureSpawner spawner, MobDataModel mob, SpawnerLevelModel levelData) {
        try {
            // Spawn radius -> vanilla spawner spawn range
            int spawnRange = Math.max(0, mob.getSpawnRadiusResolved());
            spawner.setSpawnRange(spawnRange);

            // Activation range -> vanilla spawner required player range
            int playerRange = Math.max(1, mob.getPlayerActivationRangeResolved());
            spawner.setRequiredPlayerRange(playerRange);

            // STRICT MODE: vanilla spawner should only "trigger" once; we spawn the batch ourselves
            spawner.setSpawnCount(1);

            // Delay (config is seconds -> spawner uses ticks)
            int delayTicks = levelData.getSpawnerDelayTicksResolved();
            spawner.setMinSpawnDelay(delayTicks);
            spawner.setMaxSpawnDelay(delayTicks);

            // Kickstart after placement (optional):
            // - true: spawn soon (1s) then follow delayTicks for subsequent cycles
            // - false: follow delayTicks immediately
            boolean kickstart = true;
            spawner.setDelay(levelData.getInitialDelayTicks(kickstart));
        } catch (Throwable t) {
            plugin.getLogger().warning("Spawner apply settings failed on place: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void reject(Player player, BlockPlaceEvent event, String reason) {
        MessageResponseManager.send(player, UserMessageModels.SPAWNER_PLACE_FAILED, Map.of("reason", reason));
        event.setCancelled(true);
    }
}
