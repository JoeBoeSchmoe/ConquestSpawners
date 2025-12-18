package org.conquest.conquestSpawners.configurationHandler.configurationFiles;

import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * 🛠️ UpgradeMenuGUIFile
 * Loads guiConfiguration/upgradeSpawnerGUI.yml from the plugin's data folder.
 */
public class UpgradeMenuGUIFile {

    private static final ConquestSpawners plugin = ConquestSpawners.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private UpgradeMenuGUIFile() {
        // Utility class — prevent instantiation
    }

    /**
     * Loads or creates guiConfiguration/upgradeSpawnerGUI.yml from the plugin's folder.
     */
    public static void load() {
        try {
            File guiFolder = new File(plugin.getDataFolder(), "GuiConfiguration");
            if (!guiFolder.exists() && !guiFolder.mkdirs()) {
                log.warning("⚠️  Failed to create guiConfiguration folder.");
            }

            File file = new File(guiFolder, "upgradeSpawnerGUI.yml");

            if (!file.exists()) {
                try (InputStream in = plugin.getResource("GuiConfiguration/upgradeSpawnerGUI.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default upgradeSpawnerGUI.yml in guiConfiguration/");
                    } else {
                        log.warning("⚠️  Missing upgradeSpawnerGUI.yml in plugin jar under guiConfiguration/");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded upgradeSpawnerGUI.yml");

        } catch (Exception e) {
            log.severe("❌  Failed to load upgradeSpawnerGUI.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }
}
