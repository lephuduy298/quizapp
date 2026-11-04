# Chuẩn bị báo cáo sản phẩm QuizAI

Đối chiếu mã nguồn trong workspace ngày 21/10/2026. Kịch bản mặc định: 10–12 phút, cộng thời gian hỏi đáp. Đây là tài liệu luyện trình bày và tra code, không phải báo cáo kết quả chạy thử mới. Số dòng bên dưới ứng với mã nguồn tại thời điểm chuẩn bị.

## 1. Thông điệp chính và lời mở đầu

**Thông điệp nên nhớ:** QuizAI hỗ trợ một vòng tự học: tạo ngân hàng câu hỏi → làm bài → hiểu lỗi sai → luyện điểm yếu → ôn lại theo lịch.

Lời mở đầu khoảng 45 giây:

> Em xin giới thiệu QuizAI, ứng dụng Android hỗ trợ tự học bằng câu hỏi trắc nghiệm. Khi tự ôn thi, người học thường mất thời gian nhập đề, khó biết mình yếu ở đâu và dễ quên kiến thức sau khi làm bài. QuizAI kết hợp Gemini để tạo đề từ ảnh hoặc yêu cầu văn bản và giải thích đáp án, với các thuật toán trên thiết bị để chọn câu luyện theo kết quả học và lên lịch ôn lại. Điểm em muốn nhấn mạnh là sự liên kết giữa tạo đề, luyện tập và ôn lại lỗi sai. Các đề đã lưu và các chức năng luyện, ôn có thể sử dụng ngoại tuyến.

## 2. Dàn ý trình bày 10–12 phút

| Thời lượng | Nội dung | Ý cần nói hoặc thao tác |
|---|---|---|
| 0:00–1:00 | Bài toán và người dùng | Học sinh, sinh viên, người tự ôn thi; ba vấn đề: nhập đề, tìm điểm yếu, quên kiến thức |
| 1:00–2:30 | Tạo đề bằng AI | Chụp/chọn ảnh hoặc nhập yêu cầu; nhận câu hỏi, lựa chọn và đáp án; có thể sửa lại |
| 2:30–4:00 | Làm bài và hiểu lỗi | Phân biệt luyện tập và thi; chấm điểm trên máy; xem lại và hỏi AI giải thích |
| 4:00–6:00 | Luyện thích ứng | Bản đồ kỹ năng, mức tự tin, lý do chọn câu và thay đổi độ khó |
| 6:00–7:30 | Ôn ngắt quãng | Sổ câu sai, ghi chú, đánh dấu, bốn mức nhớ và lịch ôn |
| 7:30–8:30 | Duy trì thói quen | Streak, lịch nhắc và widget mở nhanh phần ôn |
| 8:30–10:00 | Kiến trúc và luồng code | Compose → ViewModel → Repository → Room hoặc Gemini; giải thích một luồng cụ thể |
| 10:00–12:00 | Kiểm chứng, giới hạn, hướng phát triển | Test có trong repo; tài khoản local; cần kiểm tra nội dung AI; kế hoạch hoàn thiện |

Nếu chỉ có 5 phút: dành 30 giây giới thiệu, 1 phút tạo đề, 1 phút luyện thích ứng, 1 phút ôn câu sai, 1 phút kiến trúc và 30 giây giới hạn. Chỉ giới thiệu ngắn các tính năng phụ.

## 3. Các tính năng nổi bật và cách diễn đạt

| Tính năng | Giá trị đối với người dùng | Chi tiết kỹ thuật để nói khi được hỏi |
|---|---|---|
| Tạo đề từ ảnh và văn bản | Giảm thao tác nhập câu hỏi | CameraX/ảnh thư viện → Bitmap → JPEG chất lượng 80 → Base64 → Gemini; kết quả JSON được giải mã và lưu Room |
| AI Sư Phụ | Hiểu lý do chọn sai và đáp án đúng | Gửi câu hỏi, các lựa chọn, lựa chọn của người học và đáp án đang lưu; nhận giải thích tiếng Việt |
| Luyện thích ứng và bản đồ kỹ năng | Tập trung vào phần cần củng cố | Chấm thành thạo bằng lịch sử có trọng số, chọn kỹ năng và độ khó bằng quy tắc Kotlin chạy offline |
| Ôn ngắt quãng và sổ câu sai | Có danh sách cần ôn và ghi lại cách hiểu | Đúng/sai hoặc tự đánh giá thẻ cập nhật lịch; ghi chú và đánh dấu quan trọng theo tài khoản |
| Thi, luyện và flashcard | Phù hợp nhiều cách tự học | Thi có đếm ngược và tự nộp; luyện phản hồi theo câu; flashcard lật thẻ rồi tự đánh giá |
| Streak, nhắc học, hai widget | Dễ quay lại học mỗi ngày | SharedPreferences, AlarmManager, BroadcastReceiver và Jetpack Glance |
| Quản lý bộ đề và xuất PDF | Tái sử dụng và chia sẻ tài liệu | Thư mục, thêm/sửa/xóa câu, chuyển bộ đề; PDF có câu hỏi và đáp án tham khảo |

Câu chuyển ý sau phần tạo đề:

