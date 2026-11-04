# Widget tiến độ học QuizAI

Widget màn hình chính dùng Jetpack Glance 1.1.1, hỗ trợ Android 7.0 trở lên theo minSdk của app.

## Sử dụng

1. Cài APK hoặc chạy app từ Android Studio trên điện thoại/máy ảo.
2. Về Home, nhấn giữ vùng trống, chọn Widgets và tìm QuizAI.
3. Thêm **Tiến độ học QuizAI** (2×2). Kéo rộng widget để hiện tên tài khoản và trạng thái hoạt động hôm nay.
4. Bấm **Ôn ngay** để mở Ôn tập & Sổ câu sai. Khi chưa đăng nhập, widget mở đăng nhập và tiếp tục đến phần ôn sau khi đăng nhập thành công.
5. Bấm **↻** để tải lại dữ liệu.

Widget dùng tài khoản đang đăng nhập, dữ liệu Room và SharedPreferences trên thiết bị, không cần mạng.
Streak tuân theo hành vi hiện có của app: mở app/đăng nhập cũng được tính là hoạt động. Đọc hoặc cập nhật widget không tự tăng streak.

## Cập nhật và dữ liệu

- Câu đến hạn dùng chung `ReviewSummary`/`ReviewScheduler` với màn hình ôn; chỉ tính câu hợp lệ có lịch đã đến hạn. Câu mới chưa có lịch không được tính là đến hạn.
- Câu sửa nội dung được chuẩn hóa theo phiên bản mới; câu đã xóa không xuất hiện. Đọc widget không ghi thay đổi vào lịch ôn.
- `StudyWidgetUpdates` theo dõi thay đổi đã commit ở `questions`, `review_progress` và các preference tài khoản/streak; cập nhật tất cả widget sau khi dữ liệu thay đổi.
- Phiên Glance đang chạy cũng theo dõi phiên bản dữ liệu và tải lại snapshot, tránh giữ trạng thái cũ sau đăng nhập/đăng xuất hoặc trả lời câu hỏi.
- Receiver nhận cập nhật định kỳ 30 phút từ Android. Hệ thống có thể trì hoãn trong chế độ tiết kiệm pin; widget không cam kết cập nhật đúng thời điểm nửa đêm.
- Hỗ trợ sáng/tối, nhiều widget, trạng thái chưa đăng nhập, chưa có câu hỏi, không có câu đến hạn và lỗi tải dữ liệu. Snapshot được kiểm tra lại tài khoản sau khi tải để tránh dùng kết quả của tài khoản vừa đăng xuất.
- Activity xử lý Intent khi mở mới và `onNewIntent`, lưu yêu cầu qua việc tạo lại Activity và giữ yêu cầu qua bước đăng nhập.

## Kiểm tra

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

`StudyWidgetSummaryTest` kiểm tra hạn ôn, dữ liệu từng tài khoản, câu sửa/xóa/không hợp lệ và việc đọc không sửa lịch.
`StudyWidgetNavigationTest` kiểm tra mở lạnh, đăng nhập rồi mở ôn, tạo lại Activity và Intent mới khi app đang mở.
`StudyWidgetLiveStateTest` trong `androidTest` giữ cùng phiên Glance và kiểm tra đăng nhập, cập nhật lịch ôn rồi đăng xuất. Chạy trên máy ảo/thiết bị qua Android Studio hoặc `:app:connectedDebugAndroidTest`. Bài kiểm tra tạo tài khoản giả và bộ câu hỏi thử, xóa bộ câu hỏi và đăng xuất khi kết thúc.

Kiểm tra thủ công trên launcher: thêm/xóa nhiều widget, đổi kích thước, sáng/tối, học rồi quay về Home, đăng xuất/đổi tài khoản và khởi động lại máy ảo.

## Kết quả kiểm tra bản triển khai

- Build debug và AndroidTest thành công; 35 unit test và 1 Android instrumentation test đều qua.
- Kiểm tra thực tế trên máy ảo Android API 37.1: tìm/thêm widget, mở đăng nhập từ widget rồi đến màn hình ôn, hiển thị streak sau đăng nhập, bố cục nhỏ/lớn và chế độ tối.
- APK: `.build-outputs/quizai-widget-debug.apk`.
- Ảnh màn hình thực tế: `.build-outputs/widget-home.png`, `.build-outputs/widget-signed-in.png`, `.build-outputs/widget-resize.png`.
- Chưa kiểm tra trực tiếp trên điện thoại thật, Android 7 hoặc việc cập nhật định kỳ khi thiết bị ở chế độ tiết kiệm pin.

