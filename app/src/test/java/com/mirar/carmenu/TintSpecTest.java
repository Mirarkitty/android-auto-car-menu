package com.mirar.carmenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TintSpecTest {

    @Test public void namedSlugs() {
        assertEquals(TintSpec.Named.PRIMARY, TintSpec.parse("primary").named);
        assertEquals(TintSpec.Named.RED,     TintSpec.parse("red").named);
        assertEquals(TintSpec.Named.GREEN,   TintSpec.parse("GREEN").named);
        assertEquals(TintSpec.Named.YELLOW,  TintSpec.parse(" Yellow ").named);
        assertEquals(TintSpec.Named.DEFAULT, TintSpec.parse("default").named);
        assertFalse(TintSpec.parse("primary").isCustom);
    }

    @Test public void hex6_addsOpaqueAlpha() {
        TintSpec t = TintSpec.parse("#FF8800");
        assertTrue(t.isCustom);
        assertEquals(0xFFFF8800, t.argb);
    }

    @Test public void hex8_passesAlphaThrough() {
        TintSpec t = TintSpec.parse("#80FF8800");
        assertEquals(0x80FF8800, t.argb);
    }

    @Test public void invalid_returnsNull() {
        assertNull(TintSpec.parse(null));
        assertNull(TintSpec.parse(""));
        assertNull(TintSpec.parse("   "));
        assertNull(TintSpec.parse("not-a-color"));
        assertNull(TintSpec.parse("#GGG"));
        assertNull(TintSpec.parse("#12345"));     // wrong length
        assertNull(TintSpec.parse("#1234567"));   // wrong length
    }
}
