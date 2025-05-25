package org.conquest.conquestSpawners.configurationHandler.integrationFiles;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;

import java.util.*;
import java.util.logging.Logger;

/**
 * 🪧 DecentHologramsManager
 * Handles integration with the DecentHolograms plugin if present and enabled via config.
 */
public class DecentHologramsManager {

    private static final Map<String, String> activeHolograms = new HashMap<>();
    private static final Map<String, Integer> activeTimers = new HashMap<>();

    private static boolean enabled = false;

    public static void initialize(boolean shouldEnable) {
        Logger log = ConquestSpawners.getInstance().getLogger();

        if (!shouldEnable) {
            log.info("⛔  DecentHolograms integration disabled in config.");
            enabled = false;
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            enabled = true;
            log.info("✅  DecentHolograms hooked successfully.");
        } else {
            log.warning("⚠️  DecentHolograms not found. Integration skipped.");
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
    public static void showTemporaryHologram(Location spawnerLoc, Player player) {
        ConquestSpawners plugin = ConquestSpawners.getInstance();

        // Fetch spawner metadata
        BlockState state = spawnerLoc.getBlock().getState();
        if (!(state instanceof CreatureSpawner spawner)) return;

        PersistentDataContainer data = spawner.getPersistentDataContainer();
        String mobKey = data.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = data.get(ItemUtility.key("level"), PersistentDataType.INTEGER);
        if (mobKey == null || level == null) return;

        MobManager mobManager = plugin.getConfigurationManager().getMobManager();
        MobDataModel mob = mobManager.getMob(mobKey.toLowerCase(Locale.ROOT));
        if (mob == null) return;

        SpawnerLevelModel levelData = mob.getSpawnerLevels().get(level);
        if (levelData == null) return;

        // === Hologram Config Resolution ===
        boolean useCustom = mob.isOverrideDefaultHologramDisplay();
        List<String> rawLines;
        double offset;
        int durationTicks;

        if (useCustom) {
            rawLines = mob.getHologramDisplayLines();
            offset = mob.getHologramVerticalOffsetResolved();
            durationTicks = mob.getHologramDisplayTimeResolved();
        } else {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("default-values.default-hologram-display");
            if (section == null) return;
            rawLines = section.getStringList("lines");
            offset = section.getDouble("vertical-offset", 1.5);
            durationTicks = section.getInt("display-time", 80);
        }

        // === Hologram Display ===
        String displayName = mob.isOverrideDefaultDisplay() ? mob.getDisplayName() : mob.getMobType();
        int delay = levelData.getSpawnerDelayResolved();
        int count = levelData.getMobCountResolved();
        int xp = levelData.getXpDropResolved();

        List<String> formattedLines = new ArrayList<>();
        for (String line : rawLines) {
            formattedLines.add(convertMiniMessage(
                    line.replace("{Spawner}", displayName)
                            .replace("{level}", String.valueOf(level))
                            .replace("{delay}", String.valueOf(delay))
                            .replace("{mob_count}", String.valueOf(count))
                            .replace("{xp}", String.valueOf(xp))
            ));
        }

        Location holoLoc = spawnerLoc.clone().add(0.5, offset, 0.5);
        String locKey = spawnerLoc.getWorld().getName() + ":" + spawnerLoc.getBlockX() + ":" + spawnerLoc.getBlockY() + ":" + spawnerLoc.getBlockZ();

        // Timer reset logic
        if (activeTimers.containsKey(locKey)) {
            Bukkit.getScheduler().cancelTask(activeTimers.get(locKey));
            activeTimers.remove(locKey);
        }

        String hologramId = activeHolograms.getOrDefault(locKey, "csp-holo-" + UUID.randomUUID());
        boolean createdNew = !activeHolograms.containsKey(locKey);
        activeHolograms.put(locKey, hologramId);

        if (createdNew) {
            DHAPI.createHologram(hologramId, holoLoc, false, formattedLines);
        } else {
            Hologram existing = DHAPI.getHologram(hologramId);
            if (existing != null) {
                DHAPI.setHologramLines(existing, formattedLines);
            }
        }

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DHAPI.removeHologram(hologramId);
            activeHolograms.remove(locKey);
            activeTimers.remove(locKey);
        }, durationTicks).getTaskId();

        activeTimers.put(locKey, taskId);
    }

    public static void removeHologramIfExists(Location spawnerLoc) {
        String locKey = spawnerLoc.getWorld().getName() + ":" +
                spawnerLoc.getBlockX() + ":" + spawnerLoc.getBlockY() + ":" + spawnerLoc.getBlockZ();

        String hologramId = activeHolograms.remove(locKey);
        if (hologramId != null) {
            DHAPI.removeHologram(hologramId);
        }

        if (activeTimers.containsKey(locKey)) {
            Bukkit.getScheduler().cancelTask(activeTimers.remove(locKey));
        }
    }

    public static void clearAllHolograms() {
        for (String id : activeHolograms.values()) {
            Hologram holo = DHAPI.getHologram(id);
            if (holo != null) holo.delete();
        }
        activeHolograms.clear();
        activeTimers.clear();
    }

    private static String convertMiniMessage(String mini) {
        Component parsed = MiniMessage.miniMessage().deserialize(mini);
        return LegacyComponentSerializer.legacySection().serialize(parsed);
    }

}