> Tạo được đề mới là bước đầu. Em sẽ trình bày cách ứng dụng dùng kết quả làm bài để hỗ trợ người học chọn nội dung luyện tiếp và ôn lại câu đã quên.

## 4. Kịch bản demo có thể tập trước

### Chuẩn bị trước buổi báo cáo

1. Cài và mở đúng bản ứng dụng sẽ trình bày; đăng nhập một tài khoản local cố định.
2. Chuẩn bị sẵn một bộ đề đã lưu để vẫn demo được nếu mạng/API lỗi. Tạo đề thử trước buổi báo cáo để kiểm tra khóa và dịch vụ Gemini, không đưa khóa API lên màn chiếu.
3. Chuẩn bị một ảnh rõ có 3–5 câu, ít công thức/bố cục phức tạp. Prompt mẫu: **“Tạo 5 câu trắc nghiệm Android cơ bản về Activity và vòng đời, mỗi câu 4 lựa chọn, bằng tiếng Việt.”**
4. Vào **Học & ôn tập → Luyện điểm yếu → Thêm bộ đề Android mẫu (18 câu)**. Bộ mẫu có ba kỹ năng và các mức khó; thuận tiện demo offline.
5. Làm sai sẵn ít nhất một câu để sổ câu sai có dữ liệu; ghi một ghi chú và đánh dấu một câu quan trọng.
6. Thêm widget vào launcher từ trước. Bật quyền camera và thông báo nếu định demo các phần đó.
7. Mở sẵn các file code ở mục 5, tăng cỡ chữ trong Android Studio và tập tìm hàm bằng tên.

### Thứ tự demo

1. **Trang chủ:** “Đây là ngân hàng đề và lịch sử làm bài được lưu trên thiết bị.”
2. **Tạo đề:** nhập prompt hoặc chọn ảnh; chỉ thử một lần trong phần trình bày chính. Nếu lỗi, nói “Tính năng này cần dịch vụ Gemini; em chuyển sang bộ đề đã lưu để trình bày phần luyện và ôn.”
3. **Quản lý câu:** mở một câu để cho thấy nội dung/đáp án có thể được kiểm tra và sửa, không mặc định mọi đáp án AI đều đúng.
4. **Luyện tập:** cố ý chọn sai một câu, cho thấy phản hồi. Có mạng thì bấm AI giải thích; nếu không, tiếp tục phần offline.
5. **Luyện thích ứng:** chọn một kỹ năng trong bộ Android mẫu, mục tiêu 5 câu. Làm sai hai câu liên tiếp cùng kỹ năng để quan sát lý do giảm độ khó ở câu sau nếu còn câu phù hợp. Kết quả còn phụ thuộc lịch sử và ngân hàng câu, nên tập trước với tài khoản demo.
6. **Sổ câu sai:** mở câu từng sai, sửa ghi chú hoặc đánh dấu quan trọng. Chọn ôn câu, lật thẻ và bấm “Chưa nhớ”. Giải thích thẻ được hẹn sau 10 phút; không cần chờ tại chỗ.
7. **Widget:** quay về launcher, bấm “Ôn ngay” để vào **Ôn lặp lại ngắt quãng**. Câu vừa hẹn sau 10 phút chưa được tính đến hạn ngay; có thể dùng danh sách “Tất cả” để giới thiệu thẻ đó.
8. **Code:** mở `AdaptiveQuestionSelector.choose()` hoặc `ReviewScheduler.rate()`, giải thích đầu vào, quy tắc và đầu ra trong 30–45 giây.

Không đổi đồng hồ máy để tạo dữ liệu demo vì sẽ ảnh hưởng các mốc thời gian. Không gỡ app/xóa dữ liệu sát giờ báo cáo vì dữ liệu hiện chủ yếu lưu cục bộ.

## 5. Bảng tra nhanh: “Chức năng này code ở đâu?”

Mỗi dòng có điểm mở giao diện và điểm mở logic. Trong Android Studio, dùng **Navigate → File** để mở file, **Navigate → Symbol** để tìm hàm; có thể dùng **Find in Files** và tìm nguyên tên hàm nếu phím tắt trên máy đã thay đổi.

