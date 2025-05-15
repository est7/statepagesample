package com.vone.statepage.lib.statepage

import androidx.compose.runtime.Composable

/**
 * 表示页面的不同UI状态
 * @param T 内容状态显示的数据类型
 */
sealed interface PageState<out T> {
    /** 加载状态 - 通常显示进度指示器 */
    object Loading : PageState<Nothing>

    /** 空状态 - 当数据存在但为空时 */
    data class Empty(val message: String? = null) : PageState<Nothing>

    /** 错误状态 - 当出现问题时 */
    data class Error(
        val errorMsg: String? = null,
        val throwable: Throwable? = null
    ) : PageState<Nothing>

    /** 内容状态 - 成功加载的数据 */
    data class Content<T>(val data: T) : PageState<T>

    /**
     * 自定义状态 - 用于标准状态未涵盖的应用特定状态
     * @param key 自定义状态类型的标识符
     * @param payload 与此状态相关的可选数据
     */
    data class Custom(
        val key: String,
        val payload: Any? = null
    ) : PageState<Nothing>

    // 状态判断便捷属性
    val isLoading: Boolean get() = this is Loading
    val isEmpty: Boolean get() = this is Empty
    val isError: Boolean get() = this is Error
    val isContent: Boolean get() = this is Content
    val isCustom: Boolean get() = this is Custom
}

// 不同页面状态渲染器的类型别名
typealias LoadingPage = @Composable () -> Unit
typealias EmptyPage = @Composable (onClick: () -> Unit) -> Unit
typealias ErrorPage = @Composable (throwable: Throwable, onClick: () -> Unit) -> Unit
typealias ContentPage<T> = @Composable (data: T) -> Unit
typealias CustomPage = @Composable (payload: Any?, onClick: () -> Unit) -> Unit
