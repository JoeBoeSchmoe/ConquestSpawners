package org.conquest.conquestSpawners.configurationHandler.integrationFiles;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.conquest.conquestSpawners.ConquestSpawners;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.ConfigFile;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 🏰 ConquestClansManager
 * Handles integration gating + config-driven settings for ConquestClans,
 * and provides minimal query helpers (claim ownership + player clan id).
 *
 * No compile-time dependency on ConquestClans (reflection based).
 */
public final class ConquestClansManager {

    private static boolean enabled = false;         // config + plugin present + bridge ok
    private static boolean mustBeInClaims = false;  // config
    private static int maxSpawnerCountPerClan = -1; // config (only used when mustBeInClaims=true)

    // ---- Reflection bridge state (cached) ----
    private static Object clanManager;

    private static Method mGetClanManager;          // ConquestClans#getClanManager()
    private static Method mGetClanIdForPlayerFast;  // ClanManager#getClanIdForPlayerFast(String)
    private static Method mFindTerritoryOwner;      // ClanManager#findTerritoryOwner(String,int,int)

    // ClanModel#id() method is called reflectively on the model instance (reload-safe cache)
    private static Method mClanModelId;
    private static Class<?> cachedClanModelClass;

    private ConquestClansManager() { }

    public static void initialize(boolean shouldEnable) {
        Logger log = ConquestSpawners.getInstance().getLogger();

        // Pull config values every init (supports /reload)
        mustBeInClaims = ConfigFile.getBoolean("conquestclans.spawners-must-be-in-claims", false);
        maxSpawnerCountPerClan = ConfigFile.getInt("conquestclans.max-spawner-count-per-clan", 64);

        // reset bridge
        enabled = false;
        clanManager = null;

        mGetClanManager = null;
        mGetClanIdForPlayerFast = null;
        mFindTerritoryOwner = null;

        mClanModelId = null;
        cachedClanModelClass = null;

        if (!shouldEnable) {
            log.info("⛔  ConquestClans integration disabled in config.");
            return;
        }

        Plugin cc = Bukkit.getPluginManager().getPlugin("ConquestClans");
        if (cc == null || !cc.isEnabled()) {
            log.warning("⚠️  ConquestClans not found. Integration skipped.");
            return;
        }

        try {
            // ConquestClans#getClanManager()
            mGetClanManager = cc.getClass().getMethod("getClanManager");
            mGetClanManager.setAccessible(true);
            clanManager = mGetClanManager.invoke(cc);

            if (clanManager == null) {
                throw new IllegalStateException("ConquestClans#getClanManager() returned null.");
            }

            Class<?> cmType = clanManager.getClass();

            // ClanManager#getClanIdForPlayerFast(String)
            mGetClanIdForPlayerFast = cmType.getMethod("getClanIdForPlayerFast", String.class);
            mGetClanIdForPlayerFast.setAccessible(true);

            // ClanManager#findTerritoryOwner(String,int,int) -> Optional<ClanModel>
            mFindTerritoryOwner = cmType.getMethod("findTerritoryOwner", String.class, int.class, int.class);
            mFindTerritoryOwner.setAccessible(true);

            enabled = true;
            log.info("✅  ConquestClans hooked successfully.");
        } catch (Throwable t) {
            enabled = false;
            clanManager = null;

            mGetClanManager = null;
            mGetClanIdForPlayerFast = null;
            mFindTerritoryOwner = null;

            mClanModelId = null;
            cachedClanModelClass = null;

            log.warning("⚠️  ConquestClans hook failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    // ------------------------------------------------------------
    // Public gating
    // ------------------------------------------------------------

    public static boolean isEnabled() {
        return enabled && clanManager != null;
    }

    public static boolean mustBeInClaims() {
        return isEnabled() && mustBeInClaims;
    }

    /**
     * Only meaningful if mustBeInClaims() is true.
     * Returns -1 if unlimited.
     */
    public static int getMaxSpawnerCountPerClan() {
        if (!mustBeInClaims()) return -1;
        return maxSpawnerCountPerClan;
    }

    // ------------------------------------------------------------
    // Query helpers used by ConquestSpawners placement/break logic
    // ------------------------------------------------------------

    /**
     * @return player's clan id (lowercased), or empty if no clan / hook off
     */
    public static Optional<String> getPlayerClanId(Player player) {
        if (!isEnabled() || player == null) return Optional.empty();
        try {
            Object res = mGetClanIdForPlayerFast.invoke(clanManager, player.getUniqueId().toString());
            if (!(res instanceof String s) || s.isBlank()) return Optional.empty();
            return Optional.of(s.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    /**
     * @return claim owner clan id (lowercased) for the chunk containing this location,
     *         or empty if unclaimed / hook off
     */
    public static Optional<String> getClaimOwnerClanId(Location location) {
        if (!isEnabled() || location == null) return Optional.empty();
        World w = location.getWorld();
        if (w == null) return Optional.empty();

        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;

        try {
            Object opt = mFindTerritoryOwner.invoke(clanManager, w.getName(), cx, cz);
            if (!(opt instanceof Optional<?> ownerOpt) || ownerOpt.isEmpty()) return Optional.empty();

            Object clanModel = ownerOpt.get();
            if (clanModel == null) return Optional.empty();

            // ✅ Reload-safe: cache ClanModel#id() per classloader/class instance
            Class<?> modelClass = clanModel.getClass();
            if (mClanModelId == null || cachedClanModelClass != modelClass) {
                cachedClanModelClass = modelClass;
                mClanModelId = modelClass.getMethod("id");
                mClanModelId.setAccessible(true);
            }

            Object idRes = mClanModelId.invoke(clanModel);
            if (!(idRes instanceof String id) || id.isBlank()) return Optional.empty();

            return Optional.of(id.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static boolean isClaimed(Location location) {
        return getClaimOwnerClanId(location).isPresent();
    }

    /**
     * Convenience: true if location is claimed AND owned by the player's clan.
     */
    public static boolean isPlayersOwnClaim(Player player, Location location) {
        if (!isEnabled()) return false;

        Optional<String> pClan = getPlayerClanId(player);
        if (pClan.isEmpty()) return false;

        Optional<String> owner = getClaimOwnerClanId(location);
        return owner.isPresent() && owner.get().equalsIgnoreCase(pClan.get());
    }
}