| Chức năng được hỏi | Giao diện/điểm vào | Hàm hoặc nơi xử lý chính |
|---|---|---|
| Điều hướng giữa các màn | [MainActivity.kt](D:/download/android/app/src/main/java/com/example/MainActivity.kt:64) | `when (currentScreen)`; tên màn lưu bằng `rememberSaveable` |
| Đăng nhập/đăng ký | [AuthScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/AuthScreen.kt:44) | [UserManager.kt](D:/download/android/app/src/main/java/com/example/data/local/UserManager.kt:33): `register()`, `login()` |
| Camera/chọn ảnh | [CameraScanScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/CameraScanScreen.kt:48) | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:255): `generateQuizFromImage()` |
| Xử lý ảnh và gọi AI tạo đề | Màn quét ảnh | [AIRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt:88): `generateQuizFromImage()` |
| Tạo đề từ yêu cầu văn bản | [Components.kt](D:/download/android/app/src/main/java/com/example/ui/screens/Components.kt:240): `AIChatGenerationDialog()` | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:305) → [AIRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt:165): `generateQuizFromText()` |
| Định dạng phản hồi AI | [AIRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt:33) | `quizSchema`, `responseMimeType`, `cleanJsonResponse()`; DTO trong [GeminiQuizResponse.kt](D:/download/android/app/src/main/java/com/example/data/remote/GeminiQuizResponse.kt) |
| Model và endpoint Gemini | [GeminiApiService.kt](D:/download/android/app/src/main/java/com/example/data/remote/GeminiApiService.kt:8) | Chuỗi endpoint trong `@POST`; code hiện cấu hình `gemini-3.1-flash-lite` |
| Retry khi lỗi mạng/API | [AIRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt:62) | `retryIO()`: tối đa 3 lần gọi, chờ 1 giây rồi 2 giây giữa các lần |
| Lưu bộ đề và câu hỏi | [QuizRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/QuizRepository.kt:47) | [Dao.kt](D:/download/android/app/src/main/java/com/example/data/local/Dao.kt:39): `insertQuizWithQuestions()` |
| Bắt đầu thi/luyện | [QuizPlayScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/QuizPlayScreen.kt:27) | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:421): `startQuizSession()` |
| Đếm giờ và tự nộp | Màn làm bài | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:447): `startTimer()`, `onFinish()` gọi `submitQuiz()` |
| Chọn đáp án và chấm điểm | Màn làm bài | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:467): `selectAnswer()`; [submitQuiz()](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:506) |
| Lưu bài làm cùng lịch ôn | Sau khi nộp bài | [ReviewRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/ReviewRepository.kt:67): `saveQuizSession()` trong transaction |
| Xem bài cũ, hỏi AI giải thích | [QuizReviewScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/QuizReviewScreen.kt:32) | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:560): `getAIExplanation()` → [AIRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/AIRepository.kt:224): `getExplanationForAnswer()` |
| Bản đồ kỹ năng, luyện điểm yếu | [AdaptiveLearningScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/AdaptiveLearningScreen.kt:41) | [AdaptiveEngine.kt](D:/download/android/app/src/main/java/com/example/learning/AdaptiveEngine.kt:30): `MasteryCalculator.calculate()` và [choose()](D:/download/android/app/src/main/java/com/example/learning/AdaptiveEngine.kt:55) |
| Gắn kỹ năng và độ khó cho câu | Màn bản đồ → Phân loại | [LearningRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/LearningRepository.kt:19): `classify()` |
| Lưu và tiếp tục phiên thích ứng | [AdaptivePracticeViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/AdaptivePracticeViewModel.kt:20) | [LearningRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/LearningRepository.kt:35): `start()`, `answer()`, `next()`; entity trong [LearningEntities.kt](D:/download/android/app/src/main/java/com/example/data/local/LearningEntities.kt:14) |
| Lối vào Học & ôn tập | [StudyHubScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/StudyHubScreen.kt:27) | Điều hướng trong `MainActivity` đến `adaptive` hoặc `spaced_review` |
| Sổ câu sai, bộ lọc, ghi chú | [SpacedReviewScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/SpacedReviewScreen.kt:38) | [ReviewViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/ReviewViewModel.kt:22); [ReviewRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/ReviewRepository.kt:56): `annotate()` |
| Tính thời điểm ôn tiếp | Thẻ ôn → bốn nút đánh giá | [ReviewScheduler.kt](D:/download/android/app/src/main/java/com/example/learning/ReviewScheduler.kt:13): `rate()` |
| Flashcard | [FlashcardScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/FlashcardScreen.kt:48) | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:60): `rateFlashcard()` → `ReviewRepository.rate()` |
| Chống ghi lịch ôn trùng | Repository | [ReviewRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/ReviewRepository.kt:33): `record()`; [ReviewEntities.kt](D:/download/android/app/src/main/java/com/example/data/local/ReviewEntities.kt:25): khóa `(userKey, eventKey)` |
| Streak | [StreakDialogs.kt](D:/download/android/app/src/main/java/com/example/ui/screens/StreakDialogs.kt:75) | [UserManager.kt](D:/download/android/app/src/main/java/com/example/data/local/UserManager.kt:187): `updateStreak()` |
| Cấu hình và gửi nhắc học | [ReminderDialog.kt](D:/download/android/app/src/main/java/com/example/ui/screens/ReminderDialog.kt:39) | [StudyReminderHelper.kt](D:/download/android/app/src/main/java/com/example/reminder/StudyReminderHelper.kt:15): `scheduleReminder()` → [StudyReminderReceiver.kt](D:/download/android/app/src/main/java/com/example/reminder/StudyReminderReceiver.kt:25): `onReceive()` |
| Thông báo khi câu đến lịch ôn | [ReviewReminderSettings.kt](D:/download/android/app/src/main/java/com/example/ui/screens/ReviewReminderSettings.kt): bật trong Học & ôn tập | [ReviewDueReminder.kt](D:/download/android/app/src/main/java/com/example/reminder/ReviewDueReminder.kt): theo dõi lịch, đặt alarm, gộp thông báo, chống nhắc trùng và mở màn ôn; [ReviewReminderPlan.kt](D:/download/android/app/src/main/java/com/example/learning/ReviewReminderPlan.kt): tính mốc tiếp theo |
| Widget tím và widget tập trung | [StudyProgressWidget.kt](D:/download/android/app/src/main/java/com/example/widget/StudyProgressWidget.kt:38), [FocusStudyWidget.kt](D:/download/android/app/src/main/java/com/example/widget/FocusStudyWidget.kt:19) | [StudyWidgetUpdates.kt](D:/download/android/app/src/main/java/com/example/widget/StudyWidgetUpdates.kt:21): `start()` theo dõi Room/preferences |
| Widget bấm Ôn ngay | Widget tạo Intent | [MainActivity.kt](D:/download/android/app/src/main/java/com/example/MainActivity.kt:43): `onNewIntent()` và xử lý `pendingWidgetReview` trong `onCreate()` |
| Thêm/sửa/xóa câu, xuất PDF | [ManageQuestionsScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/ManageQuestionsScreen.kt:44) | [QuizRepository.kt](D:/download/android/app/src/main/java/com/example/data/repository/QuizRepository.kt:55): `updateQuestion()`; [exportQuizToPdf()](D:/download/android/app/src/main/java/com/example/ui/screens/ManageQuestionsScreen.kt:385) |
| Thư mục bộ đề | [HomeScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/HomeScreen.kt:103) | [QuizViewModel.kt](D:/download/android/app/src/main/java/com/example/ui/QuizViewModel.kt:207): `createFolder()`, `updateFolder()`, `deleteFolder()`, `moveQuizToFolder()` |
| Thống kê hiệu suất | [HomeScreen.kt](D:/download/android/app/src/main/java/com/example/ui/screens/HomeScreen.kt:934) | `AnalyticsDashboard()`: điểm trung bình, số lượt thi, tỉ lệ điểm ≥50%, biểu đồ tối đa 10 lượt gần nhất |
| Schema và migration | [AppDatabase.kt](D:/download/android/app/src/main/java/com/example/data/local/AppDatabase.kt:11) | Room version 4; `MIGRATION_2_3`, `MIGRATION_3_4`, `addMigrations()` |
| Khởi tạo và cấp dependency | [QuizApplication.kt](D:/download/android/app/src/main/java/com/example/QuizApplication.kt:5) | [DependencyContainer.kt](D:/download/android/app/src/main/java/com/example/DependencyContainer.kt:21): `AppContainerImpl`, các dependency khởi tạo `by lazy` |

