# CarMenu — Play Store submission checklist

Concrete, ordered. The companion reference is
[`docs/PUBLISH.md`](../../docs/PUBLISH.md); this file is the do-list.

Tracking-as-of: 2026-05-26. Built `app-release.aab` is 5.3 MB at
`app/build/outputs/bundle/release/app-release.aab` (compileSdk=36,
targetSdk=36, versionCode from git, versionName `git describe`).

---

## 0 · Prerequisites — one-time

- [ ] **Google Play Developer account** at <https://play.google.com/console>
      ($25 one-time). Personal email is fine. Identity verification can
      take up to 24h.
- [ ] **Privacy policy hosted at a stable URL.** Upload
      `assets/store/privacy-policy.html` to a web host you control
      (GitHub Pages, an existing personal site, or any static-file
      host). The URL must be in the app's settings so testers can
      reach it from the phone, AND in Play Console → App content →
      Privacy policy. Update the constant in
      `app/src/main/java/com/mirar/carmenu/MainActivity.java`
      (`PRIVACY_URL`) before the first signed build.

- [ ] **Protocol spec hosted alongside the policy** (referenced in
      the full description so reviewers can read the contract
      without installing the app). Upload
      `assets/store/PROTOCOL.txt` to the same host. The text is
      also bundled in the APK at `app/src/main/assets/protocol.txt`
      and reachable from MainActivity → "View / save protocol spec".

---

## 1 · Local build prep

- [ ] **Generate upload keystore** (interactive):
      ```sh
      cd ~/AndroidStudioProjects/CarMenu
      make keygen
      ```
      Stores the password in `keystore.properties` (gitignored). The
      `keystore.jks` file is also gitignored. Back both up off-machine —
      losing the upload key means a manual recovery flow with Google.

- [ ] **Build the signed AAB**:
      ```sh
      make bundle
      ```
      Output: `app/build/outputs/bundle/release/app-release.aab`. Check
      the file actually got signed:
      ```sh
      $ANDROID_HOME/build-tools/36.1.0/apksigner verify \
          --verbose app/build/outputs/bundle/release/app-release.aab
      ```
      Should report `Signed using v2/v3 scheme: true`. If "(unsigned)",
      `keystore.properties` is missing or mis-named.

- [ ] **Sanity-run on the phone** one last time before upload — the
      release build behaves slightly differently from debug (proguard
      currently `isMinifyEnabled = false`, so functionally identical,
      but worth a smoke test): `make install` then open in DHU.

---

## 2 · Play Console — first-time app setup

In <https://play.google.com/console> → **Create app**:

- App name: **CarMenu**
- Default language: **English (United States)**
- App or game: **App**
- Free or paid: **Free**
- Declarations:
  - [ ] Developer Program Policies — agreed
  - [ ] US export laws — agreed

---

## 3 · Store presence (App content)

All paths below are under `assets/store/`:

| Field                | Source / value                              |
|----------------------|---------------------------------------------|
| App icon (512×512)   | `icon-512.png`                              |
| Feature graphic      | `feature-1024x500.png`                      |
| Short description    | `description-short.txt` (71 chars)          |
| Full description     | `description-full.txt` (2188 chars)         |
| App category         | **Auto & Vehicles**                         |
| Content rating       | Questionnaire — answer "No" to all sensitive content; results in **Everyone** |
| Privacy policy URL   | `https://mirarkitty.github.io/android-auto-car-menu/privacy.html` |
| Contact email        | (developer email — set on Play account)     |

**Screenshots (phone, 2–8 required):**
- [ ] At least 2 phone screenshots, 1080×1920 (portrait) or similar.
      Capture with `./capture_screenshots.sh phone` while the phone shows
      the CarMenu settings activity. Take a few states:
      - `phone-01-settings.png` — clean settings UI
      - `phone-02-perm.png` — permission rationale dialog (manual:
        deny first, hit Save → screenshot the dialog)

**Screenshots (Android Auto, optional but recommended):**
- [ ] 1–8 screenshots at **1280×720**. Two approaches:
  - **Real DHU:** start DHU on the laptop (see BUILD.md), use
    `./capture_screenshots.sh aa-window` to grab the window.
  - **Synthetic mock:** `./render_aa_mock.py` produces
    `aa-mock-1280x720.png` showing the placeholder list. Useful as a
    seed; replace with a real DHU capture before going to production.

---

## 4 · App content forms (Play Console → App content)

