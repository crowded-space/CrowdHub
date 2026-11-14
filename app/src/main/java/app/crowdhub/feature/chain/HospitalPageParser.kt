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
    private val anchorTextRegex = Regex("""<a[^>]*XID=\d+[^>]*>([^<]+)</a>""", RegexOption.IGNORE_CASE)
    private val levelRegex = Regex("""(?:lvl|level)\D{0,10}(\d{1,3})""", RegexOption.IGNORE_CASE)
    private val hmsRegex = Regex("""\b(\d{1,2}):(\d{2}):(\d{2})\b""")
    private val tokenDurationRegex = Regex("""(\d+)\s*([dhms])\b""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")

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

        val rawName = anchorTextRegex.find(html)?.groupValues?.getOrNull(1)
            ?.replace(htmlTagRegex, "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Player $playerId"

        val plainText = html.replace(htmlTagRegex, " ").replace(Regex("""\s+"""), " ").trim()
        val level = levelRegex.find(plainText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val remainingSec = parseDurationSeconds(plainText)

        return ParsedHospitalRow(
            playerId = playerId,
            name = rawName,
            level = level,
            remainingSec = remainingSec
        )
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
