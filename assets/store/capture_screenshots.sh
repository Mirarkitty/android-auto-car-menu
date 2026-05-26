#!/usr/bin/env bash
# Capture the screenshots Play wants for the CarMenu listing.
#
# Play accepts:
#   - 2..8 phone screenshots, ≥320px short side, ≥16:9 ratio range, JPEG/24bit PNG
#   - 1..8 Android Auto screenshots — 1280×720 PNG/JPEG (16:9, ratio 1.77)
#
# We capture three categories:
#   (1) Phone settings UI    — phone screen via `adb shell screencap`, then
#                             downscale to 1080×1920 if needed.
#   (2) Phone AA-projection view via DHU window screenshot — needs the DHU
#       running on the laptop and a tool like `import` (ImageMagick) or
#       `gnome-screenshot --window`.
#   (3) Pre-rendered mock 1280×720 of an AA list — useful when you don't
#       have a real car/DHU handy. Built from the same Pillow code as
#       render_store_assets.py.
#
# Usage:
#   capture_screenshots.sh phone        # adb-capture the phone-side screens
#   capture_screenshots.sh aa-window    # grab the DHU window (requires
#                                       # 'import' from ImageMagick + DHU running)
#   capture_screenshots.sh aa-mock      # generate a synthetic 1280×720 PNG
#   capture_screenshots.sh all          # all three
set -eu

OUT=$(dirname "$(readlink -f "$0")")
# adb resolved from $PATH by default; override with:  ADB=/path/to/adb ...
ADB=${ADB:-$(command -v adb || echo adb)}

phone_shots() {
    echo "== phone settings screenshots =="
    # Make sure CarMenu is in foreground.
    "$ADB" shell am start -n com.mirar.carmenu/.MainActivity >/dev/null
    sleep 1
    "$ADB" exec-out screencap -p > "$OUT/phone-01-settings.png"
    echo "wrote $OUT/phone-01-settings.png"
    # Tap-trigger the location permission rationale by simulating a Save tap
    # is not reliable from scripts — leave that one to a manual capture.
    echo "Tip: capture the permission dialog manually via:"
    echo "     adb exec-out screencap -p > $OUT/phone-02-perm.png"
}

aa_window_shot() {
    echo "== AA DHU window screenshot =="
    if ! command -v import >/dev/null 2>&1; then
        echo "Install ImageMagick (apt install imagemagick) or use scrot/gnome-screenshot." >&2
        echo "Manual: bring DHU window to the front, then run:" >&2
        echo "    gnome-screenshot --window --file=$OUT/aa-01-list.png" >&2
        return 1
    fi
    echo "Click the DHU window in the next 4 seconds..."
    sleep 4
    import -window "$(xdotool getactivewindow)" "$OUT/aa-01-list.png"
    echo "wrote $OUT/aa-01-list.png"
    # Normalize to 1280×720 if needed
    if command -v convert >/dev/null 2>&1; then
        convert "$OUT/aa-01-list.png" -resize '1280x720>' "$OUT/aa-01-list.png"
        echo "resized to ≤1280×720"
    fi
}

aa_mock_shot() {
    echo "== synthetic 1280×720 AA mock =="
    python3 "$OUT/render_aa_mock.py"
}

case "${1:-all}" in
    phone)     phone_shots ;;
    aa-window) aa_window_shot ;;
    aa-mock)   aa_mock_shot ;;
    all)       phone_shots; aa_mock_shot; aa_window_shot || true ;;
    *) echo "usage: $0 {phone|aa-window|aa-mock|all}" >&2; exit 2 ;;
esac
