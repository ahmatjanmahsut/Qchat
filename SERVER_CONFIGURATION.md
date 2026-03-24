# 后端服务器地址管理方案

## 问题分析

当前Qchat应用的后端服务器地址是硬编码在`NetworkModule.kt`中的：

```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/" // Android模拟器访问本地后端
```

这种硬编码方式存在以下问题：
- 后端服务器地址或端口变更时需要修改代码并重新编译
- 无法适应不同环境（开发、测试、生产）
- 无法在应用内动态切换服务器
- 不支持非标准端口（如不是80或443）

## 解决方案

### 1. 服务器地址配置存储

使用`EncryptedSharedPreferences`存储服务器地址配置，确保安全性：

- **服务器地址**：完整的URL（如`http://example.com:8080`）
- **端口**：可独立配置（如果需要）
- **协议**：HTTP/HTTPS选择
- **是否使用默认端口**：自动检测80/443

### 2. 配置界面

在应用设置中添加服务器配置界面：

- **服务器地址输入**：支持完整URL输入
- **端口输入**：可选，默认从URL中提取
- **协议选择**：HTTP/HTTPS切换
- **连接测试**：验证服务器可达性
- **保存配置**：持久化存储

### 3. 动态Base URL设置

修改`NetworkModule`，使用动态Base URL：

1. 创建`ServerConfigManager`单例类
2. 在`NetworkModule`中使用动态Base URL
3. 支持运行时更新Base URL

### 4. 验证服务器连接

添加服务器连接验证功能：

- **Ping测试**：发送简单请求验证服务器响应
- **证书验证**：HTTPS证书有效性检查
- **响应时间**：测量连接速度
- **错误处理**：提供清晰的错误提示

### 5. 缓存和默认配置

- **默认配置**：内置默认服务器地址
- **配置缓存**：内存缓存，避免频繁读取存储
- **配置版本**：支持配置版本管理
- **配置重置**：提供重置到默认配置的选项

### 6. 网络安全考虑

- **证书固定**：支持SSL证书固定
- **HTTPS强制**：生产环境强制使用HTTPS
- **网络安全配置**：Android Network Security Config
- **权限控制**：服务器配置需要身份验证

## 实现步骤

### 步骤1：创建服务器配置存储

```kotlin
class ServerConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences = EncryptedSharedPreferences.create(
        "server_config",
        "master_key",
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getBaseUrl(): String {
        return preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        preferences.edit().putString(KEY_BASE_URL, url).apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"
    }
}
```

### 步骤2：修改NetworkModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient, 
        json: Json,
        serverConfigManager: ServerConfigManager
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(serverConfigManager.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory())
            .build()
    }

    // 其他提供方法...
}
```

### 步骤3：创建服务器配置界面

```kotlin
@Composable
fun ServerConfigScreen(
    viewModel: ServerConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("服务器配置", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { viewModel.updateServerUrl(it) },
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.testConnection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("测试连接")
        }

        if (state.connectionTestResult != null) {
            Text(
                text = state.connectionTestResult!!,
                color = if (state.connectionTestSuccess) Color.Green else Color.Red
            )
        }

        Button(
            onClick = { viewModel.saveConfig() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存配置")
        }
    }
}
```

### 步骤4：创建服务器配置ViewModel

```kotlin
class ServerConfigViewModel @Inject constructor(
    private val serverConfigManager: ServerConfigManager,
    private val apiService: UserApi
) : ViewModel() {

    private val _state = MutableStateFlow(ServerConfigState(
        serverUrl = serverConfigManager.getBaseUrl()
    ))
    val state = _state.asStateFlow()

    fun updateServerUrl(url: String) {
        _state.update { it.copy(serverUrl = url) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(connectionTestResult = "测试中...") }
            try {
                // 临时创建Retrofit实例测试连接
                val retrofit = Retrofit.Builder()
                    .baseUrl(state.value.serverUrl)
                    .build()
                val testApi = retrofit.create(TestApi::class.java)
                testApi.ping()
                _state.update { 
                    it.copy(
                        connectionTestSuccess = true,
                        connectionTestResult = "连接成功"
                    ) 
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        connectionTestSuccess = false,
                        connectionTestResult = "连接失败: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun saveConfig() {
        serverConfigManager.setBaseUrl(state.value.serverUrl)
        _state.update { it.copy(saveSuccess = true) }
    }

    data class ServerConfigState(
        val serverUrl: String,
        val connectionTestResult: String? = null,
        val connectionTestSuccess: Boolean = false,
        val saveSuccess: Boolean = false
    )
}

interface TestApi {
    @GET("ping")
    suspend fun ping()
}
```

### 步骤5：添加服务器配置到设置菜单

在应用的设置界面中添加服务器配置选项，只有经过身份验证的用户才能访问。

### 步骤6：添加网络安全配置

创建`network_security_config.xml`文件，配置网络安全设置：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### 步骤7：更新应用清单

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
    ...
</application>
```

## 测试计划

1. **功能测试**：
   - 验证默认服务器地址
   - 测试服务器地址修改
   - 测试连接验证功能
   - 测试配置持久化

2. **场景测试**：
   - 正常服务器地址
   - 错误服务器地址
   - 不同端口配置
   - HTTP/HTTPS切换

3. **边界测试**：
   - 空服务器地址
   - 格式错误的URL
   - 网络不可达情况
   - 超时情况

## 部署策略

1. **开发环境**：使用默认地址（10.0.2.2:8080）
2. **测试环境**：使用测试服务器地址
3. **生产环境**：使用生产服务器地址，强制HTTPS

## 安全考虑

- **配置加密**：使用EncryptedSharedPreferences存储配置
- **权限控制**：服务器配置需要用户身份验证
- **输入验证**：验证服务器地址格式
- **连接安全**：HTTPS优先，支持证书验证

## 结论

通过此方案，Qchat应用将具备以下能力：
- 动态配置后端服务器地址和端口
- 支持非标准端口（非80/443）
- 实时验证服务器连接
- 配置持久化存储
- 适应不同环境需求
- 保持网络安全

此方案完全解决了后端服务器地址变更的问题，同时提供了良好的用户体验和安全性。