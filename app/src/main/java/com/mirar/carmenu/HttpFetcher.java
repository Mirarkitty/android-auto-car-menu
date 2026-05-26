package com.mirar.carmenu;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * POST {@code /aa-screen}, parse the response, deliver on the main looper.
 *
 * <p>Built on {@code HttpURLConnection} — no OkHttp dep needed.
 *
 * <p>Caller supplies the {@link Location} (or null), the {@link
 * RequestBuilder.Permission} state, and the trigger string. The fetcher
 * is otherwise oblivious to permission machinery.
 */
public class HttpFetcher {

    private static final String TAG = "CarMenuFetch";
    private static final int CONNECT_MS = 5_000;
    private static final int READ_MS = 10_000;

    public interface Callback {
        void onResponse(ServerResponse response);
    }

    private static final Executor EXEC = Executors.newSingleThreadExecutor();

    private final Context appCtx;
    private final Handler main = new Handler(Looper.getMainLooper());

    public HttpFetcher(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
    }

    public void fetchScreen(Location location, RequestBuilder.Permission perm,
                            String trigger, Callback cb) {
        final String url = Prefs.getServerUrl(appCtx);
        final String deviceId = Prefs.getDeviceId(appCtx);
        final Double lat = location != null ? location.getLatitude() : null;
        final Double lon = location != null ? location.getLongitude() : null;
        final Long locTime = location != null ? location.getTime() : null;
        final long now = System.currentTimeMillis();
        final int appVer = BuildConfig.VERSION_CODE;

        EXEC.execute(() -> {
            ServerResponse r;
            if (url == null || url.isEmpty()) {
                r = ServerResponse.message("Configure server",
                        "Open CarMenu on the phone and enter your server URL.");
            } else {
                String body = RequestBuilder.build(deviceId, lat, lon, locTime, now,
                        perm, trigger, appVer);
                r = doPost(url, body);
            }
            final ServerResponse out = r;
            main.post(() -> cb.onResponse(out));
        });
    }

    private static ServerResponse doPost(String url, String body) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(CONNECT_MS);
            c.setReadTimeout(READ_MS);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("User-Agent",
                    "CarMenu/" + BuildConfig.VERSION_NAME + " (vc=" + BuildConfig.VERSION_CODE + ")");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) {
                os.write(bytes);
            }
            int status = c.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? c.getInputStream() : c.getErrorStream();
            String resp = is == null ? "" : readAll(is);
            if (status < 200 || status >= 300) {
                return JsonResponseParser.errorMessage(status, resp);
            }
            try {
                return JsonResponseParser.parse(resp, url);
            } catch (JSONException je) {
                Log.w(TAG, "json parse failed: " + je.getMessage());
                return JsonResponseParser.errorMessage(status, je.getMessage());
            }
        } catch (IOException e) {
            Log.w(TAG, "POST failed: " + e.getMessage());
            return ServerResponse.message("Network error", e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
}
