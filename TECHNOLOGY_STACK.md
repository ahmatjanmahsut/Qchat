# 技术栈

## 前端

### 移动应用
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **状态管理**: Kotlin Flow + ViewModel
- **本地存储**: Room数据库
- **网络**: Retrofit + OkHttp
- **WebSocket**: OkHttp WebSocket
- **依赖注入**: Hilt

## 后端

### API服务器
- **语言**: Kotlin
- **框架**: Ktor
- **认证**: JWT (JSON Web Tokens)
- **WebSocket支持**: Ktor WebSockets
- **数据库访问**: Exposed ORM

### 数据库
- **主数据库**: PostgreSQL
- **缓存**: Redis (用于会话管理和缓存)

## 加密

### 端到端加密
- **库**: libsignal-protocol-java
- **密钥交换**: Diffie-Hellman
- **加密算法**: AES-256
- **签名算法**: Ed25519

### 安全存储
- **Android KeyStore**: 用于存储加密密钥
- **EncryptedSharedPreferences**: 用于存储敏感用户数据

## 开发运维

### 构建工具
- **前端**: Gradle
- **后端**: Gradle

### 持续集成/持续部署
- **GitHub Actions**: 用于自动化构建和测试

### 监控
- **日志**: Logback
- **应用性能监控**: New Relic或Datadog

## 测试

### 单元测试
- **前端**: JUnit + Mockito
- **后端**: JUnit + MockK

### 集成测试
- **前端**: Espresso
- **后端**: Ktor Test Engine

### 安全测试
- **静态分析**: SonarQube
- **渗透测试**: OWASP ZAP

## 第三方服务

### 推送通知
- **Firebase Cloud Messaging**: 用于推送通知

### 文件存储 (可选)
- **AWS S3**: 用于存储媒体文件

## 技术选择理由

### 前端
- **Kotlin**: 现代、简洁的语言，具有出色的Android支持
- **Jetpack Compose**: 现代Android应用的声明式UI框架
- **MVVM**: 关注点分离，代码可维护性高
- **Room数据库**: 使用SQLite的高效本地存储
- **Retrofit**: 类型安全的API调用HTTP客户端

### 后端
- **Ktor**: 轻量级、异步的API构建框架
- **PostgreSQL**: 可靠、功能丰富的关系型数据库
- **Redis**: 用于会话管理的快速内存缓存

### 加密
- **libsignal-protocol-java**: 被Signal和其他安全消息应用使用的成熟加密库
- **AES-256**: 行业标准加密算法
- **Diffie-Hellman**: 安全的密钥交换协议

### 测试
- **全面的测试策略**: 确保应用可靠性和安全性
- **自动化测试**: 减少人为错误，加快开发速度

### 开发运维
- **CI/CD pipeline**: 确保一致的构建质量
- **监控**: 提供应用性能和问题的可见性

这个技术栈为构建具有端到端加密的安全、可靠和可扩展的聊天应用提供了坚实的基础，类似于Telegram。