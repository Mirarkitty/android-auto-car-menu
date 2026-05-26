package org.mirar.carmenu;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.car.app.CarContext;

/**
 * Dispatch row-tap intents from the server.
 *
 * <p>Policy: pass-through. Anything the server returns is handed to
 * {@code carContext.startCarApp(intent)}; AA is the gatekeeper and decides
 * what actually launches.
 *
 * <p>One exception: the {@code carmenu:} scheme is reserved for internal
 * actions ({@code refresh}, {@code back}) that never leave the app.
 *
 * <h2>Intent action selection</h2>
 *
 * <p>AA's {@code startCarApp()} requires an AA-specific action constant
 * paired with the URI scheme, NOT the standard Android intent action you'd
 * use on the phone:
 *
 * <ul>
 *   <li>{@code geo:} → {@link CarContext#ACTION_NAVIGATE}
 *       ({@code "androidx.car.app.action.NAVIGATE"}). Using
 *       {@code Intent.ACTION_VIEW} — which works for {@code geo:} on the
 *       phone — fails inside AA with the opaque error
 *       {@code "Remote startCarApp call failed"}.</li>
 *   <li>{@code tel:} → {@link Intent#ACTION_DIAL}, not {@code ACTION_CALL}.</li>
 *   <li>Anything else: best-effort {@code Intent.ACTION_VIEW}. AA almost
 *       always rejects, but the server operator asked for it, so we
 *       attempt the dispatch and log a warning if the host refuses.</li>
 * </ul>
 *
 * <p>{@link #classify(String)} is the pure-Java half, unit-tested without
 * Android types. {@link #parseAndDispatch} is the Android-side wrapper.
 */
public final class IntentParser {
    private static final String TAG = "CarMenuIntent";

    public enum Kind {
        INTERNAL_REFRESH,
        INTERNAL_BACK,
        INTERNAL_ACTION,   // carmenu:do?id=<id> — server-side action
        PASSTHROUGH,       // hand to startCarApp(); AA decides
        EMPTY              // null / no scheme — nothing to do
    }

    public static final class Classification {
        public final Kind kind;
        /**
         * INTERNAL_ACTION → the action id (the {@code id=...} query value).
         * PASSTHROUGH → the full URI to dispatch.
         * INTERNAL_REFRESH/BACK/EMPTY → null.
         */
        public final String detail;
        public Classification(Kind kind, String detail) {
            this.kind = kind; this.detail = detail;
        }
    }

    private IntentParser() {}

    public static Classification classify(String uri) {
        if (uri == null || uri.isEmpty()) {
            return new Classification(Kind.EMPTY, null);
        }
        int colon = uri.indexOf(':');
        if (colon <= 0) {
            return new Classification(Kind.EMPTY, null);
        }
        String scheme = uri.substring(0, colon);
        if ("carmenu".equals(scheme)) {
            String rest = uri.substring(colon + 1);
            if ("refresh".equals(rest)) return new Classification(Kind.INTERNAL_REFRESH, null);
            if ("back".equals(rest))    return new Classification(Kind.INTERNAL_BACK, null);
            // carmenu:do?id=<action-id>  — server-side action dispatch.
            // The client re-POSTs to /aa-screen with trigger="action" and
            // action_id=<id>, and the server does whatever it wants
            // server-side (send a message, toggle a switch, ...) and returns
            // a normal template (often a "done" confirmation).
            if (rest.startsWith("do?")) {
                String query = rest.substring(3);
                String id = queryParam(query, "id");
                if (id != null && !id.isEmpty()) {
                    return new Classification(Kind.INTERNAL_ACTION, id);
                }
            }
            // Unknown carmenu: action — treat as no-op rather than passing
            // through; the scheme is reserved for us.
            return new Classification(Kind.EMPTY, null);
        }
        return new Classification(Kind.PASSTHROUGH, uri);
    }

    public interface InternalHandler {
        void onRefresh();
        void onBack();
        /** {@code carmenu:do?id=<id>} — server-side action with this id. */
        void onAction(String actionId);
    }

    /**
     * Pull a value from a {@code key1=v1&key2=v2} query string. Returns null
     * if the key isn't present. URL-decodes the value (replaces '+' with space
     * and unescapes %XX). Pure Java for unit-testability.
     */
    static String queryParam(String query, String key) {
        if (query == null) return null;
        String prefix = key + "=";
        for (String pair : query.split("&")) {
            if (pair.startsWith(prefix)) {
                return urlDecode(pair.substring(prefix.length()));
            }
        }
        return null;
    }

    private static String urlDecode(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+') {
                out.append(' ');
            } else if (c == '%' && i + 2 < s.length()) {
                try {
                    int hi = Character.digit(s.charAt(i + 1), 16);
                    int lo = Character.digit(s.charAt(i + 2), 16);
                    if (hi < 0 || lo < 0) { out.append(c); continue; }
                    out.append((char) ((hi << 4) | lo));
                    i += 2;
                } catch (Throwable t) {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static void parseAndDispatch(CarContext ctx, String uri,
                                        InternalHandler internal) {
        Classification c = classify(uri);
        switch (c.kind) {
            case INTERNAL_REFRESH:
                if (internal != null) internal.onRefresh();
                break;
            case INTERNAL_BACK:
                if (internal != null) internal.onBack();
                break;
            case INTERNAL_ACTION:
                if (internal != null) internal.onAction(c.detail);
                break;
            case PASSTHROUGH:
                try {
                    Uri data = Uri.parse(c.detail);
                    String scheme = data.getScheme();
                    // AA's startCarApp() only accepts specific (action, scheme)
                    // pairs.  Get the action wrong and the host returns
                    // "Remote startCarApp call failed" — even for valid URIs.
                    //
                    //   geo:  → CarContext.ACTION_NAVIGATE (NOT Intent.ACTION_VIEW;
                    //          AA defines its own constant for nav handoff)
                    //   tel:  → Intent.ACTION_DIAL
                    //   else  → ACTION_VIEW best-effort; AA will probably reject
                    //          but the server operator asked for it, so try.
                    String action;
                    if ("geo".equals(scheme)) {
                        action = CarContext.ACTION_NAVIGATE;
                    } else if ("tel".equals(scheme)) {
                        action = Intent.ACTION_DIAL;
                    } else {
                        action = Intent.ACTION_VIEW;
                    }
                    Intent intent = new Intent(action, data);
                    ctx.startCarApp(intent);
                } catch (Throwable t) {
                    Log.w(TAG, "AA rejected intent: " + uri + " — " + t.getMessage());
                }
                break;
            case EMPTY:
            default:
                /* no-op */
        }
    }
}
