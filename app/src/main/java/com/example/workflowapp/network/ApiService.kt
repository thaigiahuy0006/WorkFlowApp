package com.example.workflowapp.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


data class User(val id: Int, val name: String, val role: String)
data class RegisterRequest(val full_name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class ApiResponse(val status: String, val message: String, val user: User? = null)
data class DeleteTaskRequest(val task_id: Int)
data class UpdateTaskRequest(val task_id: Int, val task_title: String, val description: String)
data class Task(
    val id: Int,
    val task_title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val deadline_time: String?
)

data class CreateTaskRequest(
    val task_title: String,
    val description: String,
    val assigned_to: Int,
    val deadline_time: String?,
    val priority: String = "Medium"
)

data class TaskListResponse(val status: String, val data: List<Task>)

data class UpdateStatusRequest(val task_id: Int, val status: String)

interface ApiService {
    @POST("task_api/register.php")
    fun registerUser(@Body request: RegisterRequest): Call<ApiResponse>

    @POST("task_api/login.php")
    fun loginUser(@Body request: LoginRequest): Call<ApiResponse>

    @GET("task_api/get_tasks.php")
    fun getTasks(@Query("user_id") userId: Int): Call<TaskListResponse>

    @POST("task_api/create_task.php")
    fun createTask(@Body request: CreateTaskRequest): Call<ApiResponse>

    @POST("task_api/update_task_status.php")
    fun updateTaskStatus(@Body request: UpdateStatusRequest): Call<ApiResponse>

    @POST("task_api/delete_task.php")
    fun deleteTask(@Body request: DeleteTaskRequest): Call<ApiResponse>

    @POST("task_api/update_task.php")
    fun updateTask(@Body request: UpdateTaskRequest): Call<ApiResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}