package app.crowdhub.core.network

import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet

object CrowLog {
    private const val TAG = "CrowdHub"
    private val activeSecrets = CopyOnWriteArraySet<String>()

    private val queryKeyRegex = Regex("([?&]key=)([^&\\s]+)", RegexOption.IGNORE_CASE)
    private val authHeaderRegex = Regex("(Authorization\\s*[:=]\\s*(?:ApiKey|Bearer)?\\s*)([^\\s,;]+)", RegexOption.IGNORE_CASE)
    private val passwordFieldRegex = Regex("(\"password\"\\s*:\\s*\")([^\"]+)(\")", RegexOption.IGNORE_CASE)

    fun registerSecret(secret: String) {
        val trimmed = secret.trim()
        if (trimmed.length >= 4) {
            activeSecrets.add(trimmed)
        }
    }

    fun unregisterSecret(secret: String) {
        activeSecrets.remove(secret.trim())
    }

    fun clearSecrets() {
        activeSecrets.clear()
    }

    fun redact(input: String): String {
        var sanitized = input
            .replace(queryKeyRegex, "$1[REDACTED]")
            .replace(authHeaderRegex, "$1[REDACTED]")
            .replace(passwordFieldRegex, "$1[REDACTED]$3")

        for (secret in activeSecrets) {
            if (secret.isNotEmpty() && sanitized.contains(secret)) {
                sanitized = sanitized.replace(secret, "[REDACTED]")
            }
        }
        return sanitized
    }

    fun d(tag: String = TAG, message: String) {
        runCatching { Log.d(tag, redact(message)) }
    }

    fun i(tag: String = TAG, message: String) {
        runCatching { Log.i(tag, redact(message)) }
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        val safeMsg = redact(message)
        val safeThrow = throwable?.message?.let { " | ${redact(it)}" }.orEmpty()
        runCatching { Log.w(tag, safeMsg + safeThrow) }
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        val safeMsg = redact(message)
        val safeThrow = throwable?.message?.let { " | ${redact(it)}" }.orEmpty()
        runCatching { Log.e(tag, safeMsg + safeThrow) }
    }
}
