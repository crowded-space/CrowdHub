package app.crowdhub.core.profile

import app.crowdhub.core.db.PlayerNameCacheDao
import app.crowdhub.core.db.PlayerNameCacheEntity
import app.crowdhub.core.network.Clock

class PlayerNameCache(
    private val dao: PlayerNameCacheDao,
    private val clock: Clock
) {
    suspend fun recordName(profileId: String, playerId: Long, name: String) {
        recordNames(profileId, mapOf(playerId to name))
    }

    suspend fun recordNames(profileId: String, entries: Map<Long, String>) {
        if (profileId.isBlank() || entries.isEmpty()) return
        val now = clock.nowEpochMs()
        val clean = entries.mapNotNull { (id, rawName) ->
            val trimmed = rawName.trim()
            if (id <= 0L || trimmed.isEmpty() || isPlaceholderName(trimmed, id)) {
                null
            } else {
                PlayerNameCacheEntity(
                    profileId = profileId,
                    playerId = id,
                    name = trimmed,
                    updatedAt = now
                )
            }
        }
        if (clean.isNotEmpty()) {
            clean.chunked(400).forEach { chunk ->
                dao.upsertAll(chunk)
            }
        }
    }

    suspend fun getFreshNames(profileId: String, playerIds: List<Long>): Map<Long, String> {
        if (profileId.isBlank() || playerIds.isEmpty()) return emptyMap()
        val now = clock.nowEpochMs()
        val rows = playerIds.distinct().chunked(400).flatMap { chunk ->
            dao.getByIds(profileId, chunk)
        }
        return rows
            .filter { (now - it.updatedAt) < TTL_MS && !isPlaceholderName(it.name, it.playerId) }
            .associate { it.playerId to it.name }
    }

    suspend fun getAnyCachedNames(profileId: String, playerIds: List<Long>): Map<Long, String> {
        if (profileId.isBlank() || playerIds.isEmpty()) return emptyMap()
        val rows = playerIds.distinct().chunked(400).flatMap { chunk ->
            dao.getByIds(profileId, chunk)
        }
        return rows
            .filter { !isPlaceholderName(it.name, it.playerId) }
            .associate { it.playerId to it.name }
    }

    suspend fun clearForProfile(profileId: String) {
        if (profileId.isNotBlank()) {
            dao.deleteForProfile(profileId)
        }
    }

    suspend fun pruneExpired() {
        dao.pruneExpired(clock.nowEpochMs() - TTL_MS)
    }

    companion object {
        const val TTL_MS = 7L * 86_400_000L

        fun isPlaceholderName(name: String, playerId: Long): Boolean {
            val lower = name.trim().lowercase()
            return lower == "player $playerId" ||
                lower == "player #$playerId" ||
                lower == "user #$playerId" ||
                lower == "id $playerId"
        }
    }
}
