# ReelGuard — Android prototype plan

**Status:** Planning only; no app or APK has been built or device-tested.  
**Prepared:** 17 November 2026  
**Initial audience:** One consenting user, on one Android phone, using the official Instagram app.

## 1. Product goal

**Watch a Reel a friend sent in a DM, then return to the conversation—not into an endless recommendations feed.**

| Action | Intended result |
| --- | --- |
| Open Instagram's Reels feed | Block the viewer and offer a way back. |
| Open a Reel from a profile, Explore, or another non-DM route | Block the recognised Reel viewer. |
| Open a Reel explicitly shared in an Instagram conversation | Experimental: allow that one Reel. |
| Swipe up/down, tap recommendations, or otherwise navigate to another Reel | Prevent the interaction; exit if a content change is detected. |
| Return to the chat and open another shared Reel | Allow a new, separate viewing session. |
| Use messages, photos, normal posts, or Stories | Aim to leave these unaffected; verify with regression tests. |
| Leave Instagram or turn protection off | Remove the overlay and clear the viewing session. |

This is a **companion blocker**, not a modification of Instagram. The Reels tab may remain visible. A Reel may briefly appear before a reactive blocker responds. It will not remove all videos or every inline Reel preview from the Home feed in this first version.

## 2. What “the present version” means

We will support **the Instagram build actually installed on your phone**, not assume that everyone receives the same latest interface.

Before implementation is considered compatible, record:

- Phone manufacturer/model and Android version.
- Instagram version name and version code, where obtainable.
- Instagram interface language.
- Navigation mode, display/font scaling, and relevant auto-scroll settings.
- Whether this is the standard Instagram app rather than Lite, a clone, or a work-profile installation.

### Current target configuration (user-reported)

- Phone: Google Pixel 8a.
- Android: 17.
- Instagram version name: `453.0.0.57.79` (version code not yet recorded).
- Instagram interface language: not yet provided.
- Compatibility and DM-origin detection: **unverified; no on-device probe has been run**.

Compatibility status starts as **unverified**. Instagram updates or an unexpected UI should invalidate our tested-version status. The first language and device target will be yours; wider support comes later.

## 3. Feasibility gate: test the difficult part first

Instagram does not provide an official API for selectively controlling its native Reels viewer. We must inspect what its Android accessibility interface actually exposes.

Before investing in a polished app, check whether we can reliably distinguish:

1. A conversation from the inbox and other screens.
2. A tap on a shared Reel card from an unrelated action in that conversation.
3. The resulting Reel viewer from other video screens.
4. A change from the allowed Reel to different content, ideally using a stable accessible content marker.

Use resource IDs, roles, structural relationships, and other non-message UI markers where available. Do not assume selector names or that a stable Reel ID exists. A DM being open recently is **not** sufficient proof that the next Reel came from that chat.

**Go/no-go:** If DM attribution or content-change detection is too weak, keep that feature labelled experimental or disabled. Deliver the general blocker separately. Do not represent an unreliable heuristic as a guaranteed single-Reel lock. A filtered Instagram web experience is the alternative if native-app control proves inadequate.

## 4. First-version experience

### A. Setup and consent

- Explain the goal and limitations in plain language.
- Disclose that Accessibility access can expose on-screen content, including sensitive content.
- Explain that processing stays on the phone and message bodies are not logged or uploaded.
- Ask for explicit consent before opening Android's Accessibility settings.
- Follow the phone's normal permission flow; no bypasses of Android security controls.

### B. Dashboard

- Master protection switch.
- Separate service-permission status and Instagram compatibility status.
- “Allow DM-shared Reels — experimental” toggle, initially off until the compatibility test is run.
- Setup/test checklist and local diagnostics.
- Clear instructions to disable protection or uninstall the app.

### C. Blocked viewer

A visible explanation such as “Reels feed blocked,” with a Back action. Use bounded navigation actions; never get stuck repeatedly pressing Back.

### D. One-shared-Reel viewing

A visible guard label, “Shared Reel — scrolling locked,” plus a Back-to-chat control. An accessibility overlay consumes touches across the viewer's interaction area to prevent swipes and recommendation taps, while leaving system navigation available.

For the first prototype, native like/comment/share and touch-to-pause controls may also be unavailable under the guard. Do not forward arbitrary taps through the overlay just to preserve these controls. Hardware volume remains outside our controls.

