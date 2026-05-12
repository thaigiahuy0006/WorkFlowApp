package com.example.workflowapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.workflowapp.ui.theme.WorkFlowAppTheme
import com.example.workflowapp.ui.screens.LoginScreen
import com.example.workflowapp.ui.screens.RegisterScreen
import com.example.workflowapp.ui.screens.DashboardScreen

enum class AppScreen { LOGIN, REGISTER, DASHBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkFlowAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

                    var loggedInUserName by remember { mutableStateOf("") }
                    // THÊM: Biến lưu ID người dùng
                    var loggedInUserId by remember { mutableStateOf(-1) }

                    when (currentScreen) {
                        AppScreen.LOGIN -> LoginScreen(
                            onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                            // Nhận cả userName và userId từ LoginScreen
                            onLoginSuccess = { userName, userId ->
                                loggedInUserName = userName
                                loggedInUserId = userId
                                currentScreen = AppScreen.DASHBOARD
                            }
                        )
                        AppScreen.REGISTER -> RegisterScreen(
                            onNavigateToLogin = { currentScreen = AppScreen.LOGIN }
                        )
                        AppScreen.DASHBOARD -> DashboardScreen(
                            userName = loggedInUserName,
                            userId = loggedInUserId // Truyền ID này cho Dashboard lấy dữ liệu
                        )
                    }
                }
            }
        }
    }
}