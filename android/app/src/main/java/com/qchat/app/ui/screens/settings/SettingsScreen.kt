package com.qchat.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun SettingsScreen(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "设置",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            item {
                SettingItem(
                    title = "服务器配置",
                    description = "管理后端服务器连接",
                    onClick = { navController.navigate("server_config") }
                )
                HorizontalDivider()
            }

            item {
                SettingItem(
                    title = "账户设置",
                    description = "个人资料和隐私",
                    onClick = { /* 导航到账户设置 */ }
                )
                HorizontalDivider()
            }

            item {
                SettingItem(
                    title = "通知设置",
                    description = "管理消息通知",
                    onClick = { /* 导航到通知设置 */ }
                )
                HorizontalDivider()
            }

            item {
                SettingItem(
                    title = "隐私设置",
                    description = "隐私和安全选项",
                    onClick = { /* 导航到隐私设置 */ }
                )
                HorizontalDivider()
            }

            item {
                SettingItem(
                    title = "关于",
                    description = "应用版本和信息",
                    onClick = { /* 导航到关于页面 */ }
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Text(
            text = description,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
