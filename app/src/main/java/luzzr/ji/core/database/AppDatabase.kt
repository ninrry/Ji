package luzzr.ji.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, RecognitionRecordEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recognitionRecordDao(): RecognitionRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ji_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isExtra INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO transactions_new (id, amount, type, category, note, timestamp, isExtra, source)
                    SELECT id, CAST(ROUND(amount * 100) AS INTEGER), type, category, note, timestamp, isExtra, 'MANUAL'
                    FROM transactions
                """.trimIndent())
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets_new (
                        yearMonth TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        PRIMARY KEY(yearMonth)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO budgets_new (yearMonth, amount)
                    SELECT yearMonth, CAST(ROUND(amount * 100) AS INTEGER)
                    FROM budgets
                """.trimIndent())
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_bills_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        isFallback INTEGER NOT NULL,
                        capturedAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO pending_bills_new (id, amount, category, note, isFallback, capturedAt, status)
                    SELECT id, CAST(ROUND(amount * 100) AS INTEGER), category, note, isFallback, capturedAt, status
                    FROM pending_bills
                """.trimIndent())
                db.execSQL("DROP TABLE pending_bills")
                db.execSQL("ALTER TABLE pending_bills_new RENAME TO pending_bills")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN platform TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentKind TEXT NOT NULL DEFAULT 'MERCHANT_PAYMENT'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tradeId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN occurredAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE transactions SET occurredAt = timestamp WHERE occurredAt = 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN dedupKey TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_dedupKey ON transactions (dedupKey)")

                db.execSQL("DROP TABLE IF EXISTS pending_bills")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recognition_records (
                        id TEXT NOT NULL,
                        eventFingerprint TEXT NOT NULL,
                        platform TEXT NOT NULL,
                        kindHint TEXT NOT NULL,
                        screenText TEXT NOT NULL,
                        screenshotPath TEXT,
                        capturedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        attemptCount INTEGER NOT NULL,
                        errorMessage TEXT,
                        transactionId INTEGER,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recognition_records_status ON recognition_records (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recognition_records_eventFingerprint ON recognition_records (eventFingerprint)")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_source_platform_paymentKind_amount_occurredAt ON transactions (source, platform, paymentKind, amount, occurredAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recognition_records_eventFingerprint_capturedAt ON recognition_records (eventFingerprint, capturedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recognition_records_status_capturedAt ON recognition_records (status, capturedAt)")
            }
        }
    }
}
