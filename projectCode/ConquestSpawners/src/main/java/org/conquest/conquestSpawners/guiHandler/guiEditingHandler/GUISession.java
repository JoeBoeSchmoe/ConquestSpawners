package org.conquest.conquestSpawners.guiHandler.guiEditingHandler;

import java.util.UUID;

/**
 * 🛠️ GUISession
 * Tracks session state for players using the Spawner Upgrade GUI.
 */
public class GUISession {

    private final UUID playerId;
    private long lastInteraction;

    private Runnable confirmAction;
    private Runnable cancelAction;
    private String confirmContextKey;

    // Placeholder for your actual spawner context, if needed
    private Object editingSpawnerContext;

    private boolean closed = false;

    public GUISession(UUID playerId) {
        this.playerId = playerId;
        this.lastInteraction = System.currentTimeMillis();
    }

    // ────────────── Session State ──────────────

    public UUID getPlayerId() {
        return playerId;
    }

    public long getLastInteraction() {
        return lastInteraction;
    }

    public void touch() {
        this.lastInteraction = System.currentTimeMillis();
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - lastInteraction > timeoutMillis;
    }

    public void markClosed() {
        this.closed = true;
    }

    public void markOpen() {
        this.closed = false;
    }

    public boolean wasClosed() {
        return closed;
    }

    // ────────────── Confirm Actions ──────────────

    public void setConfirmContext(String key, Runnable confirmAction, Runnable cancelAction) {
        this.confirmContextKey = key;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public void clearConfirmContext() {
        this.confirmContextKey = null;
        this.confirmAction = null;
        this.cancelAction = null;
    }

    public boolean hasConfirmContext() {
        return confirmContextKey != null && confirmAction != null;
    }

    public String getConfirmContextKey() {
        return confirmContextKey;
    }

    public Runnable getConfirmAction() {
        return confirmAction;
    }

    public Runnable getCancelAction() {
        return cancelAction;
    }

    // ────────────── Spawner Context (Optional) ──────────────

    public Object getEditingSpawnerContext() {
        return editingSpawnerContext;
    }

    public void setEditingSpawnerContext(Object context) {
        this.editingSpawnerContext = context;
    }

    public boolean isEditingSpawner() {
        return editingSpawnerContext != null;
    }

    public void clearEditingSpawner() {
        this.editingSpawnerContext = null;
    }

    public void clearAll() {
        clearConfirmContext();
        clearEditingSpawner();
    }
}
