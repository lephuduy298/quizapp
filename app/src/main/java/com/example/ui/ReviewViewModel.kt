package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.ReviewRepository
import com.example.learning.ReviewRating
import com.example.learning.ReviewSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReviewEntry(val question: QuestionEntity, val quiz: QuizEntity?, val skill: SkillEntity?,
    val progress: ReviewProgressEntity?, val changed: Boolean) {
    fun due(now: Long) = progress?.nextReviewAt?.let { it <= now } == true
}
data class ReviewUiState(val loaded: Boolean = false, val entries: List<ReviewEntry> = emptyList(),
    val now: Long = System.currentTimeMillis())

class ReviewViewModel(private val repository: ReviewRepository, private val user: String,
    private val refreshIntervalMillis: Long = 30_000) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    private val refresh = MutableStateFlow(0)
    private val time = flow {
        emit(System.currentTimeMillis())
        while (refreshIntervalMillis > 0) { delay(refreshIntervalMillis); emit(System.currentTimeMillis()) }
    }
    private val bank = combine(repository.questions, repository.quizzes, repository.skills) { q, z, s -> Triple(q, z, s) }
    val state = combine(bank, repository.progress(user), time, refresh) { bank, progress, now, _ ->
        val currentNow = maxOf(now, System.currentTimeMillis())
        val byQuestion = progress.associateBy { it.questionId }
        ReviewUiState(true, bank.first.filter(ReviewSummary::eligible).map { q ->
            val original = byQuestion[q.id]
            ReviewEntry(q, bank.second.find { it.id == q.quizId }, bank.third.find { it.id == q.skillId },
                ReviewSummary.current(q, original, currentNow),
                original != null && original.contentVersion != q.contentVersion)
        }, currentNow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewUiState())

    fun refresh() { refresh.value++ }
    private fun act(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Không thể lưu. Vui lòng thử lại." }
            finally { busy.value = false }
        }
    }
    fun rate(entry: ReviewEntry, rating: ReviewRating, eventKey: String, onSaved: () -> Unit) = act {
        check(repository.rate(user, entry.question.id, entry.question.contentVersion, rating, eventKey)) {
            "Câu hỏi đã thay đổi hoặc không còn tồn tại. Hãy quay lại danh sách." }
        refresh()
        onSaved()
    }
    fun annotate(id: Long, note: String, favorite: Boolean, onSaved: () -> Unit) = act {
        repository.annotate(user, id, note, favorite)
        onSaved()
    }
    companion object {
        fun factory(repository: ReviewRepository, user: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel(repository, user) as T
        }
    }
}
