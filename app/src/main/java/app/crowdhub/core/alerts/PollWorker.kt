package app.crowdhub.core.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.crowdhub.core.network.CrowLog
import java.util.concurrent.TimeUnit

class PollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val host = applicationContext as? AlertHost ?: return Result.success()
        return try {
            host.alertEngine.runCycle(AlertTriggerMode.BACKGROUND)
            host.runStockCleanup()
            host.settingsStore.recordSyncStatus(
                epochMs = host.clock.nowEpochMs(),
                summary = "15m background sync completed"
            )
            Result.success()
        } catch (t: Throwable) {
            CrowLog.w(message = "PollWorker cycle failed", throwable = t)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "crowdhub_background_poll"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
