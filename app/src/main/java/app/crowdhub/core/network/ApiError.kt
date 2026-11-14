package app.crowdhub.core.network

sealed class ApiError(open val message: String) {
    data class Network(override val message: String = "Network unavailable") : ApiError(message)
    data class Timeout(override val message: String = "Request timed out") : ApiError(message)
    data class InvalidKey(override val message: String = "API key is invalid or disabled") : ApiError(message)
    data class KeyPaused(override val message: String = "API key is paused or owner in federal jail") : ApiError(message)
    data class AccessLevel(override val message: String = "Key access level too low; create a Limited key") : ApiError(message)
    data class RateLimited(override val message: String = "Rate limit exceeded; waiting") : ApiError(message)
    data class IpBlocked(override val message: String = "IP temporarily blocked; paused 5 minutes") : ApiError(message)
    data class ProviderDown(override val message: String = "Service temporarily unavailable") : ApiError(message)
    data class Parse(override val message: String = "Failed to parse response") : ApiError(message)
    data class Unknown(val code: Int?, override val message: String) : ApiError(message)

    fun isPermanentKeyFailure(): Boolean = this is InvalidKey || this is KeyPaused || this is AccessLevel
}
