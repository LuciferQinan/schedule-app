package com.heqinan.schedule.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.heqinan.schedule.R
import java.util.*

class AddTaskDialog(
    context: Context,
    private val onAdd: (String, String, String, String?) -> Unit
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_task)
        
        val titleInput = findViewById<EditText>(R.id.titleInput)
        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val dateInput = findViewById<EditText>(R.id.dateInput)
        val addButton = findViewById<Button>(R.id.addButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        
        // 设置分类选项
        val categories = listOf("近期重要", "当天重要", "普通")
        val categoryValues = listOf("urgent_recent", "urgent_today", "normal")
        categorySpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categories)
        
        // 默认日期为今天
        val calendar = Calendar.getInstance()
        dateInput.setText(String.format("%04d-%02d-%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)))
        
        addButton.setOnClickListener {
            val title = titleInput.text.toString()
            val description = descriptionInput.text.toString()
            val category = categoryValues[categorySpinner.selectedItemPosition]
            val dueDate = dateInput.text.toString().takeIf { it.isNotEmpty() }
            
            if (title.isNotEmpty()) {
                onAdd(title, description, category, dueDate)
                dismiss()
            }
        }
        
        cancelButton.setOnClickListener {
            dismiss()
        }
    }
}
