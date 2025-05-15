package com.vone.statepage.lib.refreshablepaginatedlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vone.statepage.lib.paginatedlist.PaginatedLazyList
import com.vone.statepage.lib.pulltorefresh.VoneRefreshLayout
import com.vone.statepage.lib.pulltorefresh.rememberPullRefreshState
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginationState
import kotlinx.collections.immutable.PersistentList

/**
 * 一个可组合函数，用于显示带有下拉刷新功能的分页列表。
 * **注意** 这个Page 没有集成 state page,不能显示不同状态页面
 *
 * 此组件结合了 [VoneRefreshLayout] 下拉刷新功能和一个 [PaginatedLazyList]  用于显示支持用户滚动加载更多项的列表。
 *
 * @param items 要显示的项的列表。必须是一个 [PersistentList]。
 * @param loadState 列表的当前加载状态。请参见 [PaginationState]。
 * @param isRefreshing 列表当前是否正在刷新。
 * @param onRefresh 触发刷新时要调用的回调。
 * @param onLoadMore 需要加载更多项时要调用的回调。
 * @param refreshEnabled 是否启用下拉刷新。默认为 `true`。
 * @param refreshState 下拉刷新组件的状态。默认为 [rememberPullRefreshState]。
 * @param modifier 要应用于根布局的修饰符。
 * @param itemContent 用于渲染列表中每个项的可组合函数。
 */
@Composable
fun <T> VoneRefreshablePaginatedList(
    items: PersistentList<T>,
    loadState: PaginationState,
    isRefreshing: Boolean,
    immediateReset: Boolean = false,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    refreshEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    // 下拉刷新组件包装分页列表
    VoneRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        immediateReset = immediateReset,
        enabled = refreshEnabled,
        modifier = modifier
    ) {
        // 直接使用之前实现的PaginatedLazyList
        PaginatedLazyList(
            items = items,
            loadState = loadState,
            onLoadMore = onLoadMore,
            itemContent = itemContent
        )
    }
}