**Important:** Blocking touch gestures does not prevent Instagram from changing content automatically. We must test auto-scroll/auto-advance behavior and detect content changes; where we cannot do that reliably, the single-Reel guarantee is unavailable.

## 5. Technical design

### Proposed stack

- Native Kotlin Android app, with a small XML/View-based interface.
- Gradle Kotlin DSL and pinned dependency/toolchain versions.
- Provisional Android 10+ baseline, adjusted once your phone is known.
- A supported compile/target SDK and matching JDK selected during build setup; do not guess a future SDK number.
- No backend, Instagram API, account login, root access, or modified Instagram APK.

### Components

| Component | Responsibility |
| --- | --- |
| `GuardAccessibilityService` | Receive relevant events and coordinate lifecycle. |
| `InstagramScreenClassifier` | Recognise chat, shared-Reel actions, Reel viewer, and other screens. |
| `GuardStateMachine` | Make deterministic, unit-testable allow/block decisions. |
| `DmSessionGate` | Issue and consume a short-lived, one-use permission after a verified shared-Reel tap. |
| `ReelOverlayController` | Install/remove the visible touch guard or full blocking panel. |
| `CompatibilityProfile` | Keep selectors and rules separate from enforcement logic. |
| `LocalDiagnostics` | Store opt-in, sanitised state transitions and rule results only. |

Use `TYPE_ACCESSIBILITY_OVERLAY` where appropriate; do not request a separate draw-over-other-apps permission unless a demonstrated requirement justifies it.

Inspect UI hierarchies only for Instagram. Foreground-window/package-change signals may be observed solely to tear down overlays when the user leaves Instagram. Filtering exclusively to Instagram events must not leave an overlay stuck over another app.

### State model

```text
OUTSIDE_INSTAGRAM
  -> NORMAL_INSTAGRAM
  -> VERIFIED_CHAT
  -> PENDING_DM_REEL     [verified card tap; short expiry; single use]
  -> DM_REEL_GUARDED     [viewer confirmed; interaction guard attached]
  -> VERIFIED_CHAT      [back; permission consumed]

Recognised Reel viewer without a valid DM permission -> BLOCKED_REEL
Unexpected content change during guarded playback  -> BLOCKED_REEL
Leave Instagram / disable / screen lock             -> clear permission and overlay
```

Debounce repeated accessibility events and avoid navigation loops. Any timing threshold is a tunable parameter to validate on the phone, not evidence of DM origin.

On uncertainty **inside a recognised Reel viewer**, do not grant a DM exception. This does not guarantee detection of every unknown Instagram UI: missed recognition is an explicit remaining risk.

## 6. Privacy, permissions, and safety

- Request only the capabilities required for this blocker.
- No `INTERNET` permission, analytics SDK, remote crash reporting, advertising SDK, or backend in the prototype.
- No collection of Instagram passwords or session cookies.
- Do not store/export message text, usernames, captions, screenshots, screen recordings, or complete accessibility trees in diagnostics.
- If a content marker is available, process it transiently in memory; do not persist it as browsing history.
- Diagnostic exports contain only app/OS versions, sanitised rule identifiers, event categories, and state transitions, and require an explicit user action.
- Remove overlays on app switch, disable, interruption, and service teardown as appropriate. Never cover system navigation or trap the user.
- No device-admin, anti-uninstall, or anti-disabling behavior.
- An interrupted, disabled, or killed service cannot enforce blocking. This is a voluntary focus tool, not a tamper-proof security boundary.

Initial distribution is a personal-use test APK. If public Play Store distribution is later requested, review the Accessibility API declaration, prominent disclosure/consent, and other applicable policies. Do not claim `isAccessibilityTool=true` merely because this app uses Accessibility APIs. Store approval is not assumed.

## 7. Implementation milestones

### M0 — Target and build setup

- Obtain the device/version details above.
- Establish a usable source handoff and repository structure.
- Provision matching JDK, Android SDK, and Gradle wrapper, or prepare a CI build.
- Pin dependencies and document reproducible build commands.

**Output:** An installable empty test app and build instructions. Repository access alone does not install an Android toolchain or provide device testing.

### M1 — Compatibility probe

- Build a narrowly scoped diagnostic mode.
- Verify the four screen/origin/content signals in section 3 on your phone.
- Record the tested compatibility profile without retaining private messages.

