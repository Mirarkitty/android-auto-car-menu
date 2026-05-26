package org.mirar.carmenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests for the simplified IntentParser. Policy: intercept carmenu:* for
 * internal actions, pass everything else through to AA's startCarApp. AA is
 * the gatekeeper for which intent schemes actually launch.
 */
public class IntentParserTest {

    @Test public void carmenuRefresh_internal() {
        IntentParser.Classification c = IntentParser.classify("carmenu:refresh");
        assertEquals(IntentParser.Kind.INTERNAL_REFRESH, c.kind);
    }

    @Test public void carmenuBack_internal() {
        assertEquals(IntentParser.Kind.INTERNAL_BACK,
                IntentParser.classify("carmenu:back").kind);
    }

    @Test public void carmenuUnknown_dropped() {
        // Reserved scheme; unknown actions become no-ops, not pass-through.
        assertEquals(IntentParser.Kind.EMPTY,
                IntentParser.classify("carmenu:future-action").kind);
    }

    @Test public void carmenuDoActionId_extracted() {
        IntentParser.Classification c =
                IntentParser.classify("carmenu:do?id=garage-toggle");
        assertEquals(IntentParser.Kind.INTERNAL_ACTION, c.kind);
        assertEquals("garage-toggle", c.detail);
    }

    @Test public void carmenuDoUrlDecodes() {
        // %20 → space, '+' → space
        IntentParser.Classification c =
                IntentParser.classify("carmenu:do?id=open+garage%20door");
        assertEquals(IntentParser.Kind.INTERNAL_ACTION, c.kind);
        assertEquals("open garage door", c.detail);
    }

    @Test public void carmenuDoMissingId_dropped() {
        // carmenu:do without id= → no action to fire → no-op.
        assertEquals(IntentParser.Kind.EMPTY,
                IntentParser.classify("carmenu:do?foo=bar").kind);
        assertEquals(IntentParser.Kind.EMPTY,
                IntentParser.classify("carmenu:do?id=").kind);
    }

    @Test public void carmenuDoIgnoresOtherParams() {
        // Multi-param query: client just pulls id; other keys are
        // server-defined and not forwarded.
        IntentParser.Classification c = IntentParser.classify(
                "carmenu:do?id=garage-toggle&debug=1");
        assertEquals(IntentParser.Kind.INTERNAL_ACTION, c.kind);
        assertEquals("garage-toggle", c.detail);
    }

    @Test public void geo_passthrough() {
        IntentParser.Classification c = IntentParser.classify("geo:52.52,13.40?q=52.52,13.40");
        assertEquals(IntentParser.Kind.PASSTHROUGH, c.kind);
        assertEquals("geo:52.52,13.40?q=52.52,13.40", c.detail);
    }

    @Test public void tel_passthrough() {
        // Server operator's choice — AA's startCarApp does accept tel:.
        IntentParser.Classification c = IntentParser.classify("tel:+15550100123");
        assertEquals(IntentParser.Kind.PASSTHROUGH, c.kind);
    }

    @Test public void arbitraryScheme_passthrough() {
        // We don't second-guess AA. It'll reject if it doesn't like the scheme.
        assertEquals(IntentParser.Kind.PASSTHROUGH,
                IntentParser.classify("spotify:track:abc").kind);
        assertEquals(IntentParser.Kind.PASSTHROUGH,
                IntentParser.classify("http://example/x").kind);
    }

    @Test public void emptyOrSchemeless_isEmpty() {
        assertEquals(IntentParser.Kind.EMPTY, IntentParser.classify(null).kind);
        assertEquals(IntentParser.Kind.EMPTY, IntentParser.classify("").kind);
        assertEquals(IntentParser.Kind.EMPTY, IntentParser.classify("no-colon-here").kind);
    }

    @Test public void emptyClassification_hasNoDetail() {
        assertNull(IntentParser.classify("").detail);
        assertNull(IntentParser.classify(null).detail);
    }
}
