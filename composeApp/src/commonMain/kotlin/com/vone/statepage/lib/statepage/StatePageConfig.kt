package com.vone.statepage.lib.statepage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.vone.statepage.sample.default.DefaultEmptyPage
import com.vone.statepage.sample.default.DefaultErrorPage
import com.vone.statepage.sample.default.DefaultLoadingPage

/**
 * StatePage渲染的配置
 * 定义不同状态的渲染器
 */
data class StatePageConfig(
    val loading: LoadingPage,
    val empty: EmptyPage,
    val error: ErrorPage,
    val customs: Map<String, CustomPage> = emptyMap()
)

/** 带有基本实现的默认配置 */
private val DefaultConfig = StatePageConfig(
    loading = { DefaultLoadingPage() },
    empty = { retry -> DefaultEmptyPage(onAction = retry) },
    error = { throwable, retry ->
        DefaultErrorPage(errorMsg = throwable.message, onAction = retry)
    }
)

/** 用于在组合层次结构中传播配置的CompositionLocal */
val LocalStatePageConfig = compositionLocalOf { DefaultConfig }

/**
 * 为所有后代可组合项提供自定义StatePage配置
 *
 * @param config 要提供的配置
 * @param content 配置应可用的内容
 */
@Composable
fun ProvideStatePageConfig(
    config: StatePageConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalStatePageConfig provides config) {
        content()
    }
}
