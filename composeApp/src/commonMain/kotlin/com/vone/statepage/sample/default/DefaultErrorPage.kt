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
 * 默认错误页面
 * 显示错误信息，并提供一个操作按钮（例如：点击重试）。
 * 该页面默认支持垂直滚动。
 *
 * @param modifier Modifier 应用于根 Box。
 * @param errorMsg 错误信息文本。如果为 null，则显示通用错误提示。
 * @param actionText 操作按钮上显示的文本。
 * @param onAction 当操作按钮被点击时调用的回调。
 */
@Composable
fun DefaultErrorPage(
    modifier: Modifier = Modifier,
    errorMsg: String? = null,
    actionText: String = "重试", // 默认按钮文字
    onAction: () -> Unit
) {
    val displayMessage = errorMsg ?: "发生未知错误" // 如果没有具体错误信息，显示通用提示

    Box(
        modifier = modifier
            .fillMaxSize() // 填充整个可用空间
            .verticalScroll(rememberScrollState()) // 使其内容可垂直滚动
            .padding(16.dp), // 增加一些内边距
        contentAlignment = Alignment.Center // 将内部 Column 居中
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // 可以考虑在这里添加一个错误状态的图标
            // Icon(painter = painterResource(id = R.drawable.ic_error_triangle), contentDescription = "错误状态")
            // Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "哎呀，出错了",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer, // 使用更适合错误背景的颜色
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(actionText)
            }
        }
    }
}

