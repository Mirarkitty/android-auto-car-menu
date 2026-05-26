package com.mirar.carmenu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pure-JVM tests for IconRegistry. Uses the package-private fallback-only
 * constructor + register() to avoid pulling in Android R values at test time.
 */
public class IconRegistryTest {

    private static final int FALLBACK = 999;

    @Test public void registeredSlug_resolves() {
        IconRegistry r = new IconRegistry(FALLBACK);
        r.register("home", 42);
        assertEquals(42, r.resolve("home"));
    }

    @Test public void unknownSlug_fallsBackToGeneric() {
        IconRegistry r = new IconRegistry(FALLBACK);
        r.register("home", 42);
        assertEquals(FALLBACK, r.resolve("not-a-slug"));
    }

    @Test public void nullSlug_fallsBackToGeneric() {
        IconRegistry r = new IconRegistry(FALLBACK);
        assertEquals(FALLBACK, r.resolve(null));
    }

    @Test public void emptyRegistry_alwaysFalsbackToGeneric() {
        IconRegistry r = new IconRegistry(FALLBACK);
        assertEquals(FALLBACK, r.resolve("anything"));
    }
}
