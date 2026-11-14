package app.crowdhub.feature.chain

import app.crowdhub.provider.torn.TornSelectors
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HospitalPageParserTest {

    private fun loadSample(name: String): String {
        val candidates = listOf(
            File("crowd-hub-spec/03_providers/torn/_samples/$name"),
            File("../crowd-hub-spec/03_providers/torn/_samples/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
        if (file != null) return file.readText()
        throw IllegalStateException("Could not locate sample $name from ${File(".").absolutePath}")
    }

    private fun extractTopLevelListItems(html: String): List<String> {
        val cleaned = html.replace(Regex("""<!--[\s\S]*?-->"""), "")
        val openRegex = Regex("""<li\b[^>]*>""", RegexOption.IGNORE_CASE)
        val closeRegex = Regex("""</li>""", RegexOption.IGNORE_CASE)
        val items = mutableListOf<String>()
        var cursor = 0
        while (cursor < cleaned.length) {
            val firstOpen = openRegex.find(cleaned, cursor) ?: break
            var depth = 1
            var scan = firstOpen.range.last + 1
            while (depth > 0 && scan < cleaned.length) {
                val nextOpen = openRegex.find(cleaned, scan)
                val nextClose = closeRegex.find(cleaned, scan) ?: break
                if (nextOpen != null && nextOpen.range.first < nextClose.range.first) {
                    depth++
                    scan = nextOpen.range.last + 1
                } else {
                    depth--
                    scan = nextClose.range.last + 1
                    if (depth == 0) {
                        items += cleaned.substring(firstOpen.range.first, nextClose.range.last + 1)
                    }
                }
            }
            cursor = scan
        }
        return items
    }

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
    fun `parses real captured hospital-page-rows HTML sample accurately`() {
        val sampleHtml = loadSample("hospital-page-rows.html")
        val rowSnippets = extractTopLevelListItems(sampleHtml)
        assertEquals(3, rowSnippets.size)

        val jsonPayload = Json.encodeToString(rowSnippets)
        val rows = HospitalPageParser.parseCapturedJson(jsonPayload)
        assertEquals(3, rows.size)

        assertEquals(2237741L, rows[0].playerId)
        assertEquals("NotB4-S3NS3I", rows[0].name)
        assertEquals(62, rows[0].level)
        assertEquals(83 * 3600L + 17 * 60L, rows[0].remainingSec)

        assertEquals(4347309L, rows[1].playerId)
        assertEquals("Niel4740", rows[1].name)
        assertEquals(25, rows[1].level)
        assertEquals(83 * 3600L + 7 * 60L, rows[1].remainingSec)

        assertEquals(4587575L, rows[2].playerId)
        assertEquals("Extracto", rows[2].name)
        assertEquals(13, rows[2].level)
        assertEquals(83 * 3600L + 7 * 60L, rows[2].remainingSec)
    }

    @Test
    fun `parses nested honor-text name even without data-placeholder attribute`() {
        val rowHtml = """
            <li class="gray ">
              <span class="user-wrap user-name">
                <a class="user name" href="/profiles.php?XID=123456">
                  <div class="honor-text-wrap"><span class="honor-text honor-text-svg">X</span><span class="honor-text">CrowHero</span></div>
                </a>
              </span>
              <span class="info-wrap">
                <span class="time"><span class="title bold">Time left:</span> 12m</span>
                <span class="level"><span class="title bold">Level:</span> 99</span>
                <span class="reason"><span class="bold">Reason: </span>Hit by 5s stun</span>
              </span>
            </li>
        """.trimIndent()

        val row = HospitalPageParser.parseRowHtml(rowHtml)!!
        assertEquals(123456L, row.playerId)
        assertEquals("CrowHero", row.name)
        assertEquals(99, row.level)
        assertEquals(12 * 60L, row.remainingSec)
    }

    @Test
    fun `real login-page HTML matches TornSelectors primary selectors`() {
        val loginHtml = loadSample("login-page.html")
        assertTrue(loginHtml.contains("""action="/page.php?sid=Auth""""))
        assertTrue(loginHtml.contains("""name="email""""))
        assertTrue(loginHtml.contains("""name="password""""))
        assertTrue(loginHtml.contains("""name="btnLogin""""))

        assertEquals("input[name='email']", TornSelectors.LOGIN_USERNAME_SELECTORS.first())
        assertEquals("input[name='password']", TornSelectors.LOGIN_PASSWORD_SELECTORS.first())
        assertEquals("input[name='btnLogin']", TornSelectors.LOGIN_SUBMIT_SELECTORS.first())

        val js = TornSelectors.buildSafeLoginFillJs("user@example.com", "secret", autoSubmit = false)
        assertTrue(js.contains("input[name='email']"))
        assertTrue(js.contains("input[name='password']"))
        assertTrue(js.contains("input[name='btnLogin']"))
    }

    @Test
    fun `returns empty list for unrecognized layout`() {
        assertTrue(HospitalPageParser.parseCapturedJson("[]").isEmpty())
        assertTrue(HospitalPageParser.parseCapturedJson("[\"<div>no player link</div>\"]").isEmpty())
        assertNull(HospitalPageParser.parseDurationSeconds("Just Okay"))
    }
}
