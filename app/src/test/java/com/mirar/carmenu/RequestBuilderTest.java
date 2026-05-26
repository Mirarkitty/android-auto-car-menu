package com.mirar.carmenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class RequestBuilderTest {

    @Test public void withFreshLocation_includesLatLonAndFreshTrue() throws JSONException {
        long now = 10_000_000L;
        String body = RequestBuilder.build("carmenu.abcd",
                52.52, 13.40, now - 1_000L, now,
                RequestBuilder.Permission.GRANTED, "initial", 42);
        JSONObject o = new JSONObject(body);
        assertEquals("carmenu.abcd", o.getString("device_id"));
        assertEquals(52.52, o.getDouble("lat"), 1e-9);
        assertEquals(13.40, o.getDouble("lon"), 1e-9);
        assertEquals(1_000L, o.getLong("location_age_ms"));
        assertTrue(o.getBoolean("location_fresh"));
        assertEquals("granted", o.getString("location_permission"));
        assertEquals(now, o.getLong("ts"));
        assertEquals("initial", o.getString("trigger"));
        assertEquals(42, o.getInt("app_version"));
    }

    @Test public void staleLocation_freshFalse() throws JSONException {
        long now = 10_000_000L;
        long old = now - RequestBuilder.STALE_AFTER_MS - 1L;
        JSONObject o = new JSONObject(RequestBuilder.build("d",
                1.0, 2.0, old, now,
                RequestBuilder.Permission.GRANTED, "initial", 1));
        assertFalse(o.getBoolean("location_fresh"));
        assertTrue(o.getLong("location_age_ms") >= RequestBuilder.STALE_AFTER_MS);
    }

    @Test public void permissionDenied_omitsLatLonNoFreshness() throws JSONException {
        JSONObject o = new JSONObject(RequestBuilder.build("d",
                null, null, null, 12345L,
                RequestBuilder.Permission.DENIED, "initial", 1));
        assertFalse(o.has("lat"));
        assertFalse(o.has("lon"));
        assertFalse(o.has("location_fresh"));
        assertFalse(o.has("location_age_ms"));
        assertEquals("denied", o.getString("location_permission"));
    }

    @Test public void permissionGrantedNoFixYet_omitsLatLonButStillGranted() throws JSONException {
        // Cold start: user granted perm but no fix has arrived yet.
        JSONObject o = new JSONObject(RequestBuilder.build("d",
                null, null, null, 12345L,
                RequestBuilder.Permission.GRANTED, "initial", 1));
        assertFalse(o.has("lat"));
        assertEquals("granted", o.getString("location_permission"));
    }

    @Test public void nullTrigger_defaultsToInitial() throws JSONException {
        JSONObject o = new JSONObject(RequestBuilder.build("d", null, null, null,
                0L, RequestBuilder.Permission.UNKNOWN, null, 1));
        assertEquals("initial", o.getString("trigger"));
        assertEquals("unknown", o.getString("location_permission"));
    }

    @Test public void locationWithoutTime_omitsAgeAndFreshness() throws JSONException {
        // Location present but provider gave no timestamp — still send coords,
        // skip freshness fields rather than fabricating a value.
        JSONObject o = new JSONObject(RequestBuilder.build("d",
                1.0, 2.0, null, 12345L,
                RequestBuilder.Permission.GRANTED, "initial", 1));
        assertTrue(o.has("lat"));
        assertFalse(o.has("location_fresh"));
        assertFalse(o.has("location_age_ms"));
    }

    @Test public void deviceIdNeverNull() throws JSONException {
        JSONObject o = new JSONObject(RequestBuilder.build(null, null, null, null,
                0L, RequestBuilder.Permission.UNKNOWN, "initial", 1));
        assertTrue(o.has("device_id"));
        assertEquals("", o.getString("device_id"));
    }

    @Test public void triggerLocation_passesThrough() throws JSONException {
        JSONObject o = new JSONObject(RequestBuilder.build("d",
                1.0, 2.0, 1L, 2L,
                RequestBuilder.Permission.GRANTED, "location", 1));
        assertEquals("location", o.getString("trigger"));
    }
}
