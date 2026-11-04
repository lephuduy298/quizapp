# TÀI LIỆU GIẢI THÍCH CHỨC NĂNG: LUYỆN ĐIỂM YẾU & CÁC CHỨC NĂNG HỌC TẬP

Tài liệu này tóm tắt ngắn gọn cơ chế hoạt động, các **Class (Lớp)** và **Hàm (Function)** chịu trách nhiệm thực thi các chức năng học tập thông minh trong dự án QuizAI.

---

## 1. CHỨC NĂNG TRỌNG TÂM: LUYỆN ĐIỂM YẾU (ADAPTIVE LEARNING)

### A. Tóm tắt ngắn gọn cơ chế hoạt động:
* **Mục tiêu:** Không đưa câu hỏi ngẫu nhiên mà tự động điều chỉnh theo năng lực thực tế của người học.
* **Cách thức:**
  1. **Đo lường năng lực:** Tính điểm thông thạo từng kỹ năng theo trọng số thời gian (lần làm gần nhất có giá trị cao nhất).
  2. **Chu kỳ 5 câu thông minh:** 3 câu đầu tập trung vào kỹ năng yếu nhất; câu thứ 4 khám phá kỹ năng mới; câu thứ 5 củng cố kỹ năng đã vững.
  3. **Thích ứng độ khó động (DDA):** Làm đúng 2 câu liên tiếp tự động tăng độ khó; làm sai 2 câu liên tiếp tự động hạ độ khó.
  4. **Phát hiện ảo tưởng kiến thức:** Ưu tiên số 1 cho các câu người dùng "rất tự tin nhưng lại làm sai" để sửa ngay lỗ hổng nhận thức.

---

### B. Danh sách Class và Hàm phụ trách:

| Tầng / Thành phần | Tên Class / File | Hàm / Thuộc tính chính | Nhiệm vụ cụ thể |
| :--- | :--- | :--- | :--- |
| **Thuật toán cốt lõi** | `MasteryCalculator`<br>*(AdaptiveEngine.kt)* | `calculate(...)` | Lấy 20 lượt làm gần nhất, nhân trọng số lũy tiến thời gian `(i + 1)` để tính điểm `score` (0-100) và xếp loại `MasteryStatus` (NEW, INSUFFICIENT, LEARNING, MASTERED). |
| | | `validAttempts(...)` | Lọc các lần làm bài hợp lệ (bỏ qua câu bị skip, khớp đúng phiên bản câu hỏi). |
| | `SkillMastery`<br>*(AdaptiveEngine.kt)* | `needsFocus` | Xác định kỹ năng là điểm yếu khi: `recentCount >= 3 && score < 60`. |
| | `AdaptiveQuestionSelector`<br>*(AdaptiveEngine.kt)* | `choose(...)` | Hàm "bộ não" điều phối chọn câu hỏi: luân chuyển chu kỳ 5 câu, tăng/giảm độ khó động (DDA) và xếp thứ tự ưu tiên câu hỏi theo ma trận tự tin. |
| **Tầng Dữ liệu (Repository / Room)** | `LearningRepository`<br>*(LearningRepository.kt)* | `start(...)` | Khởi tạo phiên luyện tập (5, 10 hoặc 15 câu), gọi `AdaptiveQuestionSelector.choose()` để lấy câu hỏi mở đầu. |
| | | `answer(...)` | Lưu kết quả làm bài kèm mức độ tự tin (`confidence` 1-3) và thời gian làm bài vào bảng `question_attempts`. |
| | | `next(...)` | Đánh giá lịch sử phiên làm việc hiện tại và chọn câu thích ứng tiếp theo; tự động hoàn thành phiên khi đủ số câu. |
| | `LearningDao`<br>*(LearningDao.kt)* | `scopedQuestions(...)` | Truy vấn ngân hàng câu hỏi theo phạm vi đề thi, thư mục hoặc kỹ năng. |
| | | `insertAttempt(...)` | Lưu trữ bản ghi lượt làm câu hỏi (`QuestionAttemptEntity`). |
| **Tầng ViewModel** | `AdaptivePracticeViewModel`<br>*(AdaptivePracticeViewModel.kt)* | `start(...)` | Nhận yêu cầu từ UI và kích hoạt tạo phiên luyện tập mới. |
| | | `answer(option, confidence)` | Gửi đáp án lựa chọn và mức độ tự tin (1: Đoán mò, 2: Tương đối chắc, 3: Chắc chắn). |
| | | `next(skip)` | Chuyển sang câu hỏi thích ứng tiếp theo hoặc bỏ qua câu hiện tại. |
| | | `timing(resumed)` | Đo thời gian suy nghĩ thực tế của người dùng cho từng câu hỏi. |
| **Tầng Giao diện (UI)** | `AdaptiveLearningScreen`<br>*(AdaptiveLearningScreen.kt)* | `AdaptiveLearningScreen(...)` | Màn hình chính hiển thị bản đồ kỹ năng, điểm số và phiên luyện thích ứng. |
| | | `PracticeContent(...)` | Giao diện làm bài: hiển thị lý do chọn câu (`reason`), thanh tiến độ, các phương án lựa chọn và bộ chọn mức độ tự tin. |

