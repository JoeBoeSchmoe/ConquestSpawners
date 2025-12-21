package org.conquest.conquestSpawners.mobSpawningHandler.spawnerSetup;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.conquest.conquestSpawners.configurationHandler.configurationFiles.SpawnerDataFile;
import org.conquest.conquestSpawners.configurationHandler.integrationFiles.ConquestClansManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    // mobKey -> level -> set of positions
    private final Map<String, Map<Integer, Set<SpawnerPos>>> index = new ConcurrentHashMap<>();
    // clanId(lowercase) -> spawner count (only used when ConquestClans mustBeInClaims is enabled)
    private final Map<String, Integer> clanCounts = new ConcurrentHashMap<>();

    public void clear() {
        index.clear();
        clanCounts.clear();
    }


    /** Used ONLY by SpawnerDataFile.load(...) to populate memory without writing disk. */
    public boolean addToMemoryOnly(String mobKey, int level, Location loc) {
        if (mobKey == null || mobKey.isEmpty() || loc == null || loc.getWorld() == null) return false;
        return addPosToMemoryOnly(mobKey, level, new SpawnerPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    /** Used ONLY by SpawnerDataFile.load(...) when world may not be loaded yet. */
    public boolean addEncodedToMemoryOnly(String mobKey, int level, String encoded) {
        SpawnerPos pos = SpawnerPos.tryParse(encoded);
        if (pos == null) return false;
        return addPosToMemoryOnly(mobKey, level, pos);
    }

    private boolean addPosToMemoryOnly(String mobKey, int level, SpawnerPos pos) {
        if (mobKey == null || mobKey.isEmpty() || pos == null) return false;

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        Map<Integer, Set<SpawnerPos>> byLevel = index.computeIfAbsent(mobKey, k -> new ConcurrentHashMap<>());
        Set<SpawnerPos> set = byLevel.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet());
        return set.add(pos);
    }

    /** Runtime add: memory + disk */
    /** Runtime add: memory + disk */
    public void addSpawner(String mobKey, int level, Location loc) {
        if (mobKey == null || mobKey.isEmpty() || loc == null || loc.getWorld() == null) return;

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        String worldName = loc.getWorld().getName();
        SpawnerPos pos = new SpawnerPos(worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (addPosToMemoryOnly(mobKey, level, pos)) {
            // ✅ disk
            SpawnerDataFile.addSpawner(mobKey, level, loc);

            // ✅ count (only if rule is enabled)
            if (ConquestClansManager.mustBeInClaims()) {
                ConquestClansManager.getClaimOwnerClanId(loc)
                        .ifPresent(ownerClanId -> incrementClanCount(ownerClanId, +1));
            }
        }
    }

    /** Runtime remove: memory + disk */
    public void removeSpawner(String mobKey, int level, Location loc) {
        if (mobKey == null || mobKey.isEmpty() || loc == null || loc.getWorld() == null) return;

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        Map<Integer, Set<SpawnerPos>> byLevel = index.get(mobKey);
        if (byLevel == null) return;

        Set<SpawnerPos> set = byLevel.get(level);
        if (set == null) return;

        SpawnerPos pos = new SpawnerPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (set.remove(pos)) {
            // ✅ disk
            SpawnerDataFile.removeSpawner(mobKey, level, loc);

            // ✅ count (best-effort based on current claim ownership)
            if (ConquestClansManager.mustBeInClaims()) {
                ConquestClansManager.getClaimOwnerClanId(loc)
                        .ifPresent(ownerClanId -> incrementClanCount(ownerClanId, -1));
            }
        }
    }


    // ---------------------------------------------------------------------
    // ✅ Startup-only broken-data check
    // ---------------------------------------------------------------------

    /**
     * Runs ONE startup-only sequence:
     * - validates already-loaded worlds (next tick)
     * - validates worlds as they load (WorldLoadEvent)
     * - removes stale entries from memory + disk
     *
     * Call this once after SpawnerDataFile.load(spawnerManager) during startup.
     */
    public void runStartupBrokenDataCheck(Plugin plugin) {
        if (plugin == null) return;

        // ✅ If ConquestClans enforcement is enabled, rebuild counts from scratch during this scrub pass
        if (ConquestClansManager.mustBeInClaims()) {
            clearClanCounts();
        }

        // Snapshot which worlds we care about (from stored data)
        final Set<String> pendingWorlds = ConcurrentHashMap.newKeySet();
        for (Map<Integer, Set<SpawnerPos>> byLevel : index.values()) {
            for (Set<SpawnerPos> set : byLevel.values()) {
                for (SpawnerPos pos : set) {
                    if (pos.worldName != null && !pos.worldName.isEmpty()) {
                        pendingWorlds.add(pos.worldName);
                    }
                }
            }
        }

        if (pendingWorlds.isEmpty()) return;

        final StartupWorldValidator listener = new StartupWorldValidator(plugin, pendingWorlds);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        // Validate worlds that are already loaded (next tick so server is stable)
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (World w : Bukkit.getWorlds()) {
                listener.validateWorldOnce(w);
            }

            // If everything was validated immediately, clean up the listener.
            listener.finishIfDone();
        });
    }

    private final class StartupWorldValidator implements Listener {
        private final Plugin plugin;
        private final Set<String> pendingWorlds;
        private int removed = 0;
        private boolean finished = false;

        private StartupWorldValidator(Plugin plugin, Set<String> pendingWorlds) {
            this.plugin = plugin;
            this.pendingWorlds = pendingWorlds;
        }

        @EventHandler
        public void onWorldLoad(WorldLoadEvent event) {
            validateWorldOnce(event.getWorld());
            finishIfDone();
        }

        private void validateWorldOnce(World world) {
            if (world == null) return;

            String name = world.getName();
            if (!pendingWorlds.remove(name)) return; // already validated or not referenced

            removed += pruneInvalidEntriesInWorld(world);
        }

        private void finishIfDone() {
            if (finished) return;
            if (!pendingWorlds.isEmpty()) return;

            finished = true;
            HandlerList.unregisterAll(this);

            if (removed > 0) {
                Bukkit.getLogger().info("[ConquestSpawners] 🧹  Startup spawner scrub removed " + removed + " stale stored locations.");
            }
        }
    }

    /**
     * Removes entries where:
     * - block isn't a spawner anymore OR
     * - spawner is missing our PDC keys OR
     * - PDC mob/level doesn't match the bucket it was stored under
     *
     * Returns how many entries were removed.
     */
    private int pruneInvalidEntriesInWorld(World world) {
        int removed = 0;
        String worldName = world.getName();

        for (Map.Entry<String, Map<Integer, Set<SpawnerPos>>> mobEntry : index.entrySet()) {
            String mobKey = mobEntry.getKey();
            Map<Integer, Set<SpawnerPos>> byLevel = mobEntry.getValue();
            if (byLevel == null || byLevel.isEmpty()) continue;

            for (Map.Entry<Integer, Set<SpawnerPos>> lvlEntry : byLevel.entrySet()) {
                int level = lvlEntry.getKey();
                Set<SpawnerPos> set = lvlEntry.getValue();
                if (set == null || set.isEmpty()) continue;

                // iterator remove is safe on ConcurrentHashMap.newKeySet() via its iterator
                Iterator<SpawnerPos> it = set.iterator();
                while (it.hasNext()) {
                    SpawnerPos pos = it.next();
                    if (!worldName.equals(pos.worldName)) continue;

                    // 1) Validate spawner block + PDC matching
                    if (!isStillValidCustomSpawner(world, mobKey, level, pos)) {
                        it.remove();
                        SpawnerDataFile.removeEncoded(mobKey, level, pos.encode());
                        removed++;
                        continue;
                    }

                    // 2) If claims-enforcement is enabled, require this spawner to be inside ANY claim
                    if (ConquestClansManager.mustBeInClaims()) {
                        Location at = new Location(world, pos.x, pos.y, pos.z);

                        Optional<String> owner = ConquestClansManager.getClaimOwnerClanId(at);

                        if (owner.isEmpty()) {
                            // Not inside a claim anymore -> treat as invalid stored entry
                            it.remove();
                            SpawnerDataFile.removeEncoded(mobKey, level, pos.encode());
                            removed++;
                            continue;
                        }

                        // Valid + in claim => count it
                        incrementClanCount(owner.get(), +1);
                    }
                }
            }
        }

        return removed;
    }

    private boolean isStillValidCustomSpawner(World world, String expectedMobKey, int expectedLevel, SpawnerPos pos) {
        // 1) must still be a spawner block
        if (world.getBlockAt(pos.x, pos.y, pos.z).getType() != Material.SPAWNER) return false;

        // 2) must still have our PDC data and match expected mobKey/level
        if (!(world.getBlockAt(pos.x, pos.y, pos.z).getState() instanceof TileState tile)) return false;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        String mobKey = pdc.get(ItemUtility.key("mob"), PersistentDataType.STRING);
        Integer level = pdc.get(ItemUtility.key("level"), PersistentDataType.INTEGER);

        if (mobKey == null || level == null) return false;

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        return mobKey.equals(expectedMobKey.trim().toLowerCase(Locale.ROOT)) && level == expectedLevel;
    }

    // ---------------------------------------------------------------------
    // Query helpers
    // ---------------------------------------------------------------------

    public Set<Location> getLocations(String mobKey, int level) {
        if (mobKey == null || mobKey.isEmpty()) return Collections.emptySet();

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        Map<Integer, Set<SpawnerPos>> byLevel = index.get(mobKey);
        if (byLevel == null) return Collections.emptySet();

        Set<SpawnerPos> set = byLevel.get(level);
        if (set == null || set.isEmpty()) return Collections.emptySet();

        Set<Location> out = new HashSet<>(set.size());
        for (SpawnerPos pos : set) {
            World w = Bukkit.getWorld(pos.worldName);
            if (w == null) continue;
            out.add(new Location(w, pos.x, pos.y, pos.z));
        }
        return out;
    }

    public Set<String> getEncodedPositions(String mobKey, int level) {
        if (mobKey == null || mobKey.isEmpty()) return Collections.emptySet();

        mobKey = mobKey.trim().toLowerCase(Locale.ROOT);

        Map<Integer, Set<SpawnerPos>> byLevel = index.get(mobKey);
        if (byLevel == null) return Collections.emptySet();

        Set<SpawnerPos> set = byLevel.get(level);
        if (set == null || set.isEmpty()) return Collections.emptySet();

        Set<String> out = new HashSet<>(set.size());
        for (SpawnerPos pos : set) out.add(pos.encode());
        return out;
    }

    // ---------------------------------------------------------------------

    public static final class SpawnerPos {
        public final String worldName;
        public final int x, y, z;

        public SpawnerPos(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String encode() {
            return worldName + ":" + x + ":" + y + ":" + z;
        }

        public static SpawnerPos tryParse(String s) {
            try {
                if (s == null || s.isEmpty()) return null;
                String[] parts = s.split(":");
                if (parts.length != 4) return null;

                String world = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                return new SpawnerPos(world, x, y, z);
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SpawnerPos other)) return false;
            return x == other.x && y == other.y && z == other.z && Objects.equals(worldName, other.worldName);
        }

        @Override public int hashCode() {
            return Objects.hash(worldName, x, y, z);
        }
    }

    public int getSpawnerCountForClan(String clanId) {
        if (clanId == null || clanId.isBlank()) return 0;
        return clanCounts.getOrDefault(clanId.trim().toLowerCase(Locale.ROOT), 0);
    }

    private void clearClanCounts() {
        clanCounts.clear();
    }

    private void incrementClanCount(String clanId, int delta) {
        if (clanId == null || clanId.isBlank()) return;
        final String key = clanId.trim().toLowerCase(Locale.ROOT);

        clanCounts.compute(key, (k, v) -> {
            int next = (v == null ? 0 : v) + delta;
            return (next <= 0) ? null : next;
        });
    }

}
