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
data class TornUserStockDividendDto(
    val ready: JsonElement? = null,
    val increment: Int? = null,
    val progress: Int? = null,
    val frequency: Int? = null
)

@Serializable
data class TornUserStockTransactionDto(
    val id: Long? = null,
    val shares: Long? = null,
    @SerialName("bought_price") val boughtPrice: Double? = null,
    val price: Double? = null,
    @SerialName("time_bought") val timeBought: Long? = null,
    val timestamp: Long? = null
)

@Serializable
data class TornUserStockItemDto(
    @SerialName("stock_id") val stockId: Int? = null,
    val id: Int? = null,
    @SerialName("total_shares") val totalShares: Long? = null,
    val shares: Long? = null,
    val dividend: TornUserStockDividendDto? = null,
    val bonus: TornUserStockDividendDto? = null,
    val transactions: JsonElement? = null
)

@Serializable
data class TornUserStocksResponseDto(
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

@Serializable
data class TornFactionBasicDto(
    val id: Long? = null,
    @SerialName("ID") val legacyId: Long? = null,
    val name: String? = null,
    val tag: String? = null,
    @SerialName("tag_image") val tagImage: String? = null,
    @SerialName("leader_id") val leaderId: Long? = null,
    val leader: Long? = null,
    @SerialName("co_leader_id") val coLeaderId: Long? = null,
    @SerialName("co-leader_id") val coLeaderHyphenId: Long? = null,
    @SerialName("co-leader") val coLeaderLegacy: Long? = null,
    val respect: Long? = null,
    val capacity: Int? = null,
    @SerialName("members_count") val membersCount: Int? = null,
    @SerialName("best_chain") val bestChain: Long? = null,
    @SerialName("days_old") val daysOld: Int? = null,
    @SerialName("is_enlisted") val isEnlisted: Boolean? = null
)

@Serializable
data class TornFactionBasicResponseDto(
    val basic: TornFactionBasicDto? = null,
    val id: Long? = null,
    @SerialName("ID") val legacyId: Long? = null,
    val name: String? = null,
    val tag: String? = null,
    val respect: Long? = null,
    val capacity: Int? = null,
    @SerialName("members_count") val membersCount: Int? = null,
    @SerialName("best_chain") val bestChain: Long? = null,
    @SerialName("leader_id") val leaderId: Long? = null,
    val leader: Long? = null,
    @SerialName("co-leader_id") val coLeaderHyphenId: Long? = null,
    @SerialName("co_leader_id") val coLeaderId: Long? = null,
    val members: JsonElement? = null
)

@Serializable
data class TornFactionChainResponseDto(
    val chain: TornChainBarDto? = null
)

@Serializable
data class TornFactionAttacksResponseDto(
    val attacks: JsonElement? = null
)

@Serializable
data class TornAttackActorFactionDto(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class TornAttackActorDto(
    val id: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    val faction: TornAttackActorFactionDto? = null
)

@Serializable
data class TornAttackModifiersDto(
    @SerialName("fair_fight") val fairFight: Double? = null,
    val war: Double? = null,
    val retaliation: Double? = null,
    @SerialName("group_attack") val groupAttack: Double? = null,
    val overseas: Double? = null,
    @SerialName("chain_bonus") val chainBonus: Double? = null
)

@Serializable
data class TornFactionAttackDto(
    val id: Long? = null,
    val code: String? = null,
    val started: Long? = null,
    val ended: Long? = null,
    @SerialName("timestamp_started") val timestampStarted: Long? = null,
    @SerialName("timestamp_ended") val timestampEnded: Long? = null,
    val timestamp: Long? = null,
    val attacker: TornAttackActorDto? = null,
    val defender: TornAttackActorDto? = null,
    @SerialName("attacker_id") val attackerId: Long? = null,
    @SerialName("attacker_name") val attackerName: String? = null,
    @SerialName("attacker_faction") val attackerFactionId: Long? = null,
    @SerialName("attacker_factionname") val attackerFactionName: String? = null,
    @SerialName("defender_id") val defenderId: Long? = null,
    @SerialName("defender_name") val defenderName: String? = null,
    @SerialName("defender_faction") val defenderFactionId: Long? = null,
    @SerialName("defender_factionname") val defenderFactionName: String? = null,
    val result: String? = null,
    @SerialName("respect_gain") val respectGain: Double? = null,
    val respect: Double? = null,
    val chain: Int? = null,
    val modifiers: TornAttackModifiersDto? = null,
    @SerialName("is_interrupted") val isInterrupted: Boolean? = null,
    @SerialName("is_stealthed") val isStealthed: Boolean? = null
)

@Serializable
data class TornFactionBalanceContainerDto(
    val money: Long? = null,
    val points: Long? = null
)

@Serializable
data class TornFactionBalancePayloadDto(
    val faction: TornFactionBalanceContainerDto? = null,
    val member: TornFactionBalanceContainerDto? = null,
    val money: Long? = null,
    val points: Long? = null
)

@Serializable
data class TornFactionBalanceResponseDto(
    val balance: TornFactionBalancePayloadDto? = null,
    val money: Long? = null,
    val points: Long? = null
)

@Serializable
data class TornArmoryItemDto(
    val id: Long? = null,
    @SerialName("item_id") val itemId: Long? = null,
    @SerialName("ID") val legacyId: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val category: String? = null,
    val quantity: Int? = null,
    val available: Int? = null,
    val loaned: Int? = null,
    @SerialName("market_price") val marketPrice: Long? = null,
    val price: Long? = null
)

@Serializable
data class TornOcSlotUserDto(
    val id: Long? = null,
    val name: String? = null,
    val outcome: String? = null,
    val progress: Double? = null,
    @SerialName("joined_at") val joinedAt: Long? = null
)

@Serializable
data class TornOcItemRequirementDto(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("is_reusable") val isReusable: Boolean? = null,
    @SerialName("is_available") val isAvailable: Boolean? = null
)

@Serializable
data class TornOcSlotDto(
    val id: String? = null,
    @SerialName("slot_id") val slotId: JsonElement? = null,
    val position: String? = null,
    @SerialName("position_id") val positionId: String? = null,
    @SerialName("position_info") val positionInfo: JsonElement? = null,
    @SerialName("user_id") val userId: Long? = null,
    val user: TornOcSlotUserDto? = null,
    val status: String? = null,
    @SerialName("checkpoint_pass_rate") val checkpointPassRate: Double? = null,
    @SerialName("success_chance") val successChance: Double? = null,
    @SerialName("item_requirement") val itemRequirement: TornOcItemRequirementDto? = null
)

@Serializable
data class TornOrganizedCrimeDto(
    val id: Long? = null,
    val name: String? = null,
    val difficulty: Int? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("planning_at") val planningAt: Long? = null,
    @SerialName("ready_at") val readyAt: Long? = null,
    @SerialName("expired_at") val expiredAt: Long? = null,
    @SerialName("executed_at") val executedAt: Long? = null,
    val slots: JsonElement? = null
)

@Serializable
data class TornFactionCrimesResponseDto(
    val crimes: JsonElement? = null
)

@Serializable
data class TornUserOrganizedCrimeResponseDto(
    @SerialName("organizedCrime") val organizedCrime: JsonElement? = null,
    val organizedcrime: JsonElement? = null,
    val crime: JsonElement? = null
)

@Serializable
data class TornUserListItemDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("player_id") val playerId: Long? = null,
    val name: String? = null,
    val level: Int? = null,
    @SerialName("days_in_torn") val daysInTorn: Int? = null,
    val note: String? = null,
    val description: String? = null,
    val status: TornStatusDto? = null,
    @SerialName("last_action") val lastAction: TornLastActionDto? = null
)

@Serializable
data class TornUserListResponseDto(
    val list: JsonElement? = null,
    val friends: JsonElement? = null,
    val targets: JsonElement? = null,
    val enemies: JsonElement? = null
)

