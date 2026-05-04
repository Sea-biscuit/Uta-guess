# 猜歌游戏

## 环境检测
1. *powershell* 下`cd`到项目目录
2. 输入以下命令：`powershell -ExecutionPolicy Bypass -File .\check-env.ps1`,如无问题则可正常启动

## 后端启动
1. 准备 JDK 17 与 Maven
2. 在 `backend` 下运行：（运行前确保已新建`backend/music`音乐文件存放到此处，目前仅支持`flac`文件）
   - `mvn spring-boot:run`
3. 默认会扫描 `backend/music` 目录下的 `.flac` 文件。

## 前端启动
1. 准备 Node.js 18+。
2. 在 `frontend` 下运行：
   - `npm install`
   - `npm run dev`
3. 浏览器打开 `http://localhost:5173`。
