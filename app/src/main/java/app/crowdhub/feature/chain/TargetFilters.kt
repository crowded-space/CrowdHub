package app.crowdhub.feature.chain

import app.crowdhub.core.db.StakeoutEntity
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.feature.flight.FlightEstimate

enum class TargetSourceTab(val label: String) {
    HOSPITAL("Hospital"),
    FFSCOUTER("FFScouter"),
    MY_LIST("My List")
}

enum class PeopleInnerTab(val label: String, val testTag: String) {
    FRIENDS("Friends", "people_tab_friends"),
    TARGETS("Targets", "people_tab_targets"),
    ENEMIES("Enemies", "people_tab_enemies"),
    CHAIN("Chain", "people_tab_chain"),
    ADDITIONAL("Additional", "people_tab_additional"),
    STAKEOUT("Stakeout", "people_tab_stakeout")
}

enum class ChainSubTab(val label: String, val testTag: String) {
    HOSPITAL("Hospital", "chain_subtab_hospital"),
    FFSCOUTER("FFScouter", "chain_subtab_ffscouter")
}

enum class TargetStatusFilter(val label: String) {
    ALL("All"),
    HOSPITAL("Hospital"),
    OKAY("Okay")
}

enum class TargetSortMode(val label: String) {
    LEAVES_SOONEST("Leaves soonest"),
    FF_DESC("Fair Fight ↓"),
    BS_ASC("Battle Stats ↑"),
    LEVEL("Level"),
    NAME("Name")
}

data class TargetItem(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: StatusKind,
    val hospitalLeavesAtSec: Long?,
    val lastActionSec: Long?,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val note: String?,
    val watched: Boolean,
    val source: TargetSourceTab,
    val capturedAtMs: Long? = null
)

data class PeopleUserListItem(
    val playerId: Long,
    val name: String,
    val level: Int?,
    val status: StatusKind,
    val statusDescription: String,
    val hospitalLeavesAtSec: Long?,
    val lastActionStatus: String?,
    val lastActionRelative: String?,
    val lastActionSec: Long?,
    val note: String?,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val stakedOut: Boolean,
    val inAdditional: Boolean
)

data class StakeoutHubItem(
    val stakeout: StakeoutEntity,
    val name: String,
    val level: Int?,
    val status: StatusKind,
    val statusDescription: String,
    val hospitalLeavesAtSec: Long?,
    val lastActionStatus: String?,
    val lastActionRelative: String?,
    val lastActionSec: Long?,
    val flightEstimate: FlightEstimate?,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val inAdditional: Boolean
)

data class TargetFilterCriteria(
    val minFf: Double? = null,
    val maxFf: Double? = null,
    val minLevel: Int? = null,
    val maxLevel: Int? = null,
    val maxBs: Long? = null,
    val statusFilter: TargetStatusFilter = TargetStatusFilter.ALL,
    val leavingWithinMin: Int? = null,
    val hideOfflineOverHours: Int? = null
)

object TargetFilters {
    fun apply(
        items: List<TargetItem>,
        criteria: TargetFilterCriteria,
        sortMode: TargetSortMode,
        nowMs: Long
    ): List<TargetItem> {
        val nowSec = nowMs / 1_000L
        return items.filter { item ->
            if (criteria.minFf != null && (item.fairFight == null || item.fairFight < criteria.minFf)) {
                return@filter false
            }
            if (criteria.maxFf != null && (item.fairFight == null || item.fairFight > criteria.maxFf)) {
                return@filter false
            }
            if (criteria.minLevel != null && (item.level == null || item.level < criteria.minLevel)) {
                return@filter false
            }
            if (criteria.maxLevel != null && (item.level == null || item.level > criteria.maxLevel)) {
                return@filter false
            }
            if (criteria.maxBs != null && (item.bsEstimate == null || item.bsEstimate > criteria.maxBs)) {
                return@filter false
            }
            when (criteria.statusFilter) {
                TargetStatusFilter.ALL -> {}
                TargetStatusFilter.HOSPITAL -> if (item.status != StatusKind.HOSPITAL) return@filter false
                TargetStatusFilter.OKAY -> if (item.status != StatusKind.OKAY) return@filter false
            }
            if (criteria.leavingWithinMin != null) {
                val leavesAt = item.hospitalLeavesAtSec
                if (item.status != StatusKind.HOSPITAL || leavesAt == null) return@filter false
                val remSec = leavesAt - nowSec
                if (remSec < 0 || remSec > criteria.leavingWithinMin * 60L) return@filter false
            }
            if (criteria.hideOfflineOverHours != null) {
                val lastSec = item.lastActionSec
                if (lastSec != null && (nowSec - lastSec) > criteria.hideOfflineOverHours * 3600L) {
                    return@filter false
                }
            }
            true
        }.sortedWith(
            when (sortMode) {
                TargetSortMode.LEAVES_SOONEST -> compareBy<TargetItem>(
                    { if (it.status == StatusKind.OKAY) 0 else 1 },
                    { it.hospitalLeavesAtSec ?: Long.MAX_VALUE },
                    { it.playerId }
                )
                TargetSortMode.FF_DESC -> compareByDescending<TargetItem> { it.fairFight ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.playerId }
                TargetSortMode.BS_ASC -> compareBy<TargetItem> { it.bsEstimate ?: Long.MAX_VALUE }
                    .thenBy { it.playerId }
                TargetSortMode.LEVEL -> compareByDescending<TargetItem> { it.level ?: -1 }
                    .thenBy { it.playerId }
                TargetSortMode.NAME -> compareBy<TargetItem> { it.name.lowercase() }
                    .thenBy { it.playerId }
            }
        )
    }

    fun filterPeopleList(
        items: List<PeopleUserListItem>,
        query: String,
        statusFilter: TargetStatusFilter
    ): List<PeopleUserListItem> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            if (q.isNotEmpty() && !item.name.lowercase().contains(q) && !item.playerId.toString().contains(q)) {
                return@filter false
            }
            when (statusFilter) {
                TargetStatusFilter.ALL -> true
                TargetStatusFilter.HOSPITAL -> item.status == StatusKind.HOSPITAL
                TargetStatusFilter.OKAY -> item.status == StatusKind.OKAY
            }
        }
    }
}
