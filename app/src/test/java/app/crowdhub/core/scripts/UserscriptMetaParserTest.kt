package app.crowdhub.core.scripts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserscriptMetaParserTest {
    private val allowed = setOf("www.torn.com", "torn.com")

    @Test
    fun `parses userscript header filters hosts and warns on unsupported directives`() {
        val script = """
            // ==UserScript==
            // @name         Crow War Helper
            // @namespace    crow
            // @version      1.2.3
            // @description  Highlights targets
            // @match        https://www.torn.com/factions.php*
            // @match        https://evil.example.org/*
            // @exclude      https://www.torn.com/forums.php*
            // @run-at       document-start
            // @grant        GM_xmlhttpRequest
            // @grant        GM_getValue
            // @connect      ffscouter.com
            // @require      https://cdn.example.com/lib.js
            // ==/UserScript==
            console.log('hi');
        """.trimIndent()

        val meta = UserscriptMetaParser.parse(script, allowed)
        assertEquals("Crow War Helper", meta.name)
        assertEquals("1.2.3", meta.version)
        assertEquals(RunAt.DOCUMENT_START, meta.runAt)
        assertEquals(listOf("https://www.torn.com/factions.php*"), meta.matches)
        assertEquals(listOf("https://www.torn.com/forums.php*"), meta.excludes)
        assertEquals(listOf("ffscouter.com"), meta.connects)
        assertTrue(meta.warnings.any { it.contains("evil.example.org") })
        assertTrue(meta.warnings.any { it.contains("@require") })

        assertTrue(UserscriptMetaParser.matchesUrl("https://www.torn.com/factions.php?step=your", meta.matches, meta.excludes))
        assertFalse(UserscriptMetaParser.matchesUrl("https://www.torn.com/forums.php", meta.matches, meta.excludes))
    }
}