## 6. Ba luồng code nên thuộc

### A. Tạo đề từ ảnh

`CameraScanScreen` → `QuizViewModel.generateQuizFromImage()` → `DirectGeminiAIRepositoryImpl.generateQuizFromImage()` → `GeminiApiService.generateContent()` → JSON → `GeneratedQuizJson` → `QuizRepository.insertQuizWithQuestions()` → DAO → Room → Flow cập nhật UI.

Giải thích miệng:

> Giao diện nhận ảnh và chuyển cho ViewModel. ViewModel bật trạng thái đang tạo rồi gọi repository AI. Repository nén ảnh JPEG, mã hóa Base64 và gửi cùng prompt, schema đến Gemini. Khi nhận JSON, repository giải mã về đối tượng Kotlin. ViewModel chuyển thành entity và lưu bộ đề cùng câu hỏi. Giao diện quan sát Flow nên danh sách được cập nhật khi dữ liệu thay đổi.

Hai điểm cần chỉ trực tiếp: xử lý ảnh/gọi mạng ở `AIRepository`, còn lưu dữ liệu được điều phối ở `QuizViewModel`. Schema hỗ trợ cấu trúc đầu ra; nó không chứng minh nội dung hay đáp án AI đúng.

### B. Luyện thích ứng

`AdaptiveLearningScreen` → `AdaptivePracticeViewModel.answer()` → `LearningRepository.answer()` → ghi lượt trả lời + lịch ôn trong transaction → `next()` → `AdaptiveQuestionSelector.choose()` → cập nhật phiên trong Room → UI nhận state mới.

Quy tắc cần nhớ:

- Mỗi câu được gắn một kỹ năng chính và độ khó 1–3. Việc gắn nhãn hiện do người dùng thực hiện; bộ đề mẫu đã có nhãn.
- Tính thành thạo từ tối đa 20 lượt hợp lệ gần nhất của mỗi kỹ năng trong tập dữ liệu đầu vào. Lượt cũ nhất trong nhóm có trọng số 1, tiếp theo 2, v.v.
- `score = round(100 × tổng trọng số các lượt đúng / tổng trọng số mọi lượt hợp lệ)`.
- Ví dụ ba lượt **đúng, sai, đúng**: `(1 + 3) / (1 + 2 + 3) × 100 ≈ 67`. Đây chưa đủ bằng chứng để kết luận đã vững.
- “Đã vững” cần ít nhất 5 câu khác nhau trong cửa sổ đó, điểm ≥80 và ít nhất một lượt đúng câu mức vừa/khó. “Cần ưu tiên” có ít nhất 3 lượt và điểm <60.
- Chu kỳ chọn theo số lượt trong phiên: 3 vị trí ưu tiên kỹ năng yếu → 1 khám phá kỹ năng chưa học/ít dữ liệu → 1 kiểm tra kỹ năng mạnh. Không có nhóm phù hợp thì dùng kỹ năng hiện có.
- Hai lượt gần nhất cùng kỹ năng đều đúng: tăng một mức so với độ khó lượt cuối. Hai lượt đều sai: giảm một mức. Giới hạn 1–3; thiếu mức phù hợp thì lấy mức gần nhất.
- Trong cùng khoảng cách đến độ khó mục tiêu: ưu tiên câu từng sai dù rất chắc, sai khác, đúng nhưng thiếu tự tin, chưa làm, rồi đã đúng. Không lặp câu đã dùng trong cùng phiên.
- Bỏ qua không tính thành lượt sai hay bằng chứng thành thạo. Các lượt gắn nội dung cũ hoặc câu đã xóa bị loại khỏi tính thành thạo hiện tại.
- Mức tự tin ảnh hưởng ưu tiên; thời gian đọc không bị dùng để trừ điểm. Đây là thuật toán quy tắc, chưa phải mô hình học máy được huấn luyện.

