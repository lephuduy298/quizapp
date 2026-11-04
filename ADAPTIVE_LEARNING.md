# Luyện tập thích ứng và bản đồ kiến thức

## Chạy demo

1. Đăng nhập tài khoản local, vào **Học & ôn tập → Luyện điểm yếu** trên trang chủ. Màn bản đồ kiến thức cho phép xem tiến độ, chọn kỹ năng và cấu hình phiên luyện.
2. Bấm **Thêm bộ đề Android mẫu (18 câu)**. Thao tác có thể lặp lại mà không tạo bản sao cùng tên.
3. Chọn bộ đề Android mẫu hoặc để tất cả bộ đề. Có 3 kỹ năng, mỗi kỹ năng 6 câu ở 3 mức khó.
4. Bấm một kỹ năng → **Luyện kỹ năng này**, hoặc **Luyện điểm yếu** để app chọn kỹ năng.
5. Chọn đáp án và mức tự tin, bấm **Kiểm tra**, rồi **Câu tiếp theo**. Lý do chọn được hiển thị trước mỗi câu.
6. Sai hai câu cùng kỹ năng sẽ làm giảm mức khó mục tiêu; đúng hai câu sẽ làm tăng mức khó, trong giới hạn 1–3.
7. Quay về bản đồ để thấy điểm cập nhật. Phiên đang làm có nút tiếp tục; kết thúc phiên hiển thị điểm trước/sau.
8. Có thể bật chế độ máy bay: toàn bộ tính năng mới không cần Gemini hoặc backend.

## Phân loại đề của bạn

- Vào bản đồ, chọn bộ đề/thư mục → **Phân loại kỹ năng & độ khó**.
- Nhập môn và kỹ năng, hoặc chọn kỹ năng đã có. Chọn một hay nhiều câu cùng kỹ năng/độ khó rồi lưu.
- Tắt **Chỉ câu chưa phân loại** để sửa nhãn câu cũ.
- Trong màn hình quản lý câu hỏi, biểu tượng bộ não mở bản đồ với phạm vi bộ đề đó.
- Câu chưa phân loại vẫn dùng được trong thi/luyện tập thông thường, nhưng không được chọn cho luyện thích ứng.

## Dữ liệu và thuật toán

- Room version 3 bổ sung `skills`, `learning_sessions`, `question_attempts`; `questions` thêm `skillId`, `difficulty`, `contentVersion`.
- Migration 2→3 giữ đề, câu hỏi, lịch sử thi. Không dùng migration phá hủy dữ liệu.
- Ngân hàng đề vẫn là ngân hàng chung trên thiết bị như trước; tiến độ thích ứng tách theo tài khoản đã chuẩn hóa bằng trim/lowercase(Locale.ROOT).
- Mỗi câu có một kỹ năng chính; kỹ năng được định danh theo môn + tên. Bản đồ không tự suy đoán quan hệ tiên quyết giữa kỹ năng.
- Thành thạo tính từ tối đa 20 lượt hợp lệ gần nhất trong phạm vi đã chọn. Lượt thứ i có trọng số i (1-based): `score = round(100 * tổng trọng số lượt đúng / tổng trọng số)`.
- Bỏ qua không tính sai. Câu đã xóa hoặc đổi phiên bản nội dung không đóng góp vào mức thành thạo hiện tại.
- `Chưa học`: 0 lượt; `Chưa đủ dữ liệu`: dưới 5 câu khác nhau; `Đã vững`: ít nhất 5 câu khác nhau, score ≥80, và ít nhất một lượt đúng câu Vừa/Khó; còn lại `Đang luyện`.
- `Cần ưu tiên`: ít nhất 3 lượt và score <60. Điểm là chỉ báo có quy tắc để demo, không phải đánh giá năng lực đã kiểm chứng.
- Chu kỳ chọn: 3 câu ưu tiên kỹ năng yếu → 1 câu khám phá kỹ năng chưa học → 1 câu kiểm tra kỹ năng mạnh; khi không có nhóm phù hợp sẽ chọn kỹ năng hiện có.
- Mức khó mục tiêu dựa vào hai lượt gần nhất cùng kỹ năng: hai đúng tăng một mức; hai sai giảm một mức. Nếu thiếu câu mức đó, chọn mức gần nhất và ghi rõ trong lý do.
- Trong cùng mức khó, ưu tiên câu sai dù rất chắc, câu sai khác, câu đúng nhưng chưa chắc, câu chưa làm, rồi câu đã đúng. Không lặp câu trong cùng phiên.
- Mức tự tin ảnh hưởng ưu tiên câu; thời gian hoạt động chỉ để thống kê, không trừ điểm khi đọc chậm.
- Phiên có mục tiêu 5/10/15 lượt trả lời; kết thúc sớm nếu hết câu phù hợp. Không tự sinh thêm câu và không giả lập dữ liệu học.

