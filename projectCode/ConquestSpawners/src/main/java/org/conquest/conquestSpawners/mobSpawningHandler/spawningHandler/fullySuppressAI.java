package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.lang.reflect.Method;

public class fullySuppressAI {

    public static void fullySuppressAI(LivingEntity entity) {
        try {
            // Ensure this only runs on mobs
            if (!(entity instanceof CraftLivingEntity craft)) return;

            Object nmsEntity = craft.getHandle(); // net.minecraft.world.entity.EntityInsentient
            Class<?> nmsClass = nmsEntity.getClass();

            // These are goal selectors responsible for behavior
            Method getGoalSelector = nmsClass.getMethod("getGoalSelector");
            Method getTargetSelector = nmsClass.getMethod("getTargetSelector");

            Object goalSelector = getGoalSelector.invoke(nmsEntity);
            Object targetSelector = getTargetSelector.invoke(nmsEntity);

            clearGoals(goalSelector);
            clearGoals(targetSelector);

            // Optional: prevent random look direction (by removing lookController)
            Method setLookController = nmsClass.getMethod("setLookControl", Class.forName("net.minecraft.world.entity.ai.control.LookControl"));
            Object dummyController = Class.forName("net.minecraft.world.entity.ai.control.LookControl")
                    .getConstructor(Class.forName("net.minecraft.world.entity.Mob"))
                    .newInstance(nmsEntity);

            setLookController.invoke(nmsEntity, dummyController);

        } catch (Exception e) {
            ConquestSpawners.getInstance().getLogger().warning("❌  Failed to suppress AI for entity: " + e.getMessage());
        }
    }

    private static void clearGoals(Object selector) {
        try {
            Method clear = selector.getClass().getMethod("getAvailableGoals");
            ((java.util.Set<?>) clear.invoke(selector)).clear();
        } catch (Exception e) {
            ConquestSpawners.getInstance().getLogger().warning("⚠️  Failed to clear goals: " + e.getMessage());
        }
    }
}
