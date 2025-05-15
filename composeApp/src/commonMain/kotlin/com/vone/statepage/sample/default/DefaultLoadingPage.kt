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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * 默认加载中页面
 * 显示一个居中的圆形加载指示器。
 * 该页面默认支持垂直滚动 (尽管通常加载页内容不多，但保持一致性)。
 *
 * @param modifier Modifier 应用于根 Box。
 * @param loadingText 加载时显示的文本，例如 "加载中..."。
 */
@Composable
fun DefaultLoadingPage(
    modifier: Modifier = Modifier,
    loadingText: String = "加载中..." // 默认加载文本
) {
    Box(
        modifier = modifier
            .fillMaxSize() // 填充整个可用空间
            .verticalScroll(rememberScrollState()) // 使其内容可垂直滚动
            .padding(16.dp),
        contentAlignment = Alignment.Center // 将内容居中
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary // 使用主题色
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}