package com.example.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.LearningRepository
import com.example.learning.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearningBank(val skills: List<SkillEntity> = emptyList(), val questions: List<QuestionEntity> = emptyList(),
    val quizzes: List<QuizEntity> = emptyList(), val folders: List<FolderEntity> = emptyList())
data class LearningUiState(val loaded: Boolean = false, val bank: LearningBank = LearningBank(),
    val attempts: List<QuestionAttemptEntity> = emptyList(), val session: LearningSessionEntity? = null,
    val mastery: List<SkillMastery> = emptyList())

class AdaptivePracticeViewModel(private val repository: LearningRepository, private val user: String) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    private val bank = combine(repository.skills, repository.questions, repository.quizzes, repository.folders) { s, q, z, f ->
        LearningBank(s, q, z, f)
    }
    val state = combine(bank, repository.attempts(user), repository.latestSession(user)) { b, attempts, session ->
        LearningUiState(true, b, attempts, session, MasteryCalculator.calculate(b.skills, b.questions, attempts))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LearningUiState())

    private var timingKey: Triple<Long, Long, Int>? = null
    private var accumulated = 0L
    private var resumedAt: Long? = null

    fun timing(resumed: Boolean) {
        val session = state.value.session ?: return
        val questionId = session.currentQuestionId ?: return
        val key = Triple(session.id, questionId, session.currentVersion)
        if (timingKey != key) {
            timingKey = key
            accumulated = session.accumulatedMillis
            resumedAt = null
        }
        if (resumed && !session.answered && session.completedAt == null) {
            if (resumedAt == null) resumedAt = SystemClock.elapsedRealtime()
        } else {
            resumedAt?.let { accumulated += (SystemClock.elapsedRealtime() - it).coerceAtLeast(0) }
            resumedAt = null
            val elapsed = accumulated
            viewModelScope.launch { repository.saveElapsed(user, session.id, questionId, session.currentVersion, elapsed) }
        }
    }

    private fun act(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Không thể lưu dữ liệu. Vui lòng thử lại." }
            finally { busy.value = false }
        }
    }

    fun start(count: Int, quizId: Long?, folderId: Long?, skillId: Long?) = act {
        repository.start(user, count, quizId, folderId, skillId)
    }
    fun classify(ids: List<Long>, subject: String, name: String, difficulty: Int, onSaved: () -> Unit = {}) = act {
        repository.classify(ids, subject, name, difficulty)
        onSaved()
    }
    fun answer(option: Int, confidence: Int) {
        val session = state.value.session ?: return
        val elapsed = accumulated + (resumedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0)
        act { repository.answer(user, session.id, session.currentQuestionId, session.currentVersion, option, confidence, elapsed) }
    }
    fun next(skip: Boolean = false) { val session = state.value.session ?: return; act { repository.next(user, session.id, session.currentQuestionId, session.currentVersion, skip) } }
    fun finish() { val session = state.value.session ?: return; act { repository.finish(user, session.id) } }
    fun demo(onSaved: () -> Unit = {}) = act { repository.installDemo(); onSaved() }

    companion object {
        fun factory(repository: LearningRepository, user: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AdaptivePracticeViewModel(repository, user) as T
        }
    }
}
