package app.crowdhub.provider.torn

import app.crowdhub.core.ui.components.StatusKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
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

data class TornStockTransaction(
    val id: Long?,
    val shares: Long,
    val boughtPrice: Double,
    val timeBoughtSec: Long?
)

data class TornStockDividend(
    val ready: Boolean,
    val increment: Int,
    val progress: Int,
    val frequency: Int
)

data class TornUserStock(
    val stockId: Int,
    val totalShares: Long,
    val dividend: TornStockDividend?,
    val transactions: List<TornStockTransaction>
)

data class TornFactionMember(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: TornStatus,
    val lastActionStatus: String?,
    val lastActionRelative: String?,
    val lastActionTimestampSec: Long?,
    val position: String? = null,
    val daysInFaction: Int? = null,
    val isRevivable: Boolean? = null
)

data class TornFactionBasic(
    val factionId: Long,
    val name: String,
    val tag: String?,
    val respect: Long?,
    val capacity: Int?,
    val memberCount: Int?,
    val bestChain: Long?,
    val leaderId: Long?,
    val coLeaderId: Long?,
    val isEnlisted: Boolean?
)

data class TornFactionChain(
    val id: Long?,
    val current: Int,
    val max: Int,
    val timeoutSec: Int,
    val modifier: Double,
    val cooldownSec: Long,
    val startEpochSec: Long?,
    val endEpochSec: Long?
) {
    val isActive: Boolean get() = current > 0 && timeoutSec > 0 && cooldownSec <= 0L
    val isInCooldown: Boolean get() = cooldownSec > 0L
}

data class TornFactionAttack(
    val attackId: Long,
    val code: String?,
    val startedAtSec: Long?,
    val endedAtSec: Long?,
    val attackerId: Long?,
    val attackerName: String,
    val attackerFactionId: Long?,
    val attackerFactionName: String?,
    val defenderId: Long?,
    val defenderName: String,
    val defenderFactionId: Long?,
    val defenderFactionName: String?,
    val result: String,
    val respectGain: Double,
    val chainHit: Int,
    val modifierFairFight: Double?,
    val isInterrupted: Boolean,
    val isStealthed: Boolean
)

enum class ArmoryCategory(val label: String) {
    ALL("All"),
    MEDICAL("Medical"),
    DRUGS("Drugs"),
    BOOSTERS("Boosters"),
    WEAPONS("Weapons"),
    ARMOR("Armor"),
    TEMPORARY("Temporary"),
    SUPPLIES("Supplies"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): ArmoryCategory {
            val v = raw?.trim()?.lowercase().orEmpty()
            return when {
                v.contains("med") -> MEDICAL
                v.contains("drug") -> DRUGS
                v.contains("booster") || v.contains("alcohol") || v.contains("candy") || v.contains("energy drink") -> BOOSTERS
                v.contains("weapon") || v.contains("melee") || v.contains("primary") || v.contains("secondary") -> WEAPONS
                v.contains("armor") || v.contains("armour") -> ARMOR
                v.contains("temp") -> TEMPORARY
                v.contains("suppl") || v.contains("cesium") || v.contains("point") || v.contains("special") -> SUPPLIES
                else -> OTHER
            }
        }
    }
}

data class TornArmoryItem(
    val itemId: Long,
    val name: String,
    val category: ArmoryCategory,
    val rawType: String?,
    val quantity: Int,
    val available: Int,
    val loaned: Int,
    val marketPrice: Long?
)

data class TornFactionBalance(
    val factionMoney: Long?,
    val factionPoints: Long?,
    val memberMoney: Long?,
    val memberPoints: Long?
)

data class TornOcSlot(
    val position: String,
    val positionId: String?,
    val userId: Long?,
    val userName: String?,
    val status: String,
    val checkpointPassRate: Double?,
    val itemRequirementName: String?,
    val itemAvailable: Boolean?,
    val outcome: String?,
    val joinedAtSec: Long?,
    val progress: Double?
)

