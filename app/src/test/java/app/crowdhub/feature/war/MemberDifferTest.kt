package app.crowdhub.feature.war

import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.provider.torn.TornFactionMember
import app.crowdhub.provider.torn.TornStatus
import app.crowdhub.provider.torn.TravelPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberDifferTest {
    private val differ = MemberDiffer()

    @Test
    fun `detects departure landing hospital exit and went online transitions`() {
        val prev = mapOf(
            1L to MemberSnapshotRecord(
                playerId = 1L,
                name = "Duke",
                state = StatusKind.OKAY,
                description = "Okay",
                untilSec = null,
                planeType = null,
                lastActionStatus = "Offline",
                tsMs = 1_000_000L
            ),
            2L to MemberSnapshotRecord(
                playerId = 2L,
                name = "Tiny",
                state = StatusKind.TRAVELING,
                description = "Traveling to Japan",
                untilSec = null,
                planeType = "wlt",
                lastActionStatus = "Online",
                tsMs = 1_000_000L
            )
        )

        val current = listOf(
            TornFactionMember(
                playerId = 1L,
                name = "Duke",
                level = 85,
                status = TornStatus(StatusKind.TRAVELING, "Traveling", "Traveling to Mexico", null, "airstrip"),
                lastActionStatus = "Online",
                lastActionRelative = "0 minutes ago",
                lastActionTimestampSec = 1030L
            ),
            TornFactionMember(
                playerId = 2L,
                name = "Tiny",
                level = 50,
                status = TornStatus(StatusKind.ABROAD, "Abroad", "In Japan", null, null),
                lastActionStatus = "Online",
                lastActionRelative = "1 minute ago",
                lastActionTimestampSec = 1020L
            )
        )

        val events = differ.diff(prev, current, prevPollAtMs = 1_000_000L, nowMs = 1_030_000L)

        val travelStart = events.filterIsInstance<MemberEvent.TravelStarted>().firstOrNull()
        assertTrue(travelStart != null && travelStart.playerId == 1L)
        assertEquals(TravelPlace.MEXICO, travelStart!!.place)
        assertTrue(travelStart.departureObserved)

        val landed = events.filterIsInstance<MemberEvent.Landed>().firstOrNull()
        assertTrue(landed != null && landed.playerId == 2L)
        assertEquals(TravelPlace.JAPAN, landed!!.place)

        val wentOnline = events.filterIsInstance<MemberEvent.WentOnline>().firstOrNull()
        assertTrue(wentOnline != null && wentOnline.playerId == 1L)
    }
}
