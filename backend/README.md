# 日程管理后端

## 安装依赖

```bash
pip install fastapi uvicorn websockets
```

## 运行服务

```bash
python main.py
```

服务将在 `http://0.0.0.0:8080` 启动

## API 接口

### 获取所有任务
```
GET /tasks
GET /tasks?category=urgent_recent
```

### 获取今天任务
```
GET /tasks/today
```

### 获取近期任务
```
GET /tasks/recent
```

### 创建任务
```
POST /tasks
{
    "title": "任务标题",
    "description": "任务描述",
    "category": "urgent_recent",  // urgent_recent, urgent_today, normal
    "due_date": "2026-03-15"
}
```

### 更新任务
```
PUT /tasks/{id}
{
    "title": "新标题",
    "is_completed": 1
}
```

### 删除任务
```
DELETE /tasks/{id}
```

### WebSocket 实时推送
```
ws://localhost:8080/ws
```

## 任务分类

- `urgent_recent` - 近期重要事项
- `urgent_today` - 当天重要事项
- `normal` - 普通待办