### C. Ôn ngắt quãng và chống ghi trùng

`SpacedReviewScreen`/`FlashcardScreen` → đánh giá → ViewModel → `ReviewRepository.rate()` → kiểm tra câu và phiên bản → ghi `review_events` → `ReviewScheduler.rate()` → lưu `review_progress` → UI cập nhật.

| Đánh giá | Xử lý bước ôn | Thời điểm tiếp theo |
|---|---|---|
| Chưa nhớ | Về bước 0 | Sau 10 phút |
| Khó | Giữ bước, tối thiểu bước 1 | Sau 1 ngày |
| Nhớ | Tăng 1 bước | Theo dãy 1, 3, 7, 14, 30 ngày |
| Dễ | Tăng 2 bước | Theo dãy trên, tối đa bước 5 |

Đúng trắc nghiệm tương ứng “Nhớ”; sai tương ứng “Chưa nhớ”. Tự đánh giá flashcard không tăng số lần sai trắc nghiệm và không tăng thành thạo thích ứng. Câu chưa trả lời không tạo lịch ôn, dù vẫn nằm trong mẫu số tổng số câu khi tính điểm bài thi.

Khóa `(userKey, eventKey)` giúp một sự kiện không cập nhật lịch nhiều lần khi bấm lại. Trong luyện tập thường, đáp án đầu tiên đã được ghi; khi nộp bài dùng lại khóa đó nên lịch không bị cập nhật lần hai. Không nói rằng mọi loại bản ghi trong app đều có cùng mức chống trùng.

## 7. Kiến trúc và dữ liệu: câu trả lời khoảng một phút

> Dự án dùng Kotlin và Jetpack Compose, tổ chức theo hướng MVVM với lớp repository. Compose nhận thao tác và hiển thị state. ViewModel điều phối nghiệp vụ, dùng coroutine để thực hiện tác vụ bất đồng bộ và StateFlow để cung cấp trạng thái. Repository truy cập Room cho dữ liệu cục bộ hoặc Retrofit cho Gemini. AppContainer cấp dependency thủ công. Ngoài luồng chính còn có receiver cho nhắc học và Glance cho widget.

| Nhóm | Dữ liệu |
|---|---|
| Ngân hàng đề | `folders`, `quizzes`, `questions` |
| Lịch sử thi thường | `quiz_sessions` |
| Luyện thích ứng | `skills`, `learning_sessions`, `question_attempts` |
| Ôn lại | `review_progress`, `review_events` |
| Cấu hình ngoài Room | SharedPreferences lưu tài khoản local, phiên đăng nhập, streak và thiết lập nhắc |

Room hiện có **9 bảng**. Câu hỏi liên kết bộ đề; xóa bộ đề kéo theo câu hỏi và lịch sử thi qua CASCADE. Xóa thư mục dùng SET_NULL cho bộ đề, nên bộ đề trở về không thuộc thư mục. `List<String>` lựa chọn và `Map<Long, Int>` đáp án được chuyển đổi JSON bằng Moshi trong `RoomConverters`.

Ngân hàng đề và lịch sử thi thường hiện dùng chung trên thiết bị; tiến độ thích ứng và ôn lại có `userKey`. Không trình bày như toàn bộ dữ liệu đều đã được tách theo tài khoản.

## 8. Câu hỏi phản biện và câu trả lời mẫu

**1. Điểm khác biệt nổi bật của sản phẩm là gì?**

> Điểm em nhấn mạnh là một luồng liên tục: tạo đề bằng AI, làm bài, xem lỗi, luyện điểm yếu và ôn lại. Kết quả luyện/thi có thể cập nhật lịch ôn để người học tiếp tục xử lý câu đã sai. Đây là giá trị của bản triển khai; em chưa có khảo sát để khẳng định sản phẩm độc nhất trên thị trường.

**2. AI nằm ở đâu? Có tự huấn luyện mô hình không?**

> App gọi Gemini cho tạo đề từ ảnh/văn bản và giải thích đáp án. Nhóm chưa tự huấn luyện mô hình. Chọn câu thích ứng và tính lịch ôn là quy tắc Kotlin ở `AdaptiveEngine.kt` và `ReviewScheduler.kt`.

**3. Có dùng OCR riêng để đọc ảnh không?**

