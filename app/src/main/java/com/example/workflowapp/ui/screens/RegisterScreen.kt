package com.example.workflowapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Import các class mạng vừa tạo
import com.example.workflowapp.network.ApiResponse
import com.example.workflowapp.network.RegisterRequest
import com.example.workflowapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Lấy context để hiển thị thông báo Toast
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đăng ký tài khoản", fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it },
            label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Kiểm tra xem đã nhập đủ chưa
                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Tạo gói dữ liệu gửi đi
                val request = RegisterRequest(fullName, email, password)

                // Gửi qua mạng xuống PHP
                RetrofitClient.instance.registerUser(request).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val apiResponse = response.body()!!
                            // Hiện thông báo từ PHP trả về ("Đăng ký thành công!" hoặc "Email đã tồn tại")
                            Toast.makeText(context, apiResponse.message, Toast.LENGTH_LONG).show()

                            // Nếu thành công thì tự động nhảy về màn hình Đăng nhập
                            if (apiResponse.status == "success") {
                                onNavigateToLogin()
                            }
                        } else {
                            Toast.makeText(context, "Lỗi từ Server", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        // Lỗi này thường do XAMPP chưa bật, hoặc sai địa chỉ IP
                        Toast.makeText(context, "Lỗi kết nối mạng: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Đăng ký", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập ngay")
        }
    }
}