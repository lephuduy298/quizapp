package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: QuizViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = HighDensityBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High Density Indigo Brand Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LightPurpleContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "App Logo",
                    tint = DarkPurple,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "QuizAI Studio",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkPurple,
                    letterSpacing = (-1).sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isRegisterMode) 
                    "Đăng ký tài khoản để bắt đầu quét & tối ưu hóa học tập" 
                else 
                    "Hệ thống phân tích đề thi và ôn luyện thời gian thực",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Card Containers
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Đăng Ký Tài Khoản" else "Đăng Nhập",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BodyTextColor
                    )

                    // Error presentation
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFCE8E6))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = ErrorRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success presentation
                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        successMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE6F4EA))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = CorrectGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Tên đăng nhập") },
                        placeholder = { Text("Nhập tài khoản") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "User Icon", tint = PrimaryPurple)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderColor
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Mật khẩu") },
                        placeholder = { Text("Nhập mật khẩu") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Icon", tint = PrimaryPurple)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = SecondaryTextColor
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderColor
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isRegisterMode) {
                                    focusManager.clearFocus()
                                    performSubmit(
                                        viewModel = viewModel,
                                        isRegisterMode = isRegisterMode,
                                        username = username,
                                        password = password,
                                        confirmPassword = confirmPassword,
                                        setErrorMessage = { errorMessage = it },
                                        setSuccessMessage = { successMessage = it },
                                        toggleMode = { isRegisterMode = false },
                                        onSuccess = onLoginSuccess
                                    )
                                }
                            }
                        )
                    )

                    // Confirm password field (only in register mode)
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Xác nhận mật khẩu") },
                            placeholder = { Text("Nhập lại mật khẩu") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Icon", tint = PrimaryPurple)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle confirm password visibility",
                                        tint = SecondaryTextColor
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderColor
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    performSubmit(
                                        viewModel = viewModel,
                                        isRegisterMode = isRegisterMode,
                                        username = username,
                                        password = password,
                                        confirmPassword = confirmPassword,
                                        setErrorMessage = { errorMessage = it },
                                        setSuccessMessage = { successMessage = it },
                                        toggleMode = { isRegisterMode = false },
                                        onSuccess = onLoginSuccess
                                    )
                                }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary submit button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            performSubmit(
                                viewModel = viewModel,
                                isRegisterMode = isRegisterMode,
                                username = username,
                                password = password,
                                confirmPassword = confirmPassword,
                                setErrorMessage = { errorMessage = it },
                                setSuccessMessage = { successMessage = it },
                                toggleMode = { isRegisterMode = false },
                                onSuccess = onLoginSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Đăng Ký Ngay" else "Xác Nhận Đăng Nhập",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Divider representation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BorderColor.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "hoặc",
                            color = SecondaryTextColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BorderColor.copy(alpha = 0.5f)
                        )
                    }

                    // Mode switch toggle button
                    OutlinedButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMessage = null
                            successMessage = null
                            password = ""
                            confirmPassword = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_toggle_mode_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkPurple),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Đã có tài khoản? Đăng nhập" else "Tạo tài khoản mới mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun performSubmit(
    viewModel: QuizViewModel,
    isRegisterMode: Boolean,
    username: String,
    password: String,
    confirmPassword: String,
    setErrorMessage: (String?) -> Unit,
    setSuccessMessage: (String?) -> Unit,
    toggleMode: () -> Unit,
    onSuccess: () -> Unit
) {
    val sanitizedUser = username.trim()
    if (sanitizedUser.isEmpty()) {
        setErrorMessage("Vui lòng nhập tên đăng nhập!")
        return
    }
    if (password.isEmpty()) {
        setErrorMessage("Vui lòng nhập mật khẩu!")
        return
    }

    if (isRegisterMode) {
        if (confirmPassword.isEmpty()) {
            setErrorMessage("Vui lòng xác nhận mật khẩu!")
            return
        }
        if (password != confirmPassword) {
            setErrorMessage("Mật khẩu xác nhận không trùng khớp!")
            return
        }

        // Action register
        val registered = viewModel.register(sanitizedUser, password)
        if (registered) {
            setErrorMessage(null)
            setSuccessMessage("Đăng ký tài khoản thành công! Hãy đăng nhập.")
            toggleMode()
        } else {
            setErrorMessage("Tài khoản đã tồn tại hoặc không hợp lệ!")
        }
    } else {
        // Action login
        val loggedIn = viewModel.login(sanitizedUser, password)
        if (loggedIn) {
            setErrorMessage(null)
            setSuccessMessage("Đăng nhập thành công!")
            onSuccess()
        } else {
            setErrorMessage("Tên đăng nhập hoặc mật khẩu sai. Thử lại!")
        }
    }
}
