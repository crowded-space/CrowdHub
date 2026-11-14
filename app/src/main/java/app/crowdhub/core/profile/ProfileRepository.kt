package app.crowdhub.core.profile

import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.db.CrowdDatabase
import app.crowdhub.core.db.ProfileEntity
import app.crowdhub.core.db.ProviderAccountEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.ResponseCache
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.vault.SecretVault
import app.crowdhub.core.vault.VaultResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

sealed class KeySaveOutcome {
    data class Saved(val account: ProviderAccountEntity) : KeySaveOutcome()
    data class DifferentPlayerWarning(
        val existingExternalId: String,
        val newExternalId: String,
        val newDisplayName: String,
        val newAccessLevel: String
    ) : KeySaveOutcome()
    data class Error(val message: String) : KeySaveOutcome()
}

data class CredentialValidationResult(
    val externalId: String?,
    val displayName: String?,
    val accessLevel: String?,
    val error: ApiError?
)

class ProfileRepository(
    private val db: CrowdDatabase,
    private val vault: SecretVault,
    private val alertEngine: AlertEngine,
    private val scheduler: Scheduler,
    private val responseCache: ResponseCache,
    private val settingsStore: SettingsStore,
    private val clock: Clock,
    private val onWipeWebProfile: suspend (String) -> Unit
) {
    private val profileDao = db.profileDao()
    private val accountDao = db.providerAccountDao()

    fun observeProfiles(): Flow<List<ProfileEntity>> = profileDao.observeProfiles()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveState(): Flow<ActiveProfileState> {
        return profileDao.observeActiveProfile().flatMapLatest { active ->
            if (active == null) {
                flowOf(ActiveProfileState(null, null, emptyList()))
            } else {
                accountDao.observeForProfile(active.id).combine(flowOf(active)) { accounts, profile ->
                    ActiveProfileState(
                        profile = profile,
                        primaryAccount = accounts.firstOrNull(),
                        accounts = accounts
                    )
                }
            }
        }
    }

    suspend fun getActiveProfile(): ProfileEntity? = profileDao.getActiveProfile()

    suspend fun getActiveAccount(): ProviderAccountEntity? {
        val active = profileDao.getActiveProfile() ?: return null
        return accountDao.getPrimaryForProfile(active.id)
    }

    suspend fun determineStartStep(): OnboardingStep {
        val prefs = settingsStore.current()
        val activeProfile = profileDao.getActiveProfile()
        val account = activeProfile?.let { accountDao.getPrimaryForProfile(it.id) }
        val hasSecret = account?.let { vault.has(SecretVault.apiKeyAlias(it.id)) } == true

        return when {
            prefs.onboardingCompleted && activeProfile != null && account != null && hasSecret ->
                OnboardingStep.DONE
            activeProfile == null -> OnboardingStep.WELCOME
            account == null || !hasSecret -> OnboardingStep.ADD_ACCOUNT
            else -> OnboardingStep.PERMISSIONS
        }
    }

    suspend fun createOrActivateProfile(name: String): ProfileEntity {
        val existingActive = profileDao.getActiveProfile()
        val cleanName = name.trim().ifEmpty { "Main" }
        if (existingActive != null && accountDao.getPrimaryForProfile(existingActive.id) == null) {
            profileDao.rename(existingActive.id, cleanName)
            return existingActive.copy(name = cleanName)
        }
        val id = UUID.randomUUID().toString()
        val entity = ProfileEntity(
            id = id,
            name = cleanName,
            createdAt = clock.nowEpochMs(),
            isActive = true
        )
        profileDao.upsert(entity)
        profileDao.setActive(id)
        return entity
    }

    suspend fun renameProfile(profileId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            profileDao.rename(profileId, clean)
        }
    }

    suspend fun switchActiveProfile(profileId: String) {
        profileDao.setActive(profileId)
    }

    suspend fun createValidatedAccount(
        profileId: String,
        providerId: String,
        rawKey: String,
        validator: suspend (accountId: String, secret: String) -> CredentialValidationResult
    ): KeySaveOutcome {
        val cleanKey = rawKey.trim()
        if (cleanKey.isEmpty()) {
            return KeySaveOutcome.Error("Enter your API key")
        }
        val accountId = UUID.randomUUID().toString()
        val validation = validator(accountId, cleanKey)
        if (validation.error != null || validation.externalId == null) {
            return KeySaveOutcome.Error(validation.error?.message ?: "Validation failed")
        }

        val vaultSave = vault.put(SecretVault.apiKeyAlias(accountId), cleanKey)
        if (vaultSave is VaultResult.KeystoreError) {
            return KeySaveOutcome.Error(vaultSave.message)
        }

        val now = clock.nowEpochMs()
        val entity = ProviderAccountEntity(
            id = accountId,
            profileId = profileId,
            providerId = providerId,
            externalId = validation.externalId,
            displayName = validation.displayName,
            status = AccountStatus.VALID.name,
            accessLevel = validation.accessLevel ?: "Limited",
            lastValidatedAt = now,
            createdAt = now
        )
        accountDao.upsert(entity)
        return KeySaveOutcome.Saved(entity)
    }

    suspend fun replaceAccountKey(
        accountId: String,
        rawKey: String,
        confirmDifferentPlayerReset: Boolean,
        validator: suspend (accountId: String, secret: String) -> CredentialValidationResult
    ): KeySaveOutcome {
        val cleanKey = rawKey.trim()
        if (cleanKey.isEmpty()) {
            return KeySaveOutcome.Error("Enter your new API key")
        }
        val existing = accountDao.getById(accountId)
            ?: return KeySaveOutcome.Error("Account not found")

        val validation = validator(accountId, cleanKey)
        if (validation.error != null || validation.externalId == null) {
            return KeySaveOutcome.Error(validation.error?.message ?: "Validation failed")
        }

        if (!confirmDifferentPlayerReset &&
            !existing.externalId.isNullOrBlank() &&
            existing.externalId != validation.externalId
        ) {
            return KeySaveOutcome.DifferentPlayerWarning(
                existingExternalId = existing.externalId,
                newExternalId = validation.externalId,
                newDisplayName = validation.displayName ?: validation.externalId,
                newAccessLevel = validation.accessLevel ?: "Limited"
            )
        }

        if (confirmDifferentPlayerReset) {
            wipeAccountData(accountId)
        }

        val vaultSave = vault.put(SecretVault.apiKeyAlias(accountId), cleanKey)
        if (vaultSave is VaultResult.KeystoreError) {
            return KeySaveOutcome.Error(vaultSave.message)
        }

        val updated = existing.copy(
            externalId = validation.externalId,
            displayName = validation.displayName,
            status = AccountStatus.VALID.name,
            accessLevel = validation.accessLevel ?: "Limited",
            lastValidatedAt = clock.nowEpochMs()
        )
        accountDao.upsert(updated)
        responseCache.invalidateAccount(accountId)
        return KeySaveOutcome.Saved(updated)
    }

    suspend fun removeKeyOnly(accountId: String) {
        vault.delete(SecretVault.apiKeyAlias(accountId))
        accountDao.updateStatus(accountId, AccountStatus.INVALID.name)
        alertEngine.cancelAllForAccount(accountId)
        responseCache.invalidateAccount(accountId)
    }

    suspend fun reverifyAccountKey(
        accountId: String,
        validator: suspend (accountId: String, secret: String) -> CredentialValidationResult
    ): KeySaveOutcome {
        val existing = accountDao.getById(accountId)
            ?: return KeySaveOutcome.Error("Account not found")
        val storedKey = vault.get(SecretVault.apiKeyAlias(accountId))?.trim()
        if (storedKey.isNullOrEmpty()) {
            accountDao.updateStatus(accountId, AccountStatus.INVALID.name)
            return KeySaveOutcome.Error("No Torn API key saved for this account.")
        }

        val validation = validator(accountId, storedKey)
        if (validation.error != null || validation.externalId == null) {
            val apiError = validation.error
            if (apiError is ApiError.InvalidKey || apiError is ApiError.AccessLevel) {
                accountDao.updateStatus(accountId, AccountStatus.INVALID.name)
            }
            return KeySaveOutcome.Error(apiError?.message ?: "Validation failed")
        }

        val updated = existing.copy(
            externalId = validation.externalId,
            displayName = validation.displayName ?: existing.displayName,
            status = AccountStatus.VALID.name,
            accessLevel = validation.accessLevel ?: existing.accessLevel ?: "Limited",
            lastValidatedAt = clock.nowEpochMs()
        )
        accountDao.upsert(updated)
        return KeySaveOutcome.Saved(updated)
    }

    suspend fun markAccountKeyStatus(accountId: String, newStatus: AccountStatus, reason: String) {
        val current = accountDao.getById(accountId) ?: return
        if (current.status == newStatus.name) return
        accountDao.updateStatus(accountId, newStatus.name)
        alertEngine.cancelAllForAccount(accountId)
        scheduler.postNotification(
            dedupeKey = "system:key_status:$accountId:${newStatus.name}",
            channelId = NotificationChannels.SYSTEM,
            title = "API key ${newStatus.name.lowercase()}",
            body = reason,
            deepLink = "account/$accountId"
        )
    }

    suspend fun deleteAccount(accountId: String) {
        wipeAccountData(accountId)
        accountDao.deleteById(accountId)
    }

    suspend fun deleteProfile(profileId: String) {
        val accounts = accountDao.getAll().filter { it.profileId == profileId }
        for (acct in accounts) {
            wipeAccountData(acct.id)
        }
        profileDao.deleteById(profileId)
        val remaining = profileDao.getActiveProfile()
        if (remaining == null) {
            val allProfiles = db.openHelper.readableDatabase.query("SELECT id FROM profile LIMIT 1").use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (allProfiles != null) {
                profileDao.setActive(allProfiles)
            } else {
                settingsStore.setOnboardingCompleted(false)
            }
        }
    }

    private suspend fun wipeAccountData(accountId: String) {
        vault.deleteByPrefix(SecretVault.accountPrefix(accountId))
        alertEngine.cancelAllForAccount(accountId)
        responseCache.invalidateAccount(accountId)
        db.stockDao().deleteWatchesForAccount(accountId)
        db.userscriptDao().deleteForAccount(accountId)
        db.warAndFlightDao().deleteStakeoutsForAccount(accountId)
        db.targetDao().deleteTargetsForAccount(accountId)
        db.targetDao().clearHospitalCapturesForAccount(accountId)
        db.connectorLinkDao().deleteForAccount(accountId)
        db.ffCacheDao().clearAll()
        onWipeWebProfile(accountId)
    }
}
