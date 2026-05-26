package org.mirar.carmenu;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Same-origin guard + relative-URL resolver for remote icons.
 *
 * <p>Server-provided {@code icon_url} can be:
 * <ul>
 *   <li>Relative — {@code "/icons/home.png"} → resolved against the
 *       configured server URL's origin.</li>
 *   <li>Absolute — must have the same scheme + host + port as the configured
 *       server URL; otherwise rejected. This is the security boundary: a
 *       compromised server cannot make the phone fetch icons from a third
 *       party (which would leak the user's IP + a timing signal).</li>
 * </ul>
 *
 * <p>All pure Java; unit-testable without Android.
 */
public final class UrlOrigin {

    private UrlOrigin() {}

    /**
     * Resolve {@code candidate} against {@code serverBase}.
     *
     * @param serverBase  the user-configured server URL (any valid http(s) URL).
     * @param candidate   absolute or relative URL from the server.
     * @return absolute URL string if same-origin (or relative made absolute),
     *         else {@code null}.
     */
    public static String resolveSameOrigin(String serverBase, String candidate) {
        if (serverBase == null || serverBase.isEmpty()) return null;
        if (candidate == null || candidate.isEmpty())   return null;
        URL base;
        try {
            base = new URL(serverBase);
        } catch (MalformedURLException e) {
            return null;
        }
        // Reject any scheme-relative or weird input by requiring http(s).
        String baseScheme = base.getProtocol();
        if (!"http".equals(baseScheme) && !"https".equals(baseScheme)) return null;

        if (candidate.startsWith("/")) {
            // Path-absolute relative — same origin by construction.
            try {
                URL out = new URL(base, candidate);
                return out.toString();
            } catch (MalformedURLException e) { return null; }
        }
        if (candidate.contains("://")) {
            // Absolute URL — must match origin exactly.
            URL c;
            try {
                c = new URL(candidate);
            } catch (MalformedURLException e) { return null; }
            if (!baseScheme.equals(c.getProtocol())) return null;
            if (!hostEqualsCaseInsensitive(base.getHost(), c.getHost())) return null;
            if (effectivePort(base) != effectivePort(c)) return null;
            return c.toString();
        }
        // Schemeless relative (e.g. "icons/x.png") — resolve against base path.
        // Reject for simplicity: server should send "/icons/x.png" to avoid
        // depending on whether the base URL ends in a slash.
        return null;
    }

    private static boolean hostEqualsCaseInsensitive(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static int effectivePort(URL u) {
        int p = u.getPort();
        if (p != -1) return p;
        return u.getDefaultPort();
    }
}
