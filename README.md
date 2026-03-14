# 日程管理 App

本地运行的日程管理应用，支持实时同步。

## 项目结构

```
schedule-app/
├── backend/          # Python FastAPI 后端
│   ├── main.py       # 主服务
│   └── README.md     # 后端说明
└── android/          # Android App
    └── app/          # App 模块
```

## 快速开始

### 1. 启动后端服务

在 NAS 上执行：

```bash
cd schedule-app/backend
pip install fastapi uvicorn websockets
python main.py
```

服务将在 `http://0.0.0.0:8080` 启动

### 2. 修改 Android App 的服务器地址

打开 `android/app/src/main/java/com/heqinan/schedule/data/RetrofitClient.kt` 和 `WebSocketManager.kt`，
把 IP 地址 `192.168.50.163` 改成你的 NAS 实际 IP。

### 3. 构建 APK

用 Android Studio 打开 `android` 文件夹：
1. File → Open → 选择 `android` 文件夹
2. 等待 Gradle 同步完成
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK 文件在 `android/app/build/outputs/apk/debug/app-debug.apk`

### 4. 安装到手机

把 APK 传到手机安装，确保手机和 NAS 在同一 WiFi 下。

## 功能

- 📅 日程管理（近期重要 / 当天重要 / 普通）
- 🔄 实时同步（WebSocket）
- ✅ 任务完成状态
- 🗑️ 删除任务
- 📱 本地通知（可扩展）

## 配置

后端默认端口：8080
Android 需要修改的 IP：
- `RetrofitClient.kt` 中的 `BASE_URL`
- `WebSocketManager.kt` 中的 `wsUrl`

## 技术栈

- 后端：Python + FastAPI + SQLite + WebSocket
- 前端：Android (Kotlin) + Retrofit + OkHttp

