# Agent Assistant (Windows Java Edition)

<details>
<summary><strong>🇨🇳 中文说明 (点击展开)</strong></summary>

# Agent Assistant (Windows Java 版)

这是一个针对 Windows 优化的 Agent Assistant 工具实现，包含基于 Java 的服务端 (MCP + WebSocket) 和基于 Flutter 的客户端。

## 前置要求

- **Java JDK 17+**
- **Flutter SDK**
- **Protoc** (Protocol Buffers 编译器)

## 项目结构

- `server/`: Java Spring Boot 应用程序 (MCP 服务器 & WebSocket 服务器)
- `client/`: Flutter 应用程序 (Windows 客户端)
- `server/src/main/proto/`: 共享的 Protocol Buffers 定义

## 构建说明

### 1. 构建服务端

```bash
cd server
./gradlew shadowJar
```

注意：默认构建任务是 `build`，生成的可执行 JAR 位于 `server/build/libs/server-1.0.0.jar`。

### 2. 构建客户端

首先，生成 Protobuf Dart 代码（如果尚未生成）：

```bash
protoc --dart_out=client/lib/proto -Iserver/src/main/proto server/src/main/proto/agentassist.proto
```

然后构建 Windows 应用程序：

```bash
cd client
flutter pub get
flutter build windows
```

可执行文件将位于 `client/build/windows/runner/Release/client.exe`。

### 一键构建 (Windows)

运行根目录下的 `build_windows.bat` 脚本即可自动构建服务端和客户端。

## 使用方法

### 1. 启动服务端

你可以将服务端作为独立的 Java 应用程序运行。它同时充当 MCP Stdio 服务器（用于 AI Agent）和 WebSocket 服务器（用于客户端）。

```bash
java -jar server/build/libs/server-1.0.0.jar
```

服务端默认监听端口 **8080**。

### 2. 连接客户端

运行 Flutter 客户端：

```bash
./client/build/windows/runner/Release/client.exe
```

在客户端界面中：
1. 输入服务器 URL (默认 `ws://localhost:8080/ws`)。
2. 输入令牌 (默认 `test-token`)。
3. 点击 **Connect** (连接)。

### 3. MCP 集成 (Claude / AI Agent)

配置你的 AI Agent 将此服务器用作 MCP 工具。

**Claude Desktop 配置示例:**

```json
{
  "mcpServers": {
    "agent-assistant": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\agent-assistant-win-java\\server\\build\\libs\\server-1.0.0.jar"
      ]
    }
  }
}
```

服务器使用 MCP 协议通过 Stdio 进行通信。

## 功能特性

- **MCP 工具**:
    - `ask_question`: 通过 Flutter UI 向用户提问。
    - `task_finish`: 通知用户任务已完成。
- **实时通信**: 服务端和客户端之间基于 WebSocket 的实时更新。
- **Windows 优化**: 原生 Windows 客户端体验。

</details>

# Agent Assistant (Windows Java Edition)

This is a Windows-optimized implementation of the Agent Assistant tool, featuring a Java-based Server (MCP + WebSocket) and a Flutter-based Client.

## Prerequisites

- **Java JDK 17+**
- **Flutter SDK**
- **Protoc** (Protocol Buffers Compiler)

## Project Structure

- `server/`: Java Spring Boot Application (MCP Server & WebSocket Server)
- `client/`: Flutter Application (Windows Client)
- `server/src/main/proto/`: Shared Protocol Buffers definitions

## Build Instructions

### 1. Build Server

```bash
cd server
./gradlew shadowJar
```

Note: The default `build` task generates the executable JAR at `server/build/libs/server-1.0.0.jar`.

### 2. Build Client

First, generate the Protobuf Dart code (if not already done):

```bash
protoc --dart_out=client/lib/proto -Iserver/src/main/proto server/src/main/proto/agentassist.proto
```

Then build the Windows application:

```bash
cd client
flutter pub get
flutter build windows
```

The executable will be located in `client/build/windows/runner/Release/client.exe`.

### One-Click Build (Windows)

Run the `build_windows.bat` script in the root directory to automatically build both the server and client.

## Usage

### 1. Start the Server

You can run the server as a standalone Java application. It acts as both the MCP Stdio Server (for AI Agents) and the WebSocket Server (for the Client).

```bash
java -jar server/build/libs/server-1.0.0.jar
```

The server listens on port **8080** by default.

### 2. Connect the Client

Run the Flutter Client:

```bash
./client/build/windows/runner/Release/client.exe
```

In the client UI:
1. Enter the Server URL (default `ws://localhost:8080/ws`).
2. Enter the Token (default `test-token`).
3. Click **Connect**.

### 3. MCP Integration (Claude / AI Agent)

Configure your AI Agent to use this server as an MCP tool.

**Claude Desktop Configuration Example:**

```json
{
  "mcpServers": {
    "agent-assistant": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\agent-assistant-win-java\\server\\build\\libs\\server-1.0.0.jar"
      ]
    }
  }
}
```

The server communicates via Stdio using the MCP protocol.

## Features

- **MCP Tools**:
    - `ask_question`: Ask the user a question via the Flutter UI.
    - `task_finish`: Notify the user that a task is complete.
- **Real-time Communication**: WebSocket-based updates between Server and Client.
- **Windows Optimized**: Native Windows client experience.