> Luồng hiện tại gửi ảnh trực tiếp cho Gemini cùng prompt trích xuất câu hỏi; không có bước OCR bằng ML Kit hoặc Tesseract trong luồng này. CameraX đảm nhiệm chụp và preview ảnh.

**4. Tại sao dùng JSON schema? Có bảo đảm AI đúng tuyệt đối không?**

> Schema và MIME application/json giúp phản hồi có cấu trúc phù hợp với DTO. Code có làm sạch code fence và bắt lỗi giải mã. Tuy nhiên code chưa kiểm tra đầy đủ mọi ràng buộc nghiệp vụ trước khi lưu, và schema không kiểm chứng kiến thức. Người dùng vẫn cần rà soát câu hỏi, lựa chọn và đáp án.

**5. Khi mất mạng thì phần nào còn hoạt động?**

> Đề đã lưu, làm bài, flashcard, thích ứng, lịch ôn, sổ câu sai và widget đọc dữ liệu cục bộ vẫn dùng được. Tạo đề bằng Gemini và yêu cầu giải thích mới cần mạng cùng API khả dụng.

**6. Khi API lỗi 429/503 xử lý thế nào?**

> `retryIO()` thử tối đa ba lần gọi. Với lỗi mạng hoặc một số lỗi server, app đợi 1 giây rồi 2 giây trước các lần sau; các lỗi không thuộc nhóm retry được trả ra ngay. ViewModel đưa thông báo lỗi lên giao diện. Không có cơ chế tự đổi sang model khác trong luồng hiện tại.

**7. Chấm điểm có cần gọi AI không?**

> Không. `submitQuiz()` so lựa chọn với `correctOptionIndex` lưu trong câu hỏi, rồi tính số đúng chia tổng số câu nhân 100. Chất lượng đáp án chuẩn trong ngân hàng đề vì vậy rất quan trọng.

**8. Làm sao biết người học yếu phần nào?**

> Thành thạo dùng tối đa 20 lượt luyện thích ứng hợp lệ gần nhất của kỹ năng, với trọng số tăng dần cho lượt mới. Chỉ có điểm cao chưa đủ để báo đã vững; còn cần độ phủ ít nhất 5 câu khác nhau và có câu vừa/khó làm đúng. Đây là chỉ báo theo quy tắc, chưa được xác thực như một thang đo năng lực.

**9. Người học cố trả lời thật nhanh hoặc lặp một câu thì sao?**

> Thời gian không làm tăng điểm thành thạo. Một câu lặp không đáp ứng ngưỡng 5 câu khác nhau để báo đã vững. Bộ chọn cũng tránh lặp câu trong cùng phiên. Tuy nhiên đây chưa phải hệ thống chống gian lận cho thi chính thức.

**10. Ôn ngắt quãng có phải SM-2 hoặc FSRS không?**

> Bản hiện tại dùng lịch quy tắc đơn giản với các bước 1, 3, 7, 14, 30 ngày và bốn mức tự đánh giá. Chưa triển khai SM-2/FSRS hay thuật toán tối ưu cá nhân hóa thời điểm quên.

**11. Đổi tài khoản có lẫn tiến độ không?**

> Tiến độ thích ứng và ôn được truy vấn theo tài khoản chuẩn hóa. Nhưng ngân hàng đề và lịch sử thi thường vẫn chung trên thiết bị; phần tài khoản/streak cũ cũng có cách dùng khóa riêng cần được thống nhất nếu phát triển tiếp. Em không khẳng định toàn bộ dữ liệu đã cô lập hoàn toàn theo người dùng.

**12. App tắt giữa chừng có làm tiếp được không?**

> Phiên luyện thích ứng được lưu trong Room nên có thể tiếp tục từ trạng thái đã lưu. Trạng thái bài thi thường đang làm nằm trong ViewModel; không có cùng bảo đảm phục hồi sau khi process bị hủy. Em phân biệt rõ hai loại phiên này.

**13. Nếu sửa câu hỏi sau khi đã học thì sao?**

> Sửa nội dung hoặc phân loại tăng `contentVersion`. Thành thạo không dùng lượt của phiên bản cũ; lịch ôn được chuẩn hóa cho phiên bản mới, giữ ghi chú/đánh dấu, và thẻ từng có lịch được đưa về đến hạn. Tác vụ mang phiên bản cũ bị từ chối.

**14. Có bằng chứng chống bấm trùng không?**

> Lượt thích ứng có unique index theo phiên và câu. Lịch ôn có khóa sự kiện theo tài khoản và event key. Repository dùng transaction và kiểm tra phiên bản. Có test riêng cho cập nhật trùng, bỏ qua, đổi phiên bản và tách tài khoản.

**15. Streak có đòi hoàn thành bài mỗi ngày không? Có đóng băng chuỗi không?**

> Hiện mở ứng dụng khi đã đăng nhập hoặc đăng nhập cũng gọi cập nhật streak, không chỉ lúc hoàn thành bài. Code có lưu số lượt freeze nhưng `checkAndApplyStreakFreezes()` hiện reset khi đứt chuỗi, không tự tiêu lượt để bảo vệ chuỗi. Em trình bày chức năng đang chạy và xem đóng băng thực sự là phần cần hoàn thiện.

