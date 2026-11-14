package app.crowdhub.connector

interface Connector {
    val id: String
    val parentProviderId: String
    val requiresConsent: Boolean
}
