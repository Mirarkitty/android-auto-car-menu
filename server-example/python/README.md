# Python reference server

A self-contained ~150-line Flask app implementing the full CarMenu
protocol. Use it as a starting point for your own server.

## Run

```sh
cd server-example/python
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python3 carmenu_server.py        # listens on http://0.0.0.0:8080
```

Then point CarMenu's settings at `http://<your-laptop-ip>:8080/aa-screen`.

## What it does

- `POST /aa-screen` — full handler covering all three template branches:
  - `location_permission: "denied"` → returns a `message` template
    telling the user how to fix it.
  - With a fix → returns a `list` of nearby destinations sorted by
    great-circle distance, plus a server-controlled status row
    (`"live GPS"` / `"stale GPS"` / `"no GPS yet"`).
  - Mixes built-in icon slugs, hex/named tints, and `icon_url` for
    remote bitmaps — exercising every visual code path.
- `GET /icons/<name>` — optional helper; drop 96×96 PNGs in `./icons/`
  and reference them as `"icon_url": "/icons/<name>.png"` in the JSON.
- `GET /` — a one-line health check.

## Test it without a phone

```sh
curl -sS -X POST \
     -H 'Content-Type: application/json' \
     -d '{"device_id":"test",
          "lat":52.52,"lon":13.40,
          "location_fresh":true,"location_age_ms":500,
          "location_permission":"granted",
          "ts":1779441234567,"trigger":"initial",
          "app_version":1}' \
     http://localhost:8080/aa-screen | jq .
```

You should see a `list` template with destinations sorted by proximity
to (52.52, 13.40).

## Customising

Two extension points:

**Destinations.** Edit the `PLACES` list at the top of
`carmenu_server.py`. Each entry is a row in the AA list — title,
subtitle, an optional icon (slug or URL), an optional tint, and a
coordinate.

For a richer setup, replace `PLACES` with anything you like:
- Read from a database of saved spots
- Pull live data (charger availability, calendar events, etc.) at
  request time
- Make the response depend on time of day, day of week, or last-known
  fix location

**Server-side actions** (`carmenu:do?id=...`). Edit the `ACTIONS` dict.
Each key is an opaque `action_id` string; the value is a function that
receives the parsed request (lat/lon, device_id, etc.) and returns a
template dict. This is where a row tap turns into "open the garage",
"send my ETA to <person>", "activate scene X" — anything your server
can do, you can map to a button.

The bundled example has `garage-toggle` (logs + returns a confirmation
message; uncomment the `requests.post(...)` to wire it to your home
automation) and `lights-home-on` as templates to copy.

The protocol is documented in full at
`https://mirarkitty.github.io/android-auto-car-menu/protocol.txt` (also bundled inside the
app — open CarMenu's phone-side settings → "View / save protocol spec").

## Production hosting

Google Play and Android Auto require HTTPS in production. Two
common patterns:

1. **Reverse proxy** in front of this Flask app — e.g. Caddy, nginx,
   or Traefik — terminating TLS and forwarding plain HTTP to
   `localhost:8080`.
2. **Direct TLS** in Flask itself — pass an `ssl_context` to
   `app.run()`. Simpler for one-off setups, less flexible.

Run under a process supervisor (systemd, supervisord) so it restarts
on failure or reboot.
