# Build and test procedure

## Current test status

- **Source unit tests:** included for the pure guard state machine.
- **Local build in this workspace:** not run. This workspace has no JDK, Android SDK, or Gradle installation.
- **Phone test:** not run.
- **Compatibility result:** unverified; the shipped source has no active Instagram selector profile.

A successful compile does not prove accessibility compatibility with Instagram.

## Prerequisites

- JDK 17
- Android SDK Platform 35 and matching Build Tools
- An Android 10+ device or emulator for installation
- A test phone with the exact target Instagram build for compatibility work

From the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Install it using Android Studio or, with USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Safe setup check

1. Install and open ReelGuard.
2. Read the Accessibility disclosure and explicitly tick the consent checkbox.
3. Use **Open Accessibility settings** and enable only “ReelGuard protection”.
4. Return to the dashboard and confirm the service status says enabled.
5. Confirm that the compatibility status is **Unverified** and that DM mode is disabled.
6. Turn the master protection preference on and off. With no profile installed, Instagram must remain unaffected.
7. Leave Instagram, switch apps, interrupt/disable the service, and confirm that no overlay remains.

## Required compatibility test before enforcement

Follow [COMPATIBILITY.md](COMPATIBILITY.md) to collect a privacy-safe selector proposal. Then test the profile against:

- Reels tab, profile, Explore, Home, and external intent entry points;
- a verified conversation and an explicitly tapped shared Reel;
- fast/slow vertical swipes, edge drags, multiple fingers, and recommendation taps;
- automatic progression, playback completion, and auto-scroll;
- Back and opening a different shared Reel;
- chats, photos, normal posts, Stories, keyboard use, and other apps;
- rotation, lock/unlock, app switching, offline state, and service interruption.

Run the key DM route at least 20 times. Record misses and false positives. Do not claim a one-Reel guarantee if the content-change signal is absent or unreliable.
