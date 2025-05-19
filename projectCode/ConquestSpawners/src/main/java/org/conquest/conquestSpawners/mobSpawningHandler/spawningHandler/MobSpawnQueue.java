package org.conquest.conquestSpawners.mobSpawningHandler.spawningHandler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Global thread-safe queue of mobs waiting to be spawned by SpawnerTask.
 */
public class MobSpawnQueue {

    private final Queue<CustomMob> queue = new ConcurrentLinkedQueue<>();

    public void add(CustomMob mob) {
        queue.offer(mob);
    }

    public CustomMob poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
