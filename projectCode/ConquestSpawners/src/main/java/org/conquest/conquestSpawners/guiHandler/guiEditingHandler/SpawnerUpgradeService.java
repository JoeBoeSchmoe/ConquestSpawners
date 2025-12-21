package org.conquest.conquestSpawners.guiHandler.guiEditingHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.VaultManager;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuManagers.UpgradeSpawnerMenu;
import org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels.SpawnerMenuContext;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobDataModel;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.MobManager;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.SpawnerLevelModel;
import org.conquest.conquestSpawners.responseHandler.MessageResponseManager;
import org.conquest.conquestSpawners.responseHandler.messageModels.UserMessageModels;

import java.util.Collections;
import java.util.Map;

public final class SpawnerUpgradeService {

    private SpawnerUpgradeService() {}

    public static void tryUpgrade(Player player) {
        ConquestSpawners plugin = ConquestSpawners.getInstance();
        GUISession session = GUISessionManager.getOrCreate(player);

        Object raw = session.getEditingSpawnerContext();
        if (!(raw instanceof SpawnerMenuContext ctx) || !ctx.isValid()) return;

        String mobKey = ctx.getMobKey();
        int currentLevel = ctx.getLevel();
        Location loc = ctx.getSpawnerLoc();

        MobManager mobManager = plugin.getConfigurationManager().getMobManager();
        MobDataModel mob = mobManager.getMob(mobKey);
        if (mob == null || mob.getSpawnerLevels() == null) return;

        int maxLevel = mob.getSpawnerLevels().isEmpty()
                ? 0
                : Collections.max(mob.getSpawnerLevels().keySet());

        if (maxLevel <= 0 || currentLevel >= maxLevel) {
            // already maxed; just refresh menu so it shows “maxed”
            Bukkit.getScheduler().runTask(plugin, () -> UpgradeSpawnerMenu.open(player));
            return;
        }

        SpawnerLevelModel currentLevelData = mob.getSpawnerLevels().get(currentLevel);
        SpawnerLevelModel nextLevelData = mob.getSpawnerLevels().get(currentLevel + 1);
        if (currentLevelData == null || nextLevelData == null) return;

        int cost = currentLevelData.getCostToUpgradeResolved();
        if (cost < 0) cost = 0;

        // ---- 1) Ensure spawner is still there
        BlockState st = loc.getBlock().getState();
        if (!(st instanceof CreatureSpawner spawner)) return;

        // ---- 2) Verify PDC still matches this spawner
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        String pdcMob = pdc.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer pdcLevel = pdc.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        if (pdcMob == null || pdcLevel == null) return;
        if (!pdcMob.equalsIgnoreCase(mobKey) || pdcLevel != currentLevel) return;

        // ---- 3) Take money (Vault) - fail safe if Vault isn't hooked
        if (cost > 0) {
            if (!VaultManager.isUsingVault()) return;
            if (!VaultManager.has(player, cost)) {
                MessageResponseManager.send(
                        player,
                        UserMessageModels.SPAWNER_UPGRADE_INSUFFICIENT_FUNDS,
                        Map.of("cost", String.valueOf(cost))
                );
                return;
            }
            if (!VaultManager.withdraw(player, cost)) return;
        }

        // ---- 4) Apply upgrade: write new level
        int newLevel = currentLevel + 1;
        pdc.set(ItemUtility.key("level"), PersistentDataType.INTEGER, newLevel);

        // ---- 5) Re-apply spawner runtime settings for new level
        applySpawnerSettings(spawner, mob, nextLevelData);
        spawner.update(true, false);

        // ---- 6) Update session context so reopen uses the new level
        session.setEditingSpawnerContext(new SpawnerMenuContext(mobKey, newLevel, loc));

        // ---- 7) Re-open menu (rebuilds layout + placeholders)
        Bukkit.getScheduler().runTask(plugin, () -> UpgradeSpawnerMenu.open(player));
    }

    private static void applySpawnerSettings(CreatureSpawner spawner, MobDataModel mob, SpawnerLevelModel levelData) {
        try {
            int spawnRange = Math.max(0, mob.getSpawnRadiusResolved());
            spawner.setSpawnRange(spawnRange);

            int playerRange = Math.max(1, mob.getPlayerActivationRangeResolved());
            spawner.setRequiredPlayerRange(playerRange);

            spawner.setSpawnCount(1);

            int delayTicks = Math.max(20, levelData.getSpawnerDelayTicksResolved());
            spawner.setMinSpawnDelay(delayTicks);
            spawner.setMaxSpawnDelay(delayTicks);

            boolean kickstart = true;
            spawner.setDelay(levelData.getInitialDelayTicks(kickstart));
        } catch (Throwable ignored) {}
    }
}
