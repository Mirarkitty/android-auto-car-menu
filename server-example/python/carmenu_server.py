#!/usr/bin/env python3
"""
Reference server for CarMenu — full protocol coverage in ~150 lines of Flask.

Run:
    pip install -r requirements.txt
    python3 carmenu_server.py        # listens on http://0.0.0.0:8080

Then on the phone, set the server URL in CarMenu's settings to:
    http://<your-laptop-ip>:8080/aa-screen           # local LAN testing
    https://your-reverse-proxy.example/aa-screen     # production

Google Play and Android Auto require HTTPS in production. For local
LAN testing, http:// works if the app's network_security_config allows
it (the CarMenu build does — see app/src/main/res/xml).
"""

import math
import time
from flask import Flask, request, jsonify, send_from_directory

# Optional: serve a few example PNG icons from ./icons/ so the app can
# exercise the remote-icon (icon_url) path.  Drop any 96×96 PNGs in that
# directory and reference them as "/icons/<name>.png" in the JSON below.
ICONS_DIR = "icons"

app = Flask(__name__)


# ── A tiny destination registry ────────────────────────────────────────
# Each entry: title, subtitle, an optional built-in icon slug
# (PROTOCOL.txt section 5.1), an optional tint, and a coordinate.
# Replace these with whatever destinations make sense for you.
PLACES = [
    {"title": "Cafe Aroma",
     "subtitle": "Old town square",
     "icon": "lunch",   "tint": "red",
     "lat": 52.5163, "lon": 13.3777},

    {"title": "Public library",
     "subtitle": "Main branch",
     "icon": "school", "tint": "#3399FF",
     "lat": 52.5191, "lon": 13.4014},

    {"title": "Fast charger",
     "subtitle": "Highway rest stop",
     "icon_url": "/icons/charger.png",   # if you've dropped one in ./icons/
     "lat": 52.4862, "lon": 13.4275},
]


# ── Server-side actions (carmenu:do?id=<action-id>) ────────────────────
# Each row whose tap.intent is "carmenu:do?id=<name>" turns into a
# server-side handler.  This is where the magic happens — the action_id
# is opaque to the phone, you decide what it means.  Common uses:
#
#   - Toggle a smart-home device (lights, garage, locks)
#   - Send a message via Telegram / Slack / Matrix
#   - Trigger a scene in Home Assistant / Hubitat / openHAB
#   - Queue a task in your job runner
#
# Each handler receives the parsed request dict (so you can read lat/lon,
# device_id, etc.) and returns a JSON-able dict — typically a "message"
# template confirming the action with a short refresh_seconds so the
# screen rolls back to the menu.
def action_garage_toggle(req):
    """Toggle the garage door.

    In a real setup this would POST to your home-automation endpoint,
    e.g. requests.post('http://hass.local:8123/api/services/cover/toggle',
                       headers={'Authorization': 'Bearer ...'},
                       json={'entity_id': 'cover.garage_door'})
    Here we just log and return a confirmation so the end-to-end signal
    can be tested.
    """
    print(f"[action] garage-toggle from {req.get('device_id')} "
          f"at {req.get('lat')}, {req.get('lon')}")
    # TODO: call your home-automation API here.
    return {
        "template": "message",
        "title":    "Garage",
        "subtitle": "Door toggle sent.",
        "icon":     "fav",
        "tint":     "green",
        "refresh_seconds": 10,
    }


def action_lights_home_on(req):
    print(f"[action] lights-home-on from {req.get('device_id')}")
    return {
        "template": "message",
        "title":    "Lights",
        "subtitle": "Welcome scene activated.",
        "icon":     "fav",
        "tint":     "yellow",
        "refresh_seconds": 8,
    }


ACTIONS = {
    "garage-toggle":  action_garage_toggle,
    "lights-home-on": action_lights_home_on,
    # Add your own here.  Keys are the opaque action_id strings the row's
    # tap.intent ("carmenu:do?id=<key>") sends back.
}


# ── helpers ────────────────────────────────────────────────────────────
def haversine_km(lat1, lon1, lat2, lon2):
    """Great-circle distance, kilometres.  Used to sort by proximity."""
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2 * r * math.asin(math.sqrt(a))


