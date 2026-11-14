package app.crowdhub.connector.ffscouter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FfStatItemDto(
    @SerialName("player_id") val playerId: Long? = null,
    val id: Long? = null,
    @SerialName("fair_fight") val fairFight: Double? = null,
    @SerialName("bs_estimate") val bsEstimate: Long? = null,
    @SerialName("bs_estimate_human") val bsHuman: String? = null,
    @SerialName("last_updated") val lastUpdated: Long? = null,
    @SerialName("bss_public") val bssPublic: Long? = null
)

@Serializable
data class FfStatsWrapperDto(
    val stats: List<FfStatItemDto>? = null,
    val data: List<FfStatItemDto>? = null,
    val error: String? = null
)

@Serializable
data class FfTargetItemDto(
    @SerialName("player_id") val playerId: Long? = null,
    val id: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    @SerialName("fair_fight") val fairFight: Double? = null,
    @SerialName("bs_estimate") val bsEstimate: Long? = null,
    @SerialName("bs_estimate_human") val bsHuman: String? = null,
    @SerialName("last_updated") val lastUpdated: Long? = null,
    @SerialName("bss_public_timestamp") val bssPublicTimestamp: Long? = null,
    @SerialName("last_action") val lastAction: Long? = null
)

@Serializable
data class FfTargetsWrapperDto(
    val targets: List<FfTargetItemDto>? = null,
    val data: List<FfTargetItemDto>? = null,
    val error: String? = null
)

data class FfStats(
    val playerId: Long,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val lastUpdatedSec: Long?,
    val fetchedAtMs: Long,
    val isStale: Boolean = false
)

data class FfTargetCandidate(
    val playerId: Long,
    val name: String?,
    val level: Int?,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val lastUpdatedSec: Long?,
    val lastActionSec: Long? = null
)