**16. Nhắc học có hoạt động khi máy tắt nguồn không? Có đúng giờ tuyệt đối không?**

> Code đặt alarm và gửi notification qua receiver, đồng thời đăng ký khôi phục lịch sau BOOT_COMPLETED. Khi máy tắt nguồn app không thể phát thông báo. Code dùng `setAndAllowWhileIdle`, không dùng exact alarm; thời điểm hiển thị còn chịu điều phối của Android và quyền thông báo, nên em không cam kết chính xác tuyệt đối.

**17. Đăng nhập hiện có an toàn để phát hành chưa?**

> Đây là tài khoản local phục vụ bản mẫu. `UserManager.register()` đang lưu mật khẩu dạng chuỗi trong SharedPreferences, chưa có backend xác thực, băm mật khẩu hay đồng bộ tài khoản. Để phát hành cần thiết kế lại cơ chế xác thực và lưu trữ thông tin nhạy cảm.

**18. Khóa Gemini được bảo vệ thế nào?**

> Cấu hình nguồn nằm trong file env qua Secrets Gradle Plugin và được truy cập bằng BuildConfig. Việc này tách khóa khỏi mã nguồn, nhưng không ngăn trích xuất khóa đã đóng gói trong APK. Hướng phát triển là chuyển việc gọi API qua backend kiểm soát truy cập/hạn mức; cũng cần giảm log HTTP chứa dữ liệu khi phát hành.

**19. Giải thích AI có được lưu vĩnh viễn hay hiển thị Markdown không?**

> `QuizViewModel` cache giải thích trong state theo questionId trong bộ nhớ và xóa khi bắt đầu phiên mới. Màn xem lại hiện dùng Compose Text để hiển thị chuỗi; dù prompt cho phép Markdown, code này chưa dùng bộ render Markdown. Không nên giới thiệu là đã có rich Markdown hay cache bền vững.

**20. Đã kiểm thử gì và còn hạn chế gì?**

> Repo có unit test cho thuật toán thích ứng, lịch ôn, repository, migration và điều hướng widget; có test giao diện và instrumentation cho widget. Tài liệu cũ ghi nhận một số lượt build/test đã qua. Tài liệu chuẩn bị này được lập từ việc đọc code và đối chiếu test, chưa chạy lại build, Gemini hay demo thiết bị trong lượt chuẩn bị, nên em chỉ công bố kết quả chạy lại khi có log tương ứng.

## 9. Mở code thế nào để câu trả lời có sức thuyết phục?

Khi được hỏi “chức năng này ở đâu”, trả lời theo thứ tự **file → hàm → đầu vào → quy tắc → đầu ra → nơi lưu**.

Ví dụ chọn câu thích ứng:

> Phần này nằm trong `learning/AdaptiveEngine.kt`, hàm `AdaptiveQuestionSelector.choose()`. Đầu vào là danh sách câu hỏi, kỹ năng, lịch sử học và lượt trong phiên. Hàm loại câu đã dùng, chọn kỹ năng theo chu kỳ, xác định độ khó từ hai lượt gần nhất rồi ưu tiên câu trong nhóm đó. Đầu ra là `QuestionChoice` gồm câu hỏi và lý do chọn. `LearningRepository.next()` lưu câu được chọn vào phiên trong Room, giao diện quan sát state để hiển thị.

Ví dụ chấm điểm:

> Phần này ở `ui/QuizViewModel.kt`, hàm `submitQuiz()`. Hàm đọc `selectedAnswers`, so từng câu với `correctOptionIndex`, tính phần trăm và tạo `QuizSessionEntity`. `ReviewRepository.saveQuizSession()` lưu bài làm cùng cập nhật lịch ôn trong transaction. Chấm điểm hoàn toàn trên thiết bị.

Ví dụ lịch ôn:

> Quy tắc nằm ở `ReviewScheduler.rate()`. Hàm nhận tiến độ hiện tại, mức đánh giá và thời gian hiện tại, rồi trả về tiến độ mới có bước và `nextReviewAt`. Repository lo kiểm tra câu, chống ghi trùng và lưu xuống Room; hàm scheduler chỉ tính toán nên dễ unit test.

## 10. Bằng chứng kiểm thử có thể mở khi được hỏi

