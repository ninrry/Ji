package luzzr.ji

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.ji.core.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateFromVersion2To5PreservesUserTransactionsAndBudgets() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("""
                INSERT INTO transactions (id, amount, type, category, note, timestamp, isExtra)
                VALUES (7, 12.34, 'EXPENSE', '餐饮', '午餐', 1700000000000, 0)
            """.trimIndent())
            db.execSQL("""
                INSERT INTO budgets (yearMonth, amount)
                VALUES ('2026-06', 3000.55)
            """.trimIndent())
            db.execSQL("""
                INSERT INTO pending_bills (id, amount, category, note, isFallback, capturedAt, status)
                VALUES (3, 4.56, '购物', '旧待处理账单', 1, 1700000000100, 'PENDING')
            """.trimIndent())
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )

        db.query("SELECT amount, source, platform, paymentKind, occurredAt FROM transactions WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1234L, cursor.getLong(0))
            assertEquals("MANUAL", cursor.getString(1))
            assertEquals("MANUAL", cursor.getString(2))
            assertEquals("MERCHANT_PAYMENT", cursor.getString(3))
            assertEquals(1700000000000L, cursor.getLong(4))
        }
        db.query("SELECT amount FROM budgets WHERE yearMonth = '2026-06'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(300055L, cursor.getLong(0))
        }
        assertFalse(db.hasTable("pending_bills"))
        assertTrue(db.hasTable("recognition_records"))
    }

    @Test
    fun migrateFromVersion3To5CreatesRecognitionRecordsAndIndexes() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL("""
                INSERT INTO transactions (id, amount, type, category, note, timestamp, isExtra, source)
                VALUES (8, 990, 'EXPENSE', '交通', '地铁', 1700000010000, 0, 'AUTO_VLM')
            """.trimIndent())
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )

        db.query("SELECT platform, paymentKind, occurredAt FROM transactions WHERE id = 8").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("MANUAL", cursor.getString(0))
            assertEquals("MERCHANT_PAYMENT", cursor.getString(1))
            assertEquals(1700000010000L, cursor.getLong(2))
        }
        assertTrue(db.hasIndex("index_transactions_dedupKey"))
        assertTrue(db.hasIndex("index_transactions_timestamp"))
        assertTrue(db.hasIndex("index_transactions_source_platform_paymentKind_amount_occurredAt"))
        assertTrue(db.hasIndex("index_recognition_records_status"))
        assertTrue(db.hasIndex("index_recognition_records_eventFingerprint"))
        assertTrue(db.hasIndex("index_recognition_records_status_capturedAt"))
        assertTrue(db.hasIndex("index_recognition_records_eventFingerprint_capturedAt"))
    }

    @Test
    fun migrateFromVersion4To5AddsOnlyPerformanceIndexes() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL("""
                INSERT INTO transactions (
                    id, amount, type, category, note, timestamp, isExtra, source,
                    platform, paymentKind, tradeId, occurredAt, dedupKey
                ) VALUES (
                    9, 1280, 'EXPENSE', '餐饮', '咖啡', 1700000020000, 0, 'AUTO_VLM',
                    'WECHAT', 'MERCHANT_PAYMENT', 'wx-9', 1700000020000, 'WECHAT:wx-9'
                )
            """.trimIndent())
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        db.query("SELECT amount, dedupKey FROM transactions WHERE id = 9").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1280L, cursor.getLong(0))
            assertEquals("WECHAT:wx-9", cursor.getString(1))
        }
        assertTrue(db.hasIndex("index_transactions_timestamp"))
        assertTrue(db.hasIndex("index_transactions_source_platform_paymentKind_amount_occurredAt"))
        assertTrue(db.hasIndex("index_recognition_records_status_capturedAt"))
        assertTrue(db.hasIndex("index_recognition_records_eventFingerprint_capturedAt"))
    }

    private fun SupportSQLiteDatabase.hasTable(name: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { cursor ->
            cursor.moveToFirst()
        }

    private fun SupportSQLiteDatabase.hasIndex(name: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(name)).use { cursor ->
            cursor.moveToFirst()
        }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}