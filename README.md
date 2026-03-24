# Qchat 🔒

> 一款注重隐私的即时通讯应用，采用端到端加密技术

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-orange.svg)](https://developer.android.com/compose)

## ✨ 功能特点

- 🔒 **端到端加密** - 使用X3DH密钥协商和Double Ratchet算法，确保消息只有发送方和接收方可以阅读
- 👤 **隐私注册** - 直接注册，无需电话号码或邮箱，保护您的隐私
- 💬 **私人聊天** - 与朋友进行加密的一对一对话
- 👥 **群组聊天** - 创建和管理最多500人的加密群组对话
- 📱 **跨平台同步** - 支持多设备实时同步消息
- 🔑 **安全密钥** - 所有密钥存储在本地，永不上传服务器

## 🏗 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                      Android App                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │    UI层      │  │   业务层     │  │   数据层     │    │
│  │  Compose    │  │   ViewModel │  │   Room      │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│                           │                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │              加密服务 (libsignal)                 │   │
│  │   X3DH + Double Ratchet + AES-256-GCM           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      Ktor Server                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   API层     │  │   服务层     │  │   数据层     │    │
│  │   Routes    │  │   Services  │  │  PostgreSQL │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│                           │                              │
│                    ┌─────────────┐                      │
│                    │    Redis    │                      │
│                    │   缓存/会话  │                      │
│                    └─────────────┘                      │
└─────────────────────────────────────────────────────────┘
```

## 🛠 技术栈

### Android 应用
| 技术 | 说明 |
|------|------|
| Kotlin | 现代Android开发语言 |
| Jetpack Compose | 声明式UI框架 |
| MVVM | 架构模式 |
| Room | 本地数据库 |
| Hilt | 依赖注入 |
| Retrofit | 网络请求 |
| libsignal-protocol | 端到端加密 |

### 后端服务
| 技术 | 说明 |
|------|------|
| Kotlin | 服务器开发语言 |
| Ktor | 异步Web框架 |
| PostgreSQL | 主数据库 |
| Redis | 缓存和会话 |
| JWT | 身份认证 |

## 🔐 安全特性

- **X3DH密钥交换** - 使用Curve25519建立安全会话
- **Double Ratchet算法** - 每次消息使用新密钥（前向保密）
- **AES-256-GCM** - 军事级消息加密
- **Ed25519签名** - 消息完整性和真实性验证
- **本地密钥存储** - Android Keystore安全存储

## 📁 项目结构

```
Qchat/
├── android/                        # Android应用
│   └── app/src/main/java/com/qchat/app/
│       ├── data/                    # 数据层
│       │   ├── local/               # Room数据库
│       │   ├── remote/              # Retrofit API
│       │   ├── repository/          # Repository
│       │   └── sync/                # 数据同步
│       ├── di/                      # Hilt依赖注入
│       ├── crypto/                  # 加密服务
│       └── ui/                      # UI层
│
├── backend/                        # Ktor后端
│   └── src/main/kotlin/com/qchat/
│       ├── routes/                  # API路由
│       ├── services/                # 业务服务
│       ├── database/                # 数据库
│       └── security/                # 安全服务
│
├── docs/                            # 设计文档
│   ├── ARCHITECTURE.md
│   ├── SECURITY_IMPLEMENTATION.md
│   └── ...
│
└── README.md
```

## 🚀 快速开始

### 前置要求

- JDK 17+
- Android Studio Hedgehog+
- PostgreSQL 14+
- Redis 6+

### 运行后端

```bash
cd backend

# 设置配置文件
cp src/main/resources/application.conf.example src/main/resources/application.conf
# 编辑 application.conf 配置数据库连接

# 构建并运行
./gradlew build
./gradlew run
```

### 运行Android

```bash
cd android

# 在Android Studio中打开
# 同步Gradle项目
# 运行应用
```

## 📱 界面预览

| 聊天列表 | 聊天界面 | 群组设置 |
|:--------:|:--------:|:--------:|
| Telegram风格 | 消息气泡 | 加密标记 |

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

本项目采用 MIT 许可证

---

**Qchat** - 让隐私回归聊天本质
