package app.crowdhub.connector.ffscouter

import app.crowdhub.core.db.ConnectorLinkDao
import app.crowdhub.core.db.ConnectorLinkEntity
import app.crowdhub.core.db.FfCacheDao
import app.crowdhub.core.db.FfCacheEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.vault.SecretVault
import app.crowdhub.core.vault.VaultResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class FfKeyInfo(
    val hasCustomKey: Boolean,
    val maskedCustomKey: String,
    val hasTornFallback: Boolean,
    val maskedTornKey: String,
    val isEnabled: Boolean,
    val consentAtMs: Long?
) {
    val hasAnyKey: Boolean get() = hasCustomKey || hasTornFallback
    val maskedEffectiveKey: String
        get() = when {
            hasCustomKey -> maskedCustomKey
            hasTornFallback -> maskedTornKey
            else -> "Not set"
        }
}

class FfRepository(
    private val connectorLinkDao: ConnectorLinkDao,
    private val ffCacheDao: FfCacheDao,
    private val ffApi: FfApi,
    private val vault: SecretVault,
    private val clock: Clock,
    private val resolveProfileId: suspend (String) -> String = { "default" }
) {
    fun observeEnabled(accountId: String): Flow<Boolean> {
        return connectorLinkDao.observe(accountId, FfDefaults.CONNECTOR_ID).map { link ->
            link?.enabled == true && link.consentAt != null
        }
    }

    suspend fun isConsentedAndEnabled(accountId: String): Boolean {
        val link = connectorLinkDao.get(accountId, FfDefaults.CONNECTOR_ID)
        return link?.enabled == true && link.consentAt != null
    }

    suspend fun getKeyInfo(accountId: String): FfKeyInfo {
        val custom = vault.get(SecretVault.ffApiKeyAlias(accountId))?.trim()?.takeIf { it.isNotEmpty() }
        val torn = vault.get(SecretVault.apiKeyAlias(accountId))?.trim()?.takeIf { it.isNotEmpty() }
        val link = connectorLinkDao.get(accountId, FfDefaults.CONNECTOR_ID)
        return FfKeyInfo(
            hasCustomKey = custom != null,
            maskedCustomKey = SecretVault.maskSecret(custom),
            hasTornFallback = torn != null,
            maskedTornKey = SecretVault.maskSecret(torn),
            isEnabled = link?.enabled == true && link.consentAt != null,
            consentAtMs = link?.consentAt
        )
    }

    suspend fun enableWithConsent(
        accountId: String,
        ownPlayerId: Long?,
        customApiKey: String? = null
    ): ApiResult<Unit> {
        val now = clock.nowEpochMs()
        val profileId = resolveProfileId(accountId)
        val cleanCustom = customApiKey?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanCustom != null) {
            val vaultRes = vault.put(SecretVault.ffApiKeyAlias(accountId), cleanCustom)
            if (vaultRes is VaultResult.KeystoreError) {
                return ApiResult.Failure(ApiError.Unknown(0, vaultRes.message))
            }
            ffCacheDao.clearForProfile(profileId)
        }

        upsertEnabledLink(accountId, now)
        return verifyAndCacheOwnStats(profileId, accountId, ownPlayerId, overrideKey = cleanCustom)
    }

    suspend fun saveCustomKeyAndEnable(
        accountId: String,
        ownPlayerId: Long?,
        rawKey: String
    ): ApiResult<Unit> {
        val cleanKey = rawKey.trim()
        if (cleanKey.isEmpty()) {
            return ApiResult.Failure(ApiError.InvalidKey("Enter your FFScouter API key"))
        }

        val verification = ffApi.verifyKey(accountId, ownPlayerId, overrideKey = cleanKey)
        if (verification is ApiResult.Failure) {
            return ApiResult.Failure(verification.error)
        }

        val vaultRes = vault.put(SecretVault.ffApiKeyAlias(accountId), cleanKey)
        if (vaultRes is VaultResult.KeystoreError) {
            return ApiResult.Failure(ApiError.Unknown(0, vaultRes.message))
        }

        val now = clock.nowEpochMs()
        val profileId = resolveProfileId(accountId)
        upsertEnabledLink(accountId, now)
        ffCacheDao.clearForProfile(profileId)
        if (verification is ApiResult.Success && ownPlayerId != null && ownPlayerId > 0L) {
            cacheStats(profileId, verification.data.filter { it.playerId == ownPlayerId })
        }
        return ApiResult.Success(Unit, now)
    }

    suspend fun testConnection(
        accountId: String,
        ownPlayerId: Long?
    ): ApiResult<Unit> {
        val profileId = resolveProfileId(accountId)
        return verifyAndCacheOwnStats(profileId, accountId, ownPlayerId, overrideKey = null)
    }

    suspend fun removeCustomKey(accountId: String) {
        val profileId = resolveProfileId(accountId)
        vault.delete(SecretVault.ffApiKeyAlias(accountId))
        ffCacheDao.clearForProfile(profileId)
    }

    suspend fun disable(accountId: String) {
        val profileId = resolveProfileId(accountId)
        val existing = connectorLinkDao.get(accountId, FfDefaults.CONNECTOR_ID)
        if (existing != null) {
            connectorLinkDao.upsert(existing.copy(enabled = false, consentAt = null))
        }
        ffCacheDao.clearForProfile(profileId)
    }

    suspend fun stats(
        accountId: String,
        ids: List<Long>,
        forceRefresh: Boolean = false
    ): ApiResult<Map<Long, FfStats>> {
        if (!isConsentedAndEnabled(accountId)) {
            return ApiResult.Success(emptyMap(), clock.nowEpochMs())
        }
        val uniqueIds = ids.distinct()
        if (uniqueIds.isEmpty()) return ApiResult.Success(emptyMap(), clock.nowEpochMs())

        val now = clock.nowEpochMs()
        val profileId = resolveProfileId(accountId)
        val cachedRows = uniqueIds.chunked(200).flatMap { ffCacheDao.getByIds(profileId, it) }
        val cachedMap = cachedRows.associate { row ->
            val stale = (now - row.fetchedAt) >= FfDefaults.CACHE_TTL_MS
            row.playerId to FfStats(
                playerId = row.playerId,
                fairFight = row.fairFight,
                bsEstimate = row.bsEstimate,
                bsHuman = row.bsHuman,
                lastUpdatedSec = row.lastUpdatedSec,
                fetchedAtMs = row.fetchedAt,
                isStale = stale
            )
        }.toMutableMap()

        val idsToFetch = if (forceRefresh) {
            uniqueIds
        } else {
            uniqueIds.filter { id ->
                val entry = cachedMap[id]
                entry == null || entry.isStale
            }
        }

        if (idsToFetch.isEmpty()) {
            return ApiResult.Success(cachedMap, now)
        }

        var lastError: ApiError? = null
        for (batch in idsToFetch.chunked(FfDefaults.BATCH_SIZE)) {
            when (val res = ffApi.getStats(accountId, batch)) {
                is ApiResult.Success -> {
                    cacheStats(profileId, res.data)
                    for (item in res.data) {
                        cachedMap[item.playerId] = item
                    }
                }
                is ApiResult.Failure -> {
                    lastError = res.error
                    break
                }
            }
        }

        return if (lastError != null) {
            ApiResult.Failure(
                error = lastError,
                staleData = cachedMap.mapValues { (_, v) -> v.copy(isStale = true) },
                staleFetchedAtMs = cachedMap.values.maxOfOrNull { it.fetchedAtMs }
            )
        } else {
            ApiResult.Success(cachedMap, now)
        }
    }

    suspend fun targets(
        accountId: String,
        filters: FfTargetsFilterParams
    ): ApiResult<List<FfTargetCandidate>> {
        if (!isConsentedAndEnabled(accountId)) {
            return ApiResult.Failure(ApiError.AccessLevel("FFScouter is not enabled"))
        }
        return ffApi.getTargets(accountId, filters)
    }

    private suspend fun upsertEnabledLink(accountId: String, now: Long) {
        val existing = connectorLinkDao.get(accountId, FfDefaults.CONNECTOR_ID)
        connectorLinkDao.upsert(
            ConnectorLinkEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                accountId = accountId,
                connectorId = FfDefaults.CONNECTOR_ID,
                enabled = true,
                consentAt = existing?.consentAt ?: now
            )
        )
    }

    private suspend fun verifyAndCacheOwnStats(
        profileId: String,
        accountId: String,
        ownPlayerId: Long?,
        overrideKey: String?
    ): ApiResult<Unit> {
        val now = clock.nowEpochMs()
        return when (val res = ffApi.verifyKey(accountId, ownPlayerId, overrideKey)) {
            is ApiResult.Success -> {
                if (ownPlayerId != null && ownPlayerId > 0L) {
                    cacheStats(profileId, res.data.filter { it.playerId == ownPlayerId })
                }
                ApiResult.Success(Unit, now)
            }
            is ApiResult.Failure -> ApiResult.Failure(res.error)
        }
    }

    private suspend fun cacheStats(profileId: String, items: List<FfStats>) {
        if (items.isEmpty()) return
        ffCacheDao.upsertAll(
            items.map {
                FfCacheEntity(
                    profileId = profileId,
                    playerId = it.playerId,
                    fairFight = it.fairFight,
                    bsEstimate = it.bsEstimate,
                    bsHuman = it.bsHuman,
                    lastUpdatedSec = it.lastUpdatedSec,
                    fetchedAt = it.fetchedAtMs
                )
            }
        )
    }
}
