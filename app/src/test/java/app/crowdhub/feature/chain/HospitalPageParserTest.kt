package app.crowdhub.feature.chain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HospitalPageParserTest {
    @Test
    fun `parses captured row HTML array with player id name level and duration`() {
        val raw = """
            [
              "<li><a href='profiles.php?XID=2114440'>Duke</a> Level 84 Hospitalized for 01:23:45</li>",
              "<li><a href='/profiles.php?XID=987654'>Jimmy</a> lvl 45 2h 15m remaining</li>"
            ]
        """.trimIndent()

        val rows = HospitalPageParser.parseCapturedJson(raw)
        assertEquals(2, rows.size)

        assertEquals(2114440L, rows[0].playerId)
        assertEquals("Duke", rows[0].name)
        assertEquals(84, rows[0].level)
        assertEquals(1 * 3600L + 23 * 60L + 45L, rows[0].remainingSec)

        assertEquals(987654L, rows[1].playerId)
        assertEquals("Jimmy", rows[1].name)
        assertEquals(45, rows[1].level)
        assertEquals(2 * 3600L + 15 * 60L, rows[1].remainingSec)
    }

    @Test
    fun `returns empty list for unrecognized layout`() {
        assertTrue(HospitalPageParser.parseCapturedJson("[]").isEmpty())
        assertTrue(HospitalPageParser.parseCapturedJson("[\"<div>no player link</div>\"]").isEmpty())
        assertNull(HospitalPageParser.parseDurationSeconds("Just Okay"))
    }
}
