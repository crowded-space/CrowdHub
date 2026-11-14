package app.crowdhub.core.profile

import app.crowdhub.core.db.ProfileEntity
import app.crowdhub.core.db.ProviderAccountEntity

enum class AccountStatus {
    UNKNOWN,
    VALID,
    INVALID,
    PAUSED;

    companion object {
        fun fromRaw(raw: String?): AccountStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

data class ActiveProfileState(
    val profile: ProfileEntity?,
    val primaryAccount: ProviderAccountEntity?,
    val accounts: List<ProviderAccountEntity>
) {
    val accountStatus: AccountStatus
        get() = AccountStatus.fromRaw(primaryAccount?.status)
}
