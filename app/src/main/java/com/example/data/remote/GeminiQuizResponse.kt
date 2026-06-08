package com.example.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedQuizJson(
    val title: String,
    val topic: String,
    val questions: List<GeneratedQuestionJson>
)

@Serializable
data class GeneratedQuestionJson(
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int
)