data class TornOrganizedCrime(
    val crimeId: Long,
    val name: String,
    val difficulty: Int?,
    val status: String,
    val createdAtSec: Long?,
    val planningAtSec: Long?,
    val readyAtSec: Long?,
    val expiredAtSec: Long?,
    val executedAtSec: Long?,
    val slots: List<TornOcSlot>
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

    fun mapUserStocks(rawJson: String): List<TornUserStock>? {
        val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
        if (!root.containsKey("stocks")) return null
        val stocksEl = root["stocks"] ?: return emptyList()

        val rawItems = mutableListOf<Pair<Int?, TornUserStockItemDto>>()
        when (stocksEl) {
            is JsonArray -> {
                for (el in stocksEl) {
                    val item = runCatching { json.decodeFromJsonElement<TornUserStockItemDto>(el) }.getOrNull() ?: continue
                    rawItems += (item.stockId ?: item.id) to item
                }
            }
            is JsonObject -> {
                for ((key, el) in stocksEl.entries) {
                    val item = runCatching { json.decodeFromJsonElement<TornUserStockItemDto>(el) }.getOrNull() ?: continue
                    rawItems += (item.stockId ?: item.id ?: key.toIntOrNull()) to item
                }
            }
            else -> return emptyList()
        }

        return rawItems.mapNotNull { (resolvedId, item) ->
            val stockId = resolvedId ?: return@mapNotNull null
            val txs = parseTransactions(item.transactions)
            val totalShares = item.totalShares ?: item.shares ?: txs.sumOf { it.shares }
            if (totalShares <= 0L) return@mapNotNull null

            val divDto = item.dividend ?: item.bonus
            val dividend = divDto?.let {
                TornStockDividend(
                    ready = parseFlexibleBoolean(it.ready),
                    increment = (it.increment ?: 0).coerceAtLeast(0),
                    progress = (it.progress ?: 0).coerceAtLeast(0),
                    frequency = (it.frequency ?: 0).coerceAtLeast(0)
                )
            }

            TornUserStock(
                stockId = stockId,
                totalShares = totalShares,
                dividend = dividend,
                transactions = txs
            )
        }
    }

    private fun parseTransactions(el: JsonElement?): List<TornStockTransaction> {
        if (el == null) return emptyList()
        val parsed = mutableListOf<Pair<Long?, TornUserStockTransactionDto>>()
        when (el) {
            is JsonArray -> {
                for (itemEl in el) {
                    val tx = runCatching { json.decodeFromJsonElement<TornUserStockTransactionDto>(itemEl) }.getOrNull() ?: continue
                    parsed += tx.id to tx
                }
            }
            is JsonObject -> {
                for ((key, itemEl) in el.entries) {
                    val tx = runCatching { json.decodeFromJsonElement<TornUserStockTransactionDto>(itemEl) }.getOrNull() ?: continue
                    parsed += (tx.id ?: key.toLongOrNull()) to tx
                }
            }
            else -> {}
        }
        return parsed.mapNotNull { (resolvedId, tx) ->
            val shares = tx.shares?.takeIf { it > 0L } ?: return@mapNotNull null
            val boughtPrice = (tx.boughtPrice ?: tx.price)?.takeIf { it >= 0.0 } ?: return@mapNotNull null
            val timeBought = (tx.timeBought ?: tx.timestamp)?.takeIf { it > 0L }
            TornStockTransaction(
                id = resolvedId,
                shares = shares,
                boughtPrice = boughtPrice,
                timeBoughtSec = timeBought
            )
        }.sortedByDescending { it.timeBoughtSec ?: 0L }
    }

    private fun parseFlexibleBoolean(el: JsonElement?): Boolean {
        val prim = runCatching { el?.jsonPrimitive }.getOrNull() ?: return false
        prim.booleanOrNull?.let { return it }
        prim.longOrNull?.let { return it > 0L }
        return prim.content.equals("true", ignoreCase = true) || prim.content == "1"
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
                lastActionTimestampSec = m.lastAction?.timestamp,
                position = m.position?.takeIf { it.isNotBlank() },
                daysInFaction = m.daysInFaction,
                isRevivable = m.isRevivable
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

    fun mapFactionBasic(rawJson: String): TornFactionBasic? {
        val dto = runCatching { json.decodeFromString<TornFactionBasicResponseDto>(rawJson) }.getOrNull() ?: return null
        val b = dto.basic
        val id = b?.id ?: b?.legacyId ?: dto.id ?: dto.legacyId ?: return null
        val name = b?.name ?: dto.name ?: "Faction $id"
        return TornFactionBasic(
            factionId = id,
            name = name,
            tag = (b?.tag ?: dto.tag)?.takeIf { it.isNotBlank() },
            respect = b?.respect ?: dto.respect,
            capacity = b?.capacity ?: dto.capacity,
            memberCount = b?.membersCount ?: dto.membersCount ?: (dto.members as? JsonObject)?.size ?: (dto.members as? JsonArray)?.size,
            bestChain = b?.bestChain ?: dto.bestChain,
            leaderId = b?.leaderId ?: b?.leader ?: dto.leaderId ?: dto.leader,
            coLeaderId = b?.coLeaderId ?: b?.coLeaderHyphenId ?: b?.coLeaderLegacy ?: dto.coLeaderId ?: dto.coLeaderHyphenId,
            isEnlisted = b?.isEnlisted
        )
    }

    fun mapFactionChain(rawJson: String): TornFactionChain? {
        val dto = runCatching { json.decodeFromString<TornFactionChainResponseDto>(rawJson) }.getOrNull()
        val chainDto = dto?.chain ?: runCatching { json.decodeFromString<TornChainBarDto>(rawJson) }.getOrNull()
            ?: return null
        return TornFactionChain(
            id = chainDto.id,
            current = (chainDto.current ?: 0).coerceAtLeast(0),
            max = (chainDto.max ?: 0).coerceAtLeast(0),
            timeoutSec = (chainDto.timeout ?: 0).coerceAtLeast(0),
            modifier = chainDto.modifier ?: 1.0,
            cooldownSec = (chainDto.cooldown ?: 0L).coerceAtLeast(0L),
            startEpochSec = chainDto.start?.takeIf { it > 0L },
            endEpochSec = chainDto.end?.takeIf { it > 0L }
        )
    }

    fun mapFactionAttacks(rawJson: String): List<TornFactionAttack>? {
        val dto = runCatching { json.decodeFromString<TornFactionAttacksResponseDto>(rawJson) }.getOrNull()
            ?: return null
        val attacksEl = dto.attacks ?: return null
        val parsed = mutableListOf<Pair<Long?, TornFactionAttackDto>>()
        when (attacksEl) {
            is JsonArray -> {
                for (el in attacksEl) {
                    val item = runCatching { json.decodeFromJsonElement<TornFactionAttackDto>(el) }.getOrNull() ?: continue
                    parsed += item.id to item
                }
            }
            is JsonObject -> {
                for ((key, el) in attacksEl.entries) {
                    val item = runCatching { json.decodeFromJsonElement<TornFactionAttackDto>(el) }.getOrNull() ?: continue
                    parsed += (item.id ?: key.toLongOrNull()) to item
                }
            }
            else -> return emptyList()
        }

        return parsed.mapNotNull { (resolvedId, a) ->
            val id = resolvedId ?: a.ended ?: a.timestampEnded ?: return@mapNotNull null
            val attId = a.attacker?.id ?: a.attackerId?.takeIf { it > 0L }
            val defId = a.defender?.id ?: a.defenderId?.takeIf { it > 0L }
            val attName = a.attacker?.name?.takeIf { it.isNotBlank() }
                ?: a.attackerName?.takeIf { it.isNotBlank() }
                ?: if (a.isStealthed == true || attId == null) "Someone" else "Player $attId"
            val defName = a.defender?.name?.takeIf { it.isNotBlank() }
                ?: a.defenderName?.takeIf { it.isNotBlank() }
                ?: if (defId != null) "Player $defId" else "Unknown"

            TornFactionAttack(
                attackId = id,
                code = a.code,
                startedAtSec = (a.started ?: a.timestampStarted)?.takeIf { it > 0L },
                endedAtSec = (a.ended ?: a.timestampEnded ?: a.timestamp)?.takeIf { it > 0L },
                attackerId = attId,
                attackerName = attName,
                attackerFactionId = (a.attacker?.faction?.id ?: a.attackerFactionId)?.takeIf { it > 0L },
                attackerFactionName = (a.attacker?.faction?.name ?: a.attackerFactionName)?.takeIf { it.isNotBlank() },
                defenderId = defId,
                defenderName = defName,
                defenderFactionId = (a.defender?.faction?.id ?: a.defenderFactionId)?.takeIf { it > 0L },
                defenderFactionName = (a.defender?.faction?.name ?: a.defenderFactionName)?.takeIf { it.isNotBlank() },
                result = a.result?.takeIf { it.isNotBlank() } ?: "Attack",
                respectGain = a.respectGain ?: a.respect ?: 0.0,
                chainHit = (a.chain ?: 0).coerceAtLeast(0),
                modifierFairFight = a.modifiers?.fairFight,
                isInterrupted = a.isInterrupted == true,
                isStealthed = a.isStealthed == true
            )
        }.sortedByDescending { it.endedAtSec ?: it.startedAtSec ?: it.attackId }
    }

    fun mapFactionBalance(rawJson: String): TornFactionBalance? {
        val dto = runCatching { json.decodeFromString<TornFactionBalanceResponseDto>(rawJson) }.getOrNull()
            ?: return null
        val payload = dto.balance
        val factionMoney = payload?.faction?.money ?: payload?.money ?: dto.money
        val factionPoints = payload?.faction?.points ?: payload?.points ?: dto.points
        val memberMoney = payload?.member?.money
        val memberPoints = payload?.member?.points
        if (factionMoney == null && factionPoints == null && memberMoney == null && memberPoints == null) {
            return null
        }
        return TornFactionBalance(
            factionMoney = factionMoney,
            factionPoints = factionPoints,
            memberMoney = memberMoney,
            memberPoints = memberPoints
        )
    }

    fun mapFactionArmory(rawJson: String): List<TornArmoryItem>? {
        val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
        val out = mutableListOf<TornArmoryItem>()

        // v2 keys: inventory / armory / items
        val v2Key = listOf("inventory", "armory", "items").firstOrNull { root.containsKey(it) }
        if (v2Key != null) {
            val container = root[v2Key] ?: return emptyList()
            parseArmoryElement(container, defaultCategory = null, out = out)
            return out.sortedWith(compareBy({ it.category.ordinal }, { it.name.lowercase() }))
        }

        // v1 armory selections: weapons, armor, medical, drugs, boosters, temporary, cesium
        val v1Categories = mapOf(
            "medical" to ArmoryCategory.MEDICAL,
            "drugs" to ArmoryCategory.DRUGS,
            "boosters" to ArmoryCategory.BOOSTERS,
            "weapons" to ArmoryCategory.WEAPONS,
            "armor" to ArmoryCategory.ARMOR,
            "temporary" to ArmoryCategory.TEMPORARY,
            "cesium" to ArmoryCategory.SUPPLIES
        )
        var matchedAnyV1Key = false
        for ((key, cat) in v1Categories) {
            val el = root[key] ?: continue
            matchedAnyV1Key = true
            parseArmoryElement(el, defaultCategory = cat, out = out)
        }
        if (!matchedAnyV1Key) return null
        return out.sortedWith(compareBy({ it.category.ordinal }, { it.name.lowercase() }))
    }

    private fun parseArmoryElement(
        el: JsonElement,
        defaultCategory: ArmoryCategory?,
        out: MutableList<TornArmoryItem>
    ) {
        val items = mutableListOf<Pair<Long?, TornArmoryItemDto>>()
        when (el) {
            is JsonArray -> {
                for (child in el) {
                    val dto = runCatching { json.decodeFromJsonElement<TornArmoryItemDto>(child) }.getOrNull() ?: continue
                    items += (dto.itemId ?: dto.id ?: dto.legacyId) to dto
                }
            }
            is JsonObject -> {
                for ((key, child) in el.entries) {
                    val dto = runCatching { json.decodeFromJsonElement<TornArmoryItemDto>(child) }.getOrNull() ?: continue
                    items += (dto.itemId ?: dto.id ?: dto.legacyId ?: key.toLongOrNull()) to dto
                }
            }
            else -> {}
        }
        for ((resolvedId, dto) in items) {
            val id = resolvedId ?: continue
            val name = dto.name?.takeIf { it.isNotBlank() } ?: "Item #$id"
            val qty = (dto.quantity ?: dto.available ?: 0).coerceAtLeast(0)
            val loaned = (dto.loaned ?: 0).coerceAtLeast(0)
            val available = (dto.available ?: (qty - loaned)).coerceAtLeast(0)
            val rawCat = dto.category ?: dto.type
            val category = defaultCategory ?: ArmoryCategory.fromRaw(rawCat)
            out += TornArmoryItem(
                itemId = id,
                name = name,
                category = category,
                rawType = rawCat,
                quantity = qty,
                available = available,
                loaned = loaned,
                marketPrice = dto.marketPrice ?: dto.price
            )
        }
    }

    fun mapFactionCrimes(rawJson: String): List<TornOrganizedCrime>? {
        val dto = runCatching { json.decodeFromString<TornFactionCrimesResponseDto>(rawJson) }.getOrNull()
            ?: return null
        val crimesEl = dto.crimes ?: return null
        val parsed = mutableListOf<Pair<Long?, TornOrganizedCrimeDto>>()
        when (crimesEl) {
            is JsonArray -> {
                for (el in crimesEl) {
                    val crime = runCatching { json.decodeFromJsonElement<TornOrganizedCrimeDto>(el) }.getOrNull() ?: continue
                    parsed += crime.id to crime
                }
            }
            is JsonObject -> {
                for ((key, el) in crimesEl.entries) {
                    val crime = runCatching { json.decodeFromJsonElement<TornOrganizedCrimeDto>(el) }.getOrNull() ?: continue
                    parsed += (crime.id ?: key.toLongOrNull()) to crime
                }
            }
            else -> return emptyList()
        }
        return parsed.mapNotNull { (id, crime) -> mapSingleCrime(id, crime) }
            .sortedWith(
                compareBy<TornOrganizedCrime>(
                    { crimeStatusSortOrder(it.status) },
                    { it.readyAtSec ?: Long.MAX_VALUE },
                    { -it.crimeId }
                )
            )
    }

    data class TornUserOcResult(val crime: TornOrganizedCrime?)

    fun mapUserOrganizedCrime(rawJson: String): TornUserOcResult? {
        val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
        val key = listOf("organizedCrime", "organizedcrime", "crime").firstOrNull { root.containsKey(it) }
            ?: return null
        val el = root[key]
        if (el == null || el !is JsonObject) {
            return TornUserOcResult(null)
        }
        val crime = runCatching { json.decodeFromJsonElement<TornOrganizedCrimeDto>(el) }.getOrNull()
            ?: return TornUserOcResult(null)
        return TornUserOcResult(mapSingleCrime(crime.id, crime))
    }

    private fun mapSingleCrime(resolvedId: Long?, dto: TornOrganizedCrimeDto): TornOrganizedCrime? {
        val crimeId = resolvedId ?: dto.id ?: return null
        val slotsList = mutableListOf<TornOcSlot>()
        when (val slotsEl = dto.slots) {
            is JsonArray -> {
                slotsEl.forEachIndexed { idx, el ->
                    val slot = runCatching { json.decodeFromJsonElement<TornOcSlotDto>(el) }.getOrNull() ?: return@forEachIndexed
                    slotsList += mapOcSlot(slot, idx + 1)
                }
            }
            is JsonObject -> {
                var idx = 1
                for ((_, el) in slotsEl.entries) {
                    val slot = runCatching { json.decodeFromJsonElement<TornOcSlotDto>(el) }.getOrNull() ?: continue
                    slotsList += mapOcSlot(slot, idx++)
                }
            }
            else -> {}
        }
        return TornOrganizedCrime(
            crimeId = crimeId,
            name = dto.name?.takeIf { it.isNotBlank() } ?: "Organized Crime #$crimeId",
            difficulty = dto.difficulty,
            status = dto.status?.takeIf { it.isNotBlank() } ?: "Unknown",
            createdAtSec = dto.createdAt?.takeIf { it > 0L },
            planningAtSec = dto.planningAt?.takeIf { it > 0L },
            readyAtSec = dto.readyAt?.takeIf { it > 0L },
            expiredAtSec = dto.expiredAt?.takeIf { it > 0L },
            executedAtSec = dto.executedAt?.takeIf { it > 0L },
            slots = slotsList
        )
    }

    private fun mapOcSlot(slot: TornOcSlotDto, fallbackIndex: Int): TornOcSlot {
        val uid = (slot.user?.id ?: slot.userId)?.takeIf { it > 0L }
        val posInfoLabel = (slot.positionInfo as? JsonObject)
            ?.get("label")?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
        val positionLabel = slot.position?.takeIf { it.isNotBlank() }
            ?: posInfoLabel
            ?: slot.positionId?.takeIf { it.isNotBlank() }
            ?: "Slot #$fallbackIndex"
        val status = slot.status?.takeIf { it.isNotBlank() }
            ?: if (uid != null) "Filled" else "Vacant"
        val cpr = slot.checkpointPassRate ?: slot.successChance
        return TornOcSlot(
            position = positionLabel,
            positionId = slot.positionId ?: slot.id,
            userId = uid,
            userName = slot.user?.name?.takeIf { it.isNotBlank() } ?: uid?.let { "Player $it" },
            status = status,
            checkpointPassRate = cpr,
            itemRequirementName = slot.itemRequirement?.name?.takeIf { it.isNotBlank() },
            itemAvailable = slot.itemRequirement?.isAvailable,
            outcome = slot.user?.outcome?.takeIf { it.isNotBlank() },
            joinedAtSec = slot.user?.joinedAt?.takeIf { it > 0L },
            progress = slot.user?.progress
        )
    }

    private fun crimeStatusSortOrder(status: String): Int = when (status.lowercase()) {
        "ready" -> 0
        "planning", "in progress" -> 1
        "recruiting" -> 2
        "successful", "completed" -> 3
        "failure", "failed", "expired" -> 4
        else -> 5
    }

    private fun stripHtml(input: String): String {
        return input.replace(Regex("<[^>]+>"), "").trim()
    }
}
