package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "skills", indices = [Index(value = ["subject", "name"], unique = true)])
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val name: String
)

@Entity(tableName = "learning_sessions", indices = [Index("userKey")])
data class LearningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userKey: String,
    val quizId: Long? = null,
    val folderId: Long? = null,
    val skillId: Long? = null,
    val targetCount: Int = 10,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val currentQuestionId: Long? = null,
    val currentVersion: Int = 1,
    val reason: String = "",
    val answered: Boolean = false,
    val selectedOption: Int? = null,
    val accumulatedMillis: Long = 0,
    val baseline: Map<Long, Int> = emptyMap()
)

// Snapshots preserve historical results when the question is edited or deleted.
@Entity(tableName = "question_attempts", indices = [Index("userKey"), Index("sessionId"),
    Index(value = ["sessionId", "questionId"], unique = true)])
data class QuestionAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val userKey: String,
    val questionId: Long,
    val contentVersion: Int,
    val skillId: Long,
    val difficulty: Int,
    val correct: Boolean,
    val skipped: Boolean = false,
    val selectedOption: Int? = null,
    val confidence: Int? = null,
    val activeMillis: Long = 0,
    val answeredAt: Long = System.currentTimeMillis()
)

@Dao
interface LearningDao {
    @Query("SELECT * FROM skills ORDER BY subject, name")
    fun observeSkills(): Flow<List<SkillEntity>>
    @Query("SELECT * FROM skills ORDER BY subject, name")
    suspend fun skills(): List<SkillEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkill(skill: SkillEntity): Long
    @Query("SELECT * FROM skills WHERE subject = :subject AND name = :name LIMIT 1")
    suspend fun findSkill(subject: String, name: String): SkillEntity?
    @Query("SELECT * FROM questions ORDER BY id")
    fun observeQuestions(): Flow<List<QuestionEntity>>
    @Query("SELECT * FROM questions ORDER BY id")
    suspend fun questions(): List<QuestionEntity>
    @Query("SELECT q.* FROM questions q INNER JOIN quizzes z ON z.id = q.quizId WHERE (:quizId IS NULL OR q.quizId = :quizId) AND (:folderId IS NULL OR z.folderId = :folderId) AND (:skillId IS NULL OR q.skillId = :skillId) ORDER BY q.id")
    suspend fun scopedQuestions(quizId: Long?, folderId: Long?, skillId: Long?): List<QuestionEntity>
    @Query("SELECT * FROM question_attempts WHERE userKey = :user ORDER BY answeredAt, id")
    fun observeAttempts(user: String): Flow<List<QuestionAttemptEntity>>
    @Query("SELECT * FROM question_attempts WHERE userKey = :user ORDER BY answeredAt, id")
    suspend fun attempts(user: String): List<QuestionAttemptEntity>
    @Query("SELECT * FROM learning_sessions WHERE userKey = :user ORDER BY id DESC LIMIT 1")
    fun observeLatestSession(user: String): Flow<LearningSessionEntity?>
    @Query("SELECT * FROM learning_sessions WHERE id = :id AND userKey = :user")
    suspend fun session(id: Long, user: String): LearningSessionEntity?
    @Query("SELECT * FROM learning_sessions WHERE userKey = :user AND completedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun unfinished(user: String): LearningSessionEntity?
    @Insert
    suspend fun insertSession(session: LearningSessionEntity): Long
    @Update
    suspend fun updateSession(session: LearningSessionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttempt(attempt: QuestionAttemptEntity): Long
    @Query("UPDATE questions SET skillId = :skillId, difficulty = :difficulty, contentVersion = contentVersion + 1 WHERE id IN (:ids)")
    suspend fun classify(ids: List<Long>, skillId: Long, difficulty: Int)
}
