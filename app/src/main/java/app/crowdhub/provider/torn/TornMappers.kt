package app.crowdhub.provider.torn

import app.crowdhub.core.ui.components.StatusKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class TornStatus(
    val state: StatusKind,
    val rawState: String,
    val description: String,
    val untilEpochSec: Long?,
    val planeImageType: String?
)

data class TornBasicInfo(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: TornStatus,
    val factionId: Long?,
    val factionName: String?
)

data class BarValue(
    val label: String,
    val current: Int,
    val maximum: Int,
    val increment: Int?,
    val intervalSec: Int?,
    val tickTimeSec: Long?,
    val fullTimeSec: Long?
)

data class TornBars(
    val energy: BarValue?,
    val nerve: BarValue?,
    val happy: BarValue?,
    val life: BarValue?
)

data class TornCooldowns(
    val drugSec: Long,
    val medicalSec: Long,
    val boosterSec: Long
)

data class TornTravel(
    val destination: String,
    val arrivalEpochSec: Long?,
    val departedEpochSec: Long?,
    val timeLeftSec: Long?,
    val method: String?
) {
    val isActivelyFlying: Boolean
        get() = timeLeftSec != null && timeLeftSec > 0L
}

data class TornMoney(
    val cashOnHand: Long?,
    val points: Long?,
    val vault: Long? = null,
    val caymanBank: Long? = null,
    val cityBankAmount: Long? = null,
    val dailyNetworth: Long? = null
)

data class TornEventItem(
    val text: String,
    val timestampSec: Long?
)

data class TornNotifications(
    val unreadMessages: Int,
    val unreadEvents: Int,
    val unreadAwards: Int,
    val unreadCompetition: Int,
    val recentEvents: List<TornEventItem>
)

data class TornStock(
    val stockId: Int,
    val name: String,
    val acronym: String,
    val currentPrice: Double,
    val marketCap: Long?,
    val totalShares: Long?,
    val availableShares: Long?,
    val forecast: String?,
    val demand: String?,
    val benefitDescription: String?,
    val benefitRequirement: Long?,
    val investors: Long? = null,
    val bonusPassive: Boolean? = null,
    val bonusFrequencyDays: Int? = null
)

data class TornFactionMember(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: TornStatus,
    val lastActionStatus: String?,
    val lastActionRelative: String?,
    val lastActionTimestampSec: Long?
)

data class TornRankedWar(
    val warId: Long,
    val startEpochSec: Long?,
    val endEpochSec: Long?,
    val targetScore: Int?,
    val winnerFactionId: Long?,
    val ownFactionId: Long,
    val ownFactionName: String,
    val enemyFactionId: Long,
    val enemyFactionName: String,
    val isActive: Boolean,
    val ownScore: Int? = null,
    val enemyScore: Int? = null,
    val isScheduled: Boolean = false
)

data class TornOtherProfile(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: TornStatus,
    val lastActionStatus: String?,
    val lastActionTimestampSec: Long?,
    val factionId: Long?,
    val factionName: String?
)

object TornMappers {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun mapStatus(dto: TornStatusDto?): TornStatus {
        val rawState = dto?.state.orEmpty()
        return TornStatus(
            state = StatusKind.fromRaw(rawState),
            rawState = rawState,
            description = dto?.description.orEmpty().trim().ifBlank { rawState.ifBlank { "Unknown" } },
            untilEpochSec = dto?.until?.takeIf { it > 0L },
            planeImageType = dto?.planeImageType?.takeIf { it.isNotBlank() }
        )
    }

