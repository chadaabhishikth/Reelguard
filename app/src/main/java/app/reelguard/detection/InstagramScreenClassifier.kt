package app.reelguard.detection

import android.view.accessibility.AccessibilityNodeInfo
import app.reelguard.domain.GuardSignal

/**
 * Converts an Instagram accessibility tree into a small set of policy signals.
 *
 * This class never reads [AccessibilityNodeInfo.text] or content descriptions, so
 * message bodies, captions, and usernames are not used for detection. A profile must
 * be explicitly verified before any tree is inspected; the starter profile is empty.
 */
class InstagramScreenClassifier(
    private val profile: CompatibilityProfile,
) {
    fun isGeneralBlockingReady(): Boolean = profile.isGeneralBlockingVerified

    fun isDmExceptionReady(): Boolean = profile.isDmExceptionVerified

    fun classify(
        root: AccessibilityNodeInfo?,
        clickedNode: AccessibilityNodeInfo?,
        eventType: Int,
        dmModeEnabled: Boolean,
    ): List<GuardSignal> {
        if (!profile.isGeneralBlockingVerified || root == null) {
            return listOf(GuardSignal.UnknownInstagramScreen)
        }

        val resourceIds = mutableSetOf<String>()
        collectResourceIds(root, resourceIds)
        val dmExceptionAllowed = dmModeEnabled && profile.isDmExceptionVerified
        val sharedReelWasExplicitlyTapped =
            dmExceptionAllowed &&
                eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED &&
                (clickedNode?.viewIdResourceName?.let(profile.sharedReelActivationResourceIds::contains) == true)

        return buildList {
            if (dmExceptionAllowed && resourceIds.any(profile.chatRootResourceIds::contains)) {
                add(GuardSignal.VerifiedChatVisible)
            }
            // A card merely being visible is never enough to issue a permission.
            if (sharedReelWasExplicitlyTapped) {
                add(GuardSignal.VerifiedSharedReelActivated)
            }
            if (resourceIds.any(profile.reelViewerResourceIds::contains)) {
                add(GuardSignal.ReelViewerRecognized)
            }
        }.ifEmpty { listOf(GuardSignal.UnknownInstagramScreen) }
    }

    /**
     * An opaque node identifier is compared only in memory for a profile whose
     * content-change behaviour has passed the phone test. It is never logged or
     * persisted. The profile must not be marked verified unless this signal changes
     * reliably when Instagram moves to another Reel.
     */
    fun contentFingerprint(root: AccessibilityNodeInfo?): String? {
        if (!profile.isDmExceptionVerified || root == null) return null
        return findContentFingerprint(root)
    }

    private fun collectResourceIds(node: AccessibilityNodeInfo, output: MutableSet<String>) {
        node.viewIdResourceName?.let(output::add)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectResourceIds(child, output)
                child.recycle()
            }
        }
    }

    private fun findContentFingerprint(node: AccessibilityNodeInfo): String? {
        if ((node.viewIdResourceName?.let(profile.contentMarkerResourceIds::contains) == true) &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return node.uniqueId
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val fingerprint = findContentFingerprint(child)
            child.recycle()
            if (fingerprint != null) return fingerprint
        }
        return null
    }
}
