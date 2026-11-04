# Ôn ngắt quãng và sổ câu sai

## Chạy và demo offline

1. Đăng nhập một tài khoản local. Có thể bật chế độ máy bay.
2. Nếu chưa có đề, vào **Học & ôn tập → Luyện điểm yếu → Thêm bộ đề Android mẫu (18 câu)** hoặc tạo đề thủ công.
3. Làm sai một câu trong luyện tập/luyện thích ứng, hoặc nộp một bài thi có câu sai.
4. Về trang chủ → **Học & ôn tập**. **Ôn lặp lại ngắt quãng** mở danh sách đến hạn; **Sổ câu sai** mở trực tiếp danh sách câu từng sai. Màn chung hiển thị số thẻ đến hạn và số câu từng sai của tài khoản hiện tại.
5. Trong **Sổ câu sai**, lọc theo môn, kỹ năng, bộ đề; tìm theo nội dung/ghi chú.
6. Chạm câu để xem đáp án, ghi cách hiểu và đánh dấu quan trọng. **Quan trọng** hiển thị những câu đã đánh dấu.
7. Bấm **Ôn câu này** hoặc **Ôn danh sách này**. Bấm **Hiện đáp án**, rồi chọn **Chưa nhớ / Khó / Nhớ / Dễ**.
8. Kết quả được lưu trước khi chuyển thẻ. Đóng/mở app vẫn giữ lịch, ghi chú và danh sách câu sai.
9. **Tất cả** cho phép bắt đầu ôn câu chưa có lịch. **Đến hạn** hiển thị các câu có thời điểm ôn <= thời gian hiện tại.
10. Flashcard cũng có bốn mức đánh giá. Có thể đánh giá hoặc vuốt ngay ở mặt câu hỏi, không bắt buộc xem đáp án; trái = Chưa nhớ, phải = Nhớ. Chạm để lật thẻ xem đáp án khi cần.

Quay lại từ danh sách ôn hoặc bản đồ kiến thức sẽ về **Học & ôn tập**. Bản đồ mở từ quản lý bộ đề vẫn quay về bộ đề đó. Widget tiếp tục mở trực tiếp **Ôn lặp lại ngắt quãng**.

## Quy tắc lịch ôn

- Chưa nhớ: ôn lại sau 10 phút, đặt bước về 0.
- Khó: ôn lại sau 1 ngày, giữ bước hiện có (tối thiểu bước 1).
- Nhớ: tăng 1 bước; Dễ: tăng 2 bước.
- Các bước 1–5 tương ứng 1, 3, 7, 14, 30 ngày; tối đa 30 ngày.
- Đúng trắc nghiệm tương đương Nhớ; sai trắc nghiệm tương đương Chưa nhớ.
- Bỏ qua/không trả lời không được tính thành câu sai và không tạo lịch.
- Đánh giá flashcard là tự báo cáo trí nhớ, không làm tăng số lần sai trắc nghiệm hoặc điểm thành thạo thích ứng.
- Sổ câu sai giữ câu từng sai ở phiên bản hiện tại, kể cả sau khi làm đúng. Bộ lọc Đến hạn quyết định câu nào cần ôn ngay.
- Đây là quy tắc đơn giản phục vụ tự học và demo, không phải thuật toán SM-2 hay đánh giá năng lực đã được kiểm chứng.

## Dữ liệu và tích hợp

