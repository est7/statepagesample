package com.vone.statepage.lib.pulltorefresh


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MyPullToRefreshScreenNestedScroll(
    modifier: Modifier = Modifier,
) {
    var refreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 更高效的回调处理
    val onRefresh = remember {
        {
            if (!refreshing) {
                refreshing = true
                coroutineScope.launch {
                    try {
                        delay(1000) // 模拟网络请求
                    } finally {
                        refreshing = false
                    }
                }
            }
        }
    }

    // 自定义下拉刷新状态
    val customState = rememberPullRefreshState(
        refreshThreshold = 70.dp,
        refreshingOffset = 80.dp,
        maxDragDistance = 160.dp,
        dragMultiplier = 0.6f
    )

    RefreshLayout(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = customState,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(20) { v ->
                Column(Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                    Text(
                        text = "垂直列表项 ${v + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (v % 3 == 0) {
                        Text(
                            "嵌套横向列表:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(10) { h ->
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(80.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "H ${h + 1}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = "这是第 ${v + 1} 个垂直项目的一些描述内容。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
