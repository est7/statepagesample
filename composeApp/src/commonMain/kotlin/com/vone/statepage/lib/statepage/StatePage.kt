package com.vone.statepage.lib.statepage

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * 一个可组合项，用于处理不同的UI状态（加载中、空、错误、内容、自定义），
 * 并在它们之间提供交叉淡入淡出动画。
 * 此重载版本使用一个完整的 [StatePageConfig] 对象。
 *
 * @param T 内容状态的数据类型。
 * @param state 要显示的当前 [PageState]。
 * @param modifier 此可组合项的 Modifier。
 * @param overrideConfig 一个 [StatePageConfig]，用于覆盖默认或上下文提供的配置。
 * @param actions 从不同状态触发的操作（例如，重试）。
 * @param animationSpec [Crossfade] 的动画规格。
 * @param content 当状态为 [PageState.Content] 时渲染内容的可组合函数。
 */
@Composable
fun <T> StatePage(
    state: PageState<T>,
    modifier: Modifier = Modifier,
    overrideConfig: StatePageConfig? = null,
    actions: StatePageActions = remember { StatePageActions() }, // 记住默认操作
    animationSpec: FiniteAnimationSpec<Float> = tween(300), // 默认动画时长
    content: ContentPage<T>
) {
    // 使用提供的配置，如果为 null，则回退到 LocalStatePageConfig 中的配置
    val effectiveConfig = overrideConfig ?: LocalStatePageConfig.current

    StatePageContent(
        pageState = state,
        modifier = modifier,
        actions = actions,
        config = effectiveConfig,
        customContents = emptyMap(), // 此签名中没有单独的自定义覆盖
        animationSpec = animationSpec,
        content = content
    )
}

/**
 * 一个可组合项，用于处理不同的UI状态，并允许覆盖特定状态的渲染器。
 *
 * @param T 内容状态的数据类型。
 * @param pageState 要显示的当前 [PageState]。
 * @param modifier 此可组合项的 Modifier。
 * @param actions 从不同状态触发的操作。
 * @param loadingContent 加载状态的自定义可组合项。
 * @param emptyContent 空状态的自定义可组合项。
 * @param errorContent 错误状态的自定义可组合项。
 * @param customContents 自定义状态键到其可组合渲染器的映射。
 * @param animationSpec [Crossfade] 的动画规格。
 * @param content 当状态为 [PageState.Content] 时渲染内容的可组合函数。
 */
@Composable
fun <T> StatePage(
    pageState: PageState<T>,
    modifier: Modifier = Modifier,
    actions: StatePageActions = remember { StatePageActions() },
    loadingContent: LoadingPage? = null,
    emptyContent: EmptyPage? = null,
    errorContent: ErrorPage? = null,
    customContents: Map<String, CustomPage> = emptyMap(),
    animationSpec: FiniteAnimationSpec<Float> = tween(300),
    content: ContentPage<T>
) {
    val baseConfig = LocalStatePageConfig.current // 获取基础配置

    // 通过使用提供的 lambda 覆盖 baseConfig 来创建有效配置
    val effectiveConfig = remember(baseConfig, loadingContent, emptyContent, errorContent) {
        baseConfig.copy(
            loading = loadingContent ?: baseConfig.loading,
            empty = emptyContent ?: baseConfig.empty,
            error = errorContent ?: baseConfig.error
            // customContents 在下面通过合并单独处理
        )
    }
    // 合并来自配置和直接参数的 customContents。直接参数优先。
    val allCustomRenderers = remember(effectiveConfig.customs, customContents) {
        effectiveConfig.customs + customContents
    }

    StatePageContent(
        pageState = pageState,
        modifier = modifier,
        actions = actions,
        config = effectiveConfig, // 此配置已包含 loading/empty/error 的覆盖
        customContents = allCustomRenderers, // 传递合并后的自定义渲染器
        animationSpec = animationSpec,
        content = content
    )
}

/**
 * 内部可组合项，根据 [PageState] 渲染适当的UI。
 * 这是公共 StatePage 重载使用的核心渲染逻辑。
 */
@Composable
private fun <T> StatePageContent(
    pageState: PageState<T>,
    modifier: Modifier = Modifier,
    actions: StatePageActions,
    config: StatePageConfig,
    customContents: Map<String, CustomPage>,
    animationSpec: FiniteAnimationSpec<Float>,
    content: ContentPage<T>
) {
    // 只关注 PageState 的类型，而不是内容数据
    val stateTypeKey = when (pageState) {
        is PageState.Loading -> "loading"
        is PageState.Empty -> "empty"
        is PageState.Error -> "error"
        is PageState.Content -> "content" // 不包含Content的数据
        is PageState.Custom -> "custom:${pageState.key}"
    }

    Box(modifier = modifier) {
        Crossfade(
            targetState = stateTypeKey, // 使用类型键而非 pageState 本身
            animationSpec = animationSpec,
            label = "StatePageCrossfade"
        ) { targetKey ->

            when (targetKey) {
                "loading" -> config.loading()
                "empty" -> config.empty { actions.onEmptyAction() }
                "error" -> {
                    if (pageState is PageState.Error) {
                        val error = pageState.throwable ?: Exception(pageState.errorMsg ?: "未知错误")
                        config.error(error) { actions.onErrorAction(error) }
                    }
                }

                "content" -> {
                    if (pageState is PageState.Content<T>) {
                        // 内容变化不会触发 Crossfade 动画，但会更新内容
                        content(pageState.data)
                    }
                }

                else -> {
                    if (targetKey.startsWith("custom:") && pageState is PageState.Custom) {
                        val customKey = pageState.key
                        val customRenderer = customContents[customKey]
                        if (customRenderer != null) {
                            customRenderer(pageState.payload) {
                                actions.onCustomAction(customKey, pageState.payload)
                            }
                        } else {
                            Text("找不到自定义状态 '$customKey' 的渲染器")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 用于不同 [PageState] 的预览参数提供者。
 */
private class StatePagePreviewParameterProvider : PreviewParameterProvider<PageState<String>> {
    override val values = sequenceOf(
        PageState.Loading,
        PageState.Empty("这里什么都没有"),
        PageState.Error("发生了一个错误", Exception("网络请求失败")),
        PageState.Content("这是加载成功的内容!"),
        PageState.Custom("SPECIAL_OFFER", mapOf("discount" to "20%", "item" to "新用户专享"))
    )
    override val count: Int get() = values.count() // 确保 count 与 values 匹配
}

/**
 * 不同 StatePage 状态的预览。
 */
@Preview()
@Composable
private fun StatePagePreview(
    @PreviewParameter(StatePagePreviewParameterProvider::class) state: PageState<String>
) {
    // 为预览提供自定义配置的示例
    val previewConfig = statePageConfig {
        loading = { Text("自定义加载中...") }
        empty = { onRetry -> Column { Text("自定义空状态"); Button(onClick = onRetry) { Text("重试") } } }
        error = { throwable, onRetry ->
            Column {
                Text("自定义错误: ${throwable.message}"); Button(onClick = onRetry) {
                Text("重试")
            }
            }
        }
        custom("SPECIAL_OFFER") { payload, _ ->
            val data = payload as? Map<*, *> // 安全转换
            Text("特别优惠! ${data?.get("item")}: ${data?.get("discount")}")
        }
    }

    StatePage(
        state = state, overrideConfig = previewConfig, // 使用上面定义的预览配置
        content = { data -> Text("内容: $data") })
}
