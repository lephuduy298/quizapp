package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import retrofit2.HttpException

interface AIRepository {
    suspend fun generateQuizFromImage(bitmap: Bitmap, promptAddition: String = ""): Result<GeneratedQuizJson>
    suspend fun getExplanationForAnswer(
        questionText: String,
        options: List<String>,
        correctAnswer: String,
        userAnswer: String
    ): Result<String>
}

class DirectGeminiAIRepositoryImpl(
    private val apiService: GeminiApiService
) : AIRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 4000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val isRetryable = when (e) {
                    is HttpException -> e.code() == 429 || e.code() == 503 || e.code() >= 500
                    is IOException -> true
                    else -> false
                }
                if (!isRetryable) throw e
                Log.w("AIRepository", "Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // Last attempt
    }

    override suspend fun generateQuizFromImage(bitmap: Bitmap, promptAddition: String): Result<GeneratedQuizJson> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."))
        }

        // Convert bitmap to Base64 JPEG
        val base64Image = try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Failed to compress image: ${e.message}"))
        }

        val prompt = """
            Analysis the text or exercises in the attached image and generate a complete quiz.
            You MUST return a valid JSON object matching this schema:
            {
              "title": "A concise title of the quiz",
              "topic": "The general subject of the quiz (e.g. Physics, History, N3 Grammar, Chemistry)",
              "questions": [
                {
                  "text": "The full quiz question text",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 0
                }
              ]
            }
            Always provide between 2 to 4 options per question.
            Provide at least 3-5 comprehensive questions from the text.
            The correctOptionIndex must be a valid 0-based index.
            Ensure no formatting wraps like markdown ticks (```json ... ```), return raw JSON only.
            Additional instructions: $promptAddition
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.4f
            )
        )

        try {
            val response = retryIO { apiService.generateContent(apiKey, request) }
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("AI did not return any parseable response text."))

            Log.d("AIRepository", "Raw Gemini JSON Response: ${jsonText.trim()}")

            val cleanJson = cleanJsonResponse(jsonText)
            val generatedQuiz = json.decodeFromString<GeneratedQuizJson>(cleanJson)
            Result.success(generatedQuiz)
        } catch (e: Exception) {
            Log.e("AIRepository", "Error generating quiz from image", e)
            val friendlyMessage = when {
                e is HttpException && e.code() == 429 -> "Máy chủ AI đang bận (Quá tải yêu cầu). Vui lòng đợi một lát rồi thử lại."
                e is HttpException && e.code() == 503 -> "Dịch vụ AI hiện không khả dụng. Vui lòng thử lại sau."
                else -> e.message ?: "Lỗi không xác định khi kết nối với AI."
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun getExplanationForAnswer(
        questionText: String,
        options: List<String>,
        correctAnswer: String,
        userAnswer: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."))
        }

        val prompt = """
            You are an educational AI tutor. Explain clearly and concisely why the user got this question wrong and why the correct answer is correct.
            Keep the content friendly, encouraging, and clear (max 3-4 short paragraphs, markdown formatting supported, using Vietnamese since the user speaks Vietnamese).

            Context of the Question:
            Question: $questionText
            Available Options: ${options.joinToString(", ")}
            User's Selected Answer: $userAnswer
            Correct Answer: $correctAnswer
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = retryIO { apiService.generateContent(apiKey, request) }
            val explanation = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("AI did not return any explanation."))
            Result.success(explanation)
        } catch (e: Exception) {
            Log.e("AIRepository", "Error explaining quiz answer", e)
            val friendlyMessage = when {
                e is HttpException && e.code() == 429 -> "Máy chủ AI đang bận. Vui lòng đợi một lát."
                e is HttpException && e.code() == 503 -> "Dịch vụ AI đang bảo trì. Vui lòng thử lại sau."
                else -> e.message ?: "Không thể lấy giải thích từ AI."
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    /**
     * Cleans common markdown wrapper backticks if returned under mismatch configurations
     */
    private fun cleanJsonResponse(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        return str.trim()
    }
}
