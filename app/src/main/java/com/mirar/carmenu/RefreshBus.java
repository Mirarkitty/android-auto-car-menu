package com.mirar.carmenu;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tiny process-wide signal that the AA screen should re-query the server.
 *
 * <p>Use case: the user opens the phone-side {@code MainActivity}, edits
 * the server URL or grants location permission, hits Save. If the AA
 * session is currently active (phone in cradle, DHU running, etc.), we
 * want the AA screen to refetch immediately instead of waiting for the
 * next {@code refresh_seconds} tick.
 *
 * <p>No-op if no listener is subscribed (screen not currently showing).
 * Listeners are notified on the main looper.
 */
public final class RefreshBus {

    public interface Listener { void onRefreshRequested(); }

    private static final RefreshBus INSTANCE = new RefreshBus();
    public static RefreshBus get() { return INSTANCE; }

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    private RefreshBus() {}

    public void subscribe(Listener l)   { if (l != null) listeners.addIfAbsent(l); }
    public void unsubscribe(Listener l) { listeners.remove(l); }

    /** Ask any active subscribers to re-query. Safe to call from any thread. */
    public void signal() {
        for (Listener l : listeners) {
            main.post(l::onRefreshRequested);
        }
    }
}
