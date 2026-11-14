package app.crowdhub.feature.chain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive

data class ParsedHospitalRow(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val remainingSec: Long?
)

object HospitalPageParser {
    const val UNRECOGNIZED_MESSAGE = "Page layout not recognized — update TornSelectors"

    private val xidRegex = Regex("""XID=(\d+)""", RegexOption.IGNORE_CASE)
    private val profilePathRegex = Regex("""profiles\.php\?XID=(\d+)""", RegexOption.IGNORE_CASE)
    private val profileAnchorRegex = Regex(
        """<a\b([^>]*\bXID=\d+[^>]*)>([\s\S]*?)</a>""",
        RegexOption.IGNORE_CASE
    )
    private val dataPlaceholderAttrRegex = Regex(
        """\b(?:data-placeholder|aria-label|title)\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val honorTextRegex = Regex(
        """<span\b[^>]*class\s*=\s*["']([^"']*\bhonor-text\b[^"']*)["'][^>]*>([^<]+)</span>""",
        RegexOption.IGNORE_CASE
    )
    private val spanOpenTagRegex = Regex("""<span\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val spanCloseTagRegex = Regex("""</span>""", RegexOption.IGNORE_CASE)
    private val levelRegex = Regex("""(?:lvl|level)\D{0,10}(\d{1,3})""", RegexOption.IGNORE_CASE)
    private val hmsRegex = Regex("""\b(\d{1,2}):(\d{2}):(\d{2})\b""")
    private val tokenDurationRegex = Regex("""(\d+)\s*([dhms])\b""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""\s+""")

    fun parseCapturedJson(rawJsResult: String?): List<ParsedHospitalRow> {
        if (rawJsResult.isNullOrBlank() || rawJsResult == "null") return emptyList()
        val arrayElement = runCatching {
            val decoded = Json.parseToJsonElement(rawJsResult)
            if (decoded is JsonArray) {
                decoded
            } else {
                // evaluateJavascript sometimes double-encodes a JSON string
                val inner = decoded.jsonPrimitive.content
                Json.parseToJsonElement(inner) as? JsonArray
            }
        }.getOrNull() ?: return emptyList()

        return arrayElement.mapNotNull { elem ->
            val snippet = runCatching { elem.jsonPrimitive.content }.getOrNull() ?: return@mapNotNull null
            parseRowHtml(snippet)
        }.distinctBy { it.playerId }
    }

    fun parseRowHtml(html: String): ParsedHospitalRow? {
        if (html.isBlank()) return null
        val idMatch = profilePathRegex.find(html) ?: xidRegex.find(html) ?: return null
        val playerId = idMatch.groupValues[1].toLongOrNull() ?: return null

        val rawName = extractPlayerName(html, playerId) ?: "Player $playerId"

        val timeText = extractSpanText(html, "time")
        val levelText = extractSpanText(html, "level")
        val plainText by lazy { stripHtmlToText(html) }

        val level = (levelText ?: plainText).let { text ->
            levelRegex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        val remainingSec = parseDurationSeconds(timeText ?: plainText)

        return ParsedHospitalRow(
            playerId = playerId,
            name = rawName,
            level = level,
            remainingSec = remainingSec
        )
    }

    private fun extractPlayerName(html: String, playerId: Long): String? {
        val anchorMatch = profileAnchorRegex.find(html)
        if (anchorMatch != null) {
            val attrs = anchorMatch.groupValues[1]
            val innerHtml = anchorMatch.groupValues[2]

            // 1. Prefer data-placeholder / aria-label / title attribute, e.g. "NotB4-S3NS3I [2237741]"
            val attrValue = dataPlaceholderAttrRegex.find(attrs)?.groupValues?.getOrNull(1)
            val fromAttr = attrValue?.let { stripTrailingPlayerId(decodeBasicEntities(it), playerId) }
            if (!fromAttr.isNullOrBlank()) return fromAttr

            // 2. Fallback to visible <span class="honor-text">Name</span> (excluding honor-text-svg)
            val fromHonor = extractHonorTextName(innerHtml)
            if (!fromHonor.isNullOrBlank()) return fromHonor

            // 3. Fallback to stripped inner text inside the profile <a> tag
            val fromAnchorBody = stripHtmlToText(innerHtml)
                .let { stripTrailingPlayerId(it, playerId) }
            if (fromAnchorBody.isNotBlank()) return fromAnchorBody
        }

        val fallbackHonor = extractHonorTextName(html)
        if (!fallbackHonor.isNullOrBlank()) return fallbackHonor

        return null
    }

    private fun extractHonorTextName(html: String): String? {
        return honorTextRegex.findAll(html).firstNotNullOfOrNull { match ->
            val classes = match.groupValues[1].trim().split(whitespaceRegex)
            val hasHonorText = classes.any { it.equals("honor-text", ignoreCase = true) }
            val isSvgPlaceholder = classes.any { it.equals("honor-text-svg", ignoreCase = true) }
            if (hasHonorText && !isSvgPlaceholder) {
                decodeBasicEntities(match.groupValues[2]).trim().takeIf { it.isNotBlank() }
            } else {
                null
            }
        }
    }

    private fun stripTrailingPlayerId(text: String, playerId: Long): String {
        return text.trim()
            .removeSuffix("[$playerId]")
            .trim()
    }

    private fun extractSpanText(html: String, cssClass: String): String? {
        val openPattern = Regex(
            """<span\b[^>]*class\s*=\s*["'][^"']*\b${Regex.escape(cssClass)}\b[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        val startMatch = openPattern.find(html) ?: return null
        val contentStart = startMatch.range.last + 1
        var depth = 1
        var cursor = contentStart

        while (depth > 0 && cursor < html.length) {
            val nextOpen = spanOpenTagRegex.find(html, cursor)
            val nextClose = spanCloseTagRegex.find(html, cursor) ?: break
            if (nextOpen != null && nextOpen.range.first < nextClose.range.first) {
                depth++
                cursor = nextOpen.range.last + 1
            } else {
                depth--
                if (depth == 0) {
                    return stripHtmlToText(html.substring(contentStart, nextClose.range.first))
                }
                cursor = nextClose.range.last + 1
            }
        }
        return null
    }

    private fun stripHtmlToText(raw: String): String {
        return decodeBasicEntities(raw.replace(htmlTagRegex, " "))
            .replace(whitespaceRegex, " ")
            .trim()
    }

    private fun decodeBasicEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    fun parseDurationSeconds(text: String): Long? {
        hmsRegex.find(text)?.let { match ->
            val h = match.groupValues[1].toLongOrNull() ?: 0L
            val m = match.groupValues[2].toLongOrNull() ?: 0L
            val s = match.groupValues[3].toLongOrNull() ?: 0L
            return h * 3600L + m * 60L + s
        }

        val matches = tokenDurationRegex.findAll(text).toList()
        if (matches.isEmpty()) return null
        var total = 0L
        for (m in matches) {
            val amount = m.groupValues[1].toLongOrNull() ?: continue
            when (m.groupValues[2].lowercase()) {
                "d" -> total += amount * 86_400L
                "h" -> total += amount * 3_600L
                "m" -> total += amount * 60L
                "s" -> total += amount
            }
        }
        return if (total > 0L) total else null
    }
}
