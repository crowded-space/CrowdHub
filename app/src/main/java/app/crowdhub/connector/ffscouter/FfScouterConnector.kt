package app.crowdhub.connector.ffscouter

import app.crowdhub.connector.Connector

object FfDefaults {
    const val CONNECTOR_ID = "ffscouter"
    const val BASE_URL = "https://ffscouter.com/api/v1"
    const val SITE_URL = "https://ffscouter.com"
    const val RATE_LIMIT_PER_MIN = 10
    const val BATCH_SIZE = 50
    const val CACHE_TTL_MS = 3_600_000L
    const val BACKOFF_429_MS = 60_000L
}

class FfScouterConnector : Connector {
    override val id: String = FfDefaults.CONNECTOR_ID
    override val parentProviderId: String = "torn"
    override val requiresConsent: Boolean = true
}
