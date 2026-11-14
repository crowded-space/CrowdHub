package app.crowdhub.provider

import app.crowdhub.core.alerts.AlertSource
import app.crowdhub.core.network.ApiError

data class RateLimitPolicy(
    val globalCapPerMin: Int,
    val cacheTtlMs: Long
)

sealed class ValidationResult {
    data class Valid(
        val externalId: String,
        val displayName: String,
        val accessLevel: String
    ) : ValidationResult()

    data class Invalid(val error: ApiError) : ValidationResult()
}

interface Provider {
    val id: String
    val displayName: String
    val allowedWebHosts: Set<String>
    val rateLimit: RateLimitPolicy
    suspend fun validateCredential(accountId: String, secret: String): ValidationResult
    fun isLoginUrl(url: String): Boolean
    fun alertSources(): List<AlertSource>
}
