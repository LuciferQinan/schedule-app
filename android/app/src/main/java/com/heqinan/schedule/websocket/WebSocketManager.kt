package com.heqinan.schedule.websocket

import android.util.Log
import com.google.gson.Gson
import com.heqinan.schedule.data.Task
import kotlinx.coroutines.*
import okhttp3.*

class WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()
    private var listener: WebSocketListener? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 修改这里为你的 NAS IP
    private val wsUrl = "ws://192.168.50.163:8080/ws"
    
    interface WebSocketListener {
        fun onTaskCreated(task: Task)
        fun onTaskUpdated(task: Task)
        fun onTaskDeleted(taskId: Int)
        fun onTasksList(tasks: List<Task>)
        fun onConnected()
        fun onDisconnected()
    }
    
    fun setListener(listener: WebSocketListener) {
        this.listener = listener
    }
    
    fun connect() {
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                listener?.onConnected()
                // 请求任务列表
                webSocket.send(gson.toJson(mapOf("type" to "get_tasks")))
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
                try {
                    val message = gson.fromJson(text, Map::class.java)
                    when (message["type"]) {
                        "task_created" -> {
                            val task = gson.fromJson(gson.toJson(message["data"]), Task::class.java)
                            listener?.onTaskCreated(task)
                        }
                        "task_updated" -> {
                            val task = gson.fromJson(gson.toJson(message["data"]), Task::class.java)
                            listener?.onTaskUpdated(task)
                        }
                        "task_deleted" -> {
                            val id = (message["data"] as Map<*, *>)["id"] as Double
                            listener?.onTaskDeleted(id.toInt())
                        }
                        "tasks_list" -> {
                            val tasksJson = gson.toJson(message["data"])
                            val tasks = gson.fromJson(tasksJson, Array<Task>::class.java).toList()
                            listener?.onTasksList(tasks)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message", e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
                listener?.onDisconnected()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error", t)
                listener?.onDisconnected()
                // 重连
                scope.launch {
                    delay(3000)
                    connect()
                }
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        scope.cancel()
    }
}