Fill in the order the Console presents:

- [ ] **Privacy policy** — paste the public URL.
- [ ] **App access** — *All functionality is available without
      restrictions.* (No login wall.)
- [ ] **Ads** — *No, my app does not contain ads.*
- [ ] **Content rating** — answer "No" to every category (violence,
      profanity, gambling, drugs, sex, fear, etc.). Result: **Everyone**.
- [ ] **Target audience** — **18 and over** is the safest match.
- [ ] **News app** — No.
- [ ] **COVID-19 contact tracing** — No.
- [ ] **Data safety** — table below.
- [ ] **Government app** — No.
- [ ] **Financial features** — None.
- [ ] **Health** — No.
- [ ] **AI-generated content** — No (the app itself doesn't generate
      content; the user's server might, but that's outside the app).

### Data safety form

| Question | Answer |
|----------|--------|
| Does your app collect/share any of the required user data types? | **Yes** |
| Is all data encrypted in transit? | **Yes** (HTTPS) |
| Do users have a way to request that their data be deleted? | **Yes** — the user controls the server they configured |

For data type **Location → Approximate location** AND **Precise location**:

| Field | Value |
|-------|-------|
| Collected | **Yes** |
| Shared | **No** (sent only to the user-configured server, which is the user's own infrastructure) |
| Optional | **Required** (the app needs it to do its job) |
| Purposes | **App functionality** |
| Ephemeral | **Yes** (not stored on the device beyond the in-flight request) |

For **App info & performance → Device or other IDs**:

| Field | Value |
|-------|-------|
| Collected | **Yes** |
| Shared | **No** |
| Optional | **Required** |
| Purposes | **App functionality** |
| Note | The `device_id` is generated by the app itself (not a system identifier), is user-editable, and sent only to the user-configured server. |

All other categories: **No** (we don't touch them).

---

## 5 · Permission declarations (App content → Sensitive permissions)

| Permission              | Justification                                                                                                |
|-------------------------|-------------------------------------------------------------------------------------------------------------|
| `ACCESS_FINE_LOCATION`  | Send the user's current location to their own server so the server can return relevant destination shortcuts (e.g. nearest charging stations or home routes). Used only while the app is in use on the Android Auto screen — no background. |
| `INTERNET`              | Standard — no extra form. |

No foreground service. No background location. No SMS/Call_log/Contacts.
These are the policy minefields and we avoid them all.

---

## 6 · Release — Internal testing

This is the fastest path that still gives the "Play-installed" trust
stamp Android Auto wants.

- [ ] Play Console → **Testing → Internal testing → Create new release**.
- [ ] **App bundles:** upload `app-release.aab`.
- [ ] **Release name:** auto-fills from versionName.
- [ ] **Release notes:** "Initial internal-testing release."
- [ ] Save → Review → Start rollout to Internal testing.
- [ ] **Add testers:** under *Testers*, create an email list, add your
      own Google account.
- [ ] Open the opt-in URL Play gives you on the phone → "Become a
      tester" → install via Play.

Verify: open the Android Auto app drawer (in the car or DHU). CarMenu
should appear with the "Installed via Play" trust badge.

---

## 7 · Optional follow-ups (not blocking internal testing)

- [ ] Promote Internal → Closed testing for friends.
- [ ] Promote Closed → Production for public listing (this triggers
      real review; expect 1–7 days).
- [ ] Once production, swap the
      `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` in `MyCarAppService.java`
      for the strict Google-published validator. ALLOW_ALL is fine
      while you're the only tester, but production should restrict.
- [ ] Enable Play App Signing if it wasn't auto-enabled at first upload.

---

## Quick reference

| File | What |
|------|------|
| `icon-512.png`             | Listing app icon |
| `feature-1024x500.png`     | Listing feature graphic |
| `description-short.txt`    | ≤80-char short description |
| `description-full.txt`     | ≤4000-char full description |
| `privacy-policy.html`      | Ready-to-host privacy policy |
| `privacy-policy.md`        | Markdown source of the same |
| `aa-mock-1280x720.png`     | Synthetic AA screenshot |
| `PROTOCOL.txt`             | Plain-ASCII server-author reference (also bundled in APK) |
| `render_store_assets.py`   | Regenerate icon + feature |
| `render_aa_mock.py`        | Regenerate AA mock |
| `capture_screenshots.sh`   | adb + DHU capture helper |
| `SUBMISSION_CHECKLIST.md`  | This file |