- Room version 4 thêm `review_progress` (khóa tài khoản + câu hỏi) và `review_events` (khóa tài khoản + sự kiện).
- Migration 2→3→4 giữ dữ liệu hiện có. Tiến độ cũ từ lượt luyện thích ứng hợp lệ được đưa vào lịch ôn đến hạn; không suy đoán chủ sở hữu của lịch sử thi thường dùng chung.
- Mọi tài khoản được chuẩn hóa trim/lowercase(Locale.ROOT); ngân hàng đề vẫn chung trên thiết bị.
- Luyện thích ứng lưu kết quả và lịch ôn trong cùng transaction.
- Luyện tập ghi nhận lựa chọn đầu tiên. Khi nộp bài, retry các lựa chọn với cùng khóa sự kiện để tránh đếm hai lần.
- Thi ghi nhận đáp án đã chọn khi nộp bài; lưu bài và lịch ôn trong cùng transaction.
- Khóa sự kiện chống đếm trùng khi bấm lặp hoặc khôi phục giao diện sau khi đã lưu nhưng chưa chuyển thẻ.
- Sửa nội dung hoặc phân loại làm thay đổi `contentVersion`: đặt lại bằng chứng sai và bước ôn, đưa thẻ từng có lịch về đến hạn, giữ ghi chú/đánh dấu. Thao tác từ phiên bản cũ không được áp dụng.
- Xóa câu/đề xóa tiến độ và khóa sự kiện liên quan qua foreign key CASCADE.
- Chỉ lưu ghi chú/đánh dấu chưa tạo lịch ôn. Lịch được tạo khi làm bài hoặc đánh giá thẻ.
- Thời gian đến hạn dùng đồng hồ thiết bị. Màn danh sách cập nhật mỗi 30 giây khi hoạt động và khi quay lại foreground.
- **Nhắc khi đến lịch ôn:** bật trong **Học & ôn tập**, cho phép thông báo nếu Android yêu cầu. Mỗi tài khoản có thiết lập riêng, mặc định tắt.
- App đặt một alarm cho mốc ôn gần nhất trong tương lai. Khi đến hạn, thông báo gộp số câu cần ôn; chạm thông báo mở trực tiếp **Ôn lặp lại ngắt quãng → Đến hạn**, kể cả khi cần đăng nhập trước.
- Mỗi câu chỉ nhắc một lần cho cùng phiên bản và mốc ôn; bỏ thông báo không làm gửi lại liên tục. Khi học lại, lịch mới có thể tạo lần nhắc tiếp theo.
- Lịch tự cập nhật sau thay đổi Room đã commit (làm bài, đánh giá thẻ, sửa/xóa câu). Đăng xuất hoặc tắt nhắc sẽ hủy alarm và thông báo của tính năng này.
- Khôi phục lịch khi khởi động lại máy, cập nhật ứng dụng hoặc thay đổi đồng hồ/múi giờ. Không gửi khi chưa được cấp quyền; quay lại app sau khi cấp quyền sẽ kiểm tra lại.
- Dùng `AlarmManager.setAndAllowWhileIdle` nên Android có thể trì hoãn thông báo khi tiết kiệm pin; không cam kết chính xác từng phút. Không cần mạng và không sửa bước ôn chỉ vì đã gửi thông báo.

## Kiểm chứng

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\gradlew.bat :app:testDebugUnitTest --tests 'com.example.learning.*' :app:assembleDebug --offline '-Proborazzi.test.record=true'
```

Test kiểm tra mốc ôn, giới hạn khoảng cách, mất nhớ, phiên bản nội dung, chống đếm trùng, tài khoản, bỏ qua, liên kết luyện thích ứng, nộp bài, dữ liệu sau khi mở lại database và migration 3→4/2→4. Test Compose đi qua sổ câu sai, ghi chú, đánh dấu, lật thẻ và lưu đánh giá.

Với cờ `-Proborazzi.test.record=true`, ảnh kiểm tra giao diện nằm trong `.build-outputs/review-due.png`, `.build-outputs/review-notebook.png` và `.build-outputs/review-card.png`.

Kết quả kiểm chứng bản triển khai: build debug thành công; 29 test trong `com.example.learning.*` đều qua, gồm 12 test mới cho ôn ngắt quãng. Đã xem ảnh màn đến hạn, sổ câu sai và thẻ ôn. APK nằm tại `.build-outputs/quizai-spaced-review-debug.apk`. Chưa chạy trực tiếp trên emulator hoặc điện thoại trong lượt triển khai này.

## Mã nguồn

- `data/local/ReviewEntities.kt`: schema và DAO.
- `learning/ReviewScheduler.kt`: quy tắc thuần Kotlin.
- `data/repository/ReviewRepository.kt`: transaction, phiên bản, sự kiện và ghi chú.
- `ui/ReviewViewModel.kt`: dữ liệu màn ôn, thời gian và xử lý thao tác.
- `ui/screens/SpacedReviewScreen.kt`: sổ câu sai, bộ lọc, ghi chú, thẻ ôn.
- `learning/ReviewReminderPlan.kt`: chọn mốc ôn và định danh chống nhắc trùng.
- `reminder/ReviewDueReminder.kt`: theo dõi dữ liệu, đặt alarm, notification và receiver khôi phục lịch.
- `ui/screens/ReviewReminderSettings.kt`: bật/tắt nhắc và cấp quyền thông báo.
