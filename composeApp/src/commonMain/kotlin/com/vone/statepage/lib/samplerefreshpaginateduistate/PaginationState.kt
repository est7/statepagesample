package com.vone.statepage.lib.samplerefreshpaginateduistate

/**
 * 分页状态 - 处理加载更多的状态
 */
sealed class PaginationState {
    /**
     * 空闲状态 - 没有进行分页加载
     */
    data object Idle : PaginationState()

    /**
     * 加载更多中
     */
    data object Loading : PaginationState()

    /**
     * 加载更多失败
     */
    data class Error(
        val message: String = "load more failed",
        val throwable: Throwable? = null
    ) : PaginationState()

    /**
     * 已加载全部数据
     */
    data object Complete : PaginationState()
}