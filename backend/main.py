from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, date
import sqlite3
import json
import asyncio

app = FastAPI(title="日程管理后端", version="1.0.0")

# CORS 配置，允许 Android App 访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# WebSocket 连接管理
class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
    
    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
    
    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
    
    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            try:
                await connection.send_json(message)
            except:
                pass

manager = ConnectionManager()

# 数据库初始化
def init_db():
    conn = sqlite3.connect('/home/node/.openclaw/workspace/schedule-app/backend/schedule.db')
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            category TEXT NOT NULL,  -- 'urgent_recent', 'urgent_today', 'normal'
            due_date TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
            is_completed INTEGER DEFAULT 0
        )
    ''')
    conn.commit()
    conn.close()

init_db()

# 数据模型
class TaskCreate(BaseModel):
    title: str
    description: Optional[str] = ""
    category: str  # urgent_recent, urgent_today, normal
    due_date: Optional[str] = None

class TaskUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    category: Optional[str] = None
    due_date: Optional[str] = None
    is_completed: Optional[int] = None

class Task(BaseModel):
    id: int
    title: str
    description: str
    category: str
    due_date: Optional[str]
    created_at: str
    updated_at: str
    is_completed: int

# 数据库操作
def get_db():
    conn = sqlite3.connect('/home/node/.openclaw/workspace/schedule-app/backend/schedule.db')
    conn.row_factory = sqlite3.Row
    return conn

# API 路由
@app.get("/")
def read_root():
    return {"message": "日程管理后端服务运行中", "version": "1.0.0"}

@app.get("/tasks", response_model=List[Task])
def get_tasks(category: Optional[str] = None):
    """获取任务列表，可按分类筛选"""
    conn = get_db()
    cursor = conn.cursor()
    
    if category:
        cursor.execute("SELECT * FROM tasks WHERE category = ? ORDER BY due_date, created_at", (category,))
    else:
        cursor.execute("SELECT * FROM tasks ORDER BY category, due_date, created_at")
    
    rows = cursor.fetchall()
    conn.close()
    
    return [dict(row) for row in rows]

@app.get("/tasks/today", response_model=List[Task])
def get_today_tasks():
    """获取今天的任务"""
    conn = get_db()
    cursor = conn.cursor()
    today = date.today().isoformat()
    cursor.execute("SELECT * FROM tasks WHERE due_date = ? OR category = 'urgent_today' ORDER BY created_at", (today,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

@app.get("/tasks/recent", response_model=List[Task])
def get_recent_tasks():
    """获取近期重要事项（未来7天）"""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM tasks WHERE category = 'urgent_recent' OR (due_date >= date('now') AND due_date <= date('now', '+7 days')) ORDER BY due_date")
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

@app.post("/tasks", response_model=Task)
async def create_task(task: TaskCreate):
    """创建新任务"""
    conn = get_db()
    cursor = conn.cursor()
    
    now = datetime.now().isoformat()
    cursor.execute('''
        INSERT INTO tasks (title, description, category, due_date, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
    ''', (task.title, task.description, task.category, task.due_date, now, now))
    
    task_id = cursor.lastrowid
    conn.commit()
    
    # 获取创建的任务
    cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,))
    row = cursor.fetchone()
    conn.close()
    
    # 广播给所有连接的客户端
    await manager.broadcast({
        "type": "task_created",
        "data": dict(row)
    })
    
    return dict(row)

@app.put("/tasks/{task_id}", response_model=Task)
async def update_task(task_id: int, task: TaskUpdate):
    """更新任务"""
    conn = get_db()
    cursor = conn.cursor()
    
    # 获取现有数据
    cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,))
    existing = cursor.fetchone()
    if not existing:
        conn.close()
        raise HTTPException(status_code=404, detail="任务不存在")
    
    # 构建更新语句
    updates = []
    values = []
    if task.title is not None:
        updates.append("title = ?")
        values.append(task.title)
    if task.description is not None:
        updates.append("description = ?")
        values.append(task.description)
    if task.category is not None:
        updates.append("category = ?")
        values.append(task.category)
    if task.due_date is not None:
        updates.append("due_date = ?")
        values.append(task.due_date)
    if task.is_completed is not None:
        updates.append("is_completed = ?")
        values.append(task.is_completed)
    
    if updates:
        updates.append("updated_at = ?")
        values.append(datetime.now().isoformat())
        values.append(task_id)
        
        cursor.execute(f"UPDATE tasks SET {', '.join(updates)} WHERE id = ?", values)
        conn.commit()
    
    cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,))
    row = cursor.fetchone()
    conn.close()
    
    # 广播更新
    await manager.broadcast({
        "type": "task_updated",
        "data": dict(row)
    })
    
    return dict(row)

@app.delete("/tasks/{task_id}")
async def delete_task(task_id: int):
    """删除任务"""
    conn = get_db()
    cursor = conn.cursor()
    
    cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,))
    if not cursor.fetchone():
        conn.close()
        raise HTTPException(status_code=404, detail="任务不存在")
    
    cursor.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
    conn.commit()
    conn.close()
    
    # 广播删除
    await manager.broadcast({
        "type": "task_deleted",
        "data": {"id": task_id}
    })
    
    return {"message": "任务已删除", "id": task_id}

# WebSocket 路由
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            # 接收心跳或消息
            data = await websocket.receive_text()
            message = json.loads(data)
            
            if message.get("type") == "ping":
                await websocket.send_json({"type": "pong"})
            elif message.get("type") == "get_tasks":
                # 发送所有任务
                conn = get_db()
                cursor = conn.cursor()
                cursor.execute("SELECT * FROM tasks ORDER BY category, due_date")
                rows = cursor.fetchall()
                conn.close()
                await websocket.send_json({
                    "type": "tasks_list",
                    "data": [dict(row) for row in rows]
                })
                
    except WebSocketDisconnect:
        manager.disconnect(websocket)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
