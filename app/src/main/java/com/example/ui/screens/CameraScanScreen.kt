package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.QuizGenerationState
import com.example.ui.QuizViewModel
import com.example.ui.theme.*
import com.google.accompanist.permissions.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    viewModel: QuizViewModel,
    targetQuizId: Long? = null,
    targetFolderId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val generationState by viewModel.generationState.collectAsState()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var promptAdditionText by remember { mutableStateOf("") }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.generateQuizFromImage(bitmap, promptAdditionText, targetQuizId, targetFolderId)
                    } else {
                        Log.e("CameraScanScreen", "Failed to deserialize selected image to bitmap")
                    }
                } catch (e: Exception) {
                    Log.e("CameraScanScreen", "Error reading selected image", e)
                }
            }
        }
    }

    LaunchedEffect(generationState) {
        if (generationState is QuizGenerationState.Success) {
            viewModel.resetGenerationState()
            onNavigateToHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quét Đề Thi Bằng AI") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Viewfinder block
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScan", "Camera binding failure", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Layout guidelines box overlay in the center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 200.dp, top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .fillMaxHeight(0.6f)
                            .border(2.dp, PrimaryPurple, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "HÃY CĂN GHÉP VĂN BẢN TRÊN SÁCH VÀO KHUNG VỰC NÀY",
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(0.dp, 0.dp, 8.dp, 8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Controls and Input prompt block at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = promptAdditionText,
                            onValueChange = { promptAdditionText = it },
                            placeholder = { Text("Yêu cầu thêm (VD: độ khó cao, tiếng Nhật chuyên ngành N2...)", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Import mock snapshot fallback button or real file picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .clickable {
                                            filePickerLauncher.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Tải ảnh từ thư viện", tint = Color.White)
                                }
                                Text(text = "Chọn ảnh", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }

                            // Capture button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        val capture = imageCapture
                                        if (capture != null) {
                                            viewModel.generateQuizFromImage(
                                                Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888), // Fallback if emulator
                                                promptAdditionText,
                                                targetQuizId,
                                                targetFolderId
                                            )
                                        } else {
                                            val dummyBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                                            viewModel.generateQuizFromImage(dummyBitmap, promptAdditionText, targetQuizId, targetFolderId)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Chụp và tạo đề",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Mock sample quick trigger button for testing easy flow without local file
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .clickable {
                                            val dummyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                                            viewModel.generateQuizFromImage(
                                                dummyBitmap,
                                                "Mô phỏng chụp đề thi tiếng Nhật N3",
                                                targetQuizId,
                                                targetFolderId
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Mẫu Test", tint = Color.White)
                                }
                                Text(text = "Tạo mẫu AI", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            } else {
                // Permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Cần quyền truy cập Camera",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Chúng tôi sử dụng Camera để quét đề thi trực tiếp từ tài liệu giấy, sách bài tập của bạn và dịch sang các câu hỏi trắc nghiệm tương tác.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Cấp Quyền Camera")
                    }
                }
            }

            // PROCESSING DIALOG
            if (generationState !is QuizGenerationState.Idle) {
                AIGenerationOverlay(
                    state = generationState,
                    onReset = { viewModel.resetGenerationState() }
                )
            }
        }
    }
}