## Thiết kế widget cập nhật 18/10/2026

- Tông tím dùng chung màu thương hiệu của app, nền lavender và màu riêng cho chế độ tối.
- Số ngày liên tiếp lớn hơn, biểu tượng ngọn lửa vector màu cam và nút làm mới có nhãn trợ năng.
- Bốn bố cục responsive: nhỏ/thấp, nhỏ/cao, rộng/thấp, rộng/cao. Bản cao thêm khối thông tin ôn tập; bản rộng hiện tài khoản. Không suy diễn lịch sử học hay phần trăm tiến độ từ streak.
- Giữ nguyên nguồn dữ liệu, cách tính streak, cập nhật trực tiếp và điều hướng ôn tập.
- Tham khảo: Duolingo (https://blog.duolingo.com/widget-feature/) về chuỗi ngày và hành động học; Structured (https://help.structured.app/en/articles/330498) về thông tin nhanh và mở tác vụ.
- `StudyWidgetLayoutTest` render RemoteViews thật tại 140×140, 180×220, 280×140, 320×220 dp, sáng/tối; kiểm tra nút ôn hiển thị trong vùng widget và xuất ảnh để kiểm tra trực quan. Ảnh dùng dữ liệu mẫu 7 ngày / 12 câu đến hạn.
- Kết quả: 35 unit test qua; 2 instrumentation test qua trên máy ảo Android API 37.1, bao gồm tám ảnh render sáng/tối. Kiểm tra lại cả hai instrumentation test sau thay đổi nền bo góc: qua.
- APK giao diện mới: `.build-outputs/quizai-widget-polished-debug.apk`. Ảnh: `.build-outputs/widget-redesign-previews/`. Chưa kiểm tra trực tiếp trên điện thoại thật hoặc Android cũ.


## Lựa chọn thứ hai: Tập trung ôn tập

- Trong trình chọn Widgets, chọn **QuizAI · Tập trung ôn tập** để thêm thẻ màu kem/xanh lá. Kiểu tím **Tiến độ học QuizAI** vẫn có sẵn; có thể đặt cả hai cùng lúc.
- Số câu đến hạn là thông tin chính. Bản cao hoặc rộng thêm chuỗi ngày; bản rộng hiện tài khoản. Khi chưa có câu hỏi, hiển thị lời mời bắt đầu thay vì một số 0 gây nhầm lẫn.
- Hai receiver độc lập dùng chung cách tải snapshot, theo dõi thay đổi tài khoản/Room và điều hướng ôn tập. Nút làm mới cập nhật cả hai loại.
- Kiểm tra layout bao gồm cả hai kiểu, bốn kích thước và sáng/tối; kiểm tra luồng dữ liệu đăng nhập, ôn bài rồi đăng xuất cho từng kiểu.
- Kết quả bản hai lựa chọn: build debug thành công; 3 instrumentation test qua trên máy ảo API 37.1 (16 ảnh bố cục cho hai kiểu, sáng/tối; hai luồng cập nhật dữ liệu). Nhãn nút được kiểm tra không bị cắt.
- APK: `.build-outputs/quizai-two-widgets-debug.apk`. Ảnh mẫu: `.build-outputs/widget-focus-final-previews/focus-180x220-light.png`. Chưa kiểm tra trực tiếp trên điện thoại thật.

## Sửa ảnh xem trước trong trình chọn widget

- Tái hiện bằng `StudyWidgetPickerPreviewTest`: `RemoteViews.apply` báo `Class not allowed to be inflated android.widget.Space` ở bố cục xem trước cũ. Kiểm tra widget Glance thông thường trước đó không đi qua các XML này nên chưa phát hiện lỗi.
- Hai `previewLayout` hiện dùng `ImageView` tương thích RemoteViews, hiển thị ảnh PNG mẫu của từng kiểu bằng `fitCenter`. Giữ `previewImage` để hỗ trợ launcher/Android cũ. Tham khảo cơ chế ưu tiên previewLayout: https://developer.android.com/develop/ui/views/appwidgets/previews
- Kiểm thử tải metadata, ảnh fallback và inflate cả hai preview: qua trên API 37.1. Đã mở trực tiếp trình chọn Widgets của Pixel Launcher và xác nhận hai ảnh xuất hiện.
- Ảnh xác nhận: `.build-outputs/widget-picker-fixed.png`.
- APK đã sửa: `.build-outputs/quizai-widget-previews-debug.apk`.
