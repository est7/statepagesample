package com.vone.statepage.lib.paginatedlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items // 使用接受 PersistentList 的 items 重载
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vone.statepage.lib.extensions.Log
import com.vone.statepage.lib.extensions.d
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginationState
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val DEBUG_PAGINATED_LIST = false // 分页列表调试日志开关

/**
 * 一个封装了 [LazyColumn] 的可组合项，通过观察滚动位置来处理分页。
 * 当用户滚动到列表末尾附近时，它会自动调用 `onLoadMore`，
 * 前提是当前的 `loadState` 允许这样做（例如，Idle 或 Error）。
 *
 * @param T 列表中的项目类型。
 * @param items 要显示的 [PersistentList] 数据项。
 * @param loadState 数据加载过程的当前 [PaginationState]。
 * @param onLoadMore 当应加载更多数据时调用的回调。
 * @param modifier 要应用于容器 [Box] 的 [Modifier]。
 * @param lazyListState 用于控制和观察 [LazyColumn] 滚动状态的 [LazyListState]。
 * @param loadMoreThreshold 距离列表末尾多少个项目时应触发 `onLoadMore`。
 * @param errorRetryScrollThreshold 在错误状态下，通过进一步向下滚动来重试的更严格阈值。
 * @param getKey 为每个项目提供唯一且稳定键的可选函数，以增强 [LazyColumn] 性能。
 * @param itemContent 定义列表中每个项目如何渲染的可组合 lambda。
 */
@Composable
internal fun <T> PaginatedLazyList(
    items: PersistentList<T>,
    loadState: PaginationState,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    loadMoreThreshold: Int = 5, // 剩余5个项目时触发加载更多
    errorRetryScrollThreshold: Int = 2, // 错误状态下，用户再拉2个项目时触发重试
    getKey: ((item: T) -> Any)? = null,
    itemContent: @Composable (item: T) -> Unit
) {
    // 跟踪先前的加载状态，以启用特定逻辑，例如，
    // 仅当用户在出错后进一步滚动时才重新触发 onLoadMore。
    var previousLoadState by remember { mutableStateOf<PaginationState?>(null) }

    // 用于调试的状态变化日志。
    if (DEBUG_PAGINATED_LIST) {
        SideEffect { // SideEffect 用于在每次重组后执行，适合日志记录
            Log.d("PaginatedLazyList", "重组。项目数: ${items.size}, 加载状态: $loadState")
        }
    }

    // LaunchedEffect 用于监听滚动和状态变化以触发分页逻辑
    LaunchedEffect(lazyListState, loadState, items.size, loadMoreThreshold, errorRetryScrollThreshold) {
        snapshotFlow { // 将 Compose 状态转换为 Flow
            // 推导出一个布尔值，指示是否满足加载更多数据的条件。
            // 这有助于使 Flow 的发射更有意义。
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsInLayout = layoutInfo.totalItemsCount // 包括项目 + 页脚
            if (totalItemsInLayout == 0 || items.isEmpty()) return@snapshotFlow false // 没有可分页的内容

            // `items` 列表中的实际最后一个项目的索引，不包括页脚。
            // totalItemsCount 包括数据项 + 1 (页脚)。
            // 因此，items.size 是实际数据项的数量。
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1

            // 检查我们是否接近 *数据项* 的末尾。
            // 页脚位于索引 `items.size` 处。我们希望在页脚成为 *唯一* 可见内容之前触发。
            val nearEndOfDataItems = lastVisibleItemIndex >= (items.size - loadMoreThreshold)
            val furtherPullInError = lastVisibleItemIndex >= (items.size - errorRetryScrollThreshold)

            // 当前分页状态允许加载
            val canLoadBasedOnState = loadState is PaginationState.Idle ||
                    (loadState is PaginationState.Error && previousLoadState is PaginationState.Error)


            if (DEBUG_PAGINATED_LIST) {
                Log.d(
                    "PaginatedLazyList",
                    "快照: lastVisible=$lastVisibleItemIndex, totalItemsInLayout=$totalItemsInLayout, dataItems=${items.size}, nearEnd=$nearEndOfDataItems, furtherPullError=$furtherPullInError, canLoadBasedOnState=$canLoadBasedOnState, currentLoadState=$loadState"
                )
            }

            // 最终决定是否加载更多的逻辑
            (canLoadBasedOnState && nearEndOfDataItems && loadState is PaginationState.Idle) ||
                    (canLoadBasedOnState && furtherPullInError && loadState is PaginationState.Error)

        }
            .distinctUntilChanged() // 仅在触发条件实际更改时才做出反应
            .filter { shouldLoadMore -> shouldLoadMore } // 仅在 shouldLoadMore 为 true 时继续
            .collect { // 收集 Flow 发射的值
                if (DEBUG_PAGINATED_LIST) {
                    Log.d("PaginatedLazyList", "触发加载更多。当前加载状态: $loadState")
                }
                onLoadMore()
            }
    }

    // 在可能触发 onLoadMore 的 LaunchedEffect 之后更新 previousLoadState，
    // 这样 snapshotFlow 的 *下一次* 评估将看到潜在加载 *之前* 的状态。
    // 这很微妙：如果 onLoadMore 立即将 loadState 更改为 Loading，
    // previousLoadState 应反映该更改 *之前* 的状态。
    LaunchedEffect(loadState) {
        previousLoadState = loadState
    }


    Box(modifier = modifier.fillMaxSize()) { // Box 本身填充最大尺寸
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize() // LazyColumn 也填充其父 Box
        ) {
            items(
                items = items, // 直接使用 PersistentList
                key = getKey // 可选的 item key
            ) { item ->
                itemContent(item)
            }

            // 用于显示加载指示器、错误消息或“无更多数据”文本的页脚项。
            item(key = "pagination_footer") { // 为页脚项提供固定的 key
                PaginationFooter(
                    loadState = loadState,
                    onRetry = {
                        // 仅当确实是允许重试的错误状态时才调用 onLoadMore。
                        if (loadState is PaginationState.Error) {
                            if (DEBUG_PAGINATED_LIST) Log.d("PaginatedLazyList", "点击了重试。")
                            onLoadMore()
                        }
                    }
                )
            }
        }
    }
}


/**
 * 分页列表的页脚可组合项。
 * 根据当前的 [PaginationState] 显示加载指示器、带重试按钮的错误消息或“无更多数据”消息。
 */
@Composable
private fun PaginationFooter(
    loadState: PaginationState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), // 足够的内边距以确保可见性和触摸区域
        contentAlignment = Alignment.Center
    ) {
        when (loadState) {
            PaginationState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            is PaginationState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = loadState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp) // 确保消息不会过于拥挤
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        // 考虑适当地设置按钮样式
                    ) {
                        Text("重试")
                    }
                }
            }

            PaginationState.Complete -> {
                Text(
                    text = "没有更多数据了",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 使用合适的颜色
                )
            }

            PaginationState.Idle -> {
                // 空闲状态：通常，页脚不显示任何内容，
                // 或者如果需要，可以显示一个非常不显眼的指示器。
                // 目前不显示任何内容。
            }
        }
    }
}
