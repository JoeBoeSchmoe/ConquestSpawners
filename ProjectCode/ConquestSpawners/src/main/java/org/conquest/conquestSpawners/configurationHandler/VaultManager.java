package org.conquest.conquestSpawners.configurationHandler;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.util.logging.Logger;

/**
 * 💰 VaultManager — Handles economy setup through Vault
 */
public class VaultManager {

    private static Economy economy = null;
    private static boolean usingVault = false;

    public static void setup(boolean configEnabled) {
        Logger log = ConquestSpawners.getInstance().getLogger();

        if (!configEnabled) {
            log.info("💰  Vault is disabled in config.yml.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            log.warning( "⚠️  Vault is enabled in config, but Vault plugin is not found.");
            return;
        }

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            log.warning("⚠️  Vault is installed but no economy provider is registered.");
            return;
        }

        economy = provider.getProvider();
        usingVault = true;
        log.info("✅  Vault hooked successfully with economy provider: " + economy.getName());
    }

    public static boolean isUsingVault() {
        return usingVault;
    }

    public static Economy getEconomy() {
        return economy;
    }
}
