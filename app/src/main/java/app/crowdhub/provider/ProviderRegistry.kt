package app.crowdhub.provider

class ProviderRegistry(providers: List<Provider>) {
    private val map = providers.associateBy { it.id }

    fun get(id: String): Provider? = map[id]

    fun all(): List<Provider> = map.values.toList()
}