---

### C. Chi tiết cơ chế Cập nhật độ khó (DDA) & Chọn câu hỏi tiếp theo:

> **Lưu ý cốt lõi:** Độ khó của từng câu hỏi trong cơ sở dữ liệu (`QuestionEntity.difficulty`) **không bị thay đổi** (câu Dễ vẫn là Dễ, câu Khó vẫn là Khó). Thuật toán cập nhật chính là **"Cấp độ năng lực mục tiêu" (`targetDifficulty`)** của người học để quyết định câu tiếp theo đưa ra là Dễ, Vừa hay Khó.

#### 1. Thang đo độ khó & Thiết lập ban đầu:
* Có 3 cấp độ: `1` (Dễ), `2` (Vừa - Mặc định), `3` (Khó).
* Gán nhãn ban đầu: Mặc định là `2` khi tạo câu hỏi, hoặc người dùng/giáo viên phân loại thủ công qua `LearningRepository.classify(...)`.

#### 2. Vị trí mã nguồn Cập nhật độ khó mục tiêu (`AdaptiveEngine.kt` - Dòng 76 đến 82):
Hệ thống quan sát **2 lượt làm gần nhất** của kỹ năng đó (`valid.filter { it.skillId == skillId }.takeLast(2)`):
```kotlin
val recent = valid.filter { it.skillId == skillId }.takeLast(2)
val lastDifficulty = recent.lastOrNull()?.difficulty ?: 2
val targetDifficulty = when {
    recent.size == 2 && recent.all { it.correct } -> (lastDifficulty + 1).coerceAtMost(3)
    recent.size == 2 && recent.none { it.correct } -> (lastDifficulty - 1).coerceAtLeast(1)
    else -> lastDifficulty
}
```
* **Tăng độ khó:** Đúng cả 2 câu liên tiếp $\rightarrow$ Tăng lên 1 bậc: `lastDifficulty + 1` (chặn trần tối đa là 3 - Khó bằng `coerceAtMost(3)`).
* **Hạ độ khó:** Sai cả 2 câu liên tiếp $\rightarrow$ Giảm đi 1 bậc: `lastDifficulty - 1` (chặn sàn tối thiểu là 1 - Dễ bằng `coerceAtLeast(1)`).
* **Giữ nguyên:** Mới làm 1 câu hoặc 1 đúng 1 sai $\rightarrow$ Tiếp tục giữ nguyên `lastDifficulty`.
* **Khởi điểm:** Nếu kỹ năng chưa có lịch sử làm bài, mặc định mốc khởi đầu là `2` (Vừa).

