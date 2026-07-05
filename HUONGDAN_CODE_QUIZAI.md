# CẨM NANG ÔN TẬP MÃ NGUỒN DỰ ÁN QUIZAI
## Hướng dẫn giải thích chức năng, thuật toán & mã nguồn phục vụ báo cáo

Tài liệu này được biên soạn chi tiết giúp bạn hiểu rõ từng dòng code quan trọng trong dự án **QuizAI**, nắm vững cơ chế hoạt động và các điểm đặc biệt để tự tin trả lời mọi câu hỏi phản biện của thầy cô hội đồng.

---

## MỤC LỤC
1. [Kiến trúc Dự án & Dependency Injection (DI) thủ công](#1-kiến-trúc-dự-án--dependency-injection-di-thủ-công)
2. [Cơ sở Dữ liệu cục bộ Room & Type Converters (Moshi)](#2-cơ-sở-dữ-liệu-cục-bộ-room--type-converters-moshi)
3. [Sinh đề thi trắc nghiệm tự động bằng Gemini AI](#3-sinh-đề-thi-trắc-nghiệm-tự-động-bằng-gemini-ai)
4. [Tính năng "AI Sư Phụ" giải thích đáp án bằng Markdown](#4-tính-năng-ai-sư-phụ-giải-thích-đáp-án-bằng-markdown)
5. [Cơ chế Streak & Tự động Đóng băng chuỗi (Streak Freezes)](#5-cơ-chế-streak--tự-động-đóng-băng-chuỗi-streak-freezes)
6. [Hệ thống Nhắc nhở học tập hàng ngày (AlarmManager & BroadcastReceiver)](#6-hệ-thống-nhắc-nhở-học-tập-hàng-ngày-alarmmanager--broadcastreceiver)

---

## 1. Kiến trúc Dự án & Dependency Injection (DI) thủ công

### A. Chức năng chính:
Quản lý sự phụ thuộc (DI) giữa các tầng kiến trúc (UI, Repository, Data Source), cung cấp các thực thể duy nhất (Singletons) cho toàn ứng dụng như `AppDatabase`, `RetrofitClient`, `QuizRepository`, `AIRepository`.

### B. Mã nguồn quan trọng:
Tệp tin [DependencyContainer.kt](file:///d:/download/android/app/src/main/java/com/example/DependencyContainer.kt):
```kotlin
interface AppContainer {
    val quizRepository: QuizRepository
    val aiRepository: AIRepository
    val userManager: UserManager
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val geminiApiService: GeminiApiService by lazy {
        RetrofitClient.geminiApiService
    }

    override val quizRepository: QuizRepository by lazy {
        QuizRepository(database.quizDao(), database.quizSessionDao(), database.folderDao())
    }

    override val aiRepository: AIRepository by lazy {
        DirectGeminiAIRepositoryImpl(geminiApiService)
    }

    override val userManager: UserManager by lazy {
        UserManager.getInstance(context)
    }
}
```

Vòng đời của `AppContainer` được quản lý bởi tệp [QuizApplication.kt](file:///d:/download/android/app/src/main/java/com/example/QuizApplication.kt):
```kotlin
class QuizApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
```

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Tại sao không dùng Hilt hoặc Dagger?**
    *   *Trả lời:* Dự án ưu tiên thiết kế DI thủ công (Manual DI) để **tối ưu dung lượng ứng dụng**, tăng tốc độ biên dịch (compile-time) và tránh rò rỉ bộ nhớ. Việc sử dụng thuộc tính `by lazy` đảm bảo các tài nguyên nặng như Database và Client API chỉ được khởi tạo khi thực sự cần thiết (Lazy Initialization).
*   **ViewModel nhận Container như thế nào?**
    *   *Trả lời:* Sử dụng `ViewModelProvider.Factory` thủ công được định nghĩa trong [QuizViewModel.kt](file:///d:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt#L582-L593) để truyền trực tiếp các repository từ `container` vào ViewModel.

---

## 2. Cơ sở Dữ liệu cục bộ Room & Type Converters (Moshi)

### A. Chức năng chính:
Lưu trữ cục bộ (Offline) danh sách thư mục (`FolderEntity`), đề thi (`QuizEntity`), câu hỏi (`QuestionEntity`) và phiên làm bài (`QuizSessionEntity`).

### B. Mã nguồn quan trọng:
Tệp tin [Entities.kt](file:///d:/download/android/app/src/main/java/com/example/data/local/Entities.kt#L83-L114):
```kotlin
class RoomConverters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val mapType = Types.newParameterizedType(Map::class.java, java.lang.Long::class.java, java.lang.Integer::class.java)
    private val mapAdapter = moshi.adapter<Map<Long, Int>>(mapType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { stringListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { stringListAdapter.fromJson(it) }
    }

    @TypeConverter
    fun fromAnswersMap(value: Map<Long, Int>?): String? {
        return value?.let { mapAdapter.toJson(it) }
    }

    @TypeConverter
    fun toAnswersMap(value: String?): Map<Long, Int>? {
        return value?.let { mapAdapter.fromJson(it) }
    }
}
```

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Tại sao Room cần TypeConverter?**
    *   *Trả lời:* SQLite/Room chỉ hỗ trợ các kiểu dữ liệu nguyên thủy (String, Int, Long...). Câu hỏi có mảng các phương án lựa chọn (`List<String>`) và lịch sử bài làm có ánh xạ câu hỏi-đáp án (`Map<Long, Int>`) không thể lưu trực tiếp. Lớp [RoomConverters](file:///d:/download/android/app/src/main/java/com/example/data/local/Entities.kt#L84) sử dụng thư viện **Moshi** để tuần tự hóa (Serialize) chúng thành chuỗi JSON dạng String khi lưu, và giải tuần tự hóa (Deserialize) ngược lại khi truy vấn.
*   **Cascade Delete là gì và cấu hình ở đâu?**
    *   *Trả lời:* Trong `QuestionEntity` và `QuizSessionEntity`, khóa ngoại nối tới `QuizEntity` được định nghĩa với `onDelete = ForeignKey.CASCADE`. Khi người dùng xóa một đề thi, toàn bộ câu hỏi và phiên làm bài liên quan sẽ tự động bị xóa sạch khỏi cơ sở dữ liệu để tránh dữ liệu mồ côi (orphaned data).

---

## 3. Sinh đề thi trắc nghiệm tự động bằng Gemini AI

### A. Chức năng chính:
Đọc ảnh tài liệu đề thi bằng camera (CameraX/Base64) hoặc nhận đoạn text chat từ người dùng, gửi yêu cầu tới Google Gemini API và nhận về một đề thi trắc nghiệm định dạng JSON chuẩn.

### B. Mã nguồn quan trọng:
Tệp tin [AIRepository.kt](file:///d:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt):
```kotlin
private val quizSchema = Schema(
    type = "OBJECT",
    properties = mapOf(
        "title" to Schema(type = "STRING", description = "Tên đề thi trắc nghiệm ngắn gọn"),
        "topic" to Schema(type = "STRING", description = "Chủ đề tổng quát"),
        "questions" to Schema(
            type = "ARRAY",
            items = Schema(
                type = "OBJECT",
                properties = mapOf(
                    "text" to Schema(type = "STRING", description = "Nội dung câu hỏi"),
                    "options" to Schema(
                        type = "ARRAY",
                        items = Schema(type = "STRING"),
                        description = "Danh sách từ 2 đến 4 phương án"
                    ),
                    "correctOptionIndex" to Schema(type = "INTEGER", description = "Chỉ mục đáp án đúng (0-based)")
                ),
                required = listOf("text", "options", "correctOptionIndex")
            )
        )
    ),
    required = listOf("title", "topic", "questions")
)
```

Gọi Retrofit với cấu hình prompt trích xuất và JSON Schema nghiêm ngặt cho ảnh:
```kotlin
val prompt = """
    Analyze the text or exercises in the attached image and generate a complete quiz containing ALL questions present in the image.
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
    Guidelines for extraction:
    1. Extract ALL questions visible in the image. Do not limit, cap, or pad the list of questions. If there are 10 questions, extract all 10. If there are only 2, extract 2.
    2. Each numbered index (e.g., "Câu 1", "Question 2", "1.", "2)") in the image indicates a new question.
    3. The choices labeled with A, B, C, D (or A., B., C., D. or resembling format) are the options for that question. Do not include the labels "A.", "B." etc. in the option text itself.
    4. Dynamically evaluate the question and determine which option is the correct answer, setting correctOptionIndex to the correct 0-based index (0 for option A, 1 for B, etc.).
    5. Preserve the language of the questions as they appear in the image (e.g., if the image is in Vietnamese, the extracted questions and options must be in Vietnamese).
    6. Ensure no formatting wraps like markdown ticks (```json ... ```), return raw JSON only.
    
    Additional instructions: ${'$'}promptAddition
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
        responseMimeType = "application/json",
        responseSchema = quizSchema,
        temperature = 0.4f
    )
)
```

Hàm lọc sạch dữ liệu thô đề phòng AI trả về thẻ markdown:
```kotlin
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
```

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Làm sao đảm bảo AI luôn trả về đúng cấu trúc JSON để app không bị crash khi parse?**
    *   *Trả lời:* Dự án sử dụng cấu hình **Structured Output** của Gemini API bằng cách thiết lập cấu trúc [Schema](file:///d:/download/android/app/src/main/java/com/example/data/remote/GeminiModels.kt#L40) chi tiết khớp với định dạng Kotlin và đặt thuộc tính `responseMimeType = "application/json"`. Đồng thời, lớp Repository có hàm `cleanJsonResponse` xử lý các ký tự bao bọc codeblock markdown (```json ... ```) để chuỗi JSON luôn sạch trước khi đưa vào bộ giải mã `Kotlinx Serialization`.
*   **Làm sao tối ưu hóa quá trình nhận diện bố cục đề thi từ hình ảnh (nhận diện câu hỏi, lựa chọn, số lượng câu)?**
    *   *Trả lời:* Trong System Prompt của hàm tạo đề từ ảnh, chúng ta đã hướng dẫn cụ thể cho AI cách phân tích bố cục đề thi:
        1. Nhận dạng các chữ số đánh số câu (như "Câu 1", "1.", "Question 2") làm dấu hiệu bắt đầu một câu hỏi mới.
        2. Nhận dạng các ký tự lựa chọn "A", "B", "C", "D" để tự động tách phương án trả lời.
        3. Yêu cầu AI tự động giải đề và đánh chỉ số đáp án đúng thông qua `correctOptionIndex`.
        4. Loại bỏ hoàn toàn giới hạn cứng (trước đây AI thường mặc định tạo 3-5 câu), ép buộc AI trích xuất **toàn bộ** số câu hỏi có trên bức ảnh thực tế (ảnh có 10 câu thì tạo 10 câu, có 2 câu thì tạo 2 câu).
*   **Ứng dụng xử lý thế nào nếu gặp lỗi mạng hoặc API bị giới hạn tần suất (Rate Limit 429)?**
    *   *Trả lời:* Sử dụng hàm bổ trợ `retryIO` (sử dụng trễ lũy thừa - exponential backoff). Nếu API trả về mã lỗi `429` (Too Many Requests) hoặc `503`, hệ thống sẽ tự động chờ tăng dần (1s -> 2s -> 4s) và thử lại tối đa 3 lần trước khi trả về thông báo lỗi thân thiện cho người dùng.

---

## 4. Tính năng "AI Sư Phụ" giải thích đáp án bằng Markdown

### A. Chức năng chính:
Đóng vai trò gia sư ảo phân tích chi tiết câu hỏi, các đáp án lựa chọn và chỉ ra lỗi sai của người dùng bằng tiếng Việt định dạng Markdown sạch đẹp.

### B. Mã nguồn quan trọng:
Prompt chi tiết yêu cầu vai trò gia sư trong [AIRepository.kt](file:///d:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt#L231-L240):
```kotlin
val prompt = """
    You are an educational AI tutor. Explain clearly and concisely why the user got this question wrong and why the correct answer is correct.
    Keep the content friendly, encouraging, and clear (max 3-4 short paragraphs, markdown formatting supported, using Vietnamese since the user speaks Vietnamese).

    Context of the Question:
    Question: $questionText
    Available Options: ${options.joinToString(", ")}
    User's Selected Answer: $userAnswer
    Correct Answer: $correctAnswer
""".trimIndent()
```

Hiển thị Markdown trong Compose UI:
- Kết quả text Markdown trả về từ Gemini sẽ được hiển thị trực tiếp lên hộp thoại giải thích của màn hình [QuizReviewScreen.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/QuizReviewScreen.kt) (sử dụng thư viện dựng RichText/Markdown tương thích với Jetpack Compose).

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Tại sao không thiết lập JSON Schema cho chức năng giải thích?**
    *   *Trả lời:* Tính năng này cần câu trả lời mang tính tự nhiên, phân tích sâu sắc bằng ngôn ngữ tự nhiên và định dạng hiển thị Markdown phong phú (sử dụng in đậm, danh sách gạch đầu dòng, khối mã code) để tạo giao diện học tập bắt mắt, trực quan cho học sinh. Do đó, app để định dạng text tự do nhưng ép prompt tiếng Việt ngắn gọn từ 3-4 đoạn.

---

## 5. Cơ chế Streak & Tự động Đóng băng chuỗi (Streak Freezes)

### A. Chức năng chính:
Tạo động lực học tập bằng cách đếm số ngày luyện tập liên tục (Streak). Tích hợp "Bình Đóng Băng Chuỗi" (`Streak Freezes`) để tự động tiêu thụ cứu chuỗi nếu lỡ quên học bài và hiển thị lịch học tập trực quan dạng các hình lục giác liên kết.

### B. Mã nguồn quan trọng:
Hàm xử lý đóng băng tự động trong [UserManager.kt](file:///d:/download/android/app/src/main/java/com/example/data/local/UserManager.kt#L156-L204):
```kotlin
fun checkAndApplyStreakFreezes() {
    val user = getCurrentUser() ?: return
    val lastDate = getLastActiveDate() ?: return
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    if (lastDate == currentDate) return

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    try {
        val d1 = sdf.parse(lastDate)
        val d2 = sdf.parse(currentDate)
        if (d1 != null && d2 != null) {
            val diffTime = d2.time - d1.time
            val diffDays = diffTime / (1000 * 60 * 60 * 24)

            if (diffDays > 1L) { // Nếu bị đứt chuỗi lớn hơn 1 ngày
                val missedDays = (diffDays - 1).toInt()
                val freezes = getStreakFreezes()

                if (freezes >= missedDays) {
                    // Tiêu thụ lượt đóng băng
                    setStreakFreezes(freezes - missedDays)

                    // Điền bù các ngày bị bỏ lỡ vào danh sách ngày active để giữ chuỗi liên kết
                    val calendar = Calendar.getInstance()
                    for (i in 1..missedDays) {
                        calendar.time = d1
                        calendar.add(Calendar.DAY_OF_YEAR, i)
                        val missedDateStr = sdf.format(calendar.time)
                        addActiveDate(missedDateStr)
                    }

                    // Đẩy mốc ngày hoạt động cuối cùng về hôm qua để hôm nay tiếp tục cộng dồn Streak
                    calendar.time = d2
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdf.format(calendar.time)
                    prefs.edit().putString("streak_last_date_$user", yesterdayStr).apply()
                } else {
                    // Nếu không đủ lượt đóng băng để cứu -> Đứt chuỗi, reset về 0
                    prefs.edit()
                        .putInt("streak_count_$user", 0)
                        .remove("streak_last_date_$user")
                        .apply()
                }
            }
        }
    } catch (e: Exception) { /* Bỏ qua */ }
}
```

Vẽ kết nối các ô lịch lục giác trên giao diện [StreakDialogs.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/StreakDialogs.kt#L366-L386) sử dụng Canvas Jetpack Compose:
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val cy = h / 2f
    if (isActive) {
        if (isPreviousActive) { // Nối sang ô bên trái
            drawRect(
                color = Color(0xFFFF9100),
                topLeft = Offset(0f, cy - 8.dp.toPx()),
                size = Size(w / 2f, 16.dp.toPx())
            )
        }
        if (isNextActive) { // Nối sang ô bên phải
            drawRect(
                color = Color(0xFFFF9100),
                topLeft = Offset(w / 2f, cy - 8.dp.toPx()),
                size = Size(w / 2f, 16.dp.toPx())
            )
        }
    }
}
```

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Thuật toán xác định đứt chuỗi và đóng băng hoạt động ra sao?**
    *   *Trả lời:* Thuật toán tính số ngày chênh lệch (`diffDays`) giữa ngày học cuối cùng lưu trong máy (`streak_last_date`) và ngày hôm nay. Nếu `diffDays > 1`, tức là người dùng đã bỏ lỡ ít nhất 1 ngày học. Hệ thống sẽ so sánh số ngày bị lỡ với số lượt Đóng băng chuỗi hiện có. Nếu đủ lượt đóng băng, hệ thống sẽ tự động trừ lượt đóng băng, ghi nhận các ngày bị bỏ lỡ vào dữ liệu hoạt động, và đặt ngày hoạt động cuối về ngày hôm qua (yesterday) để khi hôm nay người dùng trả lời câu hỏi, chuỗi Streak vẫn được cộng dồn mà không bị reset về 0.
*   **Làm sao để vẽ các ô lịch hình lục giác liên kết với nhau?**
    *   *Trả lời:* Sử dụng cấu trúc lớp `HexagonShape` kế thừa từ `Shape` trong Jetpack Compose để cắt bo tròn ô theo 6 cạnh. Để vẽ đường nối màu cam thể hiện sự liên tục của chuỗi, app vẽ trực tiếp một hình chữ nhật (`drawRect`) nằm bên dưới ô lục giác kéo dài từ tâm ô hiện tại sang mép trái (nếu ngày hôm trước hoạt động) và/hoặc sang mép phải (nếu ngày hôm sau hoạt động).

---

## 6. Hệ thống Nhắc nhở học tập hàng ngày (AlarmManager & BroadcastReceiver)

### A. Chức năng chính:
Cho phép đặt lịch báo nhắc học tập hàng ngày theo thời gian và các thứ mong muốn trong tuần. Thông báo đẩy tự động hiển thị đúng giờ kể cả khi người dùng thoát ứng dụng hoặc tắt nguồn máy.

### B. Mã nguồn quan trọng:
Lên lịch nhắc chính xác trong [StudyReminderHelper.kt](file:///d:/download/android/app/src/main/java/com/example/reminder/StudyReminderHelper.kt#L47-L59):
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )
} else {
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )
}
```

Nhận sự kiện và điều phối kiểm tra ngày học trong [StudyReminderReceiver.kt](file:///d:/download/android/app/src/main/java/com/example/reminder/StudyReminderReceiver.kt#L25-L59):
```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val userManager = UserManager.getInstance(context)

    // Khôi phục báo thức khi khởi động lại máy
    if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
        if (userManager.isReminderEnabled()) {
            val hour = userManager.getReminderHour()
            val minute = userManager.getReminderMinute()
            StudyReminderHelper.scheduleReminder(context, hour, minute)
        }
        return
    }

    if (intent.action == "com.example.reminder.ACTION_SHOW_REMINDER") {
        if (userManager.isReminderEnabled() && userManager.isUserLoggedIn()) {
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val reminderDays = userManager.getReminderDays()

            // Chỉ bắn Notification nếu thứ trong tuần hiện tại nằm trong cài đặt của người dùng
            if (reminderDays.contains(currentDay)) {
                showNotification(context)
            }

            // Tiếp tục lập lịch nhắc lại cho ngày mai (tương tự lặp vô hạn thủ công)
            val hour = userManager.getReminderHour()
            val minute = userManager.getReminderMinute()
            StudyReminderHelper.scheduleReminder(context, hour, minute)
        }
    }
}
```

### C. Điều đặc biệt (Câu hỏi thầy cô):
*   **Tại sao không sử dụng cơ chế lặp mặc định `setRepeating` của AlarmManager?**
    *   *Trả lời:* Từ Android 4.4 trở đi, `setRepeating` không còn đảm bảo báo thức rung chính xác giờ để tối ưu pin hệ thống. Ngoài ra, việc sử dụng báo nhắc một lần (`setAndAllowWhileIdle`) và tự động lên lịch nhắc tiếp theo khi nhận báo nhắc (`onReceive`) giúp tiết kiệm pin hiệu quả, tránh bị hệ thống Android đưa vào danh sách ứng dụng chạy ngầm ngốn tài nguyên và đảm bảo báo thức hoạt động tốt ngay cả trong chế độ Doze Mode (ngủ sâu).
*   **Làm thế nào để báo thức nhắc học không bị biến mất khi người dùng khởi động lại điện thoại?**
    *   *Trả lời:* Toàn bộ báo thức đăng ký với hệ điều hành sẽ bị xóa sạch khi tắt nguồn máy. Để giải quyết, dự án đăng ký quyền `RECEIVE_BOOT_COMPLETED` trong [AndroidManifest.xml](file:///d:/download/android/app/src/main/AndroidManifest.xml). Khi máy khởi động xong, hệ thống gửi tín hiệu `BOOT_COMPLETED`, lớp `StudyReminderReceiver` sẽ bắt sự kiện này để tự động nạp lại giờ báo thức từ SharedPreferences và đặt lịch lại trên hệ thống một cách âm thầm mà không cần người dùng mở app.
