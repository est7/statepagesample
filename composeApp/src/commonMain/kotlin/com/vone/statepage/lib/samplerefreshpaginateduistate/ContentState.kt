package com.vone.statepage.lib.samplerefreshpaginateduistate

/**
 * 内容状态 - 处理整体列表内容
 */
sealed class ContentState<T> {
    /**
     * 加载中状态 - 首次加载
     */
    data object Loading : ContentState<Nothing>()

    /**
     * 内容加载成功
     * @param data 列表数据
     * @param isRefreshing 是否是刷新操作
     * @param isLastPage 是否已经是最后一页
     */
    data class Success<T>(
        val data: List<T>,
        val isRefreshing: Boolean = false,
        val isLastPage: Boolean = false
    ) : ContentState<T>()

    /**
     * 内容为空
     */
    data class Empty(val message: String? = null) : ContentState<Nothing>()

    /**
     * 加载失败
     */
    data class Error<T>(
        val message: String? = null,
        val throwable: Throwable? = null,
    ) : ContentState<T>()
}