    fun mapBasic(rawJson: String): TornBasicInfo? {
        val dto = runCatching { json.decodeFromString<TornUserBasicDto>(rawJson) }.getOrNull() ?: return null
        val id = dto.playerId ?: dto.id ?: dto.profile?.id ?: return null
        val name = dto.name ?: dto.profile?.name ?: "Player $id"
        val level = dto.level ?: dto.profile?.level
        val statusDto = dto.status ?: dto.profile?.status
        val factionId = dto.faction?.factionId ?: dto.faction?.id ?: dto.profile?.factionId
        val factionName = dto.faction?.factionName ?: dto.faction?.name
        return TornBasicInfo(
            playerId = id,
            name = name,
            level = level,
            status = mapStatus(statusDto),
            factionId = factionId?.takeIf { it > 0L },
            factionName = factionName
        )
    }

    fun mapBars(rawJson: String): TornBars? {
        val dto = runCatching { json.decodeFromString<TornUserBarsResponseDto>(rawJson) }.getOrNull() ?: return null
        val e = dto.energy ?: dto.bars?.energy
        val n = dto.nerve ?: dto.bars?.nerve
        val h = dto.happy ?: dto.bars?.happy
        val l = dto.life ?: dto.bars?.life
        return TornBars(
            energy = mapBar("Energy", e),
            nerve = mapBar("Nerve", n),
            happy = mapBar("Happy", h),
            life = mapBar("Life", l)
        )
    }

    private fun mapBar(label: String, dto: TornBarDto?): BarValue? {
        if (dto == null) return null
        val max = dto.maximum ?: return null
        val cur = dto.current ?: 0
        return BarValue(
            label = label,
            current = cur,
            maximum = max,
            increment = dto.increment,
            intervalSec = dto.interval,
            tickTimeSec = dto.tickTime ?: dto.ticktime,
            fullTimeSec = dto.fullTime ?: dto.fulltime
        )
    }

    fun mapCooldowns(rawJson: String): TornCooldowns? {
        val dto = runCatching { json.decodeFromString<TornUserCooldownsResponseDto>(rawJson) }.getOrNull() ?: return null
        val inner = dto.cooldowns
        return TornCooldowns(
            drugSec = (inner?.drug ?: dto.drug ?: 0L).coerceAtLeast(0L),
            medicalSec = (inner?.medical ?: dto.medical ?: 0L).coerceAtLeast(0L),
            boosterSec = (inner?.booster ?: dto.booster ?: 0L).coerceAtLeast(0L)
        )
    }

    fun mapTravel(rawJson: String): TornTravel? {
        val dto = runCatching { json.decodeFromString<TornUserTravelResponseDto>(rawJson) }.getOrNull() ?: return null
        val inner = dto.travel
        val dest = inner?.destination ?: dto.destination ?: return null
        val ts = (inner?.arrivalAt ?: inner?.timestamp ?: dto.arrivalAt ?: dto.timestamp)?.takeIf { it > 0L }
        val dep = (inner?.departedAt ?: inner?.departed ?: dto.departedAt ?: dto.departed)?.takeIf { it > 0L }
        val left = (inner?.timeLeft ?: dto.timeLeft)?.takeIf { it >= 0L }
        val method = inner?.method ?: dto.method
        return TornTravel(
            destination = dest,
            arrivalEpochSec = ts,
            departedEpochSec = dep,
            timeLeftSec = left,
            method = method
        )
    }

    fun mapMoney(rawJson: String): TornMoney? {
        val dto = runCatching { json.decodeFromString<TornMoneyDto>(rawJson) }.getOrNull() ?: return null
        val container = if (dto.money is JsonObject) {
            runCatching { json.decodeFromJsonElement<TornMoneyContainerDto>(dto.money) }.getOrNull()
        } else {
            null
        }
        var cash = dto.moneyOnhand ?: dto.wallet ?: container?.wallet
        var points = dto.points ?: container?.points
        if (cash == null && dto.money is JsonObject) {
            cash = dto.money["wallet"]?.jsonPrimitive?.longOrNull
                ?: dto.money["money_onhand"]?.jsonPrimitive?.longOrNull
                ?: dto.money["hand"]?.jsonPrimitive?.longOrNull
            if (points == null) {
                points = dto.money["points"]?.jsonPrimitive?.longOrNull
            }
        }
        return TornMoney(
            cashOnHand = cash,
            points = points,
            vault = container?.vault,
            caymanBank = container?.caymanBank,
            cityBankAmount = container?.cityBank?.amount,
            dailyNetworth = container?.dailyNetworth
        )
    }

