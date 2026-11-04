package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LearningMigrationTest {
    @Test fun migratingVersionTwoPreservesQuizzesQuestionsAndHistory() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "learning-migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE quizzes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, topic TEXT NOT NULL, durationMinutes INTEGER NOT NULL, folderId INTEGER, createdAt INTEGER NOT NULL, FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE SET NULL)")
            old.execSQL("CREATE INDEX index_quizzes_folderId ON quizzes(folderId)")
            old.execSQL("CREATE TABLE questions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, quizId INTEGER NOT NULL, text TEXT NOT NULL, options TEXT NOT NULL, correctOptionIndex INTEGER NOT NULL, FOREIGN KEY(quizId) REFERENCES quizzes(id) ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_questions_quizId ON questions(quizId)")
            old.execSQL("CREATE TABLE quiz_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, quizId INTEGER NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER NOT NULL, score REAL NOT NULL, correctAnswersCount INTEGER NOT NULL, totalQuestionsCount INTEGER NOT NULL, answersMap TEXT NOT NULL, FOREIGN KEY(quizId) REFERENCES quizzes(id) ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_quiz_sessions_quizId ON quiz_sessions(quizId)")
            old.execSQL("INSERT INTO quizzes VALUES (1, 'Existing', 'Android', 10, NULL, 100)")
            old.execSQL("INSERT INTO questions VALUES (1, 1, 'Existing question', '[\"A\",\"B\"]', 0)")
            old.execSQL("INSERT INTO quiz_sessions VALUES (1, 1, 100, 200, 100.0, 1, 1, '{\"1\":0}')")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
        try {
            val quiz = requireNotNull(db.quizDao().getQuizWithQuestionsById(1))
            assertEquals("Existing", quiz.quiz.title)
            assertEquals(1, quiz.questions.size)
            assertEquals(2, quiz.questions.single().difficulty)
            assertNull(quiz.questions.single().skillId)
            assertEquals(1, quiz.questions.single().contentVersion)
            assertEquals(100.0, requireNotNull(db.quizSessionDao().getSessionById(1)).score, .01)
            assertTrue(db.learningDao().skills().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
