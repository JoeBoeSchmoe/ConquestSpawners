package org.conquest.conquestSpawners.configurationHandler.integrationFiles;

import org.bukkit.Bukkit;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.logging.Logger;

/**
 * 🪧 DecentHologramsManager
 * Handles integration with the DecentHolograms plugin if present and enabled via config.
 */
public class DecentHologramsManager {

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
}
