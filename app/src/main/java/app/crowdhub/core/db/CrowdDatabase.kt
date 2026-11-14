package app.crowdhub.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ProviderAccountEntity::class,
        ConnectorLinkEntity::class,
        SecretBlobEntity::class,
        ScheduledAlertEntity::class,
        StockSnapshotEntity::class,
        StockWatchEntity::class,
        CacheEntryEntity::class,
        UserscriptEntity::class,
        ScriptKvEntity::class,
        ScriptLogEntity::class,
        FfCacheEntity::class,
        MemberSnapshotEntity::class,
        StakeoutEntity::class,
        FlightObservationEntity::class,
        PlayerFlightProfileEntity::class,
        PlaneTypeMapEntity::class,
        TargetEntity::class,
        HospitalCaptureEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class CrowdDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun providerAccountDao(): ProviderAccountDao
    abstract fun connectorLinkDao(): ConnectorLinkDao
    abstract fun secretBlobDao(): SecretBlobDao
    abstract fun scheduledAlertDao(): ScheduledAlertDao
    abstract fun stockDao(): StockDao
    abstract fun cacheDao(): CacheDao
    abstract fun userscriptDao(): UserscriptDao
    abstract fun ffCacheDao(): FfCacheDao
    abstract fun warAndFlightDao(): WarAndFlightDao
    abstract fun targetDao(): TargetDao

    companion object {
        fun build(context: Context): CrowdDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CrowdDatabase::class.java,
                "crowdhub.db"
            )
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                .build()
        }
    }
}
