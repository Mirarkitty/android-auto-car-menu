package com.mirar.carmenu;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Trivial SharedPreferences wrapper. Three keys:
 * <ul>
 *   <li>{@code server_url} — the HTTPS endpoint the user enters in
 *       MainActivity. Empty string = unconfigured.</li>
 *   <li>{@code device_id} — generated on first launch, persisted forever
 *       (user-editable). Used in the request body so the server can
 *       distinguish multiple installs if there's a fleet.</li>
 *   <li>{@code app_version_at_first_install} — for telemetry / debugging only.</li>
 * </ul>
 *
 * <p>Nothing in here ever leaves the device except via HttpFetcher posting
 * to the user-configured URL.
 */
public final class Prefs {
    private static final String PREFS_NAME = "CarMenuPrefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_DEVICE_ID = "device_id";

    private Prefs() {}

    private static SharedPreferences sp(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getServerUrl(Context ctx) {
        return sp(ctx).getString(KEY_SERVER_URL, "");
    }

    public static void setServerUrl(Context ctx, String url) {
        sp(ctx).edit().putString(KEY_SERVER_URL, url == null ? "" : url.trim()).apply();
    }

    /** Returns an id, generating one on first call. Stable across launches. */
    public static String getDeviceId(Context ctx) {
        SharedPreferences p = sp(ctx);
        String id = p.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = "carmenu." + UUID.randomUUID().toString().substring(0, 8);
            p.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public static void setDeviceId(Context ctx, String id) {
        if (id == null || id.trim().isEmpty()) return;
        sp(ctx).edit().putString(KEY_DEVICE_ID, id.trim()).apply();
    }
}