**Output:** An explicit compatibility finding and DM-feature go/no-go.

### M2 — General Reels blocker

- Implement the state engine, service lifecycle, ordinary Reel blocking, visible recovery controls, and settings.
- Test that chats, normal posts, Stories, and other apps remain usable.

**Output:** First useful blocker APK, with limitations documented.

### M3 — Experimental DM exception

- Add verified shared-card origin tracking and the one-use viewing permission.
- Attach the touch guard and clear it on every exit path.
- Test fast swipes, taps, automatic playback changes, and attempts to enter recommendations.

**Output:** DM-only test APK, still experimental until phone tests pass.

### M4 — Hardening and handoff

- Run regression, lifecycle, privacy, and build checks.
- Document the exact tested device/Instagram combination and known failures.
- Package source, APK, changelog, test results, and installation/uninstallation instructions.

**Output:** A versioned personal-use prototype, not a universal compatibility claim.

## 8. Test and acceptance checklist

### Automated tests

- State transitions, token expiry, and one-time permission consumption.
- A chat visit alone never authorises a Reel.
- A non-DM Reel cannot reuse an earlier DM permission.
- Leaving Instagram invalidates a viewing session.
- Repeated events do not create repeated Back actions or duplicate overlays.
- Unknown viewer transitions and content changes revoke the DM exception.
- Sanitised diagnostic output contains only explicitly allowed fields.

### Phone tests — required

| Test | Acceptance target on the recorded device/build |
| --- | --- |
| Open Reels via tab, profile, Explore, and Home entry points | Recognised viewers are blocked; document any unsupported route. |
| Open a Reel from a verified conversation | Only the explicitly selected Reel is viewable. |
| Fast/slow up/down swipes, edge drags, multiple fingers | No next/previous Reel navigation during the guarded session. |
| Tap recommendations, creator links, comments, and share areas | No path to another Reel through the guarded interaction surface. |
| Auto-scroll/auto-advance or playback completion | No undetected switch to another Reel; otherwise DM mode fails acceptance. |
| Back, then open a different shared Reel | A new permission is required and issued only from a verified card tap. |
| Messages, photos, regular posts, Stories, keyboard, other apps | No guard overlays or unintended navigation. |
| Rotation, lock/unlock, app switching, offline state, service interruption | No stuck overlay or stale DM permission; loss of protection is documented. |

Repeat the key DM and navigation cases across at least 20 viewing sessions on the target configuration. Record observed misses and false positives rather than claiming zero failures beyond the tested sample. A brief frame appearing before a reactive block remains possible; if absolutely no other Reel may ever appear, this native companion approach may not satisfy that requirement.

## 9. Intended repository layout

```text
app/src/main/.../ui/
app/src/main/.../accessibility/
app/src/main/.../detection/
app/src/main/.../domain/
app/src/main/.../overlay/
app/src/main/.../diagnostics/
app/src/test/
app/src/androidTest/
docs/PLAN.md
docs/PRIVACY.md
docs/TESTING.md
docs/COMPATIBILITY.md
.github/workflows/android.yml
README.md
```

The proposed CI workflow will compile, lint, and run unit tests, then upload a test APK as a workflow artifact. It cannot validate Instagram's real UI or DM behavior without phone tests. Never commit account credentials, private UI dumps, or signing secrets; manage release signing separately if needed later.

## 10. Handoff and next inputs

The GitHub connector currently reports **unsupported** in this environment; direct connector-based clone/commit/push access is not established. Practical options are a public repository URL or an uploaded source ZIP, followed by a Git-ready source archive/patch for you to commit. Do not paste private access tokens into the conversation.

Remaining inputs before the compatibility probe:

1. Instagram interface language (and version code, if convenient).
2. Willingness to install a test APK, knowingly enable Accessibility access, and run the on-device checklist.

The target device and Instagram version name have been supplied as recorded in section 2. Compatibility is still unverified until a probe is run on the phone.

The workspace inspected during planning has Java 11 but no configured Android SDK or Gradle executable. Provisioning compatible build tools or using CI is part of M0, not a completed step.

## Official implementation references

- [Android: Create an accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Android: AccessibilityService API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Android: Accessibility overlay window type](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY)
- [Google Play: Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)
