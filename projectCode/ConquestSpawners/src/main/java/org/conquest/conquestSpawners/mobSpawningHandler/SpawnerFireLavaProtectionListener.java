package org.conquest.conquestSpawners.mobSpawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup.ItemUtility;

public final class SpawnerFireLavaProtectionListener implements Listener {

    public SpawnerFireLavaProtectionListener() {

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        Entity ent = event.getEntity();
        if (!(ent instanceof Item itemEntity)) return;

        ItemStack stack = itemEntity.getItemStack();
        if (!isCustomSpawnerItem(stack)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {

            event.setCancelled(true);

            // Helps visually + prevents re-ignition loops in some edge cases
            itemEntity.setFireTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item itemEntity)) return;

        ItemStack stack = itemEntity.getItemStack();
        if (!isCustomSpawnerItem(stack)) return;

        event.setCancelled(true);
        itemEntity.setFireTicks(0);
    }

    private boolean isCustomSpawnerItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Your spawner identity keys (must exist)
        String mobKey = pdc.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = pdc.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        return mobKey != null && !mobKey.isEmpty() && level != null && level > 0;
    }
}
