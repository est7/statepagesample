package com.vone.statepage.lib.statepage

/**
 * 定义可从不同页面状态触发的操作
 *
 * @param onRetry 主要操作，通常用于重试加载
 * @param onEmptyAction 空状态交互时的操作（默认为重试）
 * @param onErrorAction 错误状态交互时的操作（默认为重试）
 * @param onCustomAction 自定义状态交互时的操作（默认为重试）
 */
data class StatePageActions(
    val onRetry: () -> Unit = {},
    val onEmptyAction: () -> Unit = { onRetry() },
    val onErrorAction: (Throwable) -> Unit = { onRetry() },
    val onCustomAction: (String, Any?) -> Unit = { _, _ -> onRetry() }
)
