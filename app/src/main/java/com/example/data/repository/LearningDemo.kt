package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.first

object LearningDemo {
    private const val TITLE = "Android • Luyện tập thích ứng (demo)"
    suspend fun install(db: AppDatabase) {
        if (db.quizDao().getAllQuizzesFlow().first().any { it.title == TITLE }) return
        val dao = db.learningDao()
        val quizId = db.quizDao().insertQuiz(QuizEntity(title = TITLE, topic = "Android", durationMinutes = 20))
        val groups = listOf(
            "Vòng đời Activity" to listOf(
                item("Hàm nào thường khởi tạo giao diện của Activity?", "onCreate()", "onDestroy()", "onStop()", 1),
                item("Hàm nào được gọi khi Activity bắt đầu hiển thị?", "onStart()", "onDestroy()", "onRestart() luôn luôn", 1),
                item("Sau onPause(), Activity có thể quay lại tương tác qua hàm nào?", "onResume()", "onCreate() luôn luôn", "onDestroy()", 2),
                item("ViewModel giúp giữ state qua trường hợp nào?", "Thay đổi cấu hình như xoay màn hình", "Mọi lần gỡ cài đặt", "Mọi lần process bị hủy mà không lưu", 2),
                item("Để khôi phục state nhỏ sau khi process bị hệ thống hủy, nên dùng gì?", "SavedStateHandle hoặc cơ chế saved state", "Biến toàn cục", "ViewModel đơn thuần", 3),
                item("Đếm giờ bài thi chính xác khi app chạy nền nên dựa vào gì?", "Thời điểm hết hạn đã lưu", "Số lần UI vẽ lại", "Chỉ giảm biến mỗi giây khi màn hình mở", 3)
            ),
            "Room" to listOf(
                item("Annotation nào khai báo một bảng Room?", "@Entity", "@Composable", "@GET", 1),
                item("DAO trong Room dùng để làm gì?", "Định nghĩa thao tác truy cập dữ liệu", "Vẽ giao diện", "Xin quyền camera", 1),
                item("Flow từ truy vấn Room hữu ích vì sao?", "Phát dữ liệu mới khi bảng liên quan thay đổi", "Tự tạo server", "Tự mã hóa mọi dữ liệu", 2),
                item("@Transaction dùng khi nào?", "Nhóm thao tác cần tính nguyên tử", "Đổi màu UI", "Tăng tốc mọi truy vấn vô điều kiện", 2),
                item("Nâng version database mà muốn giữ dữ liệu nên làm gì?", "Cung cấp migration và kiểm thử", "Luôn xóa database", "Chỉ đổi tên Entity", 3),
                item("Foreign key ON DELETE CASCADE có ý nghĩa gì?", "Xóa bản ghi con khi bản ghi cha bị xóa", "Đổi khóa chính của mọi bảng", "Luôn cấm xóa bản ghi cha", 3)
            ),
            "Coroutines" to listOf(
                item("Từ khóa khai báo hàm có thể tạm ngưng là gì?", "suspend", "static", "volatile", 1),
                item("Scope thường dùng trong ViewModel là gì?", "viewModelScope", "GlobalScope cho mọi việc", "ActivityThread", 1),
                item("withContext(Dispatchers.IO) phù hợp với việc nào?", "Đọc/ghi file kiểu blocking", "Cập nhật trực tiếp UI ở mọi nơi", "Thay thế mọi transaction", 2),
                item("delay() trong coroutine có tính chất gì?", "Tạm ngưng mà không chặn thread", "Chặn thread như Thread.sleep()", "Luôn tạo một thread mới", 2),
                item("Khi bắt Exception trong coroutine, CancellationException thường nên được xử lý thế nào?", "Ném lại để giữ cơ chế hủy", "Luôn đổi thành lỗi mạng", "Luôn nuốt bỏ", 3),
                item("Structured concurrency giúp điều gì?", "Gắn vòng đời tác vụ con với scope cha", "Mọi tác vụ chạy mãi sau khi scope hủy", "Mọi coroutine có một thread riêng", 3)
            )
        )
        for ((name, items) in groups) {
            dao.insertSkill(SkillEntity(subject = "Android", name = name))
            val skill = requireNotNull(dao.findSkill("Android", name))
            db.quizDao().insertQuestions(items.map { (text, options, level) ->
                QuestionEntity(quizId = quizId, text = text, options = options, correctOptionIndex = 0,
                    skillId = skill.id, difficulty = level)
            })
        }
    }
    private fun item(text: String, correct: String, wrong1: String, wrong2: String, level: Int) = Triple(text, listOf(correct, wrong1, wrong2), level)
}
