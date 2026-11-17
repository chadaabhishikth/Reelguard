package app.reelguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.reelguard.detection.CompatibilityProfile
import app.reelguard.detection.InstagramScreenClassifier
import app.reelguard.diagnostics.LocalDiagnostics
import app.reelguard.domain.GuardCommand
import app.reelguard.domain.GuardSignal
import app.reelguard.domain.GuardState
import app.reelguard.domain.GuardStateMachine
import app.reelguard.overlay.ReelOverlayController
import app.reelguard.ui.GuardPreferences

/**
 * Local-only Android accessibility service. It observes foreground package changes only
 * to remove an existing guard when Instagram is left. Accessibility trees are opened
 * only for Instagram and only after a compatibility profile has been verified.
 */
class GuardAccessibilityService : AccessibilityService() {
    private lateinit var preferences: GuardPreferences
    private lateinit var diagnostics: LocalDiagnostics
    private lateinit var classifier: InstagramScreenClassifier
    private lateinit var overlay: ReelOverlayController
    private val stateMachine = GuardStateMachine()
    private var guardedContentFingerprint: String? = null
    private var lastBackRequestAtMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferences = GuardPreferences(this)
        diagnostics = LocalDiagnostics(this)
        classifier = InstagramScreenClassifier(CompatibilityProfile.UNVERIFIED)
        overlay = ReelOverlayController(this, ::requestBoundedBack)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::preferences.isInitialized) return

        val packageName = event.packageName?.toString()
        if (packageName != INSTAGRAM_PACKAGE) {
            handle(GuardSignal.NonInstagramForeground)
            return
        }

        if (!preferences.protectionEnabled) {
            handle(GuardSignal.ProtectionDisabled)
            return
        }

        handle(GuardSignal.InstagramForeground)

        // No source node is obtained until a profile contains a tested Reel selector.
        // This starter build has an unverified profile, so it cannot inspect or block
        // Instagram yet rather than presenting a false claim of compatibility.
        if (!classifier.isGeneralBlockingReady()) return

        val root = rootInActiveWindow
        val clickedNode = if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) event.source else null
        try {
            // Accessibility events can race with an app switch. Do not inspect a tree
            // whose active root no longer belongs to Instagram.
            if (root?.packageName?.toString() != INSTAGRAM_PACKAGE) {
                handle(GuardSignal.NonInstagramForeground)
                return
            }

            val signals = classifier.classify(
                root = root,
                clickedNode = clickedNode,
                eventType = event.eventType,
                dmModeEnabled = preferences.dmModeEnabled,
            )
            signals.forEach { signal ->
                handle(signal)
                if (signal == GuardSignal.ReelViewerRecognized) {
                    evaluateGuardedContent(root)
                }
            }
            if (
                stateMachine.state == GuardState.DM_REEL_GUARDED &&
                GuardSignal.ReelViewerRecognized !in signals
            ) {
                // Missing viewer evidence during an exception is also an uncertainty.
                handle(GuardSignal.GuardedReelContentChanged)
            }
        } finally {
            clickedNode?.recycle()
            root?.recycle()
        }
    }

    override fun onInterrupt() {
        if (::overlay.isInitialized) {
            handle(GuardSignal.ServiceStopped)
        }
    }

    override fun onDestroy() {
        if (::overlay.isInitialized) {
            handle(GuardSignal.ServiceStopped)
        }
        super.onDestroy()
    }

    private fun evaluateGuardedContent(root: AccessibilityNodeInfo?) {
        if (stateMachine.state != GuardState.DM_REEL_GUARDED) return

        val fingerprint = classifier.contentFingerprint(root)
        if (fingerprint == null) {
            // A DM exception is fail-closed when the profile cannot confirm that the
            // viewer remains on the originally allowed content.
            guardedContentFingerprint = null
            handle(GuardSignal.GuardedReelContentChanged)
            return
        }

        val previousFingerprint = guardedContentFingerprint
        if (previousFingerprint == null) {
            guardedContentFingerprint = fingerprint
        } else if (previousFingerprint != fingerprint) {
            guardedContentFingerprint = null
            handle(GuardSignal.GuardedReelContentChanged)
        }
    }

    private fun handle(signal: GuardSignal) {
        val command = stateMachine.dispatch(signal)
        if (signal in CLEAR_FINGERPRINT_SIGNALS || stateMachine.state != GuardState.DM_REEL_GUARDED) {
            guardedContentFingerprint = null
        }
        render(command)
        if (preferences.diagnosticsEnabled) {
            diagnostics.record(signal, stateMachine.state, command)
        }
    }

    private fun render(command: GuardCommand) {
        when (command) {
            GuardCommand.NO_CHANGE -> Unit
            GuardCommand.CLEAR_OVERLAY -> overlay.clear()
            GuardCommand.SHOW_BLOCKER -> overlay.showBlockedViewer()
            GuardCommand.SHOW_DM_GUARD -> overlay.showDmGuard()
        }
    }

    private fun requestBoundedBack() {
        val now = System.currentTimeMillis()
        if (now - lastBackRequestAtMs < BACK_REQUEST_DEBOUNCE_MS) return
        lastBackRequestAtMs = now
        // The overlay remains until a verified screen change or app exit removes it.
        // This avoids repeated automatic Back actions and does not trap system controls.
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val BACK_REQUEST_DEBOUNCE_MS = 750L
        val CLEAR_FINGERPRINT_SIGNALS = setOf(
            GuardSignal.NonInstagramForeground,
            GuardSignal.ServiceStopped,
            GuardSignal.ProtectionDisabled,
            GuardSignal.ScreenLocked,
            GuardSignal.UserReturnedToChat,
        )
    }
}
