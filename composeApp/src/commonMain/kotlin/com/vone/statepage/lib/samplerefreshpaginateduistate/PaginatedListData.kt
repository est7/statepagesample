package com.vone.statepage.lib.samplerefreshpaginateduistate

import kotlinx.collections.immutable.PersistentList

/**
 * 分页列表数据包装类
 * 将列表数据和分页状态封装在一起，方便在 PageState.Content 中使用
 */
data class PaginatedListData<T>(
    val items: PersistentList<T>,
    val isRefreshing: Boolean = false,
    val paginationState: PaginationState,
    val isLastPage: Boolean
)
