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
- **环境变量配置** - 敏感信息通过环境变量管理，避免硬编码
- **JWT 安全验证** - 完整的令牌验证机制，包括过期时间和颁发者检查
- **日志脱敏** - 用户敏感信息哈希化后再记录

### 最近的更新

✅ **2024-03-25 安全更新**:
- 修复了配置文件中的硬编码密钥问题
- 使用标准 RFC 5869 实现 HKDF 密钥派生
- 修复 X3DH 实现中的安全漏洞
- 加强 JWT 验证机制
- 优化 CORS 配置

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

# 1. 配置环境变量
# 复制环境变量模板
cp .env.example .env
# 编辑 .env 文件，设置你的数据库密码和 JWT 密钥

# 或者在命令行中设置环境变量
export DB_URL="jdbc:postgresql://localhost:5432/qchat"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_secure_password"
export JWT_SECRET="your_random_generated_secret_at_least_32_chars"
export CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:8080"

# 2. 生成 JWT Secret（推荐）
# macOS/Linux:
openssl rand -base64 32
# Windows (PowerShell):
# [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])

# 3. 构建并运行
./gradlew build
./gradlew run
```

### 环境变量配置说明

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `DB_URL` | PostgreSQL 数据库连接 URL | `jdbc:postgresql://localhost:5432/qchat` |
| `DB_USERNAME` | 数据库用户名 | `postgres` |
| `DB_PASSWORD` | **数据库密码（必须修改）** | - |
| `JWT_SECRET` | **JWT 签名密钥（必须修改）** | - |
| `CORS_ALLOWED_ORIGINS` | 允许的跨域来源 | `http://localhost:3000,http://localhost:8080` |
| `REDIS_HOST` | Redis 服务器地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |

⚠️ **安全警告**：
- 生产环境必须修改 `DB_PASSWORD` 和 `JWT_SECRET`
- JWT Secret 应至少 32 个字符，建议使用 `openssl rand -base64 32` 生成
- 不要将 `.env` 文件提交到版本控制

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
