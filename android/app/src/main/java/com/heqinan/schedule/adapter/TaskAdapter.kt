package com.heqinan.schedule.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heqinan.schedule.R
import com.heqinan.schedule.data.Task

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskComplete: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleText.text = task.title
        holder.checkBox.isChecked = task.isCompleted == 1
        holder.dateText.text = task.dueDate ?: "无截止日期"
        
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onTaskComplete(task, isChecked)
        }
        
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }
    
    override fun getItemCount() = tasks.size
    
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
