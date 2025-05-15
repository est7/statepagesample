package com.vone.statepage.lib.statepage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 一个 DSL 风格的构建器，用于创建 [StatePageConfig] 实例。
 * 允许轻松自定义加载、空和错误状态的默认视图，
 * 以及定义自定义状态渲染器。
 */
class StatePageConfigBuilder {
    /** 加载状态的自定义可组合项。默认为居中的 [CircularProgressIndicator]。 */
    var loading: LoadingPage? = null

    /** 空状态的自定义可组合项。默认为简单的 "空空如也" [Text]。 */
    var empty: EmptyPage? = null

    /** 错误状态的自定义可组合项。默认为显示错误消息。 */
    var error: ErrorPage? = null

    private val _customs = mutableMapOf<String, CustomPage>()

    /**
     * 为特定状态键定义自定义渲染器。
     *
     * @param key 自定义状态的唯一标识符。
     * @param renderer 渲染此自定义状态的可组合函数。
     * 它接收一个可选的 payload 和一个用于操作的 onClick lambda。
     */
    fun custom(key: String, renderer: CustomPage) {
        _customs[key] = renderer
    }

    /**
     * 使用提供的自定义渲染器构建 [StatePageConfig]，
     * 如果未指定，则回退到默认实现。
     */
    internal fun build(): StatePageConfig = StatePageConfig(
        loading = loading ?: { DefaultLoadingIndicator() },
        empty = empty ?: { onRetry -> DefaultEmptyView(onRetry = onRetry) },
        error = error ?: { throwable, onRetry -> DefaultErrorView(throwable = throwable, onRetry = onRetry) },
        customs = _customs.toMap() // 确保将不可变映射传递给 StatePageConfig
    )
}

/**
 * 用于创建 [StatePageConfig] 的 DSL 函数。
 *
 * 示例：
 * ```
 * val myConfig = statePageConfig {
 * loading = { MyCustomLoadingIndicator() }
 * empty = { onRetry -> MyCustomEmptyView(onRetry) }
 * custom("offline") { _, onRetry -> MyOfflineView(onRetry) }
 * }
 * ```
 * @param block 使用 [StatePageConfigBuilder] 的配置块。
 * @return 构建的 [StatePageConfig]。
 */
fun statePageConfig(block: StatePageConfigBuilder.() -> Unit): StatePageConfig =
    StatePageConfigBuilder().apply(block).build()

// 默认实现（如果它们变得复杂，可以移到单独的文件中）
@Composable
private fun DefaultLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DefaultEmptyView(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂无数据", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            // Button(onClick = onRetry) { Text("刷新试试") } // 可选的重试按钮
        }
    }
}

@Composable
private fun DefaultErrorView(throwable: Throwable, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "加载失败: ${throwable.message ?: "未知错误"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
