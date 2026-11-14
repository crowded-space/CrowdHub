package app.crowdhub.core.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "provider_account",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ProviderAccountEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val providerId: String,
    val externalId: String?,
    val displayName: String?,
    val status: String, // UNKNOWN | VALID | INVALID | PAUSED
    val accessLevel: String?,
    val lastValidatedAt: Long?,
    val createdAt: Long
)

@Entity(
    tableName = "connector_link",
    foreignKeys = [
        ForeignKey(
            entity = ProviderAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId", "connectorId"], unique = true)]
)
data class ConnectorLinkEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val connectorId: String,
    val enabled: Boolean,
    val consentAt: Long?
)

@Entity(tableName = "secret_blob")
data class SecretBlobEntity(
    @PrimaryKey val alias: String,
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecretBlobEntity) return false
        return alias == other.alias && iv.contentEquals(other.iv) &&
            ciphertext.contentEquals(other.ciphertext) && updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = alias.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

@Entity(tableName = "scheduled_alert")
data class ScheduledAlertEntity(
    @PrimaryKey val dedupeKey: String,
    val accountId: String,
    val channelId: String,
    val title: String,
    val body: String,
    val fireAt: Long,
    val deepLink: String?,
    val state: String, // SCHEDULED | FIRED | CANCELLED
    val updatedAt: Long
)

@Entity(
    tableName = "stock_snapshot",
    indices = [Index(value = ["stockId", "ts"])]
)
data class StockSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stockId: Int,
    val ts: Long,
    val price: Double
)

@Entity(tableName = "stock_watch")
data class StockWatchEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val stockId: Int,
    val aboveTarget: Double?,
    val belowTarget: Double?,
    val pctMove: Double?,
    val pctWindowMin: Int?,
    val repeating: Boolean,
    val cooldownMin: Int,
    val armed: Boolean,
    val lastTriggeredAt: Long?,
    val enabled: Boolean
)

@Entity(tableName = "cache_entry")
data class CacheEntryEntity(
    @PrimaryKey val key: String,
    val body: String,
    val fetchedAt: Long,
    val ttlMs: Long
)

// Phase 1B Entities
@Entity(tableName = "userscript")
data class UserscriptEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val namespace: String?,
    val version: String?,
    val description: String?,
    val source: String,
    val matches: String, // JSON list
    val excludes: String, // JSON list
    val runAt: String, // DOCUMENT_START | DOCUMENT_END | DOCUMENT_IDLE
    val grants: String, // JSON list
    val connects: String, // JSON list
    val allowApiKey: Boolean,
    val enabled: Boolean,
    val installedFromUrl: String?,
    val installedAt: Long,
    val updatedAt: Long,
    val lastError: String? = null
)

@Entity(
    tableName = "script_kv",
    primaryKeys = ["scriptId", "key"],
    foreignKeys = [
        ForeignKey(
            entity = UserscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScriptKvEntity(
    val scriptId: String,
    val key: String,
    val value: String
)

@Entity(
    tableName = "script_log",
    indices = [Index(value = ["scriptId", "ts"])]
)
data class ScriptLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val scriptId: String,
    val ts: Long,
    val level: String,
    val message: String
)

// Phase 2 Entities
@Entity(tableName = "ff_cache")
data class FfCacheEntity(
    @PrimaryKey val playerId: Long,
    val fairFight: Double?,
    val bsEstimate: Long?,
    val bsHuman: String?,
    val lastUpdatedSec: Long?,
    val fetchedAt: Long
)

@Entity(
    tableName = "member_snapshot",
    primaryKeys = ["factionId", "playerId"]
)
data class MemberSnapshotEntity(
    val factionId: Long,
    val playerId: Long,
    val ts: Long,
    val state: String,
    val descriptionHash: String,
    val untilSec: Long?,
    val planeType: String?,
    val lastActionStatus: String?,
    val lastActionSec: Long?,
    val level: Int?
)

@Entity(
    tableName = "stakeout",
    primaryKeys = ["accountId", "playerId"]
)
data class StakeoutEntity(
    val accountId: String,
    val playerId: Long,
    val alertHospital: Boolean,
    val alertLanding: Boolean,
    val alertOnline: Boolean,
    val alertOkay: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "flight_observation",
    indices = [Index(value = ["playerId", "detectedAt"])]
)
data class FlightObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playerId: Long,
    val direction: String, // OUT | RETURN
    val place: String,
    val planeType: String?,
    val prevPollAt: Long?,
    val detectedAt: Long?,
    val landedPrevPollAt: Long?,
    val landedDetectedAt: Long?,
    val observedMinMs: Long?,
    val observedMaxMs: Long?,
    val inferredBook: Boolean?
)

@Entity(tableName = "player_flight_profile")
data class PlayerFlightProfileEntity(
    @PrimaryKey val playerId: Long,
    val hasBook: Boolean?,
    val lastObservedAt: Long?
)

@Entity(tableName = "plane_type_map")
data class PlaneTypeMapEntity(
    @PrimaryKey val apiValue: String,
    val category: String, // STANDARD | AIRSTRIP | WLT | BUSINESS | UNKNOWN
    val source: String // DEFAULT | USER
)

@Entity(
    tableName = "target_entry",
    primaryKeys = ["accountId", "playerId"]
)
data class TargetEntity(
    val accountId: String,
    val playerId: Long,
    val name: String?,
    val note: String?,
    val source: String, // HOSPITAL_PAGE | FFSCOUTER | MANUAL | WAR
    val addedAt: Long,
    val watched: Boolean = false
)

@Entity(
    tableName = "hospital_capture",
    primaryKeys = ["accountId", "playerId"]
)
data class HospitalCaptureEntity(
    val accountId: String,
    val playerId: Long,
    val name: String,
    val level: Int?,
    val remainingSecAtCapture: Long?,
    val capturedAt: Long
)
