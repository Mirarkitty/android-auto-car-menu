package com.mirar.carmenu;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.Screen;
import androidx.car.app.validation.HostValidator;

/**
 * Android Auto entry point. The host invokes this when the user taps the
 * CarMenu row in the AA app drawer.
 *
 * <p>Surface: just hands back a {@link MainScreen}. Everything else lives
 * in MainScreen + its helpers (TemplateBuilder, HttpFetcher).
 *
 * <p>Pattern reference: DeviceMonitor's
 * {@code DeviceMonitorCarAppService} does the same thing — keep this one
 * minimal.
 */
public class MyCarAppService extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        // ALLOW_ALL_HOSTS_VALIDATOR is fine for personal/internal-testing.
        // For production-Play release, swap to the strict validator that only
        // allows Google's published AA hosts. See androidx.car.app docs.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull android.content.Intent intent) {
                return new MainScreen(getCarContext());
            }
        };
    }
}
