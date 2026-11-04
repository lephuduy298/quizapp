package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "review_progress", primaryKeys = ["userKey", "questionId"],
    foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id"],
        childColumns = ["questionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("questionId"), Index(value = ["userKey", "nextReviewAt"])])
data class ReviewProgressEntity(
    val userKey: String,
    val questionId: Long,
    val contentVersion: Int,
    val stage: Int = 0,
    val nextReviewAt: Long? = null,
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0,
    val mistakeCount: Int = 0,
    val lastWrongAt: Long? = null,
    val lastSelectedOption: Int? = null,
    val note: String = "",
    val isFavorite: Boolean = false
)

// A durable idempotency key prevents a restored UI or repeated submission from grading twice.
@Entity(tableName = "review_events", primaryKeys = ["userKey", "eventKey"],
    foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id"],
        childColumns = ["questionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("questionId")])
data class ReviewEventEntity(val userKey: String, val eventKey: String, val questionId: Long,
    val contentVersion: Int, val createdAt: Long)

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_progress WHERE userKey = :user")
    suspend fun allProgress(user: String): List<ReviewProgressEntity>
    @Query("SELECT * FROM review_progress WHERE userKey = :user")
    fun observeProgress(user: String): Flow<List<ReviewProgressEntity>>
    @Query("SELECT * FROM review_progress WHERE userKey = :user AND questionId = :questionId")
    suspend fun progress(user: String, questionId: Long): ReviewProgressEntity?
    @Upsert suspend fun save(progress: ReviewProgressEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: ReviewEventEntity): Long
    @Query("SELECT * FROM review_events WHERE userKey = :user AND eventKey = :eventKey")
    suspend fun event(user: String, eventKey: String): ReviewEventEntity?
    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun question(id: Long): QuestionEntity?
}
