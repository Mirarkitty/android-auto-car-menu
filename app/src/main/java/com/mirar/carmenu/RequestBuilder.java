package com.mirar.carmenu;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Build the POST body that CarMenu sends to {@code /aa-screen}. Pure JSON
 * assembly — extracted from {@link HttpFetcher} so unit tests can exercise it
 * without an Android Location.
 *
 * <p>Schema: see PROTOCOL.md. Always-present fields: {@code device_id},
 * {@code ts}, {@code trigger}, {@code app_version}, {@code location_permission}.
 * Optional: {@code lat}, {@code lon}, {@code location_fresh},
 * {@code location_age_ms}.
 */
public final class RequestBuilder {

    /** Server-visible permission state. */
    public enum Permission {
        GRANTED,    // user granted ACCESS_FINE_LOCATION (or coarse fallback)
        DENIED,     // user explicitly denied / hasn't granted
        UNKNOWN     // can't determine (shouldn't happen in practice)
    }

    /** Threshold: a fix older than this is reported as {@code location_fresh:false}. */
    static final long STALE_AFTER_MS = 5L * 60_000L;   // 5 minutes

    private RequestBuilder() {}

    public static String build(String deviceId, Double lat, Double lon,
                                Long locationTimeMs, long nowMs,
                                Permission permission,
                                String trigger, int appVer) {
        JSONObject o = new JSONObject();
        try {
            o.put("device_id", deviceId == null ? "" : deviceId);
            if (lat != null && lon != null) {
                o.put("lat", lat.doubleValue());
                o.put("lon", lon.doubleValue());
                if (locationTimeMs != null) {
                    long age = Math.max(0L, nowMs - locationTimeMs);
                    o.put("location_age_ms", age);
                    o.put("location_fresh", age < STALE_AFTER_MS);
                }
            }
            o.put("ts", nowMs);
            o.put("trigger", trigger == null ? "initial" : trigger);
            o.put("app_version", appVer);
            o.put("location_permission", permissionString(permission));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return o.toString();
    }

    static String permissionString(Permission p) {
        if (p == null) return "unknown";
        switch (p) {
            case GRANTED: return "granted";
            case DENIED:  return "denied";
            case UNKNOWN:
            default:      return "unknown";
        }
    }
}
