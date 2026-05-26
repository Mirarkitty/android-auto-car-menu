package com.mirar.carmenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UrlOriginTest {

    private static final String BASE = "https://server.example.com/aa-screen";

    @Test public void relativePathAbsolute_resolves() {
        assertEquals("https://server.example.com/icons/home.png",
                UrlOrigin.resolveSameOrigin(BASE, "/icons/home.png"));
    }

    @Test public void absoluteSameOrigin_passes() {
        assertEquals("https://server.example.com/icons/work.png",
                UrlOrigin.resolveSameOrigin(BASE,
                        "https://server.example.com/icons/work.png"));
    }

    @Test public void absoluteSameOrigin_caseInsensitiveHost() {
        // Hosts are case-insensitive per RFC 3986.
        assertEquals("https://Server.Example.com/x.png",
                UrlOrigin.resolveSameOrigin(BASE,
                        "https://Server.Example.com/x.png"));
    }

    @Test public void differentHost_rejected() {
        assertNull(UrlOrigin.resolveSameOrigin(BASE,
                "https://evil.example/icons/x.png"));
    }

    @Test public void differentPort_rejected() {
        assertNull(UrlOrigin.resolveSameOrigin(BASE,
                "https://server.example.com:8500/x.png"));
    }

    @Test public void differentScheme_rejected() {
        // http vs https is a real downgrade; reject.
        assertNull(UrlOrigin.resolveSameOrigin(BASE,
                "http://server.example.com/x.png"));
    }

    @Test public void schemelessRelative_rejected() {
        // We require leading / for unambiguity.
        assertNull(UrlOrigin.resolveSameOrigin(BASE, "icons/x.png"));
    }

    @Test public void defaultPortsAreEquivalent() {
        // https://host/foo == https://host:443/foo
        String resolved = UrlOrigin.resolveSameOrigin(
                "https://example.com/api", "https://example.com:443/icon.png");
        assertEquals("https://example.com:443/icon.png", resolved);
    }

    @Test public void emptyOrMalformed_rejected() {
        assertNull(UrlOrigin.resolveSameOrigin(null, "/x.png"));
        assertNull(UrlOrigin.resolveSameOrigin("", "/x.png"));
        assertNull(UrlOrigin.resolveSameOrigin("not a url", "/x.png"));
        assertNull(UrlOrigin.resolveSameOrigin(BASE, null));
        assertNull(UrlOrigin.resolveSameOrigin(BASE, ""));
        assertNull(UrlOrigin.resolveSameOrigin(BASE, "https://"));
    }

    @Test public void nonHttpBase_rejected() {
        // We won't resolve relative URLs against file:// or anything weird.
        assertNull(UrlOrigin.resolveSameOrigin("file:///tmp/x", "/y.png"));
    }
}