#### 3. Vị trí mã nguồn Xác định câu tiếp theo (`AdaptiveEngine.kt` - Dòng 84 đến 88):
Thuật toán lọc kho câu hỏi chưa làm của kỹ năng đó và chọn câu có độ khó gần với `targetDifficulty` nhất theo sai số tuyệt đối $\Delta = |\text{difficulty} - \text{targetDifficulty}|$:
```kotlin
val question = available.getValue(skillId).sortedWith(
    compareBy<QuestionEntity> { kotlin.math.abs(it.difficulty - targetDifficulty) }
        .thenBy { q -> when {
            latest[q.id]?.let { !it.correct && it.confidence == 3 } == true -> 0 // Ưu tiên 1: Tự tin cao nhưng làm sai
            latest[q.id]?.correct == false -> 1                                   // Ưu tiên 2: Từng làm sai thông thường
            latest[q.id]?.let { it.correct && it.confidence == 1 } == true -> 2  // Ưu tiên 3: Từng đoán mò đúng
            latest[q.id] == null -> 3                                            // Ưu tiên 4: Câu chưa từng gặp
            else -> 4                                                            // Ưu tiên 5: Đã làm đúng và tự tin
        } }
).first()
```
* **Lý tưởng:** Chọn câu có $\Delta = 0$ (đúng độ khó mục tiêu).
* **Cơ chế Fallback (Thiếu câu):** Nếu kho đề không có câu đúng độ khó mục tiêu (ví dụ cần mức 3 nhưng chỉ có câu mức 2), thuật toán tự động chọn câu có mức độ khó gần nhất (`abs` nhỏ nhất) và kèm thông báo: *"Ngân hàng hiện có chưa đủ mức khó phù hợp; dùng mức gần nhất."* (Dòng 102).

#### 4. Vòng đời kích hoạt (Độ khó được cập nhật lúc nào?):
1. **Lưu vết:** Khi người học bấm chọn đáp án $\rightarrow$ `LearningRepository.answer(...)` lưu kết quả (đúng/sai) và độ khó câu đó vào bảng `question_attempts`.
2. **Kích hoạt tính toán:** Khi người học bấm *"Câu tiếp theo"* $\rightarrow$ `LearningRepository.next(...)` gọi `AdaptiveQuestionSelector.choose(...)`.
3. **Thực thi:** Thuật toán đọc lại 2 câu gần nhất vừa hoàn thành, thực thi khối lệnh Dòng 76–88 để cập nhật `targetDifficulty` và chọn câu hỏi mới.

---

## 2. BỔ SUNG: CÁC CHỨC NĂNG HỌC TẬP LIÊN QUAN

### A. Chức năng Học Flashcard:
* **Cơ chế:** Học thẻ ghi nhớ với hoạt ảnh lật 3D và cử chỉ vuốt kéo sang trái (Chưa nhớ) / sang phải (Đã nhớ).
* **Mã nguồn:**
  * UI: `FlashcardScreen.kt` (`handleCardOutcome(...)`, `detectHorizontalDragGestures`).
  * ViewModel: `QuizViewModel.kt` -> hàm `rateFlashcard(...)`.
  * Repository: `ReviewRepository.kt` -> hàm `rate(...)`.

---

### B. Chức năng Chuỗi ngày học liên tục (Streak):
* **Cơ chế:** Tính số ngày liên tục mở ứng dụng học tập; tự động kiểm tra và reset về 0 nếu người dùng bỏ lỡ quá 1 ngày.
* **Mã nguồn:**
  * Core: `UserManager.kt` -> hàm `updateStreak()` (cộng dồn chuỗi) và `checkAndApplyStreakFreezes()` (kiểm tra đứt chuỗi).
  * ViewModel: `QuizViewModel.kt` -> hàm `updateStreak()` và StateFlow `streakCount`.
  * UI: `HomeScreen.kt` (`StreakFlameWidget`), `StreakDialogs.kt` (`StreakDetailsDialog`), `StudyProgressWidget.kt` (Widget màn hình chính).

