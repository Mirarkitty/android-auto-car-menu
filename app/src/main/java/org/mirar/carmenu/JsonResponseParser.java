package org.mirar.carmenu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse a server JSON body into {@link ServerResponse}. See PROTOCOL.md.
 *
 * <p>Pure-ish — uses {@code org.json} (bundled with Android, available as a
 * stdlib-equivalent on the JVM via a test-scope dep). No Android types here
 * so the unit tests can hit it without Robolectric.
 *
 * <p>Unrecognised templates produce a {@code MESSAGE} fallback per
 * PROTOCOL.md ("Unsupported screen — update CarMenu").
 *
 * <p>The {@code serverBase} arg supplies the origin for resolving relative
 * {@code icon_url} fields and for the same-origin reject of absolute ones.
 * Pass {@code null} to disable remote icons (drops {@code iconUrl} on every
 * row).
 */
public final class JsonResponseParser {

    private JsonResponseParser() {}

    public static ServerResponse parse(String body) throws JSONException {
        return parse(body, null);
    }

    public static ServerResponse parse(String body, String serverBase) throws JSONException {
        if (body == null) throw new JSONException("null body");
        JSONObject o = new JSONObject(body);
        String tpl = o.optString("template", "");
        switch (tpl) {
            case "list":    return parseList(o, serverBase);
            case "pane":    return parsePane(o, serverBase);
            case "message": return parseMessage(o);
            default:
                return ServerResponse.message("CarMenu",
                        "Unsupported screen — update CarMenu");
        }
    }

    private static ServerResponse parseList(JSONObject o, String serverBase) throws JSONException {
        String title = optStringOrNull(o, "title");
        List<ServerResponse.Row> rows = parseRows(o.optJSONArray("rows"), serverBase);
        int refresh = clampRefresh(o.optInt("refresh_seconds", 0));
        return new ServerResponse(ServerResponse.Kind.LIST, title, null, null, null,
                rows, null, null, refresh);
    }

    private static ServerResponse parsePane(JSONObject o, String serverBase) throws JSONException {
        String title = optStringOrNull(o, "title");
        List<ServerResponse.Row> rows = parseRows(o.optJSONArray("rows"), serverBase);
        String imageUrlRaw = optStringOrNull(o, "image_url");
        String imageUrl = imageUrlRaw == null ? null
                : UrlOrigin.resolveSameOrigin(serverBase, imageUrlRaw);
        List<ServerResponse.Row> actions = parseRows(o.optJSONArray("actions"), serverBase);
        int refresh = clampRefresh(o.optInt("refresh_seconds", 0));
        return new ServerResponse(ServerResponse.Kind.PANE, title, null, null, null,
                rows, imageUrl, actions, refresh);
    }

    private static ServerResponse parseMessage(JSONObject o) {
        String title = optStringOrNull(o, "title");
        String subtitle = optStringOrNull(o, "subtitle");
        String icon = optStringOrNull(o, "icon");
        TintSpec tint = TintSpec.parse(optStringOrNull(o, "tint"));
        int refresh = clampRefresh(o.optInt("refresh_seconds", 0));
        return new ServerResponse(ServerResponse.Kind.MESSAGE, title, subtitle, icon, tint,
                null, null, null, refresh);
    }

    private static List<ServerResponse.Row> parseRows(JSONArray arr, String serverBase) throws JSONException {
        List<ServerResponse.Row> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.optJSONObject(i);
            if (r == null) continue;
            String title = optStringOrNull(r, "title");
            if (title == null) continue; // title is required
            String subtitle = optStringOrNull(r, "subtitle");
            String icon = optStringOrNull(r, "icon");
            String iconUrlRaw = optStringOrNull(r, "icon_url");
            String iconUrl = iconUrlRaw == null ? null
                    : UrlOrigin.resolveSameOrigin(serverBase, iconUrlRaw);
            // Remote icon wins over slug; but if same-origin guard rejected
            // it (iconUrl=null after resolve), fall back to slug-if-set or
            // nothing.
            TintSpec tint = TintSpec.parse(optStringOrNull(r, "tint"));
            String tapIntent = null;
            JSONObject tap = r.optJSONObject("tap");
            if (tap != null) {
                tapIntent = optStringOrNull(tap, "intent");
            }
            out.add(new ServerResponse.Row(title, subtitle, icon, iconUrl, tint, tapIntent));
        }
        return out;
    }

    private static String optStringOrNull(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) return null;
        String s = o.optString(key, "");
        return s.isEmpty() ? null : s;
    }

    /** Per PROTOCOL.txt: 0 = none; 1..2 → 3 (mild anti-loop floor); else passthrough. */
    static int clampRefresh(int v) {
        if (v <= 0) return 0;
        if (v < 3) return 3;
        if (v > 3600) return 3600;
        return v;
    }

    /** Build a synthetic error MESSAGE response for non-2xx / unparseable. */
    public static ServerResponse errorMessage(int httpStatus, String body) {
        String snippet = body == null ? "" : body.replace('\n', ' ').trim();
        if (snippet.length() > 80) snippet = snippet.substring(0, 80);
        String sub = httpStatus + (snippet.isEmpty() ? "" : " — " + snippet);
        return ServerResponse.message("Server error", sub);
    }
}