| Muốn chứng minh | File test hiện có |
|---|---|
| Không kết luận thành thạo chỉ từ một câu; trọng số; độ khó; ưu tiên lỗi tự tin cao | [AdaptiveEngineTest.kt](D:/download/android/app/src/test/java/com/example/learning/AdaptiveEngineTest.kt) |
| Mốc ôn, giới hạn 30 ngày, quên và thay đổi phiên bản | [ReviewSchedulerTest.kt](D:/download/android/app/src/test/java/com/example/learning/ReviewSchedulerTest.kt) |
| Tách tiến độ tài khoản, chống sự kiện trùng, không tính câu bỏ trống thành lỗi, dữ liệu sau mở lại DB | [ReviewRepositoryTest.kt](D:/download/android/app/src/test/java/com/example/learning/ReviewRepositoryTest.kt) |
| Lưu phiên, ghi lượt và quản lý ngân hàng luyện | [LearningRepositoryTest.kt](D:/download/android/app/src/test/java/com/example/learning/LearningRepositoryTest.kt) |
| Nâng schema giữ dữ liệu | [LearningMigrationTest.kt](D:/download/android/app/src/test/java/com/example/learning/LearningMigrationTest.kt), [ReviewMigrationTest.kt](D:/download/android/app/src/test/java/com/example/learning/ReviewMigrationTest.kt) |
| Widget dùng dữ liệu đến hạn, tài khoản, điều hướng | [StudyWidgetSummaryTest.kt](D:/download/android/app/src/test/java/com/example/learning/StudyWidgetSummaryTest.kt), [StudyWidgetNavigationTest.kt](D:/download/android/app/src/test/java/com/example/learning/StudyWidgetNavigationTest.kt) |
| Widget thật: bố cục, trạng thái và ảnh preview | [StudyWidgetLayoutTest.kt](D:/download/android/app/src/androidTest/java/com/example/learning/StudyWidgetLayoutTest.kt), [StudyWidgetLiveStateTest.kt](D:/download/android/app/src/androidTest/java/com/example/learning/StudyWidgetLiveStateTest.kt), [StudyWidgetPickerPreviewTest.kt](D:/download/android/app/src/androidTest/java/com/example/learning/StudyWidgetPickerPreviewTest.kt) |

Nếu muốn kiểm tra lại trước buổi báo cáo, chạy tại thư mục dự án với JDK 21 được mô tả trong tài liệu triển khai:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat :app:testDebugUnitTest --tests 'com.example.learning.*' :app:assembleDebug --offline
```

`--offline` yêu cầu dependency đã có trong cache. Chỉ báo số test thực sự có trong kết quả lần chạy; các test trên không chứng minh Gemini luôn khả dụng hoặc đáp án luôn đúng. Instrumentation cần emulator/thiết bị và chạy riêng.

## 11. Những chỗ trong tài liệu cũ cần nói lại cho đúng code

| Nội dung dễ nhầm | Cách nói đúng với bản hiện tại |
|---|---|
| Chỉ có 3 bảng | Room version 4 hiện có 9 bảng |
| Các màn chính đều nằm trong Screens.kt | Hiện tách thành HomeScreen, QuizPlayScreen, QuizReviewScreen, v.v. |
| AI tự phân loại kỹ năng | Hiện phân loại thủ công; bộ mẫu được gắn nhãn sẵn |
| Mọi chế độ đều cập nhật thành thạo | Thành thạo hiện dùng lượt luyện thích ứng; thi/luyện thường cập nhật lịch ôn |
| JSON schema bảo đảm không lỗi và đáp án đúng | Schema hỗ trợ cấu trúc; vẫn có lỗi và vẫn cần kiểm chứng nội dung |
| Retry ba lần sau lần đầu, chờ 1–2–4 giây | Mặc định tổng cộng ba lần gọi, hai khoảng chờ 1–2 giây |
| Có freeze là chuỗi sẽ được bảo vệ | Code hiện reset chuỗi, không sử dụng freeze để bù ngày |
| Phải hoàn thành bài mới tăng streak | Mở app/đăng nhập cũng có thể tăng streak |
| Notification vẫn xuất hiện khi tắt nguồn, luôn đúng giờ | Khôi phục lịch sau khởi động; không có cam kết khi tắt nguồn hoặc đúng giờ tuyệt đối |
| Giải thích đã render Markdown | Màn review hiện hiển thị bằng Text |
| Đưa API key vào env là bảo mật hoàn toàn | Khóa vẫn được đưa vào BuildConfig/APK; cần backend để bảo vệ tốt hơn |
| Toàn bộ dữ liệu theo tài khoản | Ngân hàng đề và lịch sử thi thường vẫn dùng chung trên thiết bị |
| Mọi bài đang làm đều khôi phục sau process death | Phiên thích ứng được lưu; phiên thi thường chưa có cùng cơ chế |

Chi tiết giao diện cần lưu ý: màn review có chuỗi “Làm bài trong 15 phút” viết cố định. Không dùng dòng đó để khẳng định app đã đo và hiển thị đúng thời lượng thực tế của mọi bài.

## 12. Kết bài và việc nên học thuộc

Lời kết khoảng 25 giây:

> Bản hiện tại đã kết nối việc tạo đề bằng AI với làm bài, luyện theo kỹ năng và ôn lại lỗi sai trên thiết bị. Phần em ưu tiên phát triển tiếp là kiểm tra chất lượng câu hỏi AI, hoàn thiện xác thực và bảo vệ khóa API, rồi đánh giá hiệu quả lịch luyện/ôn trên người dùng thực tế. Em sẵn sàng trình bày trực tiếp luồng xử lý trong mã nguồn.

Trước buổi báo cáo, tự trả lời thành tiếng sáu câu: sản phẩm giúp ai; giá trị chính là gì; phần nào dùng AI; thuật toán chọn câu ở đâu; dữ liệu được lưu thế nào; giới hạn hiện tại là gì. Sau đó demo trọn luồng một lần và mở đúng ba hàm `submitQuiz()`, `AdaptiveQuestionSelector.choose()`, `ReviewScheduler.rate()` mà không cần tìm tài liệu.
