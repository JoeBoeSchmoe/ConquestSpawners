package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.NamespacedKey;
import org.conquest.conquestSpawners.ConquestSpawners;

public class ItemUtility {

    public static NamespacedKey key(String namespace, String path) {
        return new NamespacedKey(namespace, path);
    }

    public static NamespacedKey key(String path) {
        return new NamespacedKey(ConquestSpawners.getInstance(), path);
    }

}
