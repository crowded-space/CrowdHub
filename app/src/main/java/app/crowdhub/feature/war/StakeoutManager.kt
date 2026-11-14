package app.crowdhub.feature.war

import app.crowdhub.core.alerts.AlertCandidate
import app.crowdhub.core.alerts.AlertContext
import app.crowdhub.core.alerts.AlertSource
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.db.MemberSnapshotEntity
import app.crowdhub.core.db.StakeoutEntity
import app.crowdhub.core.db.WarAndFlightDao
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.settings.FlightAlertTiming
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.feature.flight.FlightEstimate
import app.crowdhub.feature.flight.FlightRepository
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornFactionMember
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class StakeoutManager(
    private val warAndFlightDao: WarAndFlightDao,
    private val flightRepository: FlightRepository,
    private val tornApi: TornApi,
    private val settingsStore: SettingsStore,
    private val clock: Clock,
    private val differ: MemberDiffer = MemberDiffer()
) {
    private val previousSnapshotsByFaction = ConcurrentHashMap<Long, Map<Long, MemberSnapshotRecord>>()
    private val lastPollMsByFaction = ConcurrentHashMap<Long, Long>()

    fun observeStakeouts(accountId: String): Flow<List<StakeoutEntity>> =
        warAndFlightDao.observeStakeouts(accountId)

    suspend fun toggleStar(accountId: String, playerId: Long, currentlyStarred: Boolean) {
        if (currentlyStarred) {
            warAndFlightDao.deleteStakeout(accountId, playerId)
        } else {
            warAndFlightDao.upsertStakeout(
                StakeoutEntity(
                    accountId = accountId,
                    playerId = playerId,
                    alertHospital = true,
                    alertLanding = true,
                    alertOnline = false,
                    alertOkay = true,
                    createdAt = clock.nowEpochMs()
                )
            )
        }
    }

    suspend fun updateStakeoutPrefs(stakeout: StakeoutEntity) {
        warAndFlightDao.upsertStakeout(stakeout)
    }

    suspend fun processFactionMembers(
        accountId: String,
        factionId: Long,
        members: List<TornFactionMember>
    ): List<AlertCandidate> {
        val nowMs = clock.nowEpochMs()
        var previous = previousSnapshotsByFaction[factionId]
        if (previous == null) {
            val dbRows = warAndFlightDao.getSnapshotsForFaction(factionId)
            if (dbRows.isNotEmpty()) {
                previous = dbRows.associate { row ->
                    row.playerId to MemberSnapshotRecord(
                        playerId = row.playerId,
                        name = "Player ${row.playerId}",
                        state = StatusKind.fromRaw(row.state),
                        description = row.descriptionHash,
                        untilSec = row.untilSec,
                        planeType = row.planeType,
                        lastActionStatus = row.lastActionStatus,
                        tsMs = row.ts
                    )
                }
            } else {
                previous = emptyMap()
            }
        }

        val prevPollAt = lastPollMsByFaction[factionId]
        val events = differ.diff(previous, members, prevPollAt, nowMs)

        // Feed FlightRepository first
        for (ev in events) {
            when (ev) {
                is MemberEvent.TravelStarted -> {
                    flightRepository.onTravelObserved(
                        playerId = ev.playerId,
                        description = ev.description,
                        planeTypeApi = ev.planeTypeApi,
                        prevPollAtMs = ev.prevPollAtMs,
                        detectedAtMs = ev.detectedAtMs,
                        departureObserved = ev.departureObserved
                    )
                }
                is MemberEvent.Landed -> {
                    flightRepository.onLanded(
                        playerId = ev.playerId,
                        landedPrevPollAtMs = ev.landedPrevPollAtMs,
                        landedDetectedAtMs = ev.landedDetectedAtMs
                    )
                }
                else -> {}
            }
        }

        // Persist latest snapshot per member
        val newRecordMap = members.associate { m ->
            m.playerId to MemberSnapshotRecord(
                playerId = m.playerId,
                name = m.name,
                state = m.status.state,
                description = m.status.description,
                untilSec = m.status.untilEpochSec,
                planeType = m.status.planeImageType,
                lastActionStatus = m.lastActionStatus,
                tsMs = nowMs
            )
        }
        previousSnapshotsByFaction[factionId] = newRecordMap
        lastPollMsByFaction[factionId] = nowMs

        warAndFlightDao.upsertSnapshots(
            members.map { m ->
                MemberSnapshotEntity(
                    factionId = factionId,
                    playerId = m.playerId,
                    ts = nowMs,
                    state = m.status.state.name,
                    descriptionHash = m.status.description,
                    untilSec = m.status.untilEpochSec,
                    planeType = m.status.planeImageType,
                    lastActionStatus = m.lastActionStatus,
                    lastActionSec = m.lastActionTimestampSec,
                    level = m.level
                )
            }
        )

        val prefs = settingsStore.current()
        if (!prefs.alertsWarEnabled) return emptyList()

        val stakeouts = warAndFlightDao.getStakeouts(accountId).associateBy { it.playerId }
        if (stakeouts.isEmpty()) return emptyList()

        val candidates = mutableListOf<AlertCandidate>()
        val leadMs = prefs.warStakeoutLeadSec * 1_000L

        for (member in members) {
            val stake = stakeouts[member.playerId] ?: continue

            if (stake.alertHospital && member.status.state == StatusKind.HOSPITAL && member.status.untilEpochSec != null) {
                val fireAt = (member.status.untilEpochSec * 1_000L - leadMs).coerceAtLeast(nowMs)
                candidates += AlertCandidate(
                    dedupeKey = "war:${member.playerId}:hosp:${member.status.untilEpochSec}",
                    accountId = accountId,
                    channelId = NotificationChannels.WAR,
                    title = "${member.name} leaving hospital",
                    body = "Hospital timer ends ${if (prefs.warStakeoutLeadSec > 0) "in ${prefs.warStakeoutLeadSec}s" else "now"}.",
                    fireAtEpochMs = fireAt,
                    deepLink = "war"
                )
            }

            if (stake.alertLanding && member.status.state == StatusKind.TRAVELING) {
                val est: FlightEstimate? = flightRepository.estimateForMember(
                    playerId = member.playerId,
                    description = member.status.description,
                    planeTypeApi = member.status.planeImageType,
                    apiUntilSec = member.status.untilEpochSec
                )
                if (est != null) {
                    val baseTargetMs = if (prefs.flightAlertTiming == FlightAlertTiming.MIDPOINT) {
                        (est.windowStartMs + est.windowEndMs) / 2L
                    } else {
                        est.windowStartMs
                    }
                    val fireAt = (baseTargetMs - prefs.flightAlertLeadMin * 60_000L).coerceAtLeast(nowMs)
                    val bucket = fireAt / 60_000L
                    candidates += AlertCandidate(
                        dedupeKey = "war:${member.playerId}:land:$bucket",
                        accountId = accountId,
                        channelId = NotificationChannels.WAR,
                        title = "${member.name} landing soon",
                        body = "Estimated landing in ${est.place.displayName} (${est.confidence.name}).",
                        fireAtEpochMs = fireAt,
                        deepLink = "war"
                    )
                }
            }
        }

        for (ev in events) {
            val stake = stakeouts[ev.playerId] ?: continue
            when (ev) {
                is MemberEvent.StatusChanged -> {
                    if (stake.alertOkay && ev.newState == StatusKind.OKAY) {
                        candidates += AlertCandidate(
                            dedupeKey = "war:${ev.playerId}:okay:${nowMs / 30_000L}",
                            accountId = accountId,
                            channelId = NotificationChannels.WAR,
                            title = "${ev.playerName} is Okay",
                            body = "Target is now attackable.",
                            fireAtEpochMs = null,
                            deepLink = "war"
                        )
                    }
                }
                is MemberEvent.WentOnline -> {
                    if (stake.alertOnline) {
                        candidates += AlertCandidate(
                            dedupeKey = "war:${ev.playerId}:online:${nowMs / 60_000L}",
                            accountId = accountId,
                            channelId = NotificationChannels.WAR,
                            title = "${ev.playerName} came Online",
                            body = "Watched enemy player is now online.",
                            fireAtEpochMs = null,
                            deepLink = "war"
                        )
                    }
                }
                else -> {}
            }
        }

        return candidates
    }

    fun watchModeAlertSource(): AlertSource = object : AlertSource {
        override val id: String = "torn.war_stakeout"

        override suspend fun collect(ctx: AlertContext): List<AlertCandidate> {
            val prefs = settingsStore.current()
            if (!prefs.alertsWarEnabled) return emptyList()

            return when (ctx.mode) {
                AlertTriggerMode.BACKGROUND -> {
                    // One-time "war started?" check every 15 min via rankedwars only
                    val ownFactionId = (tornApi.getOwnFactionId(ctx.accountId, RequestPriority.BACKGROUND) as? ApiResult.Success)?.data
                        ?: return emptyList()
                    val warsRes = tornApi.getRankedWars(ctx.accountId, ownFactionId, RequestPriority.BACKGROUND)
                    val wars = (warsRes as? ApiResult.Success)?.data ?: return emptyList()
                    val active = wars.firstOrNull { it.isActive } ?: return emptyList()
                    listOf(
                        AlertCandidate(
                            dedupeKey = "war:started:${active.warId}:${ctx.accountId}",
                            accountId = ctx.accountId,
                            channelId = NotificationChannels.WAR,
                            title = "Ranked war detected vs ${active.enemyFactionName}",
                            body = "Enemy faction ID ${active.enemyFactionId}. Open War tab to monitor.",
                            fireAtEpochMs = null,
                            deepLink = "war"
                        )
                    )
                }
                AlertTriggerMode.WATCH -> {
                    val manualId = prefs.manualWarFactionId.trim().toLongOrNull()
                    val enemyFactionId = if (manualId != null && manualId > 0L) {
                        manualId
                    } else {
                        val ownId = (tornApi.getOwnFactionId(ctx.accountId, RequestPriority.WATCH) as? ApiResult.Success)?.data
                            ?: return emptyList()
                        val wars = (tornApi.getRankedWars(ctx.accountId, ownId, RequestPriority.WATCH) as? ApiResult.Success)?.data
                            ?: return emptyList()
                        wars.firstOrNull { it.isActive }?.enemyFactionId ?: return emptyList()
                    }
                    val membersRes = tornApi.getFactionMembers(ctx.accountId, enemyFactionId, RequestPriority.WATCH)
                    val members = (membersRes as? ApiResult.Success)?.data ?: return emptyList()
                    processFactionMembers(ctx.accountId, enemyFactionId, members)
                }
                AlertTriggerMode.FOREGROUND -> emptyList()
            }
        }
    }
}
