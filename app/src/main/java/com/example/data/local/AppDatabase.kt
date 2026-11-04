package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FolderEntity::class, QuizEntity::class, QuestionEntity::class, QuizSessionEntity::class,
        SkillEntity::class, LearningSessionEntity::class, QuestionAttemptEntity::class,
        ReviewProgressEntity::class, ReviewEventEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun learningDao(): LearningDao
    abstract fun folderDao(): FolderDao
    abstract fun quizDao(): QuizDao
    abstract fun quizSessionDao(): QuizSessionDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS review_progress (userKey TEXT NOT NULL, questionId INTEGER NOT NULL, contentVersion INTEGER NOT NULL, stage INTEGER NOT NULL, nextReviewAt INTEGER, lastReviewedAt INTEGER, reviewCount INTEGER NOT NULL, mistakeCount INTEGER NOT NULL, lastWrongAt INTEGER, lastSelectedOption INTEGER, note TEXT NOT NULL, isFavorite INTEGER NOT NULL, PRIMARY KEY(userKey, questionId), FOREIGN KEY(questionId) REFERENCES questions(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_review_progress_questionId ON review_progress(questionId)")
                db.execSQL("CREATE INDEX index_review_progress_userKey_nextReviewAt ON review_progress(userKey, nextReviewAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS review_events (userKey TEXT NOT NULL, eventKey TEXT NOT NULL, questionId INTEGER NOT NULL, contentVersion INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(userKey, eventKey), FOREIGN KEY(questionId) REFERENCES questions(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_review_events_questionId ON review_events(questionId)")
                // Only adaptive attempts have a reliable owner. Do not attribute shared legacy history.
                db.execSQL("INSERT INTO review_progress (userKey, questionId, contentVersion, stage, nextReviewAt, lastReviewedAt, reviewCount, mistakeCount, lastWrongAt, lastSelectedOption, note, isFavorite) SELECT a.userKey, a.questionId, q.contentVersion, 0, 0, MAX(a.answeredAt), COUNT(*), SUM(CASE WHEN a.correct = 0 THEN 1 ELSE 0 END), MAX(CASE WHEN a.correct = 0 THEN a.answeredAt ELSE NULL END), NULL, '', 0 FROM question_attempts a INNER JOIN questions q ON q.id = a.questionId AND q.contentVersion = a.contentVersion WHERE a.skipped = 0 GROUP BY a.userKey, a.questionId, q.contentVersion")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN skillId INTEGER")
                db.execSQL("ALTER TABLE questions ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE questions ADD COLUMN contentVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE TABLE IF NOT EXISTS skills (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subject TEXT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX index_skills_subject_name ON skills(subject, name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS learning_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userKey TEXT NOT NULL, quizId INTEGER, folderId INTEGER, skillId INTEGER, targetCount INTEGER NOT NULL, startedAt INTEGER NOT NULL, completedAt INTEGER, currentQuestionId INTEGER, currentVersion INTEGER NOT NULL, reason TEXT NOT NULL, answered INTEGER NOT NULL, selectedOption INTEGER, accumulatedMillis INTEGER NOT NULL, baseline TEXT NOT NULL)")
                db.execSQL("CREATE INDEX index_learning_sessions_userKey ON learning_sessions(userKey)")
                db.execSQL("CREATE TABLE IF NOT EXISTS question_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, userKey TEXT NOT NULL, questionId INTEGER NOT NULL, contentVersion INTEGER NOT NULL, skillId INTEGER NOT NULL, difficulty INTEGER NOT NULL, correct INTEGER NOT NULL, skipped INTEGER NOT NULL, selectedOption INTEGER, confidence INTEGER, activeMillis INTEGER NOT NULL, answeredAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_question_attempts_userKey ON question_attempts(userKey)")
                db.execSQL("CREATE INDEX index_question_attempts_sessionId ON question_attempts(sessionId)")
                db.execSQL("CREATE UNIQUE INDEX index_question_attempts_sessionId_questionId ON question_attempts(sessionId, questionId)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quiz_ai_studio_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
