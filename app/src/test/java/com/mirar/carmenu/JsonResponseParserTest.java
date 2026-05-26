package com.mirar.carmenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Test;

public class JsonResponseParserTest {

    private static final String BASE = "https://server.example.com/aa-screen";

    @Test public void list_typical() throws JSONException {
        String json =
                "{\"template\":\"list\",\"title\":\"Where to?\",\"rows\":[" +
                "  {\"title\":\"Home\",\"subtitle\":\"Old town\",\"icon\":\"home\"," +
                "   \"tap\":{\"intent\":\"geo:52.52,13.40?q=52.52,13.40\"}}," +
                "  {\"title\":\"Work\",\"icon\":\"work\"," +
                "   \"tap\":{\"intent\":\"geo:52.51,13.38?q=52.51,13.38\"}}" +
                "],\"refresh_seconds\":60}";
        ServerResponse r = JsonResponseParser.parse(json);
        assertEquals(ServerResponse.Kind.LIST, r.template);
        assertEquals("Where to?", r.title);
        assertEquals(2, r.rows.size());
        assertEquals("Home", r.rows.get(0).title);
        assertEquals("Old town", r.rows.get(0).subtitle);
        assertEquals("home", r.rows.get(0).icon);
        assertNull(r.rows.get(0).iconUrl);
        assertNull(r.rows.get(0).tint);
        assertEquals("geo:52.52,13.40?q=52.52,13.40", r.rows.get(0).tapIntent);
        assertEquals(60, r.refreshSeconds);
    }

    @Test public void list_remoteIconRelative_resolves() throws JSONException {
        String json = "{\"template\":\"list\",\"title\":\"x\",\"rows\":[" +
                "{\"title\":\"Home\",\"icon_url\":\"/icons/home.png\"," +
                " \"tint\":\"primary\"," +
                " \"tap\":{\"intent\":\"geo:0,0\"}}]}";
        ServerResponse r = JsonResponseParser.parse(json, BASE);
        ServerResponse.Row row = r.rows.get(0);
        assertEquals("https://server.example.com/icons/home.png", row.iconUrl);
        assertNotNull(row.tint);
        assertEquals(TintSpec.Named.PRIMARY, row.tint.named);
    }

    @Test public void list_remoteIconCrossOrigin_droppedToNull() throws JSONException {
        String json = "{\"template\":\"list\",\"title\":\"x\",\"rows\":[" +
                "{\"title\":\"Home\",\"icon\":\"home\"," +
                " \"icon_url\":\"https://evil.example/x.png\"}]}";
        ServerResponse r = JsonResponseParser.parse(json, BASE);
        // iconUrl rejected, but the slug-fallback remains usable.
        assertNull(r.rows.get(0).iconUrl);
        assertEquals("home", r.rows.get(0).icon);
    }

    @Test public void list_hexTint() throws JSONException {
        String json = "{\"template\":\"list\",\"title\":\"x\",\"rows\":[" +
                "{\"title\":\"X\",\"tint\":\"#FF8800\"}]}";
        ServerResponse r = JsonResponseParser.parse(json);
        assertTrue(r.rows.get(0).tint.isCustom);
        assertEquals(0xFFFF8800, r.rows.get(0).tint.argb);
    }

    @Test public void list_rowWithoutTitle_dropped() throws JSONException {
        ServerResponse r = JsonResponseParser.parse(
                "{\"template\":\"list\",\"title\":\"x\"," +
                "\"rows\":[{\"subtitle\":\"orphan\"},{\"title\":\"ok\"}]}");
        assertEquals(1, r.rows.size());
        assertEquals("ok", r.rows.get(0).title);
    }

    @Test public void pane_typical() throws JSONException {
        String json = "{\"template\":\"pane\",\"title\":\"EV9\"," +
                "\"rows\":[{\"title\":\"Battery 72%\"}]," +
                "\"image_url\":\"/graphs/ev9.png\"," +
                "\"actions\":[{\"title\":\"Refresh\",\"icon\":\"refresh\"," +
                "\"tap\":{\"intent\":\"carmenu:refresh\"}}]," +
                "\"refresh_seconds\":30}";
        ServerResponse r = JsonResponseParser.parse(json, BASE);
        assertEquals(ServerResponse.Kind.PANE, r.template);
        assertEquals("https://server.example.com/graphs/ev9.png", r.imageUrl);
        assertEquals(1, r.actions.size());
        assertEquals("Refresh", r.actions.get(0).title);
        assertEquals("carmenu:refresh", r.actions.get(0).tapIntent);
        assertEquals(30, r.refreshSeconds);
    }

    @Test public void message_typical() throws JSONException {
        ServerResponse r = JsonResponseParser.parse(
                "{\"template\":\"message\",\"title\":\"Waiting for GPS…\"," +
                "\"subtitle\":\"may take 10–30s\",\"icon\":\"spinner\"," +
                "\"tint\":\"yellow\"}");
        assertEquals(ServerResponse.Kind.MESSAGE, r.template);
        assertEquals("Waiting for GPS…", r.title);
        assertEquals("spinner", r.icon);
        assertEquals(TintSpec.Named.YELLOW, r.iconTint.named);
    }

    @Test public void unknownTemplate_fallbackMessage() throws JSONException {
        ServerResponse r = JsonResponseParser.parse(
                "{\"template\":\"holographic-3d\",\"title\":\"future\"}");
        assertEquals(ServerResponse.Kind.MESSAGE, r.template);
        assertNotNull(r.subtitle);
        assertTrue(r.subtitle.toLowerCase().contains("update"));
    }

    @Test(expected = JSONException.class)
    public void unparseableJson_throws() throws JSONException {
        JsonResponseParser.parse("not even {json");
    }

    @Test public void refreshClamp_belowFiveBumpsToFive() {
        assertEquals(0, JsonResponseParser.clampRefresh(0));
        assertEquals(0, JsonResponseParser.clampRefresh(-1));
        assertEquals(5, JsonResponseParser.clampRefresh(1));
        assertEquals(60, JsonResponseParser.clampRefresh(60));
        assertEquals(3600, JsonResponseParser.clampRefresh(3600));
        assertEquals(3600, JsonResponseParser.clampRefresh(99999));
    }

    @Test public void errorMessage_includesStatusAndTruncatedBody() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 20; i++) huge.append("very long error body ");
        ServerResponse r = JsonResponseParser.errorMessage(503, huge.toString());
        assertEquals(ServerResponse.Kind.MESSAGE, r.template);
        assertEquals("Server error", r.title);
        assertTrue(r.subtitle.startsWith("503"));
        assertTrue(r.subtitle.length() <= 86);
    }

    @Test public void noServerBase_dropsRemoteIcons() throws JSONException {
        String json = "{\"template\":\"list\",\"title\":\"x\",\"rows\":[" +
                "{\"title\":\"X\",\"icon_url\":\"/icons/x.png\",\"icon\":\"home\"}]}";
        // Parse without serverBase — remote icon resolves to null, slug remains.
        ServerResponse r = JsonResponseParser.parse(json);
        assertNull(r.rows.get(0).iconUrl);
        assertEquals("home", r.rows.get(0).icon);
    }
}