    fun mapNotifications(rawJson: String): TornNotifications {
        val dto = runCatching { json.decodeFromString<TornUserNotificationsResponseDto>(rawJson) }.getOrNull()
        val counts = dto?.notifications
        val eventList = mutableListOf<TornEventItem>()
        when (val ev = dto?.events) {
            is JsonArray -> {
                for (el in ev) {
                    val item = runCatching { json.decodeFromJsonElement<TornEventItemDto>(el) }.getOrNull() ?: continue
                    val text = item.event ?: item.title ?: continue
                    eventList += TornEventItem(text = stripHtml(text), timestampSec = item.timestamp)
                }
            }
            is JsonObject -> {
                for ((_, el) in ev.entries) {
                    val item = runCatching { json.decodeFromJsonElement<TornEventItemDto>(el) }.getOrNull() ?: continue
                    val text = item.event ?: item.title ?: continue
                    eventList += TornEventItem(text = stripHtml(text), timestampSec = item.timestamp)
                }
            }
            else -> {}
        }
        return TornNotifications(
            unreadMessages = counts?.messages ?: 0,
            unreadEvents = counts?.events ?: 0,
            unreadAwards = counts?.awards ?: 0,
            unreadCompetition = counts?.competition ?: 0,
            recentEvents = eventList.sortedByDescending { it.timestampSec ?: 0L }.take(5)
        )
    }

    fun mapStocks(rawJson: String): List<TornStock> {
        val dto = runCatching { json.decodeFromString<TornStocksResponseDto>(rawJson) }.getOrNull() ?: return emptyList()
        val rawItems = mutableListOf<Pair<Int?, TornStockItemDto>>()
        when (val stocksEl = dto.stocks) {
            is JsonArray -> {
                for (el in stocksEl) {
                    val item = runCatching { json.decodeFromJsonElement<TornStockItemDto>(el) }.getOrNull() ?: continue
                    rawItems += (item.stockId ?: item.id) to item
                }
            }
            is JsonObject -> {
                for ((key, el) in stocksEl.entries) {
                    val item = runCatching { json.decodeFromJsonElement<TornStockItemDto>(el) }.getOrNull() ?: continue
                    rawItems += (item.stockId ?: item.id ?: key.toIntOrNull()) to item
                }
            }
            else -> {}
        }

        return rawItems.mapNotNull { (resolvedId, item) ->
            val stockId = resolvedId ?: return@mapNotNull null
            val price = (item.market?.price ?: item.currentPrice)?.takeIf { it > 0.0 } ?: return@mapNotNull null
            val name = item.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val acronym = item.acronym?.takeIf { it.isNotBlank() } ?: name.take(4).uppercase()
            val benefit = item.bonus ?: item.benefit
            TornStock(
                stockId = stockId,
                name = name,
                acronym = acronym,
                currentPrice = price,
                marketCap = item.market?.cap ?: item.marketCap,
                totalShares = item.market?.shares ?: item.totalShares,
                availableShares = item.availableShares,
                forecast = item.forecast,
                demand = item.demand,
                benefitDescription = benefit?.description,
                benefitRequirement = benefit?.requirement,
                investors = item.market?.investors ?: item.investors,
                bonusPassive = benefit?.passive,
                bonusFrequencyDays = benefit?.frequency
            )
        }
    }

