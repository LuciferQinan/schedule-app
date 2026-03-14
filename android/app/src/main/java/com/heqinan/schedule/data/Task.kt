package com.heqinan.schedule.data

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,  // urgent_recent, urgent_today, normal
    val dueDate: String?,
    val createdAt: String,
    val updatedAt: String,
    val isCompleted: Int
)

data class TaskCreate(
    val title: String,
    val description: String = "",
    val category: String,
    val dueDate: String? = null
)

data class TaskUpdate(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val dueDate: String? = null,
    val isCompleted: Int? = null
)
