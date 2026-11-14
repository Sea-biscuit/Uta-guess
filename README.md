# 猜歌

两套版本都在本机运行，后端需要 Go 1.22+。

## 前后端分离版

1. 将 FLAC 放在 `split/backend/music`，支持子目录。
2. 在 `split/backend` 运行 `go run .`，启动题库和音频 API，地址为 `127.0.0.1:8080`。
3. 在另一个终端进入 `split/frontend`，使用 Node.js 18+ 执行 `npm ci` 和 `npm run dev`，然后打开 http://127.0.0.1:5173。

原来位于 `backend/music` 的曲库可搬到 `split/backend/music`。分离版后端依旧从后端工作目录下的 `music` 读取。

## 整合版

页面和 Vue 运行库随 Go 程序提供，可离线启动。

在 `standalone` 目录运行 `go run .`，然后打开 http://127.0.0.1:8080
整合版从**启动时的工作目录**读取 `.\music`。在 `standalone` 内启动时，将曲库放在 `standalone/music`。

在 `standalone` 运行 `go build -o uta-guess.exe .` 可打包成单个可执行文件，页面和 Vue 已嵌入。切换到包含 `music` 的目录后再运行该程序。

两套后端默认使用同一端口，请先停止正在运行的版本再切换。添加或移除歌曲后重启服务即可重新扫描。

## 抽取曲目

输入要抽取的曲目数并点击「随机抽取」。每轮从整个曲库中抽取指定数量，同一轮内不会重复。切换顺序或乱序只调整本轮题序。页面刚打开时默认使用全部曲目。

## 许可证

项目代码采用 [MIT License](LICENSE)