    fun mapFactionMembers(rawJson: String): List<TornFactionMember> {
        val dto = runCatching { json.decodeFromString<TornFactionMembersResponseDto>(rawJson) }.getOrNull()
            ?: return emptyList()
        val parsed = mutableListOf<Pair<Long?, TornFactionMemberDto>>()
        when (val el = dto.members) {
            is JsonArray -> {
                for (itemEl in el) {
                    val item = runCatching { json.decodeFromJsonElement<TornFactionMemberDto>(itemEl) }.getOrNull() ?: continue
                    parsed += (item.id ?: item.playerId) to item
                }
            }
            is JsonObject -> {
                for ((key, itemEl) in el.entries) {
                    val item = runCatching { json.decodeFromJsonElement<TornFactionMemberDto>(itemEl) }.getOrNull() ?: continue
                    parsed += (item.id ?: item.playerId ?: key.toLongOrNull()) to item
                }
            }
            else -> {}
        }

        return parsed.mapNotNull { (resolvedId, m) ->
            val pid = resolvedId ?: return@mapNotNull null
            TornFactionMember(
                playerId = pid,
                name = m.name ?: "Player $pid",
                level = m.level,
                status = mapStatus(m.status),
                lastActionStatus = m.lastAction?.status,
                lastActionRelative = m.lastAction?.relative,
                lastActionTimestampSec = m.lastAction?.timestamp
            )
        }
    }

    fun mapRankedWars(rawJson: String, ownFactionId: Long, nowSec: Long): List<TornRankedWar> {
        val dto = runCatching { json.decodeFromString<TornRankedWarsResponseDto>(rawJson) }.getOrNull()
            ?: return emptyList()
        return dto.rankedwars.orEmpty().mapNotNull { war ->
            val warId = war.id ?: return@mapNotNull null
            val factions = war.factions.orEmpty()
            val own = factions.firstOrNull { it.id == ownFactionId } ?: factions.firstOrNull() ?: return@mapNotNull null
            val enemy = factions.firstOrNull { it.id != own.id } ?: return@mapNotNull null
            val enemyId = enemy.id ?: return@mapNotNull null
            val hasWinner = war.winner != null && war.winner > 0L
            val isEnded = hasWinner || (war.end != null && war.end > 0L && war.end <= nowSec)
            val isScheduled = !isEnded && war.start != null && war.start > nowSec
            TornRankedWar(
                warId = warId,
                startEpochSec = war.start,
                endEpochSec = war.end,
                targetScore = war.target,
                winnerFactionId = war.winner?.takeIf { it > 0L },
                ownFactionId = own.id ?: ownFactionId,
                ownFactionName = own.name ?: "Faction ${own.id ?: ownFactionId}",
                enemyFactionId = enemyId,
                enemyFactionName = enemy.name ?: "Faction $enemyId",
                isActive = !isEnded,
                ownScore = own.score,
                enemyScore = enemy.score,
                isScheduled = isScheduled
            )
        }
    }

    fun mapOtherProfile(rawJson: String, fallbackPlayerId: Long): TornOtherProfile? {
        val dto = runCatching { json.decodeFromString<TornOtherProfileResponseDto>(rawJson) }.getOrNull() ?: return null
        val pid = dto.playerId ?: dto.id ?: dto.profile?.id ?: fallbackPlayerId
        val name = dto.name ?: dto.profile?.name ?: "Player $pid"
        val level = dto.level ?: dto.profile?.level
        val status = mapStatus(dto.status ?: dto.profile?.status)
        val lastAction = dto.lastAction ?: dto.profile?.lastAction
        val factionId = (dto.faction?.factionId ?: dto.faction?.id ?: dto.profile?.factionId)?.takeIf { it > 0L }
        val factionName = dto.faction?.factionName ?: dto.faction?.name
        return TornOtherProfile(
            playerId = pid,
            name = name,
            level = level,
            status = status,
            lastActionStatus = lastAction?.status,
            lastActionTimestampSec = lastAction?.timestamp,
            factionId = factionId,
            factionName = factionName
        )
    }

    private fun stripHtml(input: String): String {
        return input.replace(Regex("<[^>]+>"), "").trim()
    }
}
