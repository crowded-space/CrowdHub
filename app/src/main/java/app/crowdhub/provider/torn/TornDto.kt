package app.crowdhub.provider.torn

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TornErrorWrapperDto(
    val error: TornErrorDetailDto? = null
)

@Serializable
data class TornErrorDetailDto(
    val code: Int? = null,
    val error: String? = null
)

@Serializable
data class TornStatusDto(
    val state: String? = null,
    val description: String? = null,
    val details: String? = null,
    val until: Long? = null,
    @SerialName("plane_image_type") val planeImageType: String? = null
)

@Serializable
data class TornUserBasicDto(
    @SerialName("player_id") val playerId: Long? = null,
    val id: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    val status: TornStatusDto? = null,
    val profile: TornUserProfileInnerDto? = null,
    val faction: TornUserFactionDto? = null
)

@Serializable
data class TornUserProfileInnerDto(
    val id: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    @SerialName("faction_id") val factionId: Long? = null,
    val status: TornStatusDto? = null,
    @SerialName("last_action") val lastAction: TornLastActionDto? = null,
    val revivable: Boolean? = null,
    @SerialName("revive_setting") val reviveSetting: String? = null
)

@Serializable
data class TornUserFactionDto(
    @SerialName("faction_id") val factionId: Long? = null,
    val id: Long? = null,
    @SerialName("faction_name") val factionName: String? = null,
    val name: String? = null
)

@Serializable
data class TornBarDto(
    val current: Int? = null,
    val maximum: Int? = null,
    val increment: Int? = null,
    val interval: Int? = null,
    @SerialName("tick_time") val tickTime: Long? = null,
    val ticktime: Long? = null,
    @SerialName("full_time") val fullTime: Long? = null,
    val fulltime: Long? = null
)

@Serializable
data class TornChainBarDto(
    val id: Long? = null,
    val current: Int? = null,
    val max: Int? = null,
    val timeout: Int? = null,
    val modifier: Double? = null,
    val cooldown: Long? = null,
    val start: Long? = null,
    val end: Long? = null
)

@Serializable
data class TornUserBarsResponseDto(
    val energy: TornBarDto? = null,
    val nerve: TornBarDto? = null,
    val happy: TornBarDto? = null,
    val life: TornBarDto? = null,
    val bars: TornBarsContainerDto? = null
)

@Serializable
data class TornBarsContainerDto(
    val energy: TornBarDto? = null,
    val nerve: TornBarDto? = null,
    val happy: TornBarDto? = null,
    val life: TornBarDto? = null,
    val chain: TornChainBarDto? = null
)

@Serializable
data class TornCooldownsDto(
    val drug: Long? = null,
    val medical: Long? = null,
    val booster: Long? = null
)

@Serializable
data class TornUserCooldownsResponseDto(
    val cooldowns: TornCooldownsDto? = null,
    val drug: Long? = null,
    val medical: Long? = null,
    val booster: Long? = null
)

@Serializable
data class TornTravelDto(
    val destination: String? = null,
    @SerialName("arrival_at") val arrivalAt: Long? = null,
    val timestamp: Long? = null,
    @SerialName("departed_at") val departedAt: Long? = null,
    val departed: Long? = null,
    @SerialName("time_left") val timeLeft: Long? = null,
    val method: String? = null
)

@Serializable
data class TornUserTravelResponseDto(
    val travel: TornTravelDto? = null,
    val destination: String? = null,
    @SerialName("arrival_at") val arrivalAt: Long? = null,
    val timestamp: Long? = null,
    @SerialName("departed_at") val departedAt: Long? = null,
    val departed: Long? = null,
    @SerialName("time_left") val timeLeft: Long? = null,
    val method: String? = null
)

@Serializable
data class TornMoneyCityBankDto(
    val amount: Long? = null,
    val profit: Long? = null,
    val duration: Int? = null,
    @SerialName("interest_rate") val interestRate: Double? = null,
    @SerialName("invested_at") val investedAt: Long? = null,
    val until: Long? = null
)

@Serializable
data class TornMoneyContainerDto(
    val points: Long? = null,
    val wallet: Long? = null,
    val company: Long? = null,
    val vault: Long? = null,
    @SerialName("cayman_bank") val caymanBank: Long? = null,
    @SerialName("city_bank") val cityBank: TornMoneyCityBankDto? = null,
    @SerialName("daily_networth") val dailyNetworth: Long? = null
)

@Serializable
data class TornMoneyDto(
    @SerialName("money_onhand") val moneyOnhand: Long? = null,
    val wallet: Long? = null,
    val points: Long? = null,
    val money: JsonElement? = null
)

@Serializable
data class TornNotificationsCountsDto(
    val messages: Int? = null,
    val events: Int? = null,
    val awards: Int? = null,
    val competition: Int? = null
)

@Serializable
data class TornEventItemDto(
    val event: String? = null,
    val title: String? = null,
    val timestamp: Long? = null
)

@Serializable
data class TornUserNotificationsResponseDto(
    val notifications: TornNotificationsCountsDto? = null,
    val events: JsonElement? = null
)

@Serializable
data class TornStockMarketDto(
    val price: Double? = null,
    val cap: Long? = null,
    val shares: Long? = null,
    val investors: Long? = null
)

@Serializable
data class TornStockBenefitDto(
    val passive: Boolean? = null,
    val requirement: Long? = null,
    val description: String? = null,
    val frequency: Int? = null
)

@Serializable
data class TornStockItemDto(
    @SerialName("stock_id") val stockId: Int? = null,
    val id: Int? = null,
    val name: String? = null,
    val acronym: String? = null,
    val market: TornStockMarketDto? = null,
    val bonus: TornStockBenefitDto? = null,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("total_shares") val totalShares: Long? = null,
    @SerialName("available_shares") val availableShares: Long? = null,
    val investors: Long? = null,
    val forecast: String? = null,
    val demand: String? = null,
    val benefit: TornStockBenefitDto? = null
)

@Serializable
data class TornStocksResponseDto(
    val stocks: JsonElement? = null
)

@Serializable
data class TornLastActionDto(
    val status: String? = null,
    val relative: String? = null,
    val timestamp: Long? = null
)

@Serializable
data class TornFactionMemberDto(
    val id: Long? = null,
    @SerialName("player_id") val playerId: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    @SerialName("days_in_faction") val daysInFaction: Int? = null,
    val status: TornStatusDto? = null,
    @SerialName("last_action") val lastAction: TornLastActionDto? = null,
    val position: String? = null,
    @SerialName("is_revivable") val isRevivable: Boolean? = null
)

@Serializable
data class TornFactionMembersResponseDto(
    val members: JsonElement? = null
)

@Serializable
data class TornRankedWarFactionDto(
    val id: Long? = null,
    val name: String? = null,
    val score: Int? = null,
    val chain: Int? = null
)

@Serializable
data class TornRankedWarDto(
    val id: Long? = null,
    val start: Long? = null,
    val end: Long? = null,
    val target: Int? = null,
    val winner: Long? = null,
    val factions: List<TornRankedWarFactionDto>? = null
)

@Serializable
data class TornRankedWarsResponseDto(
    val rankedwars: List<TornRankedWarDto>? = null
)

@Serializable
data class TornOtherProfileResponseDto(
    @SerialName("player_id") val playerId: Long? = null,
    val id: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    val status: TornStatusDto? = null,
    @SerialName("last_action") val lastAction: TornLastActionDto? = null,
    val faction: TornUserFactionDto? = null,
    val profile: TornUserProfileInnerDto? = null
)
