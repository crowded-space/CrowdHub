package app.crowdhub.core.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDeduperTest {
    private val deduper = AlertDeduper()

    @Test
    fun `reschedules only when drift exceeds 10s and cancels vanished conditions`() {
        val existing = listOf(
            PersistedAlertRecord(
                dedupeKey = "torn:bar:energy:a1",
                accountId = "a1",
                channelId = NotificationChannels.TIMERS,
                title = "Energy full",
                body = "Energy reached 150/150",
                fireAt = 200_000L,
                deepLink = "dashboard",
                state = PersistedAlertState.SCHEDULED
            ),
            PersistedAlertRecord(
                dedupeKey = "torn:cooldown:drug:a1",
                accountId = "a1",
                channelId = NotificationChannels.TIMERS,
                title = "Drug cooldown ready",
                body = "Expired",
                fireAt = 300_000L,
                deepLink = "dashboard",
                state = PersistedAlertState.SCHEDULED
            )
        )

        val candidates = listOf(
            // Drift of 4s -> unchanged -> no reschedule action
            AlertCandidate(
                dedupeKey = "torn:bar:energy:a1",
                accountId = "a1",
                channelId = NotificationChannels.TIMERS,
                title = "Energy full",
                body = "Energy reached 150/150",
                fireAtEpochMs = 204_000L,
                deepLink = "dashboard"
            )
            // cooldown drug vanished -> should be cancelled
        )

        val actions = deduper.reconcile(
            accountId = "a1",
            managedKeyPrefixes = setOf("torn:bar:", "torn:cooldown:"),
            candidates = candidates,
            existing = existing,
            nowMs = 100_000L
        )
        assertEquals(1, actions.size)
        assertTrue(actions[0] is DedupeAction.Cancel)
        assertEquals("torn:cooldown:drug:a1", (actions[0] as DedupeAction.Cancel).dedupeKey)

        // Drift of 20s -> triggers Schedule action
        val drifted = deduper.reconcile(
            accountId = "a1",
            managedKeyPrefixes = setOf("torn:bar:"),
            candidates = listOf(candidates[0].copy(fireAtEpochMs = 225_000L)),
            existing = existing.take(1),
            nowMs = 100_000L
        )
        assertEquals(1, drifted.size)
        assertTrue(drifted[0] is DedupeAction.Schedule)
    }
}
