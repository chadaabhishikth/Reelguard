package app.reelguard.domain

/**
 * Pure, deterministic policy for an Instagram session. Android-specific code turns
 * accessibility events into [GuardSignal] values and renders the returned command.
 *
 * A visit to a chat is never enough to permit a Reel. The only allow path is a
 * verified shared-Reel activation followed by a recognised viewer before its short,
 * single-use permission expires.
 */
class GuardStateMachine(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val dmPermissionLifetimeMs: Long = DEFAULT_DM_PERMISSION_LIFETIME_MS,
) {
    var state: GuardState = GuardState.OUTSIDE_INSTAGRAM
        private set

    private var pendingPermissionExpiresAt: Long? = null

    fun dispatch(signal: GuardSignal): GuardCommand {
        expirePendingPermissionIfNeeded()

        return when (signal) {
            GuardSignal.InstagramForeground -> enterInstagram()
            GuardSignal.NonInstagramForeground,
            GuardSignal.ServiceStopped,
            GuardSignal.ProtectionDisabled,
            GuardSignal.ScreenLocked -> resetOutsideInstagram()

            GuardSignal.VerifiedChatVisible -> {
                state = GuardState.VERIFIED_CHAT
                pendingPermissionExpiresAt = null
                GuardCommand.CLEAR_OVERLAY
            }

            GuardSignal.VerifiedSharedReelActivated -> issueDmPermissionIfInVerifiedChat()

            GuardSignal.ReelViewerRecognized -> enterRecognizedReelViewer()

            GuardSignal.GuardedReelContentChanged -> {
                if (state == GuardState.DM_REEL_GUARDED) {
                    state = GuardState.BLOCKED_REEL
                    GuardCommand.SHOW_BLOCKER
                } else {
                    GuardCommand.NO_CHANGE
                }
            }

            GuardSignal.UserReturnedToChat -> {
                state = GuardState.VERIFIED_CHAT
                pendingPermissionExpiresAt = null
                GuardCommand.CLEAR_OVERLAY
            }

            GuardSignal.UnknownInstagramScreen -> {
                // Do not grant an exception on uncertain UI. A recognised guard remains
                // visible until an explicit exit, content change, or app switch occurs.
                GuardCommand.NO_CHANGE
            }
        }
    }

    private fun enterInstagram(): GuardCommand = when (state) {
        GuardState.OUTSIDE_INSTAGRAM -> {
            state = GuardState.NORMAL_INSTAGRAM
            GuardCommand.CLEAR_OVERLAY
        }

        else -> GuardCommand.NO_CHANGE
    }

    private fun issueDmPermissionIfInVerifiedChat(): GuardCommand {
        if (state != GuardState.VERIFIED_CHAT) return GuardCommand.NO_CHANGE

        pendingPermissionExpiresAt = clock() + dmPermissionLifetimeMs
        state = GuardState.PENDING_DM_REEL
        return GuardCommand.CLEAR_OVERLAY
    }

    private fun enterRecognizedReelViewer(): GuardCommand {
        // Window/content events can repeat for the same viewer. A valid guarded
        // session remains guarded; it is never re-evaluated as a new permission use.
        when (state) {
            GuardState.DM_REEL_GUARDED -> return GuardCommand.NO_CHANGE
            GuardState.BLOCKED_REEL -> return GuardCommand.NO_CHANGE
            else -> Unit
        }

        val validPermission =
            state == GuardState.PENDING_DM_REEL &&
                pendingPermissionExpiresAt?.let { it >= clock() } == true

        pendingPermissionExpiresAt = null // one-use even if the viewer event repeats
        return if (validPermission) {
            state = GuardState.DM_REEL_GUARDED
            GuardCommand.SHOW_DM_GUARD
        } else {
            state = GuardState.BLOCKED_REEL
            GuardCommand.SHOW_BLOCKER
        }
    }

    private fun resetOutsideInstagram(): GuardCommand {
        state = GuardState.OUTSIDE_INSTAGRAM
        pendingPermissionExpiresAt = null
        return GuardCommand.CLEAR_OVERLAY
    }

    private fun expirePendingPermissionIfNeeded() {
        val expiresAt = pendingPermissionExpiresAt ?: return
        if (clock() > expiresAt) {
            pendingPermissionExpiresAt = null
            if (state == GuardState.PENDING_DM_REEL) {
                state = GuardState.NORMAL_INSTAGRAM
            }
        }
    }

    companion object {
        const val DEFAULT_DM_PERMISSION_LIFETIME_MS = 4_000L
    }
}

enum class GuardState {
    OUTSIDE_INSTAGRAM,
    NORMAL_INSTAGRAM,
    VERIFIED_CHAT,
    PENDING_DM_REEL,
    DM_REEL_GUARDED,
    BLOCKED_REEL,
}

enum class GuardSignal {
    InstagramForeground,
    NonInstagramForeground,
    VerifiedChatVisible,
    VerifiedSharedReelActivated,
    ReelViewerRecognized,
    GuardedReelContentChanged,
    UserReturnedToChat,
    UnknownInstagramScreen,
    ProtectionDisabled,
    ServiceStopped,
    ScreenLocked,
}

enum class GuardCommand {
    NO_CHANGE,
    CLEAR_OVERLAY,
    SHOW_BLOCKER,
    SHOW_DM_GUARD,
}
