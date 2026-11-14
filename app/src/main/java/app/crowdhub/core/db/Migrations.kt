package app.crowdhub.core.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `userscript` (
                    `id` TEXT NOT NULL,
                    `accountId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `namespace` TEXT,
                    `version` TEXT,
                    `description` TEXT,
                    `source` TEXT NOT NULL,
                    `matches` TEXT NOT NULL,
                    `excludes` TEXT NOT NULL,
                    `runAt` TEXT NOT NULL,
                    `grants` TEXT NOT NULL,
                    `connects` TEXT NOT NULL,
                    `allowApiKey` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    `installedFromUrl` TEXT,
                    `installedAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `lastError` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `script_kv` (
                    `scriptId` TEXT NOT NULL,
                    `key` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`scriptId`, `key`),
                    FOREIGN KEY(`scriptId`) REFERENCES `userscript`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `script_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `scriptId` TEXT NOT NULL,
                    `ts` INTEGER NOT NULL,
                    `level` TEXT NOT NULL,
                    `message` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_script_log_scriptId_ts` ON `script_log` (`scriptId`, `ts`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ff_cache` (
                    `playerId` INTEGER NOT NULL,
                    `fairFight` REAL,
                    `bsEstimate` INTEGER,
                    `bsHuman` TEXT,
                    `lastUpdatedSec` INTEGER,
                    `fetchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`playerId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `member_snapshot` (
                    `factionId` INTEGER NOT NULL,
                    `playerId` INTEGER NOT NULL,
                    `ts` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `descriptionHash` TEXT NOT NULL,
                    `untilSec` INTEGER,
                    `planeType` TEXT,
                    `lastActionStatus` TEXT,
                    `lastActionSec` INTEGER,
                    `level` INTEGER,
                    PRIMARY KEY(`factionId`, `playerId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `stakeout` (
                    `accountId` TEXT NOT NULL,
                    `playerId` INTEGER NOT NULL,
                    `alertHospital` INTEGER NOT NULL,
                    `alertLanding` INTEGER NOT NULL,
                    `alertOnline` INTEGER NOT NULL,
                    `alertOkay` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`accountId`, `playerId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flight_observation` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `playerId` INTEGER NOT NULL,
                    `direction` TEXT NOT NULL,
                    `place` TEXT NOT NULL,
                    `planeType` TEXT,
                    `prevPollAt` INTEGER,
                    `detectedAt` INTEGER,
                    `landedPrevPollAt` INTEGER,
                    `landedDetectedAt` INTEGER,
                    `observedMinMs` INTEGER,
                    `observedMaxMs` INTEGER,
                    `inferredBook` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_flight_observation_playerId_detectedAt` ON `flight_observation` (`playerId`, `detectedAt`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `player_flight_profile` (
                    `playerId` INTEGER NOT NULL,
                    `hasBook` INTEGER,
                    `lastObservedAt` INTEGER,
                    PRIMARY KEY(`playerId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plane_type_map` (
                    `apiValue` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    PRIMARY KEY(`apiValue`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `target_entry` (
                    `accountId` TEXT NOT NULL,
                    `playerId` INTEGER NOT NULL,
                    `name` TEXT,
                    `note` TEXT,
                    `source` TEXT NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `watched` INTEGER NOT NULL,
                    PRIMARY KEY(`accountId`, `playerId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hospital_capture` (
                    `accountId` TEXT NOT NULL,
                    `playerId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `level` INTEGER,
                    `remainingSecAtCapture` INTEGER,
                    `capturedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`accountId`, `playerId`)
                )
                """.trimIndent()
            )
        }
    }
}
