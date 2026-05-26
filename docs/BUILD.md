# Build & install

## One-time setup

```sh
git clone https://github.com/Mirarkitty/android-auto-car-menu.git
cd android-auto-car-menu

# Required: a JDK 17 and the Android SDK (build-tools 36 + platform 36).
# If you have Android Studio installed, both come with it.
# Otherwise install the command-line tools and run `sdkmanager` to fetch
# `platforms;android-36` and `build-tools;36.0.0`.

# Optional: generate an upload keystore (interactive — sets a password):
make keygen      # creates keystore.jks + keystore.properties (both gitignored)
```

The build expects `JAVA_HOME` and `ANDROID_HOME` to be set. Either
export them in your shell or pass them on the make line:

```sh
make debug ANDROID_HOME=/path/to/sdk JAVA_HOME=/path/to/jbr
```

## Build

```sh
make debug          # → app/build/outputs/apk/debug/app-debug.apk
make release        # → app/build/outputs/apk/release/app-release.apk
make bundle         # → app/build/outputs/bundle/release/app-release.aab
make version        # print versionCode + versionName
make test           # run the 46 JVM unit tests (no device/emulator)
```

`versionCode` is derived from `git rev-list --count HEAD`, so it stays
monotonic across the tree.

`make release` and `make bundle` produce signed artifacts only if
`keystore.properties` exists; without it, the outputs are unsigned and
not usable for Play upload. Run `make keygen` first.

## Install (sideload)

```sh
make install
# = adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Desktop Head Unit (DHU) — test Android Auto without a car

Android ships a desktop simulator for the AA host. Short version:

```sh
# 1. Make sure DHU is installed on your laptop:
#    Android Studio → SDK Manager → SDK Tools → Android Auto API Simulators
# 2. Plug the phone in via USB. Enable Developer Options + USB debugging.
# 3. Start the head-unit server on the laptop:
$ANDROID_HOME/extras/google/auto/desktop-head-unit
# 4. Forward port 5277 from phone to laptop:
adb forward tcp:5277 tcp:5277
# 5. On the phone: Android Auto app → ⋮ → Start head unit server
```

The DHU window opens; CarMenu shows in the AA app drawer.

Quirks to remember:
- `~/.android/headunit.ini` must have `Resolution=` in the `[general]`
  section (NOT `[video]`). `1920x1080` works; `1920x720` is silently
  rejected.
- AA on DHU doesn't show notifications popping; tap the bell icon to
  see them in the list.
- Bluetooth A2DP/HEADSET status from DHU is always "disconnected" —
  only the AA projection connection is real. Use
  `CarConnection.getType()` for in-app AA detection, not BT state.

## Publish to Play

See [PUBLISH.md](PUBLISH.md) and `assets/store/SUBMISSION_CHECKLIST.md`
for the full Play submission flow.

`make bundle` produces the `.aab`. Upload via Play Console → Testing →
Internal testing → Create new release.

## Project layout

```
android-auto-car-menu/
├── .gitignore
├── Makefile               ← build / install / keystore helpers
├── build.gradle.kts       ← project-level
├── settings.gradle.kts
├── gradle.properties
├── keystore.properties    ← gitignored
├── keystore.jks           ← gitignored
├── docs/                  ← protocol spec, build/install, Play notes
├── assets/store/          ← Play listing assets
├── server-example/        ← reference servers (Python/Flask)
└── app/
    ├── build.gradle.kts                       ← gitVersionCode magic
    └── src/main/
        ├── AndroidManifest.xml                ← only 2 perms!
        ├── assets/protocol.txt                ← bundled protocol spec
        ├── res/
        │   ├── xml/automotive_app_desc.xml    ← CAR.VALIDATOR-required
        │   ├── values/strings.xml
        │   ├── drawable/ic_*.xml              ← icon registry slugs
        │   └── mipmap-*/ic_launcher.*         ← app launcher icon
        └── java/com/mirar/carmenu/
            ├── MyCarAppService.java           ← AA entry point
            ├── MainScreen.java                ← single Screen
            ├── HttpFetcher.java               ← POST + JSON parse
            ├── TemplateBuilder.java           ← JSON → AA template
            ├── IconRegistry.java              ← slug → drawable id
            ├── IconCache.java                 ← remote-bitmap LRU
            ├── IntentParser.java              ← parse + dispatch
            ├── RefreshBus.java                ← settings → AA signal
            ├── Prefs.java                     ← server URL, device id
            ├── ProtocolActivity.java          ← bundled-spec viewer
            └── MainActivity.java              ← phone-side settings UI
```

## See also

- [PROTOCOL.txt](PROTOCOL.txt) — JSON contract
- [PUBLISH.md](PUBLISH.md) — Play submission
- [../server-example/](../server-example/) — reference server implementations
