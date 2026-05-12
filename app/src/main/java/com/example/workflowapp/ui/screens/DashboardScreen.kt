package com.example.workflowapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workflowapp.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun DashboardScreen(userName: String, userId: Int) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // TRẠNG THÁI CHO CHỨC NĂNG SỬA
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<Task?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cần làm", "Đang làm", "Xong")
    val statusFilters = listOf("To Do", "In Progress", "Done")

    fun refreshTasks() {
        isLoading = true
        RetrofitClient.instance.getTasks(userId).enqueue(object : Callback<TaskListResponse> {
            override fun onResponse(call: Call<TaskListResponse>, response: Response<TaskListResponse>) {
                isLoading = false
                if (response.isSuccessful) tasks = response.body()?.data ?: listOf()
            }
            override fun onFailure(call: Call<TaskListResponse>, t: Throwable) { isLoading = false }
        })
    }

    LaunchedEffect(Unit) { refreshTasks() }

    val filteredTasks = tasks.filter { it.status == statusFilters[selectedTab] }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
            // Header (Giữ nguyên phong cách HKPH)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text("Xin chào, $userName 👋", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("HKPH WorkFlow System", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFF3B82F6)) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredTasks) { task ->
                        EnhancedTaskCard(
                            task = task,
                            onRefresh = { refreshTasks() },
                            onEditClick = { // Khi bấm sửa, lưu task và mở popup
                                selectedTaskForEdit = it
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Popup Thêm mới
    if (showAddDialog) {
        AddTaskDialog(onDismiss = { showAddDialog = false }, onTaskCreated = { showAddDialog = false; refreshTasks() }, userId = userId)
    }

    // Popup Chỉnh sửa
    if (showEditDialog && selectedTaskForEdit != null) {
        EditTaskDialog(
            task = selectedTaskForEdit!!,
            onDismiss = { showEditDialog = false },
            onTaskUpdated = { showEditDialog = false; refreshTasks() }
        )
    }
}

@Composable
fun EnhancedTaskCard(task: Task, onRefresh: () -> Unit, onEditClick: (Task) -> Unit) {
    val context = LocalContext.current
    val accentColor = when (task.status) {
        "To Do" -> Color(0xFF3B82F6)
        "In Progress" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (task.status == "Done") Icons.Default.CheckCircle else Icons.Default.List,
                    contentDescription = null, tint = accentColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(task.task_title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                Text(task.description ?: "Không có mô tả", fontSize = 13.sp, color = Color(0xFF64748B), maxLines = 1)
            }

            Row {
                // CHỈ HIỂN THỊ NÚT SỬA Ở TRẠNG THÁI "TO DO"
                if (task.status == "To Do") {
                    IconButton(onClick = { onEditClick(task) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { updateStatus(task.id, "In Progress", onRefresh) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Bắt đầu", tint = Color(0xFF3B82F6))
                    }
                }

                if (task.status == "In Progress") {
                    IconButton(onClick = { updateStatus(task.id, "To Do", onRefresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tạm dừng", tint = Color.Gray)
                    }
                    IconButton(onClick = { updateStatus(task.id, "Done", onRefresh) }) {
                        Icon(Icons.Default.Check, contentDescription = "Hoàn thành", tint = Color(0xFF10B981))
                    }
                }

                IconButton(onClick = {
                    RetrofitClient.instance.deleteTask(DeleteTaskRequest(task.id)).enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onRefresh() }
                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                    })
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// POPUP CHỈNH SỬA CÔNG VIỆC
@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onTaskUpdated: () -> Unit) {
    var title by remember { mutableStateOf(task.task_title) }
    var desc by remember { mutableStateOf(task.description ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa công việc", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên công việc") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Mô tả") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isEmpty()) return@Button
                val req = UpdateTaskRequest(task.id, title, desc)
                RetrofitClient.instance.updateTask(req).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
                            onTaskUpdated()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }) { Text("Cập nhật") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// Các hàm phụ trợ giữ nguyên
fun updateStatus(taskId: Int, newStatus: String, onComplete: () -> Unit) {
    RetrofitClient.instance.updateTaskStatus(UpdateStatusRequest(taskId, newStatus)).enqueue(object : Callback<ApiResponse> {
        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onComplete() }
        override fun onFailure(call: Call<ApiResponse>, t: Throwable) { }
    })
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskCreated: () -> Unit, userId: Int) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo công việc mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên công việc") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Mô tả") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isEmpty()) return@Button
                val req = CreateTaskRequest(title, desc, userId)
                RetrofitClient.instance.createTask(req).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onTaskCreated() }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}