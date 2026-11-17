package app.reelguard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardStateMachineTest {
    private var now = 1_000L
    private val machine = GuardStateMachine(clock = { now }, dmPermissionLifetimeMs = 500L)

    @Test
    fun `chat visit alone never authorises a Reel`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)

        val result = machine.dispatch(GuardSignal.ReelViewerRecognized)

        assertEquals(GuardCommand.SHOW_BLOCKER, result)
        assertEquals(GuardState.BLOCKED_REEL, machine.state)
    }

    @Test
    fun `verified card tap creates one guarded Reel session`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        assertEquals(GuardCommand.CLEAR_OVERLAY, machine.dispatch(GuardSignal.VerifiedSharedReelActivated))
        assertEquals(GuardState.PENDING_DM_REEL, machine.state)

        val result = machine.dispatch(GuardSignal.ReelViewerRecognized)

        assertEquals(GuardCommand.SHOW_DM_GUARD, result)
        assertEquals(GuardState.DM_REEL_GUARDED, machine.state)
    }

    @Test
    fun `permission is one use and cannot be reused by another Reel`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        machine.dispatch(GuardSignal.VerifiedSharedReelActivated)
        machine.dispatch(GuardSignal.ReelViewerRecognized)
        machine.dispatch(GuardSignal.GuardedReelContentChanged)

        val result = machine.dispatch(GuardSignal.ReelViewerRecognized)

        assertEquals(GuardCommand.NO_CHANGE, result)
        assertEquals(GuardState.BLOCKED_REEL, machine.state)
    }

    @Test
    fun `expired permission blocks the viewer`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        machine.dispatch(GuardSignal.VerifiedSharedReelActivated)
        now += 501L

        val result = machine.dispatch(GuardSignal.ReelViewerRecognized)

        assertEquals(GuardCommand.SHOW_BLOCKER, result)
        assertEquals(GuardState.BLOCKED_REEL, machine.state)
    }

    @Test
    fun `leaving Instagram clears permission and overlay`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        machine.dispatch(GuardSignal.VerifiedSharedReelActivated)

        val result = machine.dispatch(GuardSignal.NonInstagramForeground)

        assertEquals(GuardCommand.CLEAR_OVERLAY, result)
        assertEquals(GuardState.OUTSIDE_INSTAGRAM, machine.state)
        enterInstagram()
        assertEquals(GuardCommand.SHOW_BLOCKER, machine.dispatch(GuardSignal.ReelViewerRecognized))
    }

    @Test
    fun `repeated viewer events preserve the guarded session`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        machine.dispatch(GuardSignal.VerifiedSharedReelActivated)
        machine.dispatch(GuardSignal.ReelViewerRecognized)

        val result = machine.dispatch(GuardSignal.ReelViewerRecognized)

        assertEquals(GuardCommand.NO_CHANGE, result)
        assertEquals(GuardState.DM_REEL_GUARDED, machine.state)
    }

    @Test
    fun `content change revokes the guarded session`() {
        enterInstagram()
        machine.dispatch(GuardSignal.VerifiedChatVisible)
        machine.dispatch(GuardSignal.VerifiedSharedReelActivated)
        machine.dispatch(GuardSignal.ReelViewerRecognized)

        val result = machine.dispatch(GuardSignal.GuardedReelContentChanged)

        assertEquals(GuardCommand.SHOW_BLOCKER, result)
        assertEquals(GuardState.BLOCKED_REEL, machine.state)
    }

    private fun enterInstagram() {
        assertEquals(GuardCommand.CLEAR_OVERLAY, machine.dispatch(GuardSignal.InstagramForeground))
    }
}
