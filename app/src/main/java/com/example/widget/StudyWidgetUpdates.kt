package com.example.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.data.local.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Observes committed database and preference changes, including edits from every study mode. */
object StudyWidgetUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()
    private var started = false

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val preferences = callbackFlow {
            val prefs = app.getSharedPreferences("user_auth_prefs", Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit)
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        scope.launch {
            val database = AppDatabase.getDatabase(app)
            merge(preferences, database.invalidationTracker.createFlow("questions", "review_progress").map { Unit }, refresh)
                .debounce(200)
                .collect {
                    _revision.value++
                    try {
                        StudyProgressWidget().updateAll(app)
                        FocusStudyWidget().updateAll(app)
                    }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Log.w("StudyWidget", "Unable to refresh widget", e) }
                }
        }
    }

    fun requestRefresh() { refresh.tryEmit(Unit) }
}
