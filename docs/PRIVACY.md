# Privacy and permission boundary

ReelGuard is designed as an offline, voluntary focus tool.

## What the prototype does not request

The manifest contains **no** permission for:

- Internet or network access
- Instagram login, passwords, cookies, or account data
- Media, photos, camera, microphone, contacts, notifications, location, device admin, or draw-over-other-apps

It does not include analytics, ads, remote crash reporting, a backend, or a modified Instagram APK.

## Accessibility access

Android Accessibility access is required only if the user chooses to turn on Reel protection. It is sensitive: the Android system permits an enabled service to inspect screen structure. ReelGuard's service is constrained in code to:

1. clear its overlay as soon as another app is foregrounded; and
2. inspect an accessibility tree only when the foreground package is the standard Instagram package **and** a local compatibility profile has been verified.

The starter build ships with no verified profile, so it does not open Instagram accessibility trees or claim to block anything.

Detection must use only verified structural metadata. It must not use or persist message bodies, captions, usernames, screenshots, recordings, passwords, or session cookies. Any content-change fingerprint used by a future DM experiment stays only in memory for that viewing session.

## Local settings and diagnostics

The app stores only local switches for protection and optional diagnostics. Diagnostics are off by default. When explicitly enabled, the maximum 100 entries contain only a timestamp, a policy signal name, a state name, and an overlay command name. They exclude screen content, raw selectors, resource IDs, package histories, and content fingerprints.

## User control

The user can disable Reel protection in the dashboard, disable the service in Android Accessibility settings, or uninstall the app. Overlays are removed when the app changes, the service is interrupted, or the service is destroyed. ReelGuard is not a security boundary and makes no attempt to prevent disabling or uninstalling it.
