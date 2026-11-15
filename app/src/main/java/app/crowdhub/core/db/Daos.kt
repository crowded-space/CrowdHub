package app.crowdhub.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE isActive = 1 LIMIT 1")
    fun observeActiveProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profile SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: String)

    @Query("UPDATE profile SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM profile WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ProviderAccountDao {
    @Query("SELECT * FROM provider_account WHERE profileId = :profileId ORDER BY createdAt ASC")
    fun observeForProfile(profileId: String): Flow<List<ProviderAccountEntity>>

    @Query("SELECT * FROM provider_account WHERE profileId = :profileId ORDER BY createdAt ASC LIMIT 1")
    suspend fun getPrimaryForProfile(profileId: String): ProviderAccountEntity?

    @Query("SELECT * FROM provider_account WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProviderAccountEntity?

    @Query("SELECT * FROM provider_account WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ProviderAccountEntity?>

    @Query("SELECT * FROM provider_account")
    suspend fun getAll(): List<ProviderAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: ProviderAccountEntity)

    @Query("UPDATE provider_account SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM provider_account WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ConnectorLinkDao {
    @Query("SELECT * FROM connector_link WHERE accountId = :accountId AND connectorId = :connectorId LIMIT 1")
    fun observe(accountId: String, connectorId: String): Flow<ConnectorLinkEntity?>

    @Query("SELECT * FROM connector_link WHERE accountId = :accountId AND connectorId = :connectorId LIMIT 1")
    suspend fun get(accountId: String, connectorId: String): ConnectorLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: ConnectorLinkEntity)

    @Query("DELETE FROM connector_link WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}

@Dao
interface SecretBlobDao {
    @Query("SELECT * FROM secret_blob WHERE alias = :alias LIMIT 1")
    suspend fun get(alias: String): SecretBlobEntity?

    @Query("SELECT COUNT(*) FROM secret_blob WHERE alias = :alias")
    suspend fun count(alias: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(blob: SecretBlobEntity)

    @Query("DELETE FROM secret_blob WHERE alias = :alias")
    suspend fun delete(alias: String)

    @Query("DELETE FROM secret_blob WHERE alias LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)
}

@Dao
interface ScheduledAlertDao {
    @Query("SELECT * FROM scheduled_alert WHERE state = 'SCHEDULED'")
    suspend fun getScheduled(): List<ScheduledAlertEntity>

    @Query("SELECT * FROM scheduled_alert WHERE dedupeKey = :dedupeKey LIMIT 1")
    suspend fun getByKey(dedupeKey: String): ScheduledAlertEntity?

    @Query("SELECT * FROM scheduled_alert WHERE accountId = :accountId AND state = 'SCHEDULED'")
    suspend fun getScheduledForAccount(accountId: String): List<ScheduledAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: ScheduledAlertEntity)

    @Query("UPDATE scheduled_alert SET state = :state, updatedAt = :updatedAt WHERE dedupeKey = :dedupeKey")
    suspend fun updateState(dedupeKey: String, state: String, updatedAt: Long)

    @Query("DELETE FROM scheduled_alert WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_snapshot WHERE profileId = :profileId AND stockId = :stockId AND ts >= :sinceMs ORDER BY ts ASC")
    suspend fun getSnapshotsSince(profileId: String, stockId: Int, sinceMs: Long): List<StockSnapshotEntity>

    @Query("SELECT * FROM stock_snapshot WHERE profileId = :profileId AND ts >= :sinceMs ORDER BY stockId ASC, ts ASC")
    suspend fun getAllSnapshotsSince(profileId: String, sinceMs: Long): List<StockSnapshotEntity>

    @Query("SELECT MAX(ts) FROM stock_snapshot WHERE profileId = :profileId AND stockId = :stockId")
    suspend fun getLatestTs(profileId: String, stockId: Int): Long?

    @Insert
    suspend fun insertSnapshots(items: List<StockSnapshotEntity>)

    @Query("DELETE FROM stock_snapshot WHERE ts < :cutoffMs")
    suspend fun deleteSnapshotsBefore(cutoffMs: Long)

    @Query("DELETE FROM stock_snapshot WHERE profileId = :profileId")
    suspend fun clearSnapshotsForProfile(profileId: String)

    @Query("DELETE FROM stock_snapshot")
    suspend fun clearAllSnapshots()

    @Query("DELETE FROM stock_snapshot WHERE id IN (:ids)")
    suspend fun deleteSnapshotsByIds(ids: List<Long>)

    @Query("SELECT * FROM stock_snapshot WHERE ts >= :fromMs AND ts < :toMs ORDER BY stockId ASC, ts ASC")
    suspend fun getSnapshotsBetween(fromMs: Long, toMs: Long): List<StockSnapshotEntity>

    @Query("SELECT * FROM stock_watch WHERE accountId = :accountId ORDER BY stockId ASC")
    fun observeWatches(accountId: String): Flow<List<StockWatchEntity>>

    @Query("SELECT * FROM stock_watch WHERE accountId = :accountId AND stockId = :stockId")
    fun observeWatchesForStock(accountId: String, stockId: Int): Flow<List<StockWatchEntity>>

    @Query("SELECT * FROM stock_watch WHERE accountId = :accountId AND enabled = 1")
    suspend fun getEnabledWatches(accountId: String): List<StockWatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatch(watch: StockWatchEntity)

    @Update
    suspend fun updateWatches(watches: List<StockWatchEntity>)

    @Query("DELETE FROM stock_watch WHERE id = :id")
    suspend fun deleteWatch(id: String)

    @Query("DELETE FROM stock_watch WHERE accountId = :accountId")
    suspend fun deleteWatchesForAccount(accountId: String)
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entry WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntryEntity)

    @Query("DELETE FROM cache_entry WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)

    @Query("DELETE FROM cache_entry")
    suspend fun clearAll()
}

@Dao
interface PlayerNameCacheDao {
    @Query("SELECT * FROM player_name_cache WHERE profileId = :profileId AND playerId IN (:playerIds)")
    suspend fun getByIds(profileId: String, playerIds: List<Long>): List<PlayerNameCacheEntity>

    @Query("SELECT * FROM player_name_cache WHERE profileId = :profileId")
    suspend fun getAllForProfile(profileId: String): List<PlayerNameCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<PlayerNameCacheEntity>)

    @Query("DELETE FROM player_name_cache WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)

    @Query("DELETE FROM player_name_cache WHERE updatedAt < :cutoffMs")
    suspend fun pruneExpired(cutoffMs: Long)
}

@Dao
interface UserscriptDao {
    @Query("SELECT * FROM userscript WHERE accountId = :accountId ORDER BY installedAt DESC")
    fun observeForAccount(accountId: String): Flow<List<UserscriptEntity>>

    @Query("SELECT * FROM userscript WHERE accountId = :accountId AND enabled = 1")
    suspend fun getEnabledForAccount(accountId: String): List<UserscriptEntity>

    @Query("SELECT * FROM userscript WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(script: UserscriptEntity)

    @Query("UPDATE userscript SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("UPDATE userscript SET lastError = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setLastError(id: String, error: String?, updatedAt: Long)

    @Query("DELETE FROM userscript WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM userscript WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)

    @Query("SELECT * FROM script_kv WHERE scriptId = :scriptId")
    suspend fun getAllKv(scriptId: String): List<ScriptKvEntity>

    @Query("SELECT value FROM script_kv WHERE scriptId = :scriptId AND `key` = :key LIMIT 1")
    suspend fun getKv(scriptId: String, key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putKv(kv: ScriptKvEntity)

    @Query("DELETE FROM script_kv WHERE scriptId = :scriptId AND `key` = :key")
    suspend fun deleteKv(scriptId: String, key: String)

    @Query("DELETE FROM script_kv WHERE scriptId = :scriptId")
    suspend fun clearKvForScript(scriptId: String)

    @Query("SELECT * FROM script_log WHERE scriptId = :scriptId ORDER BY ts DESC, id DESC LIMIT 200")
    fun observeLogs(scriptId: String): Flow<List<ScriptLogEntity>>

    @Insert
    suspend fun insertLog(log: ScriptLogEntity)

    @Query("DELETE FROM script_log WHERE scriptId = :scriptId AND id NOT IN (SELECT id FROM script_log WHERE scriptId = :scriptId ORDER BY ts DESC, id DESC LIMIT 200)")
    suspend fun trimLogs(scriptId: String)

    @Query("DELETE FROM script_log WHERE scriptId = :scriptId")
    suspend fun clearLogsForScript(scriptId: String)
}

@Dao
interface FfCacheDao {
    @Query("SELECT * FROM ff_cache WHERE profileId = :profileId AND playerId IN (:playerIds)")
    suspend fun getByIds(profileId: String, playerIds: List<Long>): List<FfCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<FfCacheEntity>)

    @Query("DELETE FROM ff_cache WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: String)

    @Query("DELETE FROM ff_cache")
    suspend fun clearAll()
}

@Dao
interface WarAndFlightDao {
    @Query("SELECT * FROM member_snapshot WHERE profileId = :profileId AND factionId = :factionId")
    suspend fun getSnapshotsForFaction(profileId: String, factionId: Long): List<MemberSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshots(snapshots: List<MemberSnapshotEntity>)

    @Query("DELETE FROM member_snapshot WHERE profileId = :profileId AND factionId = :factionId")
    suspend fun clearFactionSnapshots(profileId: String, factionId: Long)

    @Query("DELETE FROM member_snapshot WHERE profileId = :profileId")
    suspend fun clearSnapshotsForProfile(profileId: String)

    @Query("SELECT * FROM stakeout WHERE accountId = :accountId")
    fun observeStakeouts(accountId: String): Flow<List<StakeoutEntity>>

    @Query("SELECT * FROM stakeout WHERE accountId = :accountId")
    suspend fun getStakeouts(accountId: String): List<StakeoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStakeout(stakeout: StakeoutEntity)

    @Query("DELETE FROM stakeout WHERE accountId = :accountId AND playerId = :playerId")
    suspend fun deleteStakeout(accountId: String, playerId: Long)

    @Query("DELETE FROM stakeout WHERE accountId = :accountId")
    suspend fun deleteStakeoutsForAccount(accountId: String)

    @Query("SELECT * FROM flight_observation WHERE profileId = :profileId ORDER BY id DESC LIMIT 100")
    fun observeRecentObservations(profileId: String): Flow<List<FlightObservationEntity>>

    @Query("SELECT * FROM flight_observation WHERE profileId = :profileId AND playerId = :playerId ORDER BY id DESC LIMIT 50")
    suspend fun getObservationsForPlayer(profileId: String, playerId: Long): List<FlightObservationEntity>

    @Insert
    suspend fun insertObservation(obs: FlightObservationEntity)

    @Query("DELETE FROM flight_observation WHERE profileId = :profileId AND playerId = :playerId AND id NOT IN (SELECT id FROM flight_observation WHERE profileId = :profileId AND playerId = :playerId ORDER BY id DESC LIMIT 50)")
    suspend fun trimObservationsForPlayer(profileId: String, playerId: Long)

    @Query("DELETE FROM flight_observation WHERE profileId = :profileId")
    suspend fun clearObservationsForProfile(profileId: String)

    @Query("DELETE FROM flight_observation WHERE detectedAt IS NOT NULL AND detectedAt < :cutoffMs")
    suspend fun pruneOldObservations(cutoffMs: Long)

    @Query("SELECT * FROM player_flight_profile WHERE profileId = :profileId AND playerId = :playerId LIMIT 1")
    suspend fun getFlightProfile(profileId: String, playerId: Long): PlayerFlightProfileEntity?

    @Query("SELECT * FROM player_flight_profile WHERE profileId = :profileId AND playerId IN (:playerIds)")
    suspend fun getFlightProfiles(profileId: String, playerIds: List<Long>): List<PlayerFlightProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlightProfile(profile: PlayerFlightProfileEntity)

    @Query("DELETE FROM player_flight_profile WHERE profileId = :profileId")
    suspend fun clearFlightProfilesForProfile(profileId: String)

    @Query("SELECT * FROM plane_type_map")
    fun observePlaneTypeMap(): Flow<List<PlaneTypeMapEntity>>

    @Query("SELECT * FROM plane_type_map")
    suspend fun getPlaneTypeMap(): List<PlaneTypeMapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaneTypeMapping(mapping: PlaneTypeMapEntity)

    @Query("DELETE FROM plane_type_map WHERE apiValue = :apiValue")
    suspend fun deletePlaneTypeMapping(apiValue: String)

    @Query("SELECT * FROM active_flight WHERE profileId = :profileId")
    suspend fun getAllActiveFlights(profileId: String): List<ActiveFlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveFlight(flight: ActiveFlightEntity)

    @Query("DELETE FROM active_flight WHERE profileId = :profileId AND playerId = :playerId")
    suspend fun deleteActiveFlight(profileId: String, playerId: Long)

    @Query("DELETE FROM active_flight WHERE profileId = :profileId")
    suspend fun clearActiveFlightsForProfile(profileId: String)
}

@Dao
interface TargetDao {
    @Query("SELECT * FROM target_entry WHERE accountId = :accountId ORDER BY addedAt DESC")
    fun observeTargets(accountId: String): Flow<List<TargetEntity>>

    @Query("SELECT * FROM target_entry WHERE accountId = :accountId")
    suspend fun getTargets(accountId: String): List<TargetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: TargetEntity)

    @Query("DELETE FROM target_entry WHERE accountId = :accountId AND playerId = :playerId")
    suspend fun deleteTarget(accountId: String, playerId: Long)

    @Query("DELETE FROM target_entry WHERE accountId = :accountId")
    suspend fun deleteTargetsForAccount(accountId: String)

    @Query("SELECT * FROM hospital_capture WHERE accountId = :accountId ORDER BY capturedAt DESC")
    fun observeHospitalCaptures(accountId: String): Flow<List<HospitalCaptureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHospitalCaptures(captures: List<HospitalCaptureEntity>)

    @Query("DELETE FROM hospital_capture WHERE accountId = :accountId")
    suspend fun clearHospitalCapturesForAccount(accountId: String)
}
