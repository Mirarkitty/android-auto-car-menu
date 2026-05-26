# CarMenu — Google Play publish checklist

> **For the concrete, ordered submit-now do-list, see
> `~/AndroidStudioProjects/CarMenu/assets/store/SUBMISSION_CHECKLIST.md`.**
> That file is kept in sync with the actual generated assets (icon,
> feature graphic, descriptions, privacy policy, AA mock screenshot).
> This file is the background reference; the checklist is the
> follow-along.

One-time setup to get the app onto Play (internal testing track at minimum,
which gives the "Play-installed" trust stamp Android Auto wants).

## Cost

| Item | Cost |
| --- | --- |
| Google Play Developer account (one-time) | **$25 USD** |
| Annual fees | none |
| Free-app revenue split | n/a (no IAP) |

## One-time account setup

1. Go to https://play.google.com/console — sign in with the Google account
   that will own the listing (personal email is fine for an individual dev
   account).
2. Pay $25, fill in identity verification (Google ID check), tax forms,
   developer profile.
3. Wait for verification (usually < 24 h).

## Pre-build checklist

| Item | Where in repo |
| --- | --- |
| `targetSdk` ≥ Android 14 (API 34) | `app/build.gradle.kts` |
| `compileSdk` ≥ 34 | `app/build.gradle.kts` |
| `versionCode` is monotonic | derived from `git rev-list --count HEAD` |
| App icon ≥ 48dp adaptive (foreground + background layers) | `res/mipmap-anydpi-v26/ic_launcher.xml` |
| Permissions declared in manifest match runtime use | `AndroidManifest.xml` |
| No debug log spam in release build | proguard / `Log.d` is fine (not stripped, but inert without adb attached) |
| `automotive_app_desc.xml` with `<uses name="template"/>` | `res/xml/` — CAR.VALIDATOR will refuse otherwise |
| App-bundle build (`.aab`) | `make release` produces `.aab` via `bundleRelease` |

## Store listing content

| Item | Notes |
| --- | --- |
| App name | `CarMenu` |
| Short description (≤ 80 chars) | "Server-driven Android Auto menu — destinations and shortcuts your way." |
| Full description (≤ 4000 chars) | See template below. |
| App icon (512×512 PNG) | `assets/store/icon-512.png` |
| Feature graphic (1024×500 PNG) | `assets/store/feature.png` |
| Phone screenshots (2–8, 1080×1920) | DHU + screenshot of AA screen + screenshot of phone settings UI |
| Android Auto screenshot (1280×720) | Optional but recommended — DHU run gives this |
| Category | "Auto & Vehicles" |
| Content rating | Questionnaire — answer "no" to everything: Everyone rating |

### Full description template

```
CarMenu is a minimal Android Auto companion that shows a list of
shortcuts (destinations, places, actions) defined by a server you own.

How it works:
1. Configure a server URL in the app's phone-side settings.
2. When you connect Android Auto, the app sends your current location
   to the server.
3. The server returns a list of rows — titles, subtitles, icons, and a
   tap action (typically a navigation intent).
4. Tap a row in Android Auto to launch navigation in Waze or Google
   Maps.

Designed for personal home-automation setups where you already run a
home server and want a custom shortcut menu in the car.

Privacy:
- The app sends only your current location, a device id, and a
  timestamp — to the server URL you configured.
- Nothing is stored on the device beyond the server URL and device id.
- No analytics, no crash reporters, no third-party SDKs.
- The server is owned and operated by you.

Permissions:
- Location: to send to your server.
- Internet: to talk to your server.

This app does not include navigation itself — it hands off to Waze or
Google Maps via standard Android intents.
```

## Privacy policy

Required by Play if the app requests sensitive permissions (location is
one). Must be hosted at a public URL.

Cheapest hosting: GitHub Pages on a repo named e.g. `carmenu-privacy`.
A single `index.md` containing this text:

```markdown
# CarMenu — Privacy Policy

Last updated: 2026-MM-DD

CarMenu sends the following data to the HTTPS server URL configured by
the user in the app's settings:

- The device's current geographic location (latitude, longitude)
- A device identifier (generated at first launch, user-editable)
- A timestamp

This data is transmitted only while the app is in use (foreground or
Android Auto session), only to the user-configured server, and is not
stored on the device beyond the lifetime of an in-flight request.

CarMenu does not transmit any data to any other party. CarMenu does not
include any analytics SDK, crash reporter, or advertising library. The
server URL is owned and operated by the user.

CarMenu does not collect, store, or transmit any other personal data.

To contact the developer about this policy: <your email or GitHub
username>.
```

URL: `https://<github-user>.github.io/carmenu-privacy/`

Paste this URL into the Play Console "Privacy policy" field.

## Data Safety form

Play Console > App content > Data safety. Fill in:

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | Yes |
| Location > Approximate location | Collected: Yes. Shared: No. Required: Yes. Purpose: App functionality. Ephemeral: Yes. |
| Location > Precise location | Collected: Yes. Shared: No. Required: Yes. Purpose: App functionality. Ephemeral: Yes. |
| App activity (device identifiers etc.) | None. (device_id is generated by the app, not a system identifier.) |
| All other categories | No |
| Is all of the user data collected by your app encrypted in transit? | Yes (HTTPS) |
| Do you provide a way for users to request that their data be deleted? | Yes — user controls the server, can delete server-side data themselves; app sends nothing to anyone else. |

## Permission declarations

Required justifications in Play Console > Policy > App content:

| Permission | Justification |
| --- | --- |
| `ACCESS_FINE_LOCATION` | "Send current location to user-configured server so the server can return relevant destination shortcuts (e.g. nearest charging stations or home routes)." |
| `INTERNET` | Standard — no extra form. |

No background location, no foreground service type (since CarMenu only
runs while AA is active or the settings activity is open). Avoids the
biggest Play review pitfalls.

## App signing

1. Generate an upload keystore (`make keygen` — interactive password).
2. In Play Console, enroll in **Play App Signing** (Google manages the
   release key; you only ever hold the upload key).
3. First-time upload: provide the upload key's fingerprint to Play; Play
   generates a new release key under the hood.

## Internal testing track

Quickest path to get the app onto a phone via Play:

1. Play Console > Testing > Internal testing > Create new release.
2. Upload `.aab`.
3. Add tester emails (up to 100).
4. Get the opt-in link, share with testers (or just yourself).
5. Tester clicks link → goes to Play Store → installs.

Internal testing skips most of the public review checklist. You still need
the privacy policy URL and data safety form to be set; you don't need
screenshots / full description.

Once you're happy with internal testing, promote to closed or open
testing, then production.

## Once live: what changes

- AA recognises the app as Play-installed → "trusted" stamp set → some
  internal AA UX hooks now work that didn't for sideloaded.
- Notifications fire through the Play-trusted path (relevant if CarMenu
  ever posts an AA-connect notification — currently we don't).
- App appears in the AA app drawer reliably.

## Updates

| When | What |
| --- | --- |
| Code change | bump versionCode (auto via git commit count), `make release`, upload new `.aab` to Play. |
| Server-side change (template, rows, icons-by-slug, intents) | **no app update needed** — server controls it all. |

This is the architectural payoff of the server-driven design: app
updates are rare because behavior lives on the server. Most likely the
only app updates after launch are: bug fixes, new icon slugs, support
for new templates.
