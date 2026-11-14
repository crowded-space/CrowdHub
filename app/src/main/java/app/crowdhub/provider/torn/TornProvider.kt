package app.crowdhub.provider.torn

import app.crowdhub.core.alerts.AlertSource
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.RateLimitConfig
import app.crowdhub.core.network.ResponseCache
import app.crowdhub.provider.Provider
import app.crowdhub.provider.RateLimitPolicy
import app.crowdhub.provider.ValidationResult

class TornProvider(
    private val api: TornApi,
    private val sources: List<AlertSource>
) : Provider {
    override val id: String = PROVIDER_ID
    override val displayName: String = "Torn"
    override val allowedWebHosts: Set<String> = TornUrls.ALLOWED_WEB_HOSTS
    override val rateLimit: RateLimitPolicy = RateLimitPolicy(
        globalCapPerMin = RateLimitConfig.GLOBAL_CAP_PER_MIN,
        cacheTtlMs = ResponseCache.DEFAULT_TTL_MS
    )

    override suspend fun validateCredential(accountId: String, secret: String): ValidationResult {
        return when (val result = api.validateKey(accountId, secret)) {
            is ApiResult.Success -> ValidationResult.Valid(
                externalId = result.data.playerId.toString(),
                displayName = result.data.name,
                accessLevel = "Limited"
            )
            is ApiResult.Failure -> ValidationResult.Invalid(result.error)
        }
    }

    override fun isLoginUrl(url: String): Boolean = TornUrls.isLoginUrl(url)

    override fun alertSources(): List<AlertSource> = sources

    companion object {
        const val PROVIDER_ID = "torn"
    }
}
