package app.crowdhub.provider.torn

import app.crowdhub.core.alerts.AlertCandidate
import app.crowdhub.core.alerts.AlertContext
import app.crowdhub.core.alerts.AlertSource
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.StatusKind

class TornTimerAlertSource(
    private val api: TornApi,
    private val settingsStore: SettingsStore
) : AlertSource {
    override val id: String = "torn.timers"

    override suspend fun collect(ctx: AlertContext): List<AlertCandidate> {
        val prefs = settingsStore.current()
        val priority = when (ctx.mode) {
            AlertTriggerMode.FOREGROUND -> RequestPriority.USER
            AlertTriggerMode.WATCH -> RequestPriority.WATCH
            AlertTriggerMode.BACKGROUND -> RequestPriority.BACKGROUND
        }
        val candidates = mutableListOf<AlertCandidate>()
        val nowMs = ctx.nowEpochMs

        if (prefs.alertsBarsEnabled) {
            val barsRes = api.getBars(ctx.accountId, priority)
            if (barsRes is ApiResult.Success) {
                val b = barsRes.data
                listOfNotNull(b.energy, b.nerve, b.happy, b.life).forEach { bar ->
                    val fullSec = bar.fullTimeSec
                    if (bar.current < bar.maximum && fullSec != null && fullSec > 0L) {
                        val fireAtMs = nowMs + fullSec * 1_000L
                        candidates += AlertCandidate(
                            dedupeKey = "torn:bar:${bar.label.lowercase()}:${ctx.accountId}",
                            accountId = ctx.accountId,
                            channelId = NotificationChannels.TIMERS,
                            title = "${bar.label} full",
                            body = "${bar.label} reached ${bar.maximum}/${bar.maximum}",
                            fireAtEpochMs = fireAtMs,
                            deepLink = "dashboard"
                        )
                    }
                }
            }
        }

        if (prefs.alertsCooldownsEnabled) {
            val cdRes = api.getCooldowns(ctx.accountId, priority)
            if (cdRes is ApiResult.Success) {
                val cd = cdRes.data
                mapOf(
                    "drug" to cd.drugSec,
                    "medical" to cd.medicalSec,
                    "booster" to cd.boosterSec
                ).forEach { (kind, sec) ->
                    if (sec > 0L) {
                        candidates += AlertCandidate(
                            dedupeKey = "torn:cooldown:$kind:${ctx.accountId}",
                            accountId = ctx.accountId,
                            channelId = NotificationChannels.TIMERS,
                            title = "${kind.replaceFirstChar { it.uppercase() }} cooldown ready",
                            body = "Your $kind cooldown has expired.",
                            fireAtEpochMs = nowMs + sec * 1_000L,
                            deepLink = "dashboard"
                        )
                    }
                }
            }
        }

        if (prefs.alertsStatusEnabled) {
            val basicRes = api.getBasic(ctx.accountId, priority)
            if (basicRes is ApiResult.Success) {
                val st = basicRes.data.status
                val untilSec = st.untilEpochSec
                if (untilSec != null && untilSec * 1_000L > nowMs) {
                    when (st.state) {
                        StatusKind.HOSPITAL -> {
                            candidates += AlertCandidate(
                                dedupeKey = "torn:status:hospital:${ctx.accountId}",
                                accountId = ctx.accountId,
                                channelId = NotificationChannels.TIMERS,
                                title = "Out of hospital",
                                body = "You have been released from the hospital.",
                                fireAtEpochMs = untilSec * 1_000L,
                                deepLink = "dashboard"
                            )
                        }
                        StatusKind.JAIL, StatusKind.FEDERAL -> {
                            candidates += AlertCandidate(
                                dedupeKey = "torn:status:jail:${ctx.accountId}",
                                accountId = ctx.accountId,
                                channelId = NotificationChannels.TIMERS,
                                title = "Out of jail",
                                body = "Your jail timer has finished.",
                                fireAtEpochMs = untilSec * 1_000L,
                                deepLink = "dashboard"
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        if (prefs.alertsTravelEnabled) {
            val travelRes = api.getTravel(ctx.accountId, priority)
            if (travelRes is ApiResult.Success) {
                val t = travelRes.data
                val arrivalSec = t.arrivalEpochSec
                if (t.isActivelyFlying && arrivalSec != null) {
                    val arrivalMs = arrivalSec * 1_000L
                    if (arrivalMs > nowMs) {
                        candidates += AlertCandidate(
                            dedupeKey = "torn:travel:land:${ctx.accountId}:$arrivalSec",
                            accountId = ctx.accountId,
                            channelId = NotificationChannels.TIMERS,
                            title = "Landed in ${t.destination}",
                            body = "Your flight has arrived in ${t.destination}.",
                            fireAtEpochMs = arrivalMs,
                            deepLink = "dashboard"
                        )
                        val leadMin = prefs.landingLeadMin
                        if (leadMin > 0) {
                            val leadMs = arrivalMs - leadMin * 60_000L
                            if (leadMs > nowMs) {
                                candidates += AlertCandidate(
                                    dedupeKey = "torn:travel:lead:${ctx.accountId}:$arrivalSec:$leadMin",
                                    accountId = ctx.accountId,
                                    channelId = NotificationChannels.TIMERS,
                                    title = "Landing in $leadMin min",
                                    body = "Arriving in ${t.destination} in $leadMin minutes.",
                                    fireAtEpochMs = leadMs,
                                    deepLink = "dashboard"
                                )
                            }
                        }
                    }
                }
            }
        }

        return candidates
    }
}
