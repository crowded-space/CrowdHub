package app.crowdhub.feature.chain

import app.crowdhub.core.ui.components.StatusKind
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetFiltersTest {
    @Test
    fun `filters by fair fight status leaving window and sorts correctly`() {
        val nowMs = 1_000_000_000L
        val nowSec = nowMs / 1_000L

        val items = listOf(
            TargetItem(
                playerId = 1,
                name = "Alpha",
                level = 50,
                status = StatusKind.HOSPITAL,
                hospitalLeavesAtSec = nowSec + 180L,
                lastActionSec = nowSec - 600L,
                fairFight = 2.5,
                bsEstimate = 100_000L,
                bsHuman = "100k",
                note = null,
                watched = false,
                source = TargetSourceTab.HOSPITAL
            ),
            TargetItem(
                playerId = 2,
                name = "Bravo",
                level = 70,
                status = StatusKind.HOSPITAL,
                hospitalLeavesAtSec = nowSec + 60L,
                lastActionSec = nowSec - 10_000L,
                fairFight = 3.2,
                bsEstimate = 50_000L,
                bsHuman = "50k",
                note = null,
                watched = false,
                source = TargetSourceTab.HOSPITAL
            ),
            TargetItem(
                playerId = 3,
                name = "Charlie",
                level = 40,
                status = StatusKind.OKAY,
                hospitalLeavesAtSec = null,
                lastActionSec = nowSec - 100L,
                fairFight = 1.4,
                bsEstimate = 10_000L,
                bsHuman = "10k",
                note = null,
                watched = false,
                source = TargetSourceTab.MY_LIST
            )
        )

        val leavingSoon = TargetFilters.apply(
            items = items,
            criteria = TargetFilterCriteria(minFf = 2.0, leavingWithinMin = 5),
            sortMode = TargetSortMode.LEAVES_SOONEST,
            nowMs = nowMs
        )
        assertEquals(listOf(2L, 1L), leavingSoon.map { it.playerId })

        val byBs = TargetFilters.apply(
            items = items,
            criteria = TargetFilterCriteria(),
            sortMode = TargetSortMode.BS_ASC,
            nowMs = nowMs
        )
        assertEquals(listOf(3L, 2L, 1L), byBs.map { it.playerId })
    }
}
