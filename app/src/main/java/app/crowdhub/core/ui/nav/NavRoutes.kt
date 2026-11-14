package app.crowdhub.core.ui.nav

import kotlinx.serialization.Serializable

sealed interface CrowRoute {
    @Serializable
    data object Onboarding : CrowRoute

    @Serializable
    data object Dashboard : CrowRoute

    @Serializable
    data object Stocks : CrowRoute

    @Serializable
    data class StockDetail(val stockId: Int) : CrowRoute

    @Serializable
    data class Faction(val initialTab: String? = null) : CrowRoute

    @Serializable
    data object War : CrowRoute

    @Serializable
    data object Targets : CrowRoute

    @Serializable
    data class Browser(val initialUrl: String? = null) : CrowRoute

    @Serializable
    data object Scripts : CrowRoute

    @Serializable
    data class ScriptEditor(val scriptId: String? = null) : CrowRoute

    @Serializable
    data object Settings : CrowRoute

    @Serializable
    data object Profiles : CrowRoute

    @Serializable
    data class AccountDetail(val accountId: String) : CrowRoute

    @Serializable
    data object Privacy : CrowRoute

    @Serializable
    data object About : CrowRoute

    @Serializable
    data object Reliability : CrowRoute

    @Serializable
    data object DeveloperProbe : CrowRoute

    @Serializable
    data object FlightDebug : CrowRoute
}
