package com.example.workflowapp.ui.screens

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(userName: String, userId: Int) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

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
            ) { Icon(Icons.Default.Add, contentDescription = "Thêm mới") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
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
                    Text("Hôm nay bạn có ${tasks.count { it.status != "Done" }} việc chưa xong", color = Color.LightGray, fontSize = 13.sp)
                }
            }

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
                    if (filteredTasks.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Chưa có công việc nào.", color = Color.Gray) } }
                    }
                    items(filteredTasks) { task ->
                        EnhancedTaskCard(
                            task = task,
                            onRefresh = { refreshTasks() },
                            onEditClick = { selectedTaskForEdit = it; showEditDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(onDismiss = { showAddDialog = false }, onTaskCreated = { showAddDialog = false; refreshTasks() }, userId = userId)
    }

    if (showEditDialog && selectedTaskForEdit != null) {
        EditTaskDialog(task = selectedTaskForEdit!!, onDismiss = { showEditDialog = false }, onTaskUpdated = { showEditDialog = false; refreshTasks() })
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

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = if (task.status == "Done") Icons.Default.CheckCircle else Icons.Default.List, contentDescription = null, tint = accentColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.task_title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                if (!task.description.isNullOrEmpty()) Text(task.description, fontSize = 13.sp, color = Color(0xFF64748B), maxLines = 1)
            }
            Row {
                if (task.status == "To Do") {
                    IconButton(onClick = { onEditClick(task) }) { Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { updateStatus(task.id, "In Progress", onRefresh) }) { Icon(Icons.Default.PlayArrow, contentDescription = "Bắt đầu", tint = Color(0xFF3B82F6)) }
                }
                if (task.status == "In Progress") {
                    IconButton(onClick = { updateStatus(task.id, "To Do", onRefresh) }) { Icon(Icons.Default.Refresh, contentDescription = "Tạm dừng", tint = Color.Gray) }
                    IconButton(onClick = { updateStatus(task.id, "Done", onRefresh) }) { Icon(Icons.Default.Check, contentDescription = "Hoàn thành", tint = Color(0xFF10B981)) }
                }
                IconButton(onClick = {
                    RetrofitClient.instance.deleteTask(DeleteTaskRequest(task.id)).enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onRefresh() }
                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                    })
                }) { Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskCreated: () -> Unit, userId: Int) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Quản lý thời gian
    val calendar = remember { Calendar.getInstance() }
    var selectedTimeText by remember { mutableStateOf("Chưa chọn giờ") }
    var timeInMillis by remember { mutableLongStateOf(0L) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            timeInMillis = calendar.timeInMillis
            selectedTimeText = String.format("%02d:%02d", hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo công việc mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên công việc") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Mô tả") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { timePickerDialog.show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (timeInMillis == 0L) "Hẹn giờ báo thức" else "Báo thức lúc: $selectedTimeText")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập tên công việc", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val deadlineStr = if (timeInMillis > 0L) formatter.format(calendar.time) else null

                val req = CreateTaskRequest(title, desc, userId, deadlineStr)

                RetrofitClient.instance.createTask(req).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            // neu co hen gio thi goi ham` nay
                            if (timeInMillis > System.currentTimeMillis()) {
                                scheduleNotification(context, title, timeInMillis)
                            }
                            onTaskCreated()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                })
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// sua , cap nhat , thong bao
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
                RetrofitClient.instance.updateTask(UpdateTaskRequest(task.id, title, desc)).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onTaskUpdated() }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }) { Text("Cập nhật") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

fun updateStatus(taskId: Int, newStatus: String, onComplete: () -> Unit) {
    RetrofitClient.instance.updateTaskStatus(UpdateStatusRequest(taskId, newStatus)).enqueue(object : Callback<ApiResponse> {
        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) { if (response.isSuccessful) onComplete() }
        override fun onFailure(call: Call<ApiResponse>, t: Throwable) { }
    })
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleNotification(context: Context, title: String, timeInMillis: Long) {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("task_title", title)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, timeInMillis.toInt(), intent, PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
}