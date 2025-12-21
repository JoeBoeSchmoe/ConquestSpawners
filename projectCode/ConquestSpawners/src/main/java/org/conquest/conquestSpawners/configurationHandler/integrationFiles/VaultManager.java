package org.conquest.conquestSpawners.configurationHandler.integrationFiles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.conquest.conquestSpawners.ConquestSpawners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * 💰 SafeVaultHook - safely hooks into Vault only if present
 */
public class VaultManager {

    private static Object economy = null;
    private static boolean usingVault = false;

    public static void initialize(boolean configEnabled) {
        Logger log = ConquestSpawners.getInstance().getLogger();

        if (!configEnabled) {
            log.info("💰  Vault disabled via config.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            log.warning("⚠️  Vault plugin is missing.");
            return;
        }

        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(econClass);

            if (provider == null) {
                log.warning("⚠️  Vault found, but no economy provider registered.");
                return;
            }

            economy = provider.getProvider();
            usingVault = true;
            log.info("✅  Vault hooked with provider: " + provider.getProvider().getClass().getSimpleName());

        } catch (ClassNotFoundException e) {
            log.severe("❌  Vault classes not found. Is it installed?");
        } catch (Exception e) {
            log.severe("❌  Unexpected error while hooking Vault: " + e.getMessage());
        }
    }

    public static boolean isUsingVault() {
        return usingVault;
    }

    public static Object getEconomy() {
        return economy;
    }

    // ------------------------------------------------------------------------
    // 💸 Economy helpers (reflection, no hard dependency)
    // ------------------------------------------------------------------------

    public static boolean has(Player player, double amount) {
        if (player == null) return false;
        if (amount <= 0) return true;

        if (!usingVault || economy == null) {
            // No economy hooked -> treat as "can’t check" (fail safe)
            return false;
        }

        try {
            double bal = getBalance(player);
            return bal >= amount;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean withdraw(Player player, double amount) {
        if (player == null) return false;
        if (amount <= 0) return true;

        if (!usingVault || economy == null) return false;

        try {
            // Prefer OfflinePlayer signature: withdrawPlayer(OfflinePlayer, double)
            Method withdraw = findMethod(
                    economy.getClass(),
                    "withdrawPlayer",
                    new Class<?>[]{Class.forName("org.bukkit.OfflinePlayer"), double.class}
            );

            Object response;
            if (withdraw != null) {
                response = withdraw.invoke(economy, player, amount); // Player implements OfflinePlayer
            } else {
                // Fallback: withdrawPlayer(String, double)
                Method withdrawName = findMethod(economy.getClass(), "withdrawPlayer", new Class<?>[]{String.class, double.class});
                if (withdrawName == null) return false;
                response = withdrawName.invoke(economy, player.getName(), amount);
            }

            return economyResponseSuccess(response);

        } catch (Throwable t) {
            return false;
        }
    }

    private static double getBalance(Player player) throws Exception {
        // Prefer OfflinePlayer signature: getBalance(OfflinePlayer)
        Method bal = findMethod(
                economy.getClass(),
                "getBalance",
                new Class<?>[]{Class.forName("org.bukkit.OfflinePlayer")}
        );

        if (bal != null) {
            Object out = bal.invoke(economy, player);
            return out instanceof Number n ? n.doubleValue() : 0.0;
        }

        // Fallback: getBalance(String)
        Method balName = findMethod(economy.getClass(), "getBalance", new Class<?>[]{String.class});
        if (balName != null) {
            Object out = balName.invoke(economy, player.getName());
            return out instanceof Number n ? n.doubleValue() : 0.0;
        }

        return 0.0;
    }

    private static boolean economyResponseSuccess(Object response) {
        if (response == null) return false;

        try {
            // EconomyResponse#transactionSuccess()
            Method m = findMethod(response.getClass(), "transactionSuccess", new Class<?>[]{});
            if (m != null) {
                Object out = m.invoke(response);
                return out instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {}

        try {
            // EconomyResponse has public boolean transactionSuccess; (some impls)
            Field f = response.getClass().getField("transactionSuccess");
            Object out = f.get(response);
            return out instanceof Boolean b && b;
        } catch (Throwable ignored) {}

        return false;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>[] params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
