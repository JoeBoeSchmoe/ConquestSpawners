package org.conquest.conquestSpawners.guiHandler.guiEditingHandler.guiMenuModels;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.UpgradeMenuGUIFile;

/**
 * 📁 GUIFileEnums
 *
 * Enum to link each GUI type to its corresponding configuration file source.
 */
public enum GUIFileEnums {

    SPAWNER_UPGRADE {
        @Override
        public FileConfiguration getConfig() {
            return UpgradeMenuGUIFile.getConfig();
        }
    };

    /**
     * Returns the Bukkit config object associated with this menu type.
     *
     * @return FileConfiguration instance tied to the enum
     */
    public abstract FileConfiguration getConfig();
}
