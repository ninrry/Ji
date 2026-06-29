package luzzr.ji.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recognition_records",
    indices = [
        Index(value = ["status"]),
        Index(value = ["eventFingerprint"]),
        Index(value = ["eventFingerprint", "capturedAt"]),
        Index(value = ["status", "capturedAt"])
    ]
)
data class RecognitionRecordEntity(
    @PrimaryKey val id: String,
    val eventFingerprint: String,
    val platform: String,
    val kindHint: String,
    val screenText: String,
    val screenshotPath: String?,
    val capturedAt: Long,
    val status: String = "PENDING",
    val attemptCount: Int = 0,
    val errorMessage: String? = null,
    val transactionId: Long? = null
)