---

### C. Chức năng Ôn lặp lại ngắt quãng (Spaced Repetition):
* **Cơ chế:** Thuật toán SuperMemo rút gọn với các mốc giãn cách `[1, 3, 7, 14, 30]` ngày dựa trên 4 mức đánh giá: `AGAIN`, `HARD`, `GOOD`, `EASY`.
* **Mã nguồn:**
  * Thuật toán: `ReviewScheduler.kt` -> hàm `rate(...)` tính `nextReviewAt`.
  * Repository: `ReviewRepository.kt` -> hàm `recordAnswer(...)` (tự động gom câu làm trắc nghiệm bị sai vào hàng đợi ôn).
  * ViewModel: `ReviewViewModel.kt` -> hàm `rate(...)`, lọc danh sách câu đến hạn `due(now)`.
  * UI: `StudyHubScreen.kt` (Trung tâm ôn tập), `SpacedReviewScreen.kt` (Màn hình ôn thẻ ngắt quãng và Sổ câu sai).

---

### D. Hệ thống Thông báo nhắc nhở học tập (Push Notifications & AlarmManager):

Ứng dụng sở hữu **2 cơ chế thông báo** thông minh độc lập:

#### 1. Thông báo nhắc học hàng ngày (Daily Study Reminder - Giữ chuỗi Streak):
* **Mục tiêu:** Báo thức nhắc người dùng mở app làm quiz vào khung giờ cố định và các ngày trong tuần mong muốn để duy trì thói quen và không bị đứt chuỗi Streak.
* **Cơ chế kỹ thuật:**
  * **Báo thức chính xác cả trong Doze Mode:** Sử dụng `AlarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ...)` đảm bảo đánh thức thiết bị và phát thông báo chuẩn xác từng phút ngay cả khi điện thoại đang ở chế độ ngủ sâu.
  * **Lặp lại thủ công chính xác (Reschedule for tomorrow):** Không sử dụng `setRepeating` (vốn bị hệ điều hành Android gom cụm làm sai lệch giờ để tiết kiệm pin), hệ thống đặt báo thức từng ngày một. Khi chuông kích hoạt trong `onReceive`, hệ thống kiểm tra thứ trong tuần (`reminderDays.contains(currentDay)`) rồi tự động lên lịch tiếp cho ngày hôm sau.
  * **Tự khôi phục sau khi khởi động lại máy (Boot Completed):** Lắng nghe sự kiện `BOOT_COMPLETED` và `QUICKBOOT_POWERON` để tự động nạp lại giờ báo thức từ SharedPreferences và đăng ký lại với hệ điều hành một cách âm thầm mà không cần người dùng mở app.
* **Mã nguồn:**
  * Lên lịch: `StudyReminderHelper.kt` -> hàm `scheduleReminder(...)`, `cancelReminder(...)`.
  * Bộ thu sóng (Receiver): `StudyReminderReceiver.kt` -> hàm `onReceive(...)` và `showNotification(...)`.
  * Cài đặt & Dữ liệu: `UserManager.kt` (`isReminderEnabled()`, `getReminderDays()`, `getReminderHour()`, `getReminderMinute()`).

