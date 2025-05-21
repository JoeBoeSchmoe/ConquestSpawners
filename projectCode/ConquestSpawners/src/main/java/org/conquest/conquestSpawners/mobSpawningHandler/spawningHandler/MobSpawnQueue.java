package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Global thread-safe queue of mobs waiting to be spawned by SpawningListener.
 * Includes queue size limiting, safety logging, and inspection helpers.
 */
public class MobSpawnQueue {

    private static final int MAX_QUEUE_SIZE = 1000; // Prevent queue from overflowing
    private final Queue<CustomMob> queue = new ConcurrentLinkedQueue<>();

    /**
     * Attempts to add a mob to the queue.
     * Logs a warning if the queue is full.
     *
     * @param mob The mob to enqueue
     * @return true if successfully added, false if rejected due to overflow
     */
    public boolean add(CustomMob mob) {
        if (queue.size() >= MAX_QUEUE_SIZE) {
            logLimitReached(mob.getType());
            return false;
        }
        return queue.offer(mob);
    }

    /**
     * Polls the next mob to be spawned from the queue.
     *
     * @return The next mob, or null if queue is empty
     */
    public CustomMob poll() {
        return queue.poll();
    }

    /**
     * Peeks at the next mob in the queue without removing it.
     *
     * @return The next mob, or null if empty
     */
    public CustomMob peek() {
        return queue.peek();
    }

    /**
     * @return The current number of mobs waiting to be spawned.
     */
    public int size() {
        return queue.size();
    }

    /**
     * @return True if the queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clears all mobs currently queued for spawning.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Logs a warning if the queue is full.
     */
    private void logLimitReached(EntityType type) {
        Logger logger = ConquestSpawners.getInstance().getLogger(); // Safe access even before plugin is initialized
        logger.warning("⚠️ Mob spawn queue limit reached (" + MAX_QUEUE_SIZE + "). Skipping mob: " + type);
    }
}
