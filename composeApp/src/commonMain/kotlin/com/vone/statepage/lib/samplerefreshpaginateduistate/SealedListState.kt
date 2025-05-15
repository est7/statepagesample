package com.vone.statepage.lib.samplerefreshpaginateduistate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vone.statepage.lib.statepage.PageState
import kotlinx.collections.immutable.PersistentList

/**
 * 表示支持刷新和分页的列表的各种状态。
 * 此密封类有助于根据当前列表操作和数据状态管理UI渲染。
 *
 * @param T 列表中的项目类型。
 */
sealed class SealedListState<out T> {
    /** 表示列表的初始加载状态。 */
    data object Loading : SealedListState<Nothing>()

    /**
     * 表示成功加载后未找到任何项目的空状态。
     * @param message 为空状态显示的可选消息。
     */
    data class Empty(val message: String? = null) : SealedListState<Nothing>()

    /**
     * 表示数据加载成功。
     * @param items 已加载项目的 [PersistentList]。
     * @param isRefreshing 如果当前正在进行刷新操作，则为 true。
     * @param isLastPage 如果已加载所有可用项目（没有更多页面），则为 true。
     * @param paginationState 用于加载更多项目的当前 [PaginationState]。
     */
    data class Success<T>(
        val items: PersistentList<T>,
        val isRefreshing: Boolean = false,
        val isLastPage: Boolean = false,
        val paginationState: PaginationState = PaginationState.Idle,
    ) : SealedListState<T>() {
        /**
         * 判断是否可以通过分页加载更多项目。
         * 条件：当前未刷新，不是最后一页，并且分页未在加载中。
         */
        fun canLoadMore(): Boolean = !isRefreshing && !isLastPage && paginationState != PaginationState.Loading
    }

    /**
     * 表示在数据加载或刷新期间的错误状态。
     * @param message 描述性错误消息。
     * @param throwable 导致错误的可选 [Throwable]。
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : SealedListState<Nothing>() // 更改为 Nothing，因为错误状态通常不直接保存列表数据

    /**
     * 判断是否可以启动刷新操作。
     * 通常可以刷新，除非：
     * - 列表处于其初始 [Loading] 状态。
     * - 列表处于 [Success] 状态并且已经在刷新 (`isRefreshing = true`)。
     */
    fun canRefresh(): Boolean {
        return when (this) {
            is Loading -> false // 如果已处于初始加载状态，则无法刷新
            is Success -> !this.isRefreshing // 如果未在刷新，则可以刷新
            is Error -> true // 可以尝试从错误状态刷新
            is Empty -> true // 可以尝试从空状态刷新
        }
    }
}

/**
 * 将 [SealedListState] 转换为 [PageState]，以便与通用状态处理组件一起使用。
 * 使用 `remember` 来优化 [PageState] 实例的创建，从而在基础数据未更改时防止不必要的重组。
 *
 * @param T 列表中的项目类型。
 * @return 相应的 [PageState<PaginatedListData<T>>]。
 */
@Composable
fun <T> SealedListState<T>.toPageState(): PageState<PaginatedListData<T>> {
    return when (this) {
        is SealedListState.Loading -> PageState.Loading
        is SealedListState.Empty -> {
            // 根据消息记住 PageState.Empty。
            // 如果消息是稳定的或很少更改，这将避免创建新实例。
            remember(message) { PageState.Empty(message) }
        }

        is SealedListState.Error -> {
            // 根据消息和 throwable 记住 PageState.Error。
            remember(message, throwable) { PageState.Error(message, throwable) }
        }

        is SealedListState.Success -> {
            // 创建 PaginatedListData，并根据其组成部分进行记忆。
            // 这确保了如果其输入是稳定的，PaginatedListData 也是稳定的。
            val rememberedData = remember(items, isRefreshing, paginationState, isLastPage) {
                PaginatedListData(
                    items = items,
                    isRefreshing = isRefreshing,
                    paginationState = paginationState,
                    isLastPage = isLastPage
                )
            }
            // 创建 PageState.Content，并根据稳定的 PaginatedListData 进行记忆。
            remember(rememberedData) {
                PageState.Content(rememberedData)
            }
        }
    }
}

/**
 * 将 [SealedListState] 转换为 [PageState] 的示例转换，不进行 `remember` 优化。
 * 此版本在每次调用时直接创建新的 [PageState] 实例。
 * 对于理解底层映射或在不需要 `remember` 开销的场景中很有用
 * （尽管通常 `remember` 对 Compose 性能是有益的）。
 *
 * @param T 列表中的项目类型。
 * @return 相应的 [PageState<PaginatedListData<T>>]。
 */
@Composable
fun <T> SealedListState<T>.toPageStateSample(): PageState<PaginatedListData<T>> {
    return when (this) {
        is SealedListState.Loading -> PageState.Loading
        is SealedListState.Empty -> PageState.Empty(message)
        is SealedListState.Error -> PageState.Error(message, throwable)
        is SealedListState.Success -> {
            val data = PaginatedListData(
                items = items,
                isRefreshing = isRefreshing,
                paginationState = paginationState,
                isLastPage = isLastPage
            )
            PageState.Content(data)
        }
    }
}
