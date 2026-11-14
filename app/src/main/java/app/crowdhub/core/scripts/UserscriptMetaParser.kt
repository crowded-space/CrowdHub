package app.crowdhub.core.scripts

import android.net.Uri

enum class RunAt {
    DOCUMENT_START,
    DOCUMENT_END,
    DOCUMENT_IDLE;

    companion object {
        fun fromRaw(raw: String?): RunAt {
            return when (raw?.trim()?.lowercase()) {
                "document-start" -> DOCUMENT_START
                "document-idle" -> DOCUMENT_IDLE
                else -> DOCUMENT_END
            }
        }
    }
}

data class ParsedUserscriptMeta(
    val name: String,
    val namespace: String?,
    val version: String?,
    val description: String?,
    val matches: List<String>,
    val excludes: List<String>,
    val runAt: RunAt,
    val grants: List<String>,
    val connects: List<String>,
    val warnings: List<String>
)

object UserscriptMetaParser {
    private val headerStartRegex = Regex("//\\s*==UserScript==")
    private val headerEndRegex = Regex("//\\s*==/UserScript==")
    private val metaLineRegex = Regex("^\\s*//\\s*@([A-Za-z0-9_-]+)\\s*(.*)$")

    fun parse(source: String, allowedHosts: Set<String> = emptySet()): ParsedUserscriptMeta {
        val lines = source.lines()
        var inBlock = false
        var name: String? = null
        var namespace: String? = null
        var version: String? = null
        var description: String? = null
        var runAt = RunAt.DOCUMENT_END
        val matches = mutableListOf<String>()
        val excludes = mutableListOf<String>()
        val grants = mutableListOf<String>()
        val connects = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (line in lines) {
            if (!inBlock) {
                if (headerStartRegex.containsMatchIn(line)) {
                    inBlock = true
                }
                continue
            }
            if (headerEndRegex.containsMatchIn(line)) {
                break
            }
            val match = metaLineRegex.find(line) ?: continue
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].trim()
            if (value.isEmpty()) continue

            when (key) {
                "name" -> if (name == null) name = value
                "namespace" -> if (namespace == null) namespace = value
                "version" -> if (version == null) version = value
                "description" -> if (description == null) description = value
                "run-at" -> runAt = RunAt.fromRaw(value)
                "match", "include" -> {
                    if (allowedHosts.isEmpty() || isMatchPatternAllowed(value, allowedHosts)) {
                        matches += value
                    } else {
                        warnings += "Ignored @match outside allowed hosts: $value"
                    }
                }
                "exclude" -> excludes += value
                "grant" -> grants += value
                "connect" -> connects += value.lowercase()
                "require" -> warnings += "@require is not supported ($value); script may fail."
                "resource" -> warnings += "@resource is not supported ($value); script may fail."
            }
        }

        if (name == null) {
            warnings += "Missing @name; defaulted to 'Unnamed Script'."
        }
        if (matches.isEmpty()) {
            warnings += "No valid @match patterns found; script will not run on any page."
        }

        return ParsedUserscriptMeta(
            name = name ?: "Unnamed Script",
            namespace = namespace,
            version = version,
            description = description,
            matches = matches.distinct(),
            excludes = excludes.distinct(),
            runAt = runAt,
            grants = grants.distinct(),
            connects = connects.distinct(),
            warnings = warnings
        )
    }

    fun matchesUrl(
        url: String,
        matches: List<String>,
        excludes: List<String>
    ): Boolean {
        if (matches.isEmpty()) return false
        val excluded = excludes.any { patternMatchesUrl(it, url) }
        if (excluded) return false
        return matches.any { patternMatchesUrl(it, url) }
    }

    fun patternMatchesUrl(pattern: String, url: String): Boolean {
        val trimmed = pattern.trim()
        if (trimmed == "*" || trimmed == "<all_urls>") return true
        val regexStr = buildString {
            append("^")
            for (ch in trimmed) {
                when (ch) {
                    '*' -> append(".*")
                    '?', '.', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|', '\\' -> {
                        append("\\")
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
            append("$")
        }
        return runCatching { Regex(regexStr, RegexOption.IGNORE_CASE).matches(url) }
            .getOrDefault(false)
    }

    fun isMatchPatternAllowed(pattern: String, allowedHosts: Set<String>): Boolean {
        val clean = pattern.trim()
        val afterScheme = clean.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return false
        val hostPart = afterScheme.substringBefore("/").substringBefore(":").lowercase()
        if (hostPart.isEmpty() || hostPart == "*") return false
        return allowedHosts.any { allowed ->
            val lowerAllowed = allowed.lowercase()
            hostPart == lowerAllowed ||
                (hostPart.startsWith("*.") && lowerAllowed.endsWith(hostPart.removePrefix("*"))) ||
                (hostPart.startsWith("*.") && lowerAllowed == hostPart.removePrefix("*."))
        }
    }

    fun extractHostForTest(url: String): String? {
        return runCatching { Uri.parse(url).host?.lowercase() }.getOrNull()
    }
}
