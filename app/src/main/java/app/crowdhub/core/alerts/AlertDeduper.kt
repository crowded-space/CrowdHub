package app.crowdhub.core.alerts

import kotlin.math.abs

enum class PersistedAlertState {
    SCHEDULED,
    FIRED,
    CANCELLED
}

data class PersistedAlertRecord(
    val dedupeKey: String,
    val accountId: String,
    val channelId: String,
    val title: String,
    val body: String,
    val fireAt: Long,
    val deepLink: String?,
    val state: PersistedAlertState
)

sealed class DedupeAction {
    data class FireNow(val candidate: AlertCandidate) : DedupeAction()
    data class Schedule(val candidate: AlertCandidate) : DedupeAction()
    data class Cancel(val dedupeKey: String) : DedupeAction()
}

class AlertDeduper(
    private val rescheduleThresholdMs: Long = RESCHEDULE_THRESHOLD_MS
) {
    fun reconcile(
        accountId: String,
        managedKeyPrefixes: Set<String>,
        candidates: List<AlertCandidate>,
        existing: List<PersistedAlertRecord>,
        nowMs: Long
    ): List<DedupeAction> {
        val actions = mutableListOf<DedupeAction>()
        val existingMap = existing.associateBy { it.dedupeKey }
        val candidateKeys = candidates.map { it.dedupeKey }.toSet()

        for (candidate in candidates) {
            val prev = existingMap[candidate.dedupeKey]
            val targetFireAt = candidate.fireAtEpochMs
            if (targetFireAt == null || targetFireAt <= nowMs) {
                val alreadyFiredForThisTime = prev != null &&
                    prev.state == PersistedAlertState.FIRED &&
                    (targetFireAt == null || abs(prev.fireAt - targetFireAt) <= rescheduleThresholdMs)
                if (!alreadyFiredForThisTime) {
                    actions += DedupeAction.FireNow(candidate)
                }
            } else {
                val unchanged = prev != null &&
                    prev.state == PersistedAlertState.SCHEDULED &&
                    abs(prev.fireAt - targetFireAt) <= rescheduleThresholdMs &&
                    prev.title == candidate.title &&
                    prev.body == candidate.body
                val alreadyFiredSimilar = prev != null &&
                    prev.state == PersistedAlertState.FIRED &&
                    abs(prev.fireAt - targetFireAt) <= rescheduleThresholdMs
                if (!unchanged && !alreadyFiredSimilar) {
                    actions += DedupeAction.Schedule(candidate)
                }
            }
        }

        for (record in existing) {
            if (record.accountId != accountId) continue
            if (record.state != PersistedAlertState.SCHEDULED) continue
            val belongsToManagedPrefix = managedKeyPrefixes.any { record.dedupeKey.startsWith(it) }
            if (belongsToManagedPrefix && record.dedupeKey !in candidateKeys) {
                actions += DedupeAction.Cancel(record.dedupeKey)
            }
        }

        return actions
    }

    companion object {
        const val RESCHEDULE_THRESHOLD_MS = 10_000L
    }
}
