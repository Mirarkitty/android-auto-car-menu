package com.mirar.carmenu;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * LRU cache of remote icon bitmaps. Same-origin guard is enforced by
 * {@link JsonResponseParser} before icons even reach this cache, so URLs
 * here are already trusted.
 *
 * <p>Process-scoped singleton. Two states per URL:
 * <ul>
 *   <li>Cache miss — return {@code null} synchronously, schedule a fetch on
 *       the background executor, invoke the {@link Listener} when the
 *       bitmap lands.</li>
 *   <li>Cache hit — return the bitmap synchronously.</li>
 * </ul>
 *
 * <p>Bitmaps are decoded at native size and downscaled if larger than
 * {@link #MAX_DIM}px on either side — AA icons render at ~96dp so anything
 * larger wastes memory.
 */
public final class IconCache {

    private static final String TAG = "CarMenuIcons";
    private static final int CAPACITY = 32;
    private static final int CONNECT_MS = 5_000;
    private static final int READ_MS = 10_000;
    private static final int MAX_BYTES = 512 * 1024;
    private static final int MAX_DIM = 256;

    public interface Listener {
        /** Called on the main looper when a previously missing icon is ready. */
        void onIconReady(String url, Bitmap bitmap);
    }

    private static volatile IconCache INSTANCE;
    public static IconCache get() {
        IconCache c = INSTANCE;
        if (c == null) {
            synchronized (IconCache.class) {
                if (INSTANCE == null) INSTANCE = new IconCache();
                c = INSTANCE;
            }
        }
        return c;
    }

    private final LruCache<String, Bitmap> cache = new LruCache<>(CAPACITY);
    private final Set<String> inflight = new HashSet<>();
    private final Set<String> failed = new HashSet<>();
    private final Executor exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private IconCache() {}

    /**
     * @return cached bitmap (cache hit) or {@code null} (miss — fetch
     *         scheduled, listener will fire later). If a previous fetch
     *         permanently failed, returns null and does not retry until
     *         {@link #clearFailures()} is called.
     */
    public Bitmap get(String url, Listener listener) {
        if (url == null || url.isEmpty()) return null;
        Bitmap b = cache.get(url);
        if (b != null) return b;
        if (failed.contains(url)) return null;
        if (inflight.add(url)) {
            exec.execute(() -> fetch(url, listener));
        }
        return null;
    }

    /** Drop the "permanently failed" set so subsequent get() will re-fetch. */
    public void clearFailures() { failed.clear(); }

    private void fetch(String url, Listener listener) {
        Bitmap bm = null;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(CONNECT_MS);
            c.setReadTimeout(READ_MS);
            c.setRequestProperty("User-Agent",
                    "CarMenu/" + BuildConfig.VERSION_NAME + " (icon)");
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) {
                Log.w(TAG, "icon " + status + " " + url);
            } else {
                try (InputStream is = c.getInputStream()) {
                    byte[] bytes = readCapped(is, MAX_BYTES);
                    if (bytes == null) {
                        Log.w(TAG, "icon too large: " + url);
                    } else {
                        bm = decodeDownscaled(bytes);
                        if (bm == null) Log.w(TAG, "icon decode failed: " + url);
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "icon fetch failed: " + url + " — " + e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
        final Bitmap out = bm;
        main.post(() -> {
            inflight.remove(url);
            if (out == null) {
                failed.add(url);
                return;
            }
            cache.put(url, out);
            if (listener != null) listener.onIconReady(url, out);
        });
    }

    private static byte[] readCapped(InputStream is, int cap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0, n;
        while ((n = is.read(buf)) > 0) {
            total += n;
            if (total > cap) return null;
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private static Bitmap decodeDownscaled(byte[] bytes) {
        BitmapFactory.Options probe = new BitmapFactory.Options();
        probe.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, probe);
        int max = Math.max(probe.outWidth, probe.outHeight);
        int sample = 1;
        while (max / (sample * 2) >= MAX_DIM) sample *= 2;
        BitmapFactory.Options out = new BitmapFactory.Options();
        out.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, out);
    }
}
