package app.crowdhub.core.alerts

enum class AlertTriggerMode {
    FOREGROUND,
    WATCH,
    BACKGROUND
}

data class AlertCandidate(
    val dedupeKey: String,
    val accountId: String,
    val channelId: String,
    val title: String,
    val body: String,
    val fireAtEpochMs: Long?, // null = fire immediately
    val deepLink: String? = null
)

data class AlertContext(
    val accountId: String,
    val externalId: String?,
    val mode: AlertTriggerMode,
    val nowEpochMs: Long
)

interface AlertSource {
    val id: String
    suspend fun collect(ctx: AlertContext): List<AlertCandidate>
}
