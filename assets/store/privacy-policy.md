---
title: CarMenu — Privacy Policy
---

# CarMenu — Privacy Policy

*Last updated: 2026-05-26*

CarMenu is an Android Auto application that sends the following data to the
HTTPS server URL the user configures in the app's settings:

- The device's current geographic location (latitude, longitude), including
  an indicator of how recent the fix is and whether location access has been
  granted.
- A device identifier. This identifier is generated on the device at first
  launch (a random string prefixed with `carmenu.`) and is user-editable. It
  is not derived from any system identifier and is not shared with any third
  party.
- A timestamp.
- The app's version code, used by the user's server to handle protocol
  evolution.

This data is transmitted only while the app is in use — either while the
phone-side settings screen is open, or while an active Android Auto session
is showing the CarMenu screen on the car display. Location is not collected
in the background. Nothing is stored on the device beyond the lifetime of an
in-flight request, except for the user-configured server URL and device id
in the app's local settings.

CarMenu does not transmit any data to any other party. CarMenu does not
include any analytics SDK, crash reporter, or advertising library. The
HTTPS server URL is owned and operated by the user.

If a row tapped in the Android Auto interface contains a navigation intent
(such as `geo:` for a coordinate or `tel:` for a phone number), Android Auto
hands the action off to the user's chosen navigation or telephony app on the
phone. CarMenu does not transmit information to those third-party apps —
only the standard intent URI defined by the user's own server.

The user's server, being operated by the user, is outside the scope of this
policy. The user is responsible for any data their own server stores or
retransmits.

## Permissions

| Permission             | Purpose                                                |
|------------------------|--------------------------------------------------------|
| `ACCESS_FINE_LOCATION` | Sent to the user-configured server.                    |
| `INTERNET`             | Used to communicate with the user-configured server.   |

CarMenu does not use foreground services, background location, Bluetooth,
or any other sensitive runtime capabilities.

## Data deletion

The app does not maintain any centrally-held user records — the developer
of CarMenu does not receive any data from the app. To delete data the user
has sent through CarMenu, the user should delete it from their own server.
Uninstalling CarMenu removes the locally-stored server URL and device id
from the device.

## Children

CarMenu is not directed at children under 13 and does not knowingly collect
data from children.

## Contact

For questions about this policy, contact the developer via the email
address or repository link on the CarMenu Play Store listing.
