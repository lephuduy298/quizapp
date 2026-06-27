package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

class QuizRepository(
    private val quizDao: QuizDao,
    private val quizSessionDao: QuizSessionDao,
    private val folderDao: FolderDao
) {
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFoldersFlow()

    suspend fun insertFolder(folder: FolderEntity): Long {
        return folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    suspend fun deleteFolderById(folderId: Long) {
        folderDao.deleteFolderById(folderId)
    }

    suspend fun updateQuizFolder(quizId: Long, folderId: Long?) {
        val quiz = quizDao.getQuizById(quizId)
        if (quiz != null) {
            quizDao.updateQuiz(quiz.copy(folderId = folderId))
        }
    }
    val allQuizzesWithQuestions: Flow<List<QuizWithQuestions>> = quizDao.getAllQuizzesWithQuestionsFlow()
    val allQuizzes: Flow<List<QuizEntity>> = quizDao.getAllQuizzesFlow()
    val allSessions: Flow<List<QuizSessionEntity>> = quizSessionDao.getAllSessionsFlow()

    suspend fun getQuizWithQuestionsById(quizId: Long): QuizWithQuestions? {
        return quizDao.getQuizWithQuestionsById(quizId)
    }

    suspend fun getQuizById(quizId: Long): QuizEntity? {
        return quizDao.getQuizById(quizId)
    }

    suspend fun insertQuizWithQuestions(quiz: QuizEntity, questions: List<QuestionEntity>): Long {
        return quizDao.insertQuizWithQuestions(quiz, questions)
    }

    suspend fun addQuestion(question: QuestionEntity): Long {
        return quizDao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: QuestionEntity) {
        quizDao.updateQuestion(question)
    }

    suspend fun deleteQuestion(question: QuestionEntity) {
        quizDao.deleteQuestion(question)
    }

    suspend fun addQuestions(questions: List<QuestionEntity>) {
        quizDao.insertQuestions(questions)
    }

    suspend fun deleteQuizById(quizId: Long) {
        quizDao.deleteQuizById(quizId)
    }

    suspend fun deleteQuiz(quiz: QuizEntity) {
        quizDao.deleteQuiz(quiz)
    }

    fun getSessionsForQuiz(quizId: Long): Flow<List<QuizSessionEntity>> {
        return quizSessionDao.getSessionsForQuizFlow(quizId)
    }

    suspend fun getSessionById(sessionId: Long): QuizSessionEntity? {
        return quizSessionDao.getSessionById(sessionId)
    }

    suspend fun insertSession(session: QuizSessionEntity): Long {
        return quizSessionDao.insertSession(session)
    }

    suspend fun deleteSessionById(sessionId: Long) {
        quizSessionDao.deleteSessionById(sessionId)
    }
}
