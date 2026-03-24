package com.qchat.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qchat.app.data.remote.api.UserApi
import com.qchat.app.data.remote.config.ServerConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

class ServerConfigViewModel(
    private val serverConfigManager: ServerConfigManager,
    private val userApi: UserApi
) : ViewModel() {

    private val _state = MutableStateFlow(ServerConfigState(
        serverUrl = serverConfigManager.getBaseUrl()
    ))
    val state: StateFlow<ServerConfigState> = _state

    fun updateServerUrl(url: String) {
        _state.value = _state.value.copy(serverUrl = url)
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                connectionTestResult = "测试中...",
                connectionTestSuccess = false
            )
            try {
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
                val retrofit = Retrofit.Builder()
                    .baseUrl(_state.value.serverUrl)
                    .addConverterFactory(json.asConverterFactory())
                    .build()
                val testApi = retrofit.create(TestApi::class.java)
                testApi.ping()
                _state.value = _state.value.copy(
                    connectionTestSuccess = true,
                    connectionTestResult = "连接成功"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connectionTestSuccess = false,
                    connectionTestResult = "连接失败: ${e.message}"
                )
            }
        }
    }

    fun saveConfig() {
        serverConfigManager.setBaseUrl(_state.value.serverUrl)
        _state.value = _state.value.copy(saveSuccess = true)
    }

    fun resetToDefault() {
        serverConfigManager.resetToDefault()
        _state.value = _state.value.copy(
            serverUrl = ServerConfigManager.DEFAULT_BASE_URL,
            saveSuccess = true
        )
    }

    data class ServerConfigState(
        val serverUrl: String,
        val connectionTestResult: String? = null,
        val connectionTestSuccess: Boolean = false,
        val saveSuccess: Boolean = false
    )

    interface TestApi {
        @retrofit2.http.GET("ping")
        suspend fun ping()
    }
}
