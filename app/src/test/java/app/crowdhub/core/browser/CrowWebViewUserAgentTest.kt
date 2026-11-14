package app.crowdhub.core.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CrowWebViewUserAgentTest {

    @Test
    fun `sanitizeUserAgent strips WebView wv and Version markers while preserving Chrome version`() {
        val raw = "Mozilla/5.0 (Linux; Android 14; Pixel 8; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.58 Mobile Safari/537.36"
        val cleaned = sanitizeUserAgent(raw)
        assertFalse(cleaned.contains("; wv"))
        assertFalse(cleaned.contains("Version/4.0"))
        assertEquals(
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.58 Mobile Safari/537.36",
            cleaned
        )
    }

    @Test
    fun `sanitizeUserAgent handles blank and already-clean UA strings`() {
        assertEquals("", sanitizeUserAgent(""))
        val clean = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        assertEquals(clean, sanitizeUserAgent(clean))
    }
}
