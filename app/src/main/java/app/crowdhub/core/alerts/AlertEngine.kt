package app.crowdhub.core.alerts

import app.crowdhub.core.db.ProfileDao
import app.crowdhub.core.db.ProviderAccountDao
import app.crowdhub.core.db.ScheduledAlertDao
import app.crowdhub.core.db.ScheduledAlertEntity
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.CrowLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AlertEngine(
    private val profileDao: ProfileDao,
    private val accountDao: ProviderAccountDao,
    private val scheduledAlertDao: ScheduledAlertDao,
    private val scheduler: Scheduler,
    private val clock: Clock,
    private val deduper: AlertDeduper = AlertDeduper()
) {
    private val mutex = Mutex()
    private val sources = mutableListOf<AlertSource>()

    fun registerSources(newSources: List<AlertSource>) {
        sources.clear()
        sources.addAll(newSources)
    }

    suspend fun runCycle(
        mode: AlertTriggerMode,
        managedPrefixes: Set<String> = DEFAULT_MANAGED_PREFIXES
    ) = mutex.withLock {
        val activeProfile = profileDao.getActiveProfile() ?: return@withLock
        val account = accountDao.getPrimaryForProfile(activeProfile.id) ?: return@withLock

        if (account.status != "VALID") {
            CrowLog.d(message = "Skipping alert cycle for account=${account.id} status=${account.status}")
            return@withLock
        }

        val nowMs = clock.nowEpochMs()
        val ctx = AlertContext(
            accountId = account.id,
            externalId = account.externalId,
            mode = mode,
            nowEpochMs = nowMs
        )

        val candidates = mutableListOf<AlertCandidate>()
        for (source in sources) {
            runCatching { source.collect(ctx) }
                .onSuccess { candidates.addAll(it) }
                .onFailure { CrowLog.w(message = "AlertSource ${source.id} failed", throwable = it) }
        }

        reconcileCandidates(
            accountId = account.id,
            managedPrefixes = managedPrefixes,
            candidates = candidates,
            nowMs = nowMs
        )
    }

    suspend fun reconcileCandidates(
        accountId: String,
        managedPrefixes: Set<String>,
        candidates: List<AlertCandidate>,
        nowMs: Long = clock.nowEpochMs()
    ) {
        val existingEntities = scheduledAlertDao.getScheduledForAccount(accountId) +
            candidates.mapNotNull { scheduledAlertDao.getByKey(it.dedupeKey) }
        val existingRecords = existingEntities.distinctBy { it.dedupeKey }.map { entity ->
            PersistedAlertRecord(
                dedupeKey = entity.dedupeKey,
                accountId = entity.accountId,
                channelId = entity.channelId,
                title = entity.title,
                body = entity.body,
                fireAt = entity.fireAt,
                deepLink = entity.deepLink,
                state = runCatching { PersistedAlertState.valueOf(entity.state) }
                    .getOrDefault(PersistedAlertState.SCHEDULED)
            )
        }

        val actions = deduper.reconcile(
            accountId = accountId,
            managedKeyPrefixes = managedPrefixes,
            candidates = candidates,
            existing = existingRecords,
            nowMs = nowMs
        )

        for (action in actions) {
            when (action) {
                is DedupeAction.FireNow -> {
                    val c = action.candidate
                    scheduler.postNotification(c.dedupeKey, c.channelId, c.title, c.body, c.deepLink)
                    scheduledAlertDao.upsert(
                        ScheduledAlertEntity(
                            dedupeKey = c.dedupeKey,
                            accountId = c.accountId,
                            channelId = c.channelId,
                            title = c.title,
                            body = c.body,
                            fireAt = c.fireAtEpochMs ?: nowMs,
                            deepLink = c.deepLink,
                            state = PersistedAlertState.FIRED.name,
                            updatedAt = nowMs
                        )
                    )
                }
                is DedupeAction.Schedule -> {
                    val c = action.candidate
                    val fireAt = c.fireAtEpochMs ?: continue
                    scheduler.scheduleAlarm(c.dedupeKey, c.channelId, c.title, c.body, fireAt, c.deepLink)
                    scheduledAlertDao.upsert(
                        ScheduledAlertEntity(
                            dedupeKey = c.dedupeKey,
                            accountId = c.accountId,
                            channelId = c.channelId,
                            title = c.title,
                            body = c.body,
                            fireAt = fireAt,
                            deepLink = c.deepLink,
                            state = PersistedAlertState.SCHEDULED.name,
                            updatedAt = nowMs
                        )
                    )
                }
                is DedupeAction.Cancel -> {
                    scheduler.cancelAlarm(action.dedupeKey)
                    scheduledAlertDao.updateState(action.dedupeKey, PersistedAlertState.CANCELLED.name, nowMs)
                }
            }
        }
    }

    suspend fun rescheduleAllOnBoot() {
        val nowMs = clock.nowEpochMs()
        val scheduled = scheduledAlertDao.getScheduled()
        for (item in scheduled) {
            val acct = accountDao.getById(item.accountId)
            if (acct == null || acct.status != "VALID") {
                scheduledAlertDao.updateState(item.dedupeKey, PersistedAlertState.CANCELLED.name, nowMs)
                continue
            }
            if (item.fireAt <= nowMs) {
                scheduler.postNotification(item.dedupeKey, item.channelId, item.title, item.body, item.deepLink)
                scheduledAlertDao.updateState(item.dedupeKey, PersistedAlertState.FIRED.name, nowMs)
            } else {
                scheduler.scheduleAlarm(item.dedupeKey, item.channelId, item.title, item.body, item.fireAt, item.deepLink)
            }
        }
    }

    suspend fun cancelAllForAccount(accountId: String) {
        val scheduled = scheduledAlertDao.getScheduledForAccount(accountId)
        for (item in scheduled) {
            scheduler.cancelAlarm(item.dedupeKey)
        }
        scheduledAlertDao.deleteForAccount(accountId)
    }

    companion object {
        val DEFAULT_MANAGED_PREFIXES = setOf(
            "torn:bar:",
            "torn:cooldown:",
            "torn:status:",
            "torn:travel:"
        )
    }
}
