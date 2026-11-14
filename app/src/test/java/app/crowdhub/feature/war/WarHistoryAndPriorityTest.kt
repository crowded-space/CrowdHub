package app.crowdhub.feature.war

import app.crowdhub.provider.torn.TornMappers
import app.crowdhub.provider.torn.TornRankedWar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WarHistoryAndPriorityTest {

    private fun loadSample(name: String): String {
        val candidates = listOf(
            File("crowd-hub-spec/03_providers/torn/_samples/$name"),
            File("../crowd-hub-spec/03_providers/torn/_samples/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
        if (file != null) return file.readText()
        throw IllegalStateException("Could not locate sample $name from ${File(".").absolutePath}")
    }

    private fun sampleWar(
        warId: Long,
        enemyId: Long,
        enemyName: String,
        isActive: Boolean,
        isScheduled: Boolean = false,
        end: Long? = if (isActive) null else 1_700_100_000L,
        start: Long = (end ?: 1_700_000_500L) - 500L,
        winner: Long? = if (isActive) null else 9036L
    ) = TornRankedWar(
        warId = warId,
        startEpochSec = start,
        endEpochSec = end,
        targetScore = 10_000,
        winnerFactionId = winner,
        ownFactionId = 9036L,
        ownFactionName = "The Black Pearl",
        enemyFactionId = enemyId,
        enemyFactionName = enemyName,
        isActive = isActive,
        ownScore = 12_000,
        enemyScore = 9_500,
        isScheduled = isScheduled
    )

    @Test
    fun `prioritizes active war over upcoming and ended wars`() {
        val wars = listOf(
            sampleWar(warId = 10L, enemyId = 100L, enemyName = "Ended Foe", isActive = false, end = 1_795_000_000L),
            sampleWar(warId = 11L, enemyId = 200L, enemyName = "Upcoming Foe", isActive = true, isScheduled = true, start = 1_800_000_000L),
            sampleWar(warId = 12L, enemyId = 300L, enemyName = "Active Foe", isActive = true, isScheduled = false, start = 1_794_000_000L)
        )

        val status = resolveCurrentWarStatus(wars = wars, ownFactionId = 9036L, manualFactionId = null)
        assertTrue(status is CurrentWarStatus.Active)
        assertEquals(300L, (status as CurrentWarStatus.Active).war.enemyFactionId)
    }

    @Test
    fun `shows upcoming war if no war has started yet`() {
        val wars = listOf(
            sampleWar(warId = 10L, enemyId = 100L, enemyName = "Ended Foe", isActive = false),
            sampleWar(warId = 11L, enemyId = 200L, enemyName = "Upcoming Foe", isActive = true, isScheduled = true)
        )

        val status = resolveCurrentWarStatus(wars = wars, ownFactionId = 9036L, manualFactionId = null)
        assertTrue(status is CurrentWarStatus.Upcoming)
        assertEquals("Upcoming Foe", (status as CurrentWarStatus.Upcoming).war.enemyFactionName)
    }

    @Test
    fun `returns NotInWar when only ended wars exist`() {
        val wars = listOf(
            sampleWar(warId = 10L, enemyId = 100L, enemyName = "Ended Foe", isActive = false)
        )

        val status = resolveCurrentWarStatus(wars = wars, ownFactionId = 9036L, manualFactionId = null)
        assertTrue(status is CurrentWarStatus.NotInWar)
        assertEquals(9036L, (status as CurrentWarStatus.NotInWar).ownFactionId)
    }

    @Test
    fun `groups ended wars by opponent faction newest-first without duplicates`() {
        val wars = listOf(
            sampleWar(warId = 1L, enemyId = 500L, enemyName = "Alpha", isActive = false, end = 1000L),
            sampleWar(warId = 2L, enemyId = 600L, enemyName = "Beta", isActive = false, end = 3000L),
            sampleWar(warId = 3L, enemyId = 500L, enemyName = "Alpha", isActive = false, end = 4000L),
            sampleWar(warId = 4L, enemyId = 700L, enemyName = "Ongoing", isActive = true, end = null)
        )

        val grouped = groupEndedWarsByOpponent(wars)
        assertEquals(2, grouped.size)

        // Alpha's latest war is at end=4000, Beta's at end=3000, Ongoing excluded
        assertEquals(500L, grouped[0].enemyFactionId)
        assertEquals("Alpha", grouped[0].enemyFactionName)
        assertEquals(listOf(3L, 1L), grouped[0].wars.map { it.warId })

        assertEquals(600L, grouped[1].enemyFactionId)
        assertEquals(listOf(2L), grouped[1].wars.map { it.warId })
    }

    @Test
    fun `groups real faction-rankedwars sample by unique opponent factions`() {
        val wars = TornMappers.mapRankedWars(
            rawJson = loadSample("faction-rankedwars.json"),
            ownFactionId = 9036L,
            nowSec = 1794668700L
        )
        assertEquals(100, wars.size)
        assertFalse(wars.any { it.isActive })

        val status = resolveCurrentWarStatus(wars = wars, ownFactionId = 9036L, manualFactionId = null)
        assertTrue(status is CurrentWarStatus.NotInWar)

        val grouped = groupEndedWarsByOpponent(wars)
        assertTrue(grouped.isNotEmpty())
        assertEquals(grouped.size, grouped.map { it.enemyFactionId }.distinct().size)
        assertEquals(100, grouped.sumOf { it.wars.size })
        assertEquals("Dead Fragment", grouped.first().enemyFactionName)
        assertEquals(22300, grouped.first().wars.first().ownScore)
        assertEquals(13399, grouped.first().wars.first().enemyScore)
    }
}