def status_line(req):
    """Server-controlled status row that reflects what the client sent."""
    has_fix = "lat" in req and "lon" in req
    fresh = bool(req.get("location_fresh"))
    if not has_fix:
        return "no GPS yet — waiting…"
    return "live GPS" if fresh else "stale GPS — waiting for fresh fix"


def make_geo_intent(lat, lon):
    """Build an Android geo: intent the AA host can hand to Waze / Maps."""
    return f"geo:{lat:.4f},{lon:.4f}?q={lat:.4f},{lon:.4f}"


# ── /aa-screen — the one endpoint CarMenu speaks ───────────────────────
@app.route("/aa-screen", methods=["POST"])
def aa_screen():
    req = request.get_json(silent=True) or {}
    perm = req.get("location_permission", "unknown")
    trigger = req.get("trigger", "?")
    app.logger.info("carmenu /aa-screen perm=%s trigger=%s lat=%s",
                    perm, trigger, req.get("lat"))

    # ── Server-side action dispatch (carmenu:do?id=...) ────────────────
    # Row taps with carmenu:do?id=<id> arrive here with trigger="action"
    # and the id in action_id.  Look it up in ACTIONS and run the handler.
    if trigger == "action":
        action_id = req.get("action_id", "")
        handler = ACTIONS.get(action_id)
        if handler is not None:
            return jsonify(handler(req))
        return jsonify({
            "template": "message",
            "title":    "Unknown action",
            "subtitle": f"Server has no handler for {action_id!r}.",
            "icon":     "generic",
            "refresh_seconds": 30,
        })

    # Case (a) — permission denied.  Send a message template and let the
    # user fix it on the phone.  No destinations would be relevant anyway.
    if perm == "denied":
        return jsonify({
            "template": "message",
            "title":    "Location off",
            "subtitle": "Grant CarMenu location access on the phone "
                        "to see destination suggestions.",
            "icon":     "generic",
            "refresh_seconds": 30,
        })

    # Case (b)+(c) — sort places by proximity if we have a fix.
    places = list(PLACES)
    if "lat" in req and "lon" in req:
        for p in places:
            p["_dist_km"] = haversine_km(req["lat"], req["lon"],
                                         p["lat"], p["lon"])
        places.sort(key=lambda p: p["_dist_km"])

    rows = []
    # 3 destinations + 1 server-side action + 1 status row = 5 (AA caps at 6).
    for p in places[:3]:
        row = {
            "title":    p["title"],
            "subtitle": p["subtitle"],
            "tap":      {"intent": make_geo_intent(p["lat"], p["lon"])},
        }
        if "icon" in p:     row["icon"]     = p["icon"]
        if "icon_url" in p: row["icon_url"] = p["icon_url"]
        if "tint" in p:     row["tint"]     = p["tint"]
        rows.append(row)

    # Server-side action button — taps fire the garage-toggle handler.
    # See ACTIONS above; add your own there and another row here.
    rows.append({
        "title":    "Open garage",
        "subtitle": "Tap to toggle (server-side action)",
        "icon":     "fav",
        "tint":     "green",
        "tap":      {"intent": "carmenu:do?id=garage-toggle"},
    })

    # Status / refresh row — every screen should have an escape hatch
    # back to the server.
    rows.append({
        "title": status_line(req),
        "icon":  "refresh",
        "tint":  "yellow",
        "tap":   {"intent": "carmenu:refresh"},
    })

    return jsonify({
        "template":        "list",
        "title":           "Pick a spot",
        "rows":            rows,
        "refresh_seconds": 120,
    })


# ── /icons/<name> — serves the icons referenced by icon_url ────────────
@app.route("/icons/<path:name>")
def serve_icon(name):
    return send_from_directory(ICONS_DIR, name)


# ── health check ───────────────────────────────────────────────────────
@app.route("/")
def index():
    return ("CarMenu reference server is running.\n"
            "POST JSON to /aa-screen — see "
            "https://mirarkitty.github.io/android-auto-car-menu/protocol.txt\n")


if __name__ == "__main__":
    # 0.0.0.0 so phones on the same LAN can reach it.
    app.run(host="0.0.0.0", port=8080, debug=False)
