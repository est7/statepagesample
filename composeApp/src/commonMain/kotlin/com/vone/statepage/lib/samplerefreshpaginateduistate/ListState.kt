package com.vone.statepage.lib.samplerefreshpaginateduistate

/**
 * 列表加载状态 - 分离内容状态和分页状态
 */
data class ListState<out T>(
    val contentState: ContentState<out T> = ContentState.Loading,
    val paginationState: PaginationState = PaginationState.Idle
) {
    /**
     * 判断是否可以加载更多
     */
    fun canLoadMore(): Boolean {
        return contentState is ContentState.Success &&
                !contentState.isLastPage &&
                !contentState.isRefreshing &&
                paginationState != PaginationState.Loading
    }

    /**
     * 判断是否可以刷新
     */
    fun canRefresh(): Boolean {
        return when (contentState) {
            is ContentState.Loading -> false
            is ContentState.Success -> !contentState.isRefreshing
            else -> true
        }
    }
}