#### 2. Thông báo nhắc ôn thẻ đến hạn (Spaced Review Due Reminder):
* **Mục tiêu:** Báo cho người dùng khi có câu hỏi / thẻ flashcard đến hạn ôn tập theo chu kỳ lặp lại ngắt quãng (Spaced Repetition).
* **Cơ chế kỹ thuật:**
  * **Lắng nghe thay đổi cơ sở dữ liệu:** Sử dụng Room `InvalidationTracker` quan sát trực tiếp các bảng `questions` và `review_progress`. Khi có thay đổi dữ liệu hoặc chuyển người dùng, hệ thống tự động tính toán lại kế hoạch ôn (`ReviewReminderPlan`).
  * **Lên lịch theo hạn gần nhất:** Đặt `AlarmManager` đúng vào mốc thời gian của câu hỏi có hạn ôn tiếp theo gần nhất (`plan.nextAlarmAt`).
  * **Chống bắn thông báo trùng lặp (Deduplication):** Lưu trữ danh sách câu hỏi đã gửi thông báo vào SharedPreferences (`notified:$user`), chỉ cập nhật thông báo khi xuất hiện câu hỏi mới đến hạn hoặc thay đổi số lượng thẻ cần ôn.
  * **Deep Link:** Chạm vào thông báo sẽ mở ứng dụng và điều hướng thẳng vào màn hình ôn tập thông qua `MainActivity.ACTION_REVIEW_REMINDER`.
* **Mã nguồn:**
  * Quản lý & Điều phối: `ReviewDueReminder.kt` -> `ReviewDueReminder.start(...)`, `ReviewReminderController.refresh()`.
  * Bộ thu sóng: `ReviewDueReminderReceiver` (lắng nghe `ACTION_DUE`, `BOOT_COMPLETED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`).
  * Giao diện & Xin quyền: `ReviewReminderSettings.kt` (quản lý Switch bật/tắt và xin quyền `POST_NOTIFICATIONS` trên Android 13+).

---

### E. Hệ thống Widget màn hình chính (Android AppWidget bằng Jetpack Glance):

* **Mục tiêu:** Hiển thị trực quan tiến độ học tập, chuỗi ngày Streak liên tiếp và số câu hỏi đến hạn ôn tập ngay trên màn hình chính (Home Screen) của điện thoại Android mà không cần phải mở ứng dụng.
* **Công nghệ áp dụng:** Sử dụng thư viện **Jetpack Glance** (`androidx.glance`) hiện đại của Google cho phép xây dựng Widget bằng cú pháp Jetpack Compose khai báo (declarative) thay vì viết XML `RemoteViews` truyền thống cồng kềnh.

#### 1. Các loại Widget trong ứng dụng:
* **`StudyProgressWidget` (Tiến độ học tập & Chuỗi Streak):**
  * Hiển thị số ngày duy trì chuỗi Streak liên tiếp (kèm biểu tượng ngọn lửa 🔥), số câu hỏi đến hạn ôn và trạng thái hoạt động trong ngày hôm nay.
  * Hỗ trợ giao diện co giãn thích ứng kích thước (**Responsive Size Mode**): tự động dàn trang linh hoạt theo 4 kích cỡ (nhỏ `140x140 dp`, vừa `140x220 dp`, ngang rộng `280x140 dp`, lớn `280x220 dp`).
* **`FocusStudyWidget` (Tập trung ôn tập - Focus Review):**
  * Kế thừa từ `StudyProgressWidget`, tập trung làm nổi bật số lượng câu hỏi đang chờ ôn (`dueCount`) cùng huy hiệu Streak nhỏ gọn để thúc đẩy người học vào ôn bài nhanh.

#### 2. Tính năng tương tác trên Widget (AppWidget Interactions):
* **Nút Làm mới (Refresh Button):** Chạm vào biểu tượng xoay để gọi `actionRunCallback<RefreshStudyWidget>()`, kích hoạt tải lại tiến độ mới nhất trong nền.
* **Nút "Ôn ngay" (Action Button):** Chạm vào nút để kích hoạt `actionStartActivity(...)`, mở thẳng ứng dụng `MainActivity` kèm cờ `ACTION_WIDGET_REVIEW` đưa người dùng vào màn hình ôn tập ngay lập tức.

#### 3. Cơ chế tự động cập nhật dữ liệu Reactive (Real-time Sync):
* **Lắng nghe thay đổi tự động:** Lớp `StudyWidgetUpdates.kt` sử dụng Coroutine Flow kết hợp:
  * Room `invalidationTracker.createFlow("questions", "review_progress")` (lắng nghe mọi thay đổi về câu hỏi và tiến độ ôn tập).
  * `SharedPreferences.OnSharedPreferenceChangeListener` (lắng nghe thay đổi tài khoản người dùng và streak).
