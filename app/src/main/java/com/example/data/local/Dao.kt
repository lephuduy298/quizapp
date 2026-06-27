package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Wrapper class to retrieve a complete quiz with all its questions easily
data class QuizWithQuestions(
    @Embedded val quiz: QuizEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quizId"
    )
    val questions: List<QuestionEntity>
)

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId LIMIT 1")
    suspend fun getQuizById(quizId: Long): QuizEntity?

    @Transaction
    @Query("SELECT * FROM quizzes WHERE id = :quizId LIMIT 1")
    suspend fun getQuizWithQuestionsById(quizId: Long): QuizWithQuestions?

    @Transaction
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzesWithQuestionsFlow(): Flow<List<QuizWithQuestions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Transaction
    suspend fun insertQuizWithQuestions(quiz: QuizEntity, questions: List<QuestionEntity>): Long {
        val quizId = insertQuiz(quiz)
        val questionsWithQuizId = questions.map { it.copy(quizId = quizId) }
        insertQuestions(questionsWithQuizId)
        return quizId
    }

    @Insert
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Update
    suspend fun updateQuiz(quiz: QuizEntity)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuiz(quiz: QuizEntity)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: Long)
}

@Dao
interface QuizSessionDao {
    @Query("SELECT * FROM quiz_sessions ORDER BY endTime DESC")
    fun getAllSessionsFlow(): Flow<List<QuizSessionEntity>>

    @Query("SELECT * FROM quiz_sessions WHERE quizId = :quizId ORDER BY endTime DESC")
    fun getSessionsForQuizFlow(quizId: Long): Flow<List<QuizSessionEntity>>

    @Query("SELECT * FROM quiz_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): QuizSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QuizSessionEntity): Long

    @Query("DELETE FROM quiz_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)
}
