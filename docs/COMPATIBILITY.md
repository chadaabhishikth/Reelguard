# Compatibility profile and phone-test gate

**Current state: unverified.** The source ships with `CompatibilityProfile.UNVERIFIED`, which contains no Instagram selectors. Therefore the starter APK must not be described as a working Reel blocker.

## Target under investigation

| Field | Value |
| --- | --- |
| Phone | Google Pixel 8a |
| Android | 17 (user-reported) |
| Instagram version name | `453.0.0.57.79` |
| Instagram version code | Not recorded |
| Instagram interface language | Not recorded |
| Standard Instagram app | To confirm |
| Navigation/display/auto-scroll settings | To record |

Instagram frequently changes its native interface. A selector that works on one build, language, display scale, or account state may fail on another. Each profile is tied to the observed configuration and must be re-tested after an Instagram update.

## Required evidence before a profile is added

A reviewer must confirm, on the target phone, all of the following without collecting message text or screenshots:

1. A structural indicator reliably identifies an open conversation.
2. A **click event on a shared-Reel card**, not merely the card being visible, can be identified.
3. A structural indicator reliably identifies the native Reel viewer from each relevant entry route.
4. A non-content fingerprint is available and changes reliably when Instagram switches to another Reel, including automatic advancement.

The first three can enable a **general Reel blocker** only after a specific test profile is reviewed. The DM exception needs all four conditions. If condition 2 or 4 is weak, the DM exception remains disabled. A recent chat visit is never sufficient authority for a Reel.

## Profile rules

A future profile may include only stable, app-owned resource IDs, roles, and structural relationships. It must not include message text, captions, names, account identifiers, or content copied from a conversation. It must document:

- exact device, Android version, Instagram version name/code, language, navigation mode, display/font scaling, and relevant auto-scroll settings;
- selectors and the routes tested;
- observed misses, false positives, and the 20-session DM test result; and
- the date tested and expiry/retest conditions.

Do not set `contentChangeDetectionVerified = true` until automatic and manual content changes have both been observed to invalidate the transient fingerprint.
