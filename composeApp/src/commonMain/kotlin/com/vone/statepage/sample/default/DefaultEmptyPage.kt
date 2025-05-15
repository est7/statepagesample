package com.vone.statepage.sample.default

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


/**
 * 默认空数据页面
 * 显示空状态信息，并提供一个操作按钮（例如：点击重试或刷新）。
 * 该页面默认支持垂直滚动。
 *
 * @param modifier Modifier 应用于根 Box。
 * @param message 空状态时显示的主要信息文本。
 * @param subMessage 空状态时显示的辅助信息文本。
 * @param actionText 操作按钮上显示的文本。
 * @param onAction 当操作按钮被点击时调用的回调。
 */
@Composable
fun DefaultEmptyPage(
    modifier: Modifier = Modifier,
    message: String = "暂无内容", // 默认提示信息
    subMessage: String = "下拉或点击按钮刷新", // 默认辅助提示
    actionText: String = "刷新", // 默认按钮文字
    onAction: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize() // 填充整个可用空间
            .verticalScroll(rememberScrollState()) // 使其内容可垂直滚动
            .padding(16.dp), // 增加一些内边距，避免内容紧贴边缘
        contentAlignment = Alignment.Center // 将内部 Column 居中
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp) // 给内容一些呼吸空间
        ) {
            // 可以考虑在这里添加一个空状态的图标
            // Icon(painter = painterResource(id = R.drawable.ic_empty_box), contentDescription = "空状态")
            // Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionText)
            }
        }
    }
}