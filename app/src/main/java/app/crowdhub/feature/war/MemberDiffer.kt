package app.crowdhub.feature.war

import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.provider.torn.FlightTable
import app.crowdhub.provider.torn.PlaneCategory
import app.crowdhub.provider.torn.TornFactionMember
import app.crowdhub.provider.torn.TravelDirection
import app.crowdhub.provider.torn.TravelPlace

data class MemberSnapshotRecord(
    val playerId: Long,
    val name: String,
    val state: StatusKind,
    val description: String,
    val untilSec: Long?,
    val planeType: String?,
    val lastActionStatus: String?,
    val tsMs: Long
)

sealed class MemberEvent {
    abstract val playerId: Long
    abstract val playerName: String

    data class StatusChanged(
        override val playerId: Long,
        override val playerName: String,
        val oldState: StatusKind,
        val newState: StatusKind
    ) : MemberEvent()

    data class HospitalEntered(
        override val playerId: Long,
        override val playerName: String,
        val untilSec: Long?
    ) : MemberEvent()

    data class HospitalLeft(
        override val playerId: Long,
        override val playerName: String
    ) : MemberEvent()

    data class TravelStarted(
        override val playerId: Long,
        override val playerName: String,
        val direction: TravelDirection,
        val place: TravelPlace,
        val description: String,
        val planeTypeApi: String?,
        val prevPollAtMs: Long?,
        val detectedAtMs: Long,
        val departureObserved: Boolean
    ) : MemberEvent()

    data class Landed(
        override val playerId: Long,
        override val playerName: String,
        val place: TravelPlace,
        val landedPrevPollAtMs: Long,
        val landedDetectedAtMs: Long
    ) : MemberEvent()

    data class WentOnline(
        override val playerId: Long,
        override val playerName: String
    ) : MemberEvent()

    data class WentOffline(
        override val playerId: Long,
        override val playerName: String
    ) : MemberEvent()

    data class IdleChanged(
        override val playerId: Long,
        override val playerName: String,
        val oldAction: String?,
        val newAction: String?
    ) : MemberEvent()

    data class UntilChanged(
        override val playerId: Long,
        override val playerName: String,
        val oldUntilSec: Long?,
        val newUntilSec: Long?
    ) : MemberEvent()
}

class MemberDiffer {
    fun diff(
        previous: Map<Long, MemberSnapshotRecord>,
        current: List<TornFactionMember>,
        prevPollAtMs: Long?,
        nowMs: Long
    ): List<MemberEvent> {
        val events = mutableListOf<MemberEvent>()

        for (member in current) {
            val prev = previous[member.playerId]
            val curState = member.status.state
            val curDesc = member.status.description
            val curUntil = member.status.untilEpochSec
            val curAction = member.lastActionStatus

            if (prev == null) {
                // First time seeing this member: if already traveling, emit TravelStarted with departureObserved = false
                if (curState == StatusKind.TRAVELING) {
                    val parsed = FlightTable.parseDescription(curDesc)
                    events += MemberEvent.TravelStarted(
                        playerId = member.playerId,
                        playerName = member.name,
                        direction = parsed.direction,
                        place = parsed.place,
                        description = curDesc,
                        planeTypeApi = member.status.planeImageType,
                        prevPollAtMs = null,
                        detectedAtMs = nowMs,
                        departureObserved = false
                    )
                }
                continue
            }

            if (prev.state != curState) {
                events += MemberEvent.StatusChanged(member.playerId, member.name, prev.state, curState)

                if (curState == StatusKind.HOSPITAL) {
                    events += MemberEvent.HospitalEntered(member.playerId, member.name, curUntil)
                } else if (prev.state == StatusKind.HOSPITAL) {
                    events += MemberEvent.HospitalLeft(member.playerId, member.name)
                }

                if (curState == StatusKind.TRAVELING) {
                    val parsed = FlightTable.parseDescription(curDesc)
                    events += MemberEvent.TravelStarted(
                        playerId = member.playerId,
                        playerName = member.name,
                        direction = parsed.direction,
                        place = parsed.place,
                        description = curDesc,
                        planeTypeApi = member.status.planeImageType,
                        prevPollAtMs = prevPollAtMs ?: prev.tsMs,
                        detectedAtMs = nowMs,
                        departureObserved = true
                    )
                } else if (prev.state == StatusKind.TRAVELING) {
                    val parsedPrev = FlightTable.parseDescription(prev.description)
                    events += MemberEvent.Landed(
                        playerId = member.playerId,
                        playerName = member.name,
                        place = parsedPrev.place,
                        landedPrevPollAtMs = prevPollAtMs ?: prev.tsMs,
                        landedDetectedAtMs = nowMs
                    )
                }
            } else {
                // Same state; check if hospital/jail until changed significantly (> 10s)
                if (prev.untilSec != curUntil && curUntil != null &&
                    (prev.untilSec == null || kotlin.math.abs(curUntil - prev.untilSec) > 10L)
                ) {
                    events += MemberEvent.UntilChanged(member.playerId, member.name, prev.untilSec, curUntil)
                }
            }

            val prevOnline = prev.lastActionStatus?.equals("Online", ignoreCase = true) == true
            val curOnline = curAction?.equals("Online", ignoreCase = true) == true
            if (prev.lastActionStatus != null && curAction != null && !prev.lastActionStatus.equals(curAction, ignoreCase = true)) {
                if (!prevOnline && curOnline) {
                    events += MemberEvent.WentOnline(member.playerId, member.name)
                } else if (prevOnline && !curOnline) {
                    events += MemberEvent.WentOffline(member.playerId, member.name)
                } else {
                    events += MemberEvent.IdleChanged(member.playerId, member.name, prev.lastActionStatus, curAction)
                }
            }
        }
        return events
    }
}
