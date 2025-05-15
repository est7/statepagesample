package com.vone.statepage.lib.refreshablepaginatedlist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vone.statepage.lib.paginatedlist.PaginatedLazyList
import com.vone.statepage.lib.pulltorefresh.PullRefreshState
import com.vone.statepage.lib.pulltorefresh.RefreshLayout
import com.vone.statepage.lib.pulltorefresh.rememberPullRefreshState
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginationState
import kotlinx.collections.immutable.PersistentList

/**
 * 一个可组合函数，用于显示带有下拉刷新功能的分页列表。
 * 此版本使用默认的 [RefreshLayout]。
 *
 * **注意：** 此组件未与 `StatePage` 集成以显示初始加载的
 * 整页加载、空或错误状态。它仅处理分页状态（加载更多、分页错误）和刷新状态。
 *
 * @param T 列表中的项目类型。
 * @param items 要显示的项目的 [PersistentList]。
 * @param paginationState 列表的当前 [PaginationState]（用于加载更多项目）。
 * @param isRefreshing 列表当前是否通过下拉刷新正在刷新。
 * @param onRefresh 下拉刷新操作触发时调用的回调。
 * @param onLoadMore 需要加载更多项目以进行分页时调用的回调。
 * @param modifier 要应用于根布局的 [Modifier]。
 * @param refreshEnabled 是否启用下拉刷新功能。默认为 `true`。
 * @param pullRefreshState 下拉刷新机制的 [PullRefreshState]。
 * @param lazyListState 底层 [PaginatedLazyList] 的 [LazyListState]。
 * @param loadMoreThreshold 距离列表底部多少个项目时触发 `onLoadMore`。
 * @param getKey 为每个项目提供稳定键的可选 lambda 表达式，以提高 LazyColumn 性能。
 * @param itemContent 用于渲染列表中每个项目的可组合函数。
 */
@Composable
fun <T> RefreshablePaginatedList(
    items: PersistentList<T>,
    paginationState: PaginationState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    refreshEnabled: Boolean = true,
    pullRefreshState: PullRefreshState = rememberPullRefreshState(),
    lazyListState: LazyListState = rememberLazyListState(),
    loadMoreThreshold: Int = 5, // PaginatedLazyList 的默认阈值
    getKey: ((item: T) -> Any)? = null,
    itemContent: @Composable (item: T) -> Unit
) {
    RefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        enabled = refreshEnabled,
        state = pullRefreshState,
        modifier = modifier // 将主修饰符应用于 RefreshLayout
    ) {
        // PaginatedLazyList 应填充 RefreshLayout 内容槽提供的空间
        PaginatedLazyList(
            items = items,
            loadState = paginationState, // 将 paginationState 作为 loadState 传递
            onLoadMore = onLoadMore,
            lazyListState = lazyListState,
            // modifier = Modifier.fillMaxSize(), // PaginatedLazyList 通常会填充其父级
            loadMoreThreshold = loadMoreThreshold,
            getKey = getKey,
            itemContent = itemContent
        )
    }
}
