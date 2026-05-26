package org.mirar.carmenu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * The single Screen.
 *
 * <p>Location strategy (three cases):
 * <ol>
 *   <li><b>Permission denied</b> — POST once with
 *       {@code location_permission:"denied"} and no lat/lon. Server decides
 *       what to render. No location work after that.</li>
 *   <li><b>Cold start (granted, no fresh fix yet)</b> — POST immediately
 *       with {@code last-known} (may be stale or null), then request live
 *       location updates from the fused provider. Re-POST as soon as a
 *       fresh fix lands (trigger=location), and again on any subsequent
 *       fix &gt;{@link #DELTA_M} from the last-sent location.</li>
 *   <li><b>Easy case (granted, fresh last-known)</b> — POST with the
 *       last-known fix; live updates still start but the throttle usually
 *       blocks the immediate duplicate.</li>
 * </ol>
 *
 * <p>Live updates run only while the screen is in STARTED state, and stop
 * on onStop (and on the standard refresh polling between fetches).
 */
public class MainScreen extends Screen implements DefaultLifecycleObserver {

    private static final String TAG = "CarMenuScreen";

    /** Move at least this far (meters) before considering a re-POST. */
    private static final float DELTA_M = 100f;
    /** Re-POSTs from location updates at most once per this interval. */
    private static final long MIN_REPOST_INTERVAL_MS = 8_000L;
    /** Fused provider update interval. */
    private static final long LOC_INTERVAL_MS = 5_000L;

    private final HttpFetcher fetcher;
    private final IconRegistry icons;
    private final TemplateBuilder builder;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final FusedLocationProviderClient fused;

    private ServerResponse current;
    private boolean fetching;
    private boolean alive = true;
    private final Runnable refreshTask = this::refresh;
    private LocationCallback locCallback;
    private Location lastSentLocation;
    private long lastSentAtMs;
    private final RefreshBus.Listener busListener = this::onBusSignal;

    public MainScreen(@NonNull CarContext carContext) {
        super(carContext);
        this.fetcher = new HttpFetcher(carContext);
        this.icons = new IconRegistry();
        this.builder = new TemplateBuilder(carContext, icons,
                new IntentParser.InternalHandler() {
                    @Override public void onRefresh() { refresh(); }
                    @Override public void onBack()    { getScreenManager().pop(); }
                },
                () -> { if (alive) invalidate(); });
        this.fused = LocationServices.getFusedLocationProviderClient(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        alive = true;
        RefreshBus.get().subscribe(busListener);
        if (hasLocationPermission()) {
            fetch(lastKnownLocation(), RequestBuilder.Permission.GRANTED, "initial");
            startLocationUpdates();
        } else {
            fetch(null, RequestBuilder.Permission.DENIED, "initial");
            // No updates — we have nothing to listen for.
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        alive = false;
        RefreshBus.get().unsubscribe(busListener);
        main.removeCallbacks(refreshTask);
        stopLocationUpdates();
    }

    private void onBusSignal() {
        if (!alive) return;
        // Permission may have changed since we last started; recompute and
        // (re)bind location updates accordingly.
        boolean granted = hasLocationPermission();
        if (granted && locCallback == null) startLocationUpdates();
        if (!granted && locCallback != null) stopLocationUpdates();
        // Bypass the fetch-in-flight guard: a settings change should win
        // over an old in-flight POST that may be against the wrong server.
        fetching = false;
        Location loc = granted ? lastKnownLocation() : null;
        RequestBuilder.Permission perm = granted
                ? RequestBuilder.Permission.GRANTED
                : RequestBuilder.Permission.DENIED;
        fetch(loc, perm, "refresh");
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        if (current == null) {
            // Pre-first-response state. The server has nothing to tell us
            // yet because we haven't asked. Keep this as minimal as possible
            // — empty title, only the body text marks it as a transient
            // state. The app icon in the header identifies the app.
            return new MessageTemplate.Builder("Loading…")
                    .setTitle(" ")
                    .setHeaderAction(Action.APP_ICON)
                    .build();
        }
        return builder.build(current);
    }

    private void refresh() {
        RequestBuilder.Permission p = hasLocationPermission()
                ? RequestBuilder.Permission.GRANTED
                : RequestBuilder.Permission.DENIED;
        Location loc = p == RequestBuilder.Permission.GRANTED ? lastKnownLocation() : null;
        fetch(loc, p, "refresh");
    }

    private void fetch(Location loc, RequestBuilder.Permission perm, String trigger) {
        if (!alive || fetching) return;
        fetching = true;
        if (loc != null) {
            lastSentLocation = loc;
            lastSentAtMs = System.currentTimeMillis();
        }
        fetcher.fetchScreen(loc, perm, trigger, response -> {
            fetching = false;
            if (!alive) return;
            current = response;
            invalidate();
            scheduleNextRefresh(response);
        });
    }

    private void scheduleNextRefresh(ServerResponse r) {
        main.removeCallbacks(refreshTask);
        if (r == null || r.refreshSeconds <= 0) return;
        main.postDelayed(refreshTask, r.refreshSeconds * 1000L);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(getCarContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private Location lastKnownLocation() {
        CarContext ctx = getCarContext();
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(CarContext.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            }
            return best;
        } catch (Throwable t) {
            Log.w(TAG, "lastKnownLocation failed", t);
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locCallback != null) return;
        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location l = result.getLastLocation();
                if (l != null) onLocationUpdate(l);
            }
        };
        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOC_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOC_INTERVAL_MS)
                .build();
        try {
            fused.requestLocationUpdates(req, locCallback, Looper.getMainLooper());
        } catch (SecurityException se) {
            // Permission was revoked between check and call — give up quietly.
            Log.w(TAG, "fused requestLocationUpdates SecurityException", se);
            locCallback = null;
        }
    }

    private void stopLocationUpdates() {
        if (locCallback == null) return;
        try { fused.removeLocationUpdates(locCallback); } catch (Throwable ignore) {}
        locCallback = null;
    }

    private void onLocationUpdate(Location l) {
        if (!alive || fetching) return;
        long now = System.currentTimeMillis();
        if (now - lastSentAtMs < MIN_REPOST_INTERVAL_MS) return;
        // Re-POST if we never sent a real location yet, or moved enough.
        if (lastSentLocation == null || lastSentLocation.distanceTo(l) >= DELTA_M) {
            fetch(l, RequestBuilder.Permission.GRANTED, "location");
        }
    }
}
