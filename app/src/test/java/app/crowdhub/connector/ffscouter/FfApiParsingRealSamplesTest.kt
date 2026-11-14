package app.crowdhub.connector.ffscouter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FfApiParsingRealSamplesTest {

    private fun loadSample(name: String): String {
        val candidates = listOf(
            File("crowd-hub-spec/03_providers/torn/_samples/$name"),
            File("../crowd-hub-spec/03_providers/torn/_samples/$name")
        )
        return candidates.first { it.exists() }.readText()
    }

    @Test
    fun `parses real ffscouter-get-stats top-level array including large bs_estimate`() {
        val nowMs = 1_794_668_700_000L
        val stats = FfApi.parseStatsResponse(loadSample("ffscouter-get-stats.json"), nowMs)
        assertEquals(3, stats.size)

        val first = stats[0]
        assertEquals(3962657L, first.playerId)
        assertEquals(3.54, first.fairFight!!, 0.001)
        assertEquals(321502L, first.bsEstimate)
        assertEquals("322k", first.bsHuman)
        assertEquals(1794438879L, first.lastUpdatedSec)

        val third = stats[2]
        assertEquals(1426381L, third.playerId)
        assertEquals(9391434787L, third.bsEstimate)
        assertEquals("9.39b", third.bsHuman)
    }

    @Test
    fun `parses real ffscouter-get-targets wrapper including last_action and bss_public_timestamp`() {
        val targets = FfApi.parseTargetsResponse(loadSample("ffscouter-get-targets.json"))
        assertEquals(5, targets.size)

        val first = targets.first()
        assertEquals(4154099L, first.playerId)
        assertEquals("Jenpine", first.name)
        assertEquals(29, first.level)
        assertEquals(5.43, first.fairFight!!, 0.001)
        assertEquals(973499L, first.bsEstimate)
        assertEquals("973k", first.bsHuman)
        assertEquals(1793547164L, first.lastUpdatedSec)
        assertEquals(1794559999L, first.lastActionSec)
    }
}
