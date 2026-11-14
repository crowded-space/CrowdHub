package app.crowdhub.feature.faction

import app.crowdhub.core.network.ApiError
import app.crowdhub.feature.flight.FlightEstimate
import app.crowdhub.provider.torn.ArmoryCategory
import app.crowdhub.provider.torn.TornArmoryItem
import app.crowdhub.provider.torn.TornFactionAttack
import app.crowdhub.provider.torn.TornFactionBalance
import app.crowdhub.provider.torn.TornFactionBasic
import app.crowdhub.provider.torn.TornFactionChain
import app.crowdhub.provider.torn.TornFactionMember
import app.crowdhub.provider.torn.TornOrganizedCrime

enum class FactionInnerTab(val label: String, val testTag: String) {
    MEMBERS("Members", "faction_tab_members"),
    WAR("War", "faction_tab_war"),
    CHAIN("Chain & Log", "faction_tab_chain"),
    ARMORY("Armory", "faction_tab_armory"),
    OC("OC", "faction_tab_oc");

    companion object {
        fun fromArg(raw: String?): FactionInnerTab {
            val v = raw?.trim()?.lowercase().orEmpty()
            return when (v) {
                "war" -> WAR
                "chain", "attacks" -> CHAIN
                "armory", "inventory" -> ARMORY
                "oc", "crimes" -> OC
                else -> MEMBERS
            }
        }
    }
}

enum class FactionMemberFilter(val label: String) {
    ALL("All"),
    OKAY("Okay"),
    HOSPITAL("Hospital"),
    TRAVELING("Traveling"),
    ABROAD("Abroad"),
    ONLINE("Online")
}

enum class FactionMemberSort(val label: String) {
    FLIGHT_ETA("Flight ETA"),
    STATUS("Status"),
    LEVEL("Level"),
    NAME("Name"),
    DAYS("Days")
}

enum class FactionAttackFilter(val label: String) {
    ALL("All"),
    OUTGOING("Outgoing"),
    INCOMING("Incoming"),
    CHAIN("Chain Hits"),
    RESPECT("Respect > 0")
}

enum class FactionOcFilter(val label: String) {
    ACTIVE("Active"),
    READY("Ready"),
    RECRUITING("Recruiting"),
    COMPLETED("Completed"),
    ALL("All")
}

data class FactionMemberItem(
    val member: TornFactionMember,
    val flightEstimate: FlightEstimate?,
    val postLandingGraceEndMs: Long?,
    val isStarred: Boolean
)

data class FactionSectionState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val fetchedAtMs: Long = 0L,
    val isStale: Boolean = false,
    val accessRestricted: Boolean = false,
    val error: ApiError? = null
) {
    val hasLoadedOnce: Boolean get() = data != null || error != null || accessRestricted
}

data class FactionOcData(
    val factionCrimes: List<TornOrganizedCrime>,
    val selfCrime: TornOrganizedCrime?,
    val factionCrimesRestricted: Boolean
)

data class FactionUiState(
    val accountId: String? = null,
    val ownPlayerId: Long? = null,
    val ownFactionId: Long? = null,
    val notInFaction: Boolean = false,
    val basic: FactionSectionState<TornFactionBasic> = FactionSectionState(isLoading = true),
    val members: FactionSectionState<List<FactionMemberItem>> = FactionSectionState(isLoading = true),
    val chain: FactionSectionState<TornFactionChain> = FactionSectionState(),
    val attacks: FactionSectionState<List<TornFactionAttack>> = FactionSectionState(),
    val armory: FactionSectionState<List<TornArmoryItem>> = FactionSectionState(),
    val balance: FactionSectionState<TornFactionBalance> = FactionSectionState(),
    val oc: FactionSectionState<FactionOcData> = FactionSectionState(),
    val memberSearch: String = "",
    val memberFilter: FactionMemberFilter = FactionMemberFilter.ALL,
    val memberSort: FactionMemberSort = FactionMemberSort.FLIGHT_ETA,
    val attackFilter: FactionAttackFilter = FactionAttackFilter.ALL,
    val armoryCategory: ArmoryCategory = ArmoryCategory.ALL,
    val armorySearch: String = "",
    val ocFilter: FactionOcFilter = FactionOcFilter.ACTIVE
)
