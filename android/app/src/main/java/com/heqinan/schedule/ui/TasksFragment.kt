package com.heqinan.schedule.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heqinan.schedule.adapter.TaskAdapter
import com.heqinan.schedule.data.*
import com.heqinan.schedule.databinding.FragmentTasksBinding
import com.heqinan.schedule.websocket.WebSocketManager
import kotlinx.coroutines.launch
import java.util.*

class TasksFragment : Fragment(), WebSocketManager.WebSocketListener {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TaskAdapter
    private val webSocketManager = WebSocketManager()
    private var allTasks = listOf<Task>()
    private var currentFilter = "all" // all, urgent_recent, urgent_today, normal
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupButtons()
        setupFilter()
        
        // 连接 WebSocket
        webSocketManager.setListener(this)
        webSocketManager.connect()
    }
    
    private fun setupRecyclerView() {
        adapter = TaskAdapter(emptyList(), { task ->
            // 点击任务，可以编辑
            showEditTaskDialog(task)
        }, { task, isCompleted ->
            // 完成任务
            completeTask(task, isCompleted)
        })
        
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupButtons() {
        binding.addButton.setOnClickListener {
            showAddTaskDialog()
        }
        
        binding.refreshButton.setOnClickListener {
            loadTasks()
        }
    }
    
    private fun setupFilter() {
        val filters = listOf("全部", "近期重要", "当天重要", "普通")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = adapter
        
        binding.filterSpinner.onItemSelectedListener = 
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFilter = when (position) {
                        0 -> "all"
                        1 -> "urgent_recent"
                        2 -> "urgent_today"
                        3 -> "normal"
                        else -> "all"
                    }
                    filterTasks()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }
    
    private fun filterTasks() {
        val filtered = when (currentFilter) {
            "all" -> allTasks
            else -> allTasks.filter { it.category == currentFilter }
        }
        adapter.updateTasks(filtered)
    }
    
    private fun loadTasks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.taskApi.getTasks()
                if (response.isSuccessful) {
                    allTasks = response.body() ?: emptyList()
                    filterTasks()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddTaskDialog() {
        val dialog = AddTaskDialog(requireContext()) { title, description, category, dueDate ->
            createTask(title, description, category, dueDate)
        }
        dialog.show()
    }
    
    private fun showEditTaskDialog(task: Task) {
        // 简化版，可以直接删除或标记完成
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(task.title)
            .setMessage(task.description)
            .setPositiveButton("删除") { _, _ ->
                deleteTask(task.id)
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun createTask(title: String, description: String, category: String, dueDate: String?) {
        lifecycleScope.launch {
            try {
                val task = TaskCreate(title, description, category, dueDate)
                val response = RetrofitClient.taskApi.createTask(task)
                if (response.isSuccessful) {
                    Toast.makeText(context, "创建成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun completeTask(task: Task, isCompleted: Boolean) {
        lifecycleScope.launch {
            try {
                val update = TaskUpdate(isCompleted = if (isCompleted) 1 else 0)
                RetrofitClient.taskApi.updateTask(task.id, update)
            } catch (e: Exception) {
                Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteTask(id: Int) {
        lifecycleScope.launch {
            try {
                RetrofitClient.taskApi.deleteTask(id)
                Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // WebSocket 回调
    override fun onTaskCreated(task: Task) {
        activity?.runOnUiThread {
            allTasks = allTasks + task
            filterTasks()
        }
    }
    
    override fun onTaskUpdated(task: Task) {
        activity?.runOnUiThread {
            allTasks = allTasks.map { if (it.id == task.id) task else it }
            filterTasks()
        }
    }
    
    override fun onTaskDeleted(taskId: Int) {
        activity?.runOnUiThread {
            allTasks = allTasks.filter { it.id != taskId }
            filterTasks()
        }
    }
    
    override fun onTasksList(tasks: List<Task>) {
        activity?.runOnUiThread {
            allTasks = tasks
            filterTasks()
        }
    }
    
    override fun onConnected() {
        activity?.runOnUiThread {
            binding.connectionStatus.text = "已连接"
            binding.connectionStatus.setTextColor(android.graphics.Color.GREEN)
        }
    }
    
    override fun onDisconnected() {
        activity?.runOnUiThread {
            binding.connectionStatus.text = "未连接"
            binding.connectionStatus.setTextColor(android.graphics.Color.RED)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        webSocketManager.disconnect()
        _binding = null
    }
}