* **Cập nhật mượt mà (Debounce):** Gom các sự kiện thay đổi trong `200ms` (`debounce(200)`) và gọi `updateAll(context)` để cập nhật dữ liệu Widget ngay lập tức khi người học vừa làm xong bài hoặc thêm câu hỏi mới.
* **An toàn dữ liệu đa tài khoản:** Kiểm tra xác thực `manager.getCurrentUser() == user` trước khi hiển thị, tránh rò rỉ dữ liệu của tài khoản cũ khi đăng xuất/chuyển tài khoản.

#### 4. Mã nguồn chi tiết:
* Giao diện & Xử lý chính:
  * [StudyProgressWidget.kt](file:///d:/download/android/app/src/main/java/com/example/widget/StudyProgressWidget.kt) -> `StudyProgressWidget`, `StudyProgressWidgetReceiver`, `RefreshStudyWidget`.
  * [FocusStudyWidget.kt](file:///d:/download/android/app/src/main/java/com/example/widget/FocusStudyWidget.kt) -> `FocusStudyWidget`, `FocusStudyWidgetReceiver`.
* Quản lý cập nhật thời gian thực:
  * [StudyWidgetUpdates.kt](file:///d:/download/android/app/src/main/java/com/example/widget/StudyWidgetUpdates.kt) -> `StudyWidgetUpdates.start(context)`, `StudyWidgetUpdates.requestRefresh()`.

---

### F. Sinh đề thi tự động từ Ảnh chụp Camera bằng Gemini AI (AI Vision & Structured Outputs):

* **Mục tiêu:** Chụp ảnh trực tiếp tài liệu, sách bài tập hoặc đề thi giấy để tự động nhận dạng và tạo thành một bộ câu hỏi trắc nghiệm hoàn chỉnh trong vài giây mà không cần nhập liệu thủ công.
* **Cơ chế kỹ thuật cốt lõi:**
  1. **CameraX & Chụp ảnh tối ưu:** Dùng thư viện Android CameraX ([CameraScanScreen.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/CameraScanScreen.kt)) để xem trước (Preview) và chụp ảnh độ nét cao, tự động xoay đúng chiều và nén thành chuỗi `Base64`.
  2. **JSON Schema nghiêm ngặt (Structured Outputs):** Gửi yêu cầu tới mô hình Google Gemini 1.5 Flash kèm cấu trúc [Schema](file:///d:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt#L33-L59) bắt buộc. Điều này đảm bảo AI **100% trả về đúng định dạng JSON có cấu trúc** (chứa `title`, `topic`, danh sách `questions`, các lựa chọn `options` và chỉ số đáp án đúng `correctOptionIndex`), loại bỏ hoàn toàn nguy cơ app bị crash do lỗi phân tích cú pháp (parse error).
  3. **Tự động bóc tách và giải đề:** Prompt được tinh chỉnh đặc biệt để AI tự động nhận biết số câu (Câu 1, Câu 2...), tách phương án A, B, C, D, và tự suy luận giải đề để đánh dấu đáp án đúng.
  4. **Cơ chế làm sạch & Thử lại khi gặp lỗi (Fault Tolerance):**
     * `cleanJsonResponse(...)`: Tự động cắt bỏ các thẻ code block markdown (```json ... ```) nếu AI trả về kèm định dạng văn bản.
     * `retryIO(...)`: Tự động thử lại tối đa 3 lần với thuật toán giãn cách lũy thừa (**Exponential Backoff**: 1s $\rightarrow$ 2s $\rightarrow$ 4s) khi gặp lỗi mạng chập chờn hoặc bị giới hạn tần suất API (mã 429 Too Many Requests).
* **Mã nguồn:**
  * Giao diện Camera: [CameraScanScreen.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/CameraScanScreen.kt).
  * Xử lý gọi AI & Schema: [AIRepository.kt](file:///d:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt) -> hàm `generateQuizFromImage(...)`.
  * Tầng ViewModel: [QuizViewModel.kt](file:///d:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt) -> hàm `generateQuizFromImage(...)`.

---

### G. Tính năng "AI Sư Phụ" - Gia sư ảo giải thích đáp án bằng Markdown:

* **Mục tiêu:** Giúp người học hiểu sâu bản chất lỗi sai sau khi làm quiz thay vì chỉ biết đáp án đúng/sai một cách máy móc.
* **Cơ chế kỹ thuật:**
  * Đóng vai trò như một gia sư sư phạm ảo (AI Tutor). Khi người dùng nhấn *"Nhờ AI giải thích"*, hệ thống đóng gói ngữ cảnh gồm: câu hỏi, các phương án lựa chọn, đáp án bạn chọn sai và đáp án đúng gửi lên Gemini AI.
  * AI sẽ phân tích rõ:
    * Vì sao đáp án bạn chọn lại sai (chỉ ra bẫy tư duy thường gặp).
    * Vì sao đáp án chuẩn lại đúng (giải thích ngắn gọn, dễ hiểu trong 3-4 đoạn).
  * Định dạng trả về hỗ trợ **Markdown** (in đậm, danh sách gạch đầu dòng, công thức) và hiển thị trực tiếp trên giao diện Jetpack Compose.
* **Mã nguồn:**
  * Xử lý Prompt AI: [AIRepository.kt](file:///d:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt#L231-L260) -> hàm `getExplanationForAnswer(...)`.
  * Hiển thị kết quả: [QuizReviewScreen.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/QuizReviewScreen.kt).

---

### H. "Sổ câu sai" (Mistake Notebook) & Ghi chú cá nhân hóa:

* **Mục tiêu:** Tự động gom tất cả các câu hỏi mà người dùng từng làm sai để ôn tập lại riêng biệt, tránh lặp lại sai lầm trong các kỳ thi thực tế.
* **Cơ chế kỹ thuật:**
  * **Tự động gom lỗi từ mọi chế độ:** Bất kể người học làm trắc nghiệm thông thường, thi thử, hay làm bài thích ứng Adaptive Learning, hễ chọn sai phương án thì hàm `ReviewRepository.recordAnswer(...)` sẽ tự động ghi nhận vào bảng `review_progress`.
  * **Lưu vết chi tiết:** Lưu trữ số lần làm sai (`mistakeCount`), mốc thời gian sai gần nhất (`lastWrongAt`) và đặc biệt là **phương án bạn từng chọn sai** (`lastSelectedOption`). Khi mở sổ câu sai, app sẽ nhắc lại: *"Lần sai gần nhất bạn chọn: [Đáp án sai]"*.
  * **Ghi chú cá nhân hóa (Personalized Notes):** Cho phép người học tự viết ghi chú cách hiểu hoặc mẹo ghi nhớ cho riêng mình (`note`, hỗ trợ tối đa 5000 ký tự) và đánh dấu sao câu hỏi quan trọng (`isFavorite`).
* **Mã nguồn:**
  * Lưu trữ: [ReviewEntities.kt](file:///d:/download/android/app/src/main/java/com/example/data/local/ReviewEntities.kt) (`ReviewProgressEntity`).
  * Xử lý dữ liệu: [ReviewRepository.kt](file:///d:/download/android/app/src/main/java/com/example/data/repository/ReviewRepository.kt) -> hàm `recordAnswer(...)`, `annotate(...)`.
  * Giao diện: [SpacedReviewScreen.kt](file:///d:/download/android/app/src/main/java/com/example/ui/screens/SpacedReviewScreen.kt) (tab *"Câu sai"*, component `ReviewNoteEditor`).


