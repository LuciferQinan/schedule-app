package com.heqinan.schedule.data

import retrofit2.Response
import retrofit2.http.*

interface TaskApi {
    @GET("tasks")
    suspend fun getTasks(@Query("category") category: String? = null): Response<List<Task>>
    
    @GET("tasks/today")
    suspend fun getTodayTasks(): Response<List<Task>>
    
    @GET("tasks/recent")
    suspend fun getRecentTasks(): Response<List<Task>>
    
    @POST("tasks")
    suspend fun createTask(@Body task: TaskCreate): Response<Task>
    
    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: TaskUpdate): Response<Task>
    
    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Map<String, Any>>
}
