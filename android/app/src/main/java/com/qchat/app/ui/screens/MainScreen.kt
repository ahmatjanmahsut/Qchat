package com.qchat.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Qchat",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = { navController.navigate("settings") }) {
                Text("设置")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "数据库集成和数据同步功能已实现",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = """
                功能列表:
                1. Room数据库实体类 (User, Session, Message, Group, GroupMember, Contact)
                2. Room DAO接口
                3. Room数据库类和迁移脚本
                4. Repository层
                5. 后端PostgreSQL数据库实体和DAO
                6. 数据同步服务 (SyncService)
                7. 冲突解决策略 (Last Write Wins)
                8. 消息历史同步功能
                9. Redis缓存服务
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "服务器配置",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "已实现动态服务器地址配置功能",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "- 支持自定义服务器地址和端口",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "- 支持HTTP/HTTPS协议",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "- 连接测试功能",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "- 配置持久化存储",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
