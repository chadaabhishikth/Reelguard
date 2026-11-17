# ReelGuard

ReelGuard is a native Android **companion prototype** for a narrow goal: block recognised Instagram Reels locally, while investigating whether a Reel explicitly tapped from a DM can be allowed safely without exposing an endless Reel feed.

## Current implementation status

This repository now contains an Android/Kotlin build scaffold, a consent-first dashboard, an AccessibilityService lifecycle, safe overlay infrastructure, a deterministic and unit-tested policy state machine, local opt-in diagnostics, and CI.

**It is not yet a working Instagram blocker.** The source deliberately ships with an empty compatibility profile. Until the target phone's Instagram accessibility interface has been tested, the service does not inspect Instagram's UI tree, identify Reels, or enable DM mode. This is intentional: a recent chat visit or a guessed selector must never authorise a Reel.

No APK has been built or phone-tested in this workspace because it has no JDK or Android SDK installed.

## What has been built

- XML/View Android dashboard with explicit Accessibility disclosure and a master preference
- No `INTERNET` permission, login, backend, analytics, screenshots, or message storage
- `GuardAccessibilityService`, restricted by code to Instagram before any tree inspection
- `GuardStateMachine` enforcing one-use, short-lived DM permission semantics
- Accessibility overlay controller for a blocked viewer and a future locked shared-Reel session
- Safe default `CompatibilityProfile.UNVERIFIED`; DM mode is disabled
- Local opt-in, sanitised diagnostic trace (maximum 100 entries)
- Unit tests for the policy state machine and GitHub Actions build/lint/test/APK workflow

## Build

See [docs/TESTING.md](docs/TESTING.md) for prerequisites and commands. With JDK 17 and Android SDK Platform 35 installed:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Next required step: compatibility probe

The difficult and unresolved part is whether the installed Instagram build exposes enough stable accessibility information to tell all of these apart:

1. a conversation screen;
2. a tap on a shared-Reel card in that conversation;
3. the native Reel viewer; and
4. a switch from the allowed Reel to another Reel, including automatic progression.

The first target is a Google Pixel 8a on Android 17 with Instagram `453.0.0.57.79`. The Instagram interface language and version code still need recording. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) and [docs/TESTING.md](docs/TESTING.md) before adding any selector profile.

## Privacy and limits

Read [docs/PRIVACY.md](docs/PRIVACY.md). ReelGuard is voluntary focus support, not a security boundary. Accessibility overlays react after Instagram changes UI, so a frame may be visible before a future blocker responds. It does not modify Instagram or guarantee detection of unknown screens.

The product plan and full acceptance criteria remain in [docs/PLAN.md](docs/PLAN.md).
