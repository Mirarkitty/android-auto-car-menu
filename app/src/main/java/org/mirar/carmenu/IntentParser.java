package org.mirar.carmenu;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.car.app.CarContext;

/**
 * Dispatch row-tap intents from the server.
 *
 * <p>Policy: pass-through. Anything the server returns is handed to
 * {@code carContext.startCarApp(intent)} as {@code ACTION_VIEW <uri>}. AA
 * itself is the gatekeeper — it accepts {@code geo:} (nav handoff), {@code
 * tel:} (dialer), and a small handful of others, and silently rejects the
 * rest. We don't second-guess AA's whitelist.
 *
 * <p>One exception: the {@code carmenu:} scheme is reserved for internal
 * actions (refresh, back) that never leave the app.
 *
 * <p>{@link #classify(String)} is the pure-Java half, unit-tested without
 * Android types. {@link #parseAndDispatch} is the Android-side wrapper.
 */
public final class IntentParser {
    private static final String TAG = "CarMenuIntent";

    public enum Kind {
        INTERNAL_REFRESH,
        INTERNAL_BACK,
        PASSTHROUGH,    // hand to startCarApp(); AA decides
        EMPTY           // null / no scheme — nothing to do
    }

    public static final class Classification {
        public final Kind kind;
        public final String detail;   // for INTERNAL_*: null. For PASSTHROUGH: the uri.
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
            // Unknown carmenu: action — treat as no-op rather than passing
            // through; the scheme is reserved for us.
            return new Classification(Kind.EMPTY, null);
        }
        return new Classification(Kind.PASSTHROUGH, uri);
    }

    public interface InternalHandler {
        void onRefresh();
        void onBack();
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
            case PASSTHROUGH:
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.detail));
                    ctx.startCarApp(intent);
                } catch (Throwable t) {
                    // AA rejects intents it doesn't recognize (most non-geo/tel).
                    // Log + swallow — the user just sees the row not respond.
                    Log.w(TAG, "AA rejected intent: " + uri + " — " + t.getMessage());
                }
                break;
            case EMPTY:
            default:
                /* no-op */
        }
    }
}
