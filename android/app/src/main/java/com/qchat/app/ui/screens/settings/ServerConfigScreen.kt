package com.qchat.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ServerConfigScreen() {
    val viewModel: ServerConfigViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "服务器配置",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { viewModel.updateServerUrl(it) },
            label = { Text("服务器地址") },
            placeholder = { Text("例如: http://example.com:8080") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.testConnection() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("测试连接")
        }

        if (state.connectionTestResult != null) {
            Text(
                text = state.connectionTestResult,
                color = if (state.connectionTestSuccess) Color.Green else Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { viewModel.saveConfig() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("保存配置")
        }

        TextButton(
            onClick = { viewModel.resetToDefault() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置为默认配置")
        }

        if (state.saveSuccess) {
            Text(
                text = "配置已保存",
                color = Color.Green,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