## Độ bền dữ liệu

- Mỗi lần kiểm tra ghi kết quả và trạng thái phản hồi trong một transaction.
- Unique index `(sessionId, questionId)` và kiểm tra id/phiên bản câu hiện tại chống ghi trùng, thao tác cũ không áp dụng sang câu mới.
- Phiên, câu hiện tại, phản hồi và thời gian đã tích lũy được lưu trong Room. Trở lại từ background tính thời gian hoạt động bằng monotonic clock; thời gian chạy nền không cộng vào lượt trả lời.
- Sau khi process bị hủy, tiếp tục từ phiên đã lưu. Lựa chọn chưa kiểm tra có saved state khi hệ thống khôi phục Activity, chưa được tính là lượt học.
- Sửa câu hoặc nhãn làm tăng `contentVersion`; lịch sử cũ vẫn được giữ để xem kết quả phiên, nhưng không tác động đánh giá mới. Câu thay đổi giữa phiên được bỏ qua bằng **Chọn câu khác**.
- Mỗi tài khoản chỉ có một phiên chưa kết thúc. Đổi tài khoản không chia sẻ tiến độ thích ứng.

## Cấu trúc mã

- `learning/AdaptiveEngine.kt`: thuật toán thuần Kotlin.
- `data/local/LearningEntities.kt`: entities và DAO.
- `data/repository/LearningRepository.kt`: transaction và vòng đời phiên.
- `data/repository/LearningDemo.kt`: bộ câu hỏi mẫu offline.
- `ui/AdaptivePracticeViewModel.kt`: trạng thái UI, thao tác và đo thời gian.
- `ui/screens/AdaptiveLearningScreen.kt`: bản đồ, phân loại, luyện và kết quả.

## Kiểm chứng

Lệnh build: `./gradlew :app:assembleDebug`.

Test tính năng: `./gradlew :app:testDebugUnitTest --tests "com.example.learning.*"`.

Xuất ảnh QA: thêm `-Proborazzi.test.record=true` vào lệnh test. Ảnh của màn bản đồ, luyện tập và phân loại được lưu trong `.build-outputs` ở thư mục gốc dự án.

Môi trường xác minh tính năng: JDK 21, Gradle 9.4.1 có sẵn trên máy, Android SDK tại `D:\DownloadProgram\SDK`. File `local.properties` (không theo dõi bằng Git) đã được trỏ về SDK này. Wrapper đã được cập nhật sang Gradle 9.4.1 cùng checksum chính thức để Android Studio dùng bản có sẵn trên máy. Khi dùng terminal, bảo đảm JAVA_HOME trỏ tới JDK phù hợp thay vì JDK 11.

Test bao phủ thuật toán, giới hạn đủ dữ liệu, độ khó, bỏ qua, bản ghi cũ, chống ghi trùng, tách tài khoản, lưu phiên, bộ đề mẫu, kết thúc khi hết câu và migration từ schema version 2.

## Phạm vi bản đầu

Luyện thích ứng và ôn ngắt quãng dùng chung lối vào **Học & ôn tập**. Kết quả luyện thích ứng cập nhật lịch ôn; **Ôn lặp lại ngắt quãng** mở danh sách thẻ đến hạn, còn **Sổ câu sai** là nơi xem lỗi và ghi chú. Xem `SPACED_REVIEW.md` để biết quy tắc lịch ôn.

Chưa có AI tự phân loại, quan hệ kỹ năng tiên quyết, hoặc ghi nhận bài thi/flashcard thường vào bộ đánh giá thành thạo. Tiến độ thành thạo hiện được tạo từ chế độ luyện thích ứng để tránh trộn dữ liệu thiếu thời gian/mức tự tin.
