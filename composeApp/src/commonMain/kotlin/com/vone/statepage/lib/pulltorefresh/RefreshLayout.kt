package com.vone.statepage.lib.pulltorefresh

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.vone.statepage.lib.extensions.Log
import com.vone.statepage.lib.extensions.d
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.roundToInt

/* ---------- 全局常量 ---------- */

private const val TAG = "PullToRefresh"

// 将此值设为 true 以启用详细日志记录
private const val DEBUG_LOG = true

// 设置常量
private const val DEFAULT_DRAG_MULTIPLIER = 0.5f
private val DEFAULT_REFRESH_THRESHOLD = 60.dp
private val DEFAULT_MAX_DRAG = 120.dp
private const val DEFAULT_PIN_DURATION = 200 // ms
private const val EPSILON = 0.01f // 用于浮点比较的小阈值

// 动画规格
private val SnapBackSpec = SpringSpec<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

private val RefreshFlingSpec = SpringSpec<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium
)

/* ---------- 状态枚举 ---------- */

/**
 * 下拉刷新状态枚举
 */
enum class RefreshStatus {
    Idle,         // 无交互
    Dragging,     // 正在拖拽
    Triggered,    // 到达阈值等待释放
    Refreshing,   // 刷新中
    Done;         // 刷新完成回弹中

    fun isRefreshingOrDone() = this == Refreshing || this == Done
    fun isIdle() = this == Idle
}

/* ---------- PullRefreshState ---------- */

/**
 * 下拉刷新状态管理类，负责协调下拉手势、偏移量动画和状态转换。
 */
@Stable
class PullRefreshState internal constructor(
    val refreshThreshold: Dp,
    val refreshingOffset: Dp,
    val maxDragDistance: Dp,
    val dragMultiplier: Float,
    internal val scope: CoroutineScope,
) {
    // 像素尺寸（通过 density 计算后设置）
    internal var refreshThresholdPx = 0f
    internal var refreshingOffsetPx = 0f
    internal var maxDragDistancePx = 0f

    private val mutatorMutex = MutatorMutex()
    private val _offset = Animatable(0f)
    private val _status = MutableStateFlow(RefreshStatus.Idle)

    val offset get() = _offset.value
    val status get() = _status.value

    val progress by derivedStateOf {
        if (refreshThresholdPx <= 0f) 0f
        else (offset / refreshThresholdPx).coerceIn(0f, 1f)
    }

    internal val hasVisibleOffset: Boolean get() = offset > EPSILON

    internal suspend fun dragByRawDelta(rawDelta: Float) {
        if (status.isRefreshingOrDone() && rawDelta > 0) {
            logDebug("dragByRawDelta: IGNORING pull down. Status: $status, rawDelta: $rawDelta, currentOffset: ${offset.roundToInt()}")
            return
        }

        mutatorMutex.mutate(MutatePriority.UserInput) {
            val currentOffset = offset
            val targetOffset = if (status.isRefreshingOrDone()) {
                (currentOffset + rawDelta).coerceIn(0f, refreshingOffsetPx)
            } else {
                (currentOffset + rawDelta * dragMultiplier).coerceIn(0f, maxDragDistancePx)
            }
            logDebug("dragByRawDelta: current=$currentOffset, rawDelta=$rawDelta (multiplied: ${rawDelta * dragMultiplier}), target=$targetOffset, status=$status")
            _offset.snapTo(targetOffset)
            updateStatusBasedOnOffset("dragByRawDelta")
        }
    }

    internal suspend fun dispatchScrollDelta(delta: Float) {
        mutatorMutex.mutate(MutatePriority.UserInput) {
            val currentOffset = offset
            val newOffset = (currentOffset + delta).coerceIn(0f, maxDragDistancePx)
            logDebug("dispatchScrollDelta: current=$currentOffset, delta=$delta, newOffset=$newOffset, status=$status")
            _offset.snapTo(newOffset)
            // 在偏移量通过 dispatchScrollDelta 改变后，立即更新状态
            // 这是确保在动画被中断并立即通过用户输入将偏移量设置为0时，状态能正确转换的关键
            updateStatusBasedOnOffset("dispatchScrollDelta.afterSnap")
        }
    }

    internal suspend fun release(onRefresh: () -> Unit, velocity: Float = 0f, immediateReset: Boolean = false) {
        logDebug("release: status: $status, offset: ${offset.roundToInt()}, velocity: $velocity, immediateReset: $immediateReset")
        when (status) {
            RefreshStatus.Triggered -> {
                logDebug("release: Status is Triggered. Calling onRefresh().")
                onRefresh()

                if (immediateReset) {
                    logDebug("release: immediateReset is true. Animating to 0f immediately.")
                    animateOffsetTo(0f, SnapBackSpec, velocity, "release.immediateReset")
                    // 确保状态重置为 Idle
                    updateStatus(RefreshStatus.Idle, "release.immediateReset.forceIdle")
                }
            }

            RefreshStatus.Dragging, RefreshStatus.Idle -> {
                if (hasVisibleOffset || (status == RefreshStatus.Dragging && abs(offset) < EPSILON)) {
                    logDebug("release: Status is Dragging/Idle with visible offset or at zero. Animating to 0f.")
                    animateOffsetTo(0f, SnapBackSpec, velocity, "release.toIdle")
                } else if (status != RefreshStatus.Idle) {
                    logDebug("release: Status is $status, offset is zero, not Idle. Forcing Idle.")
                    updateStatus(RefreshStatus.Idle, "release.forceIdleAtZero")
                } else {
                    logDebug("release: Status is Idle and no visible offset. No action.")
                }
            }

            RefreshStatus.Refreshing, RefreshStatus.Done -> {
                logDebug("release: Status is Refreshing/Done. No action on release.")
            }
        }
    }

    internal suspend fun flingDuringRefresh(velocity: Float) {
        if (status != RefreshStatus.Refreshing) {
            logDebug("flingDuringRefresh: Ignored. Status is $status, not Refreshing.")
            return
        }
        val target = if (velocity < 0f) 0f else refreshingOffsetPx
        logDebug("flingDuringRefresh: Flinging. Velocity: $velocity, Target: $target")
        animateOffsetTo(
            target.coerceIn(0f, refreshingOffsetPx),
            RefreshFlingSpec,
            velocity,
            "flingDuringRefresh"
        )
    }

    internal fun syncExternalRefreshing(isRefreshingExternal: Boolean): Job = scope.launch {
        logDebug("syncExternalRefreshing: External: $isRefreshingExternal, Current Status: $status, Offset: ${offset.roundToInt()}")
        if (isRefreshingExternal) {
            handleExternalRefreshingTrue()
        } else {
            handleExternalRefreshingFalse()
        }
        logDebug("syncExternalRefreshing: Finished handling. External: $isRefreshingExternal, New Status: $status, New Offset: ${offset.roundToInt()}")
    }

    private suspend fun handleExternalRefreshingTrue() {
        logDebug("handleExternalRefreshingTrue: Entering. Current status: $status, offset: ${offset.roundToInt()}")
        if (status != RefreshStatus.Refreshing) {
            updateStatus(RefreshStatus.Refreshing, "sync.toRefreshing")
        }
        if (abs(offset - refreshingOffsetPx) > 0.1f) {
            logDebug("handleExternalRefreshingTrue: Offset not at refreshingOffsetPx (${refreshingOffsetPx.roundToInt()}). Animating.")
            animateOffsetTo(
                refreshingOffsetPx,
                tween(DEFAULT_PIN_DURATION),
                caller = "sync.pinToRefreshingOffset"
            )
        } else {
            logDebug("handleExternalRefreshingTrue: Offset already at refreshingOffsetPx. No animation needed.")
        }
    }

    private suspend fun handleExternalRefreshingFalse() {
        logDebug("handleExternalRefreshingFalse: Entering. Current status: $status, offset: ${offset.roundToInt()}")
        val initialStatus = status

        if (initialStatus == RefreshStatus.Refreshing || initialStatus == RefreshStatus.Done) {
            if (initialStatus != RefreshStatus.Done) {
                updateStatus(RefreshStatus.Done, "sync.setToDone")
            }

            if (offset > EPSILON) {
                try {
                    animateOffsetTo(0f, SnapBackSpec, caller = "sync.animateToZeroFromDone")
                } catch (e: CancellationException) {
                    logDebug(
                        "handleExternalRefreshingFalse: animateToZeroFromDone was interrupted by new mutation. Current offset: ${offset.roundToInt()}",
                        e
                    )
                    // 动画被中断，偏移量可能已被其他交互（如dispatchScrollDelta）改变。
                    // 我们将在下面的检查中处理当前偏移量。
                } catch (e: Exception) {
                    logDebug(
                        "handleExternalRefreshingFalse: animateToZeroFromDone failed with other exception. Current offset: ${offset.roundToInt()}",
                        e
                    )
                    throw e // 对于其他异常，我们可能需要重新抛出
                }
            }

            // 无论动画是否完成、被跳过或中断，我们都需要根据当前的实际偏移量来决定最终状态。
            // 关键：如果偏移量现在是0（可能是由于dispatchScrollDelta的介入），并且状态是Done，则转换为Idle。
            if (abs(offset) < EPSILON) {
                if (status == RefreshStatus.Done) { // 再次检查status，因为它可能已被dispatchScrollDelta->updateStatusBasedOnOffset更改
                    updateStatus(RefreshStatus.Idle, "sync.ensureIdleAfterRefreshFalse.offsetIsZero")
                } else if (status == RefreshStatus.Idle) {
                    logDebug("handleExternalRefreshingFalse: Offset is zero, and status is already Idle. Correct.")
                } else {
                    logDebug("handleExternalRefreshingFalse: Offset is zero, but status is ${status} (not Done or Idle). This is unexpected here.")
                }
            } else {
                logDebug("handleExternalRefreshingFalse: Offset still visible (${offset.roundToInt()}) after trying to animate to zero. Status: $status. Will remain Done.")
            }

        } else if (hasVisibleOffset) {
            logDebug("handleExternalRefreshingFalse: Status is $initialStatus (intermediate), has visible offset. Animating to 0f.")
            try {
                animateOffsetTo(0f, SnapBackSpec, caller = "sync.animateToZeroFromIntermediate")
            } catch (e: CancellationException) {
                logDebug(
                    "handleExternalRefreshingFalse: animateToZeroFromIntermediate was interrupted. Current offset: ${offset.roundToInt()}",
                    e
                )
            } catch (e: Exception) {
                logDebug(
                    "handleExternalRefreshingFalse: animateToZeroFromIntermediate failed. Current offset: ${offset.roundToInt()}",
                    e
                )
                throw e
            }
            // 动画后再次检查
            if (abs(offset) < EPSILON && status != RefreshStatus.Idle) {
                updateStatus(RefreshStatus.Idle, "sync.ensureIdleAfterIntermediate.offsetIsZero")
            }
        } else if (initialStatus != RefreshStatus.Idle) {
            logDebug("handleExternalRefreshingFalse: Status is $initialStatus, no visible offset, not Idle. Forcing Idle.")
            updateStatus(RefreshStatus.Idle, "sync.forceIdleAtZeroNoOffset")
        } else {
            logDebug("handleExternalRefreshingFalse: Status is already Idle or no relevant conditions met.")
        }
    }

    private suspend fun animateOffsetTo(
        target: Float,
        spec: AnimationSpec<Float>,
        velocity: Float = 0f,
        caller: String,
    ) {
        mutatorMutex.mutate {
            val currentOffsetAtStart = offset
            logDebug("[$caller] animateOffsetTo: Start. ${currentOffsetAtStart.roundToInt()} -> ${target.roundToInt()}, Velocity: $velocity, Status: $status")

            if (abs(currentOffsetAtStart - target) < 0.5f &&
                abs(velocity) < EPSILON &&
                abs(target - _offset.targetValue) < EPSILON
            ) {
                logDebug("[$caller] animateOffsetTo: Skipping animation, already at or very near target. Current: ${offset.roundToInt()}, Target: ${target.roundToInt()}")
                if (abs(_offset.value - target) > EPSILON) _offset.snapTo(target) // 确保精确
                ensureFinalStateCorrect(target, "$caller.skippedAnimation")
                return@mutate
            }

            try {
                _offset.animateTo(target, spec, initialVelocity = velocity)
                logDebug("[$caller] animateOffsetTo: Animation completed. Final Offset: ${offset.roundToInt()}, Status: $status")
                ensureFinalStateCorrect(target, "$caller.completed")
            } catch (e: CancellationException) {
                // 当此动画被另一个使用相同 MutatorMutex 的突变中断时，会捕获此异常。
                // 例如，用户在回弹动画期间进行了滚动操作。
                // 我们记录中断，但不重新抛出此特定异常，允许调用者（如 handleExternalRefreshingFalse）
                // 根据中断后的实际状态继续其逻辑。
                // 中断此动画的突变（例如 dispatchScrollDelta）将负责更新偏移量。
                logDebug(
                    "[$caller] animateOffsetTo: Animation interrupted by new mutation. Offset after interrupting mutation might be different: ${offset.roundToInt()}. Target was: $target",
                    e
                )
                // 注意：此时的 'offset' 可能已经反映了中断操作造成的变化。
                // 我们不再在此处调用 ensureFinalStateCorrect，因为：
                // 1. 如果是 dispatchScrollDelta 中断了我们，它会调用 updateStatusBasedOnOffset。
                // 2. 如果是 handleExternalRefreshingFalse 调用了我们，它在 animateOffsetTo 返回后有自己的检查逻辑。
            } catch (e: Exception) {
                // 对于其他类型的异常，我们应该记录并重新抛出，因为它们可能是意外的。
                logDebug(
                    "[$caller] animateOffsetTo: Animation failed with other (non-mutation) exception. Offset: ${offset.roundToInt()}",
                    e
                )
                throw e
            }
        }
    }

    private fun ensureFinalStateCorrect(target: Float, caller: String) {
        logDebug("[$caller] ensureFinalStateCorrect: Check. Target=$target, CurrentOffset=${offset.roundToInt()}, CurrentStatus=$status")
        if (target == 0f && abs(offset) < 0.5f) {
            logDebug("[$caller] ensureFinalStateCorrect: Target is 0 and offset (${offset.roundToInt()}) is close to 0.")
            // 只有当状态是 Done，或者是一个非 Refreshing/Done/Idle 的中间状态（如 Dragging）时，才重置为 Idle
            if (status == RefreshStatus.Done ||
                (!status.isRefreshingOrDone() && status != RefreshStatus.Idle)
            ) {
                logDebug("[$caller] ensureFinalStateCorrect: Conditions met to reset to Idle. Current status: $status. Changing to Idle.")
                updateStatus(RefreshStatus.Idle, "$caller.resetToIdle")
            } else {
                logDebug("[$caller] ensureFinalStateCorrect: Conditions NOT met to reset to Idle (e.g. already Idle, or Refreshing). Current status: $status.")
            }
        } else {
            logDebug("[$caller] ensureFinalStateCorrect: Target not 0 or offset (${offset.roundToInt()}) not close enough to 0. Target=$target.")
            // 如果目标不是0，并且我们不在刷新或完成状态，则根据当前偏移量更新状态。
            if (!status.isRefreshingOrDone()) {
                updateStatusBasedOnOffset("$caller.ensureFinalStateCorrect.targetNotZero")
            }
        }
    }

    private fun updateStatusBasedOnOffset(caller: String) {
        val currentLocalStatus = status
        val currentLocalOffset = offset

        logDebug("[$caller] updateStatusBasedOnOffset: Called. CurrentStatus=$currentLocalStatus, CurrentOffset=${currentLocalOffset.roundToInt()}")

        if (currentLocalStatus == RefreshStatus.Refreshing) {
            logDebug("[$caller] updateStatusBasedOnOffset: Status is Refreshing. No status change based on offset. Offset: ${currentLocalOffset.roundToInt()}")
            return
        }

        if (currentLocalStatus == RefreshStatus.Done) {
            if (abs(currentLocalOffset) < EPSILON) {
                logDebug("[$caller] updateStatusBasedOnOffset: Status is Done and offset is ~0. Transitioning to Idle.")
                updateStatus(RefreshStatus.Idle, "$caller.DoneToIdleAtZeroOffset")
            } else {
                logDebug("[$caller] updateStatusBasedOnOffset: Status is Done and offset is ${currentLocalOffset.roundToInt()} (not zero). Staying Done.")
            }
            return
        }

        val newStatus = when {
            currentLocalOffset >= refreshThresholdPx && refreshThresholdPx > 0f -> RefreshStatus.Triggered
            currentLocalOffset > EPSILON -> RefreshStatus.Dragging
            else -> RefreshStatus.Idle
        }
        updateStatus(newStatus, "$caller.updateTo_$newStatus")
    }

    internal fun updateStatus(new: RefreshStatus, who: String) {
        val old = _status.value
        if (old != new) {
            _status.value = new
            logDebug("[$who] STATUS_UPDATE: $old -> $new (Offset: ${offset.roundToInt()}, ThresholdPx: ${refreshThresholdPx.roundToInt()})")
        }
    }

    internal fun logDebug(message: String, throwable: Throwable? = null) {
        if (DEBUG_LOG) {
            if (throwable != null) {
                Log.d(
                    message = message,
                    throwable = throwable,
                    tag = TAG
                )
            } else {
                Log.d(TAG, message)
            }
        }
    }
}

/* ---------- rememberPullRefreshState ---------- */

@Composable
fun rememberPullRefreshState(
    refreshThreshold: Dp = DEFAULT_REFRESH_THRESHOLD,
    refreshingOffset: Dp = refreshThreshold,
    maxDragDistance: Dp = DEFAULT_MAX_DRAG,
    dragMultiplier: Float = DEFAULT_DRAG_MULTIPLIER,
): PullRefreshState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val state = remember(scope, refreshThreshold, refreshingOffset, maxDragDistance, dragMultiplier) {
        PullRefreshState(
            refreshThreshold,
            refreshingOffset,
            maxDragDistance,
            dragMultiplier,
            scope
        )
    }

    LaunchedEffect(density, refreshThreshold, refreshingOffset, maxDragDistance) {
        with(density) {
            state.refreshThresholdPx = refreshThreshold.toPx()
            state.refreshingOffsetPx = refreshingOffset.toPx()
            state.maxDragDistancePx = maxDragDistance.toPx().coerceAtLeast(state.refreshingOffsetPx)
        }
        state.logDebug(
            "Pixel values updated: Threshold=${state.refreshThresholdPx}, " +
                    "RefreshingOffset=${state.refreshingOffsetPx}, MaxDrag=${state.maxDragDistancePx}"
        )
    }
    return state
}

/* ---------- NestedScrollConnection ---------- */

private class PullRefreshNestedScrollConnection(
    private val state: PullRefreshState,
    private val onRefresh: () -> Unit,
    private val enabled: Boolean,
    private val immediateReset: Boolean = false
) : NestedScrollConnection {

    private fun logNested(message: String) {
        state.logDebug("[NestedScroll] $message")
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        logNested("onPreScroll: available.y=${available.y}, source=$source, enabled=$enabled, state.status=${state.status}, state.offset=${state.offset.roundToInt()}, state.hasVisibleOffset=${state.hasVisibleOffset}")
        if (!enabled || source != NestedScrollSource.UserInput || available.y == 0f) {
            logNested("onPreScroll: Bailing out (enabled=$enabled, source=$source, available.y=${available.y})")
            return Offset.Zero
        }

        if (available.y < 0 && state.hasVisibleOffset) {
            val currentOffset = state.offset
            val consumedY = available.y.coerceAtLeast(-currentOffset)
            logNested("onPreScroll: Scrolling UP. currentOffset=$currentOffset, available.y=${available.y}, consumedY=$consumedY")

            if (abs(consumedY) > EPSILON) {
                logNested("onPreScroll: Consuming delta $consumedY. Launching dispatchScrollDelta.")
                // 使用 state.scope.launch 确保在正确的协程上下文中执行
                state.scope.launch { state.dispatchScrollDelta(consumedY) }
            }
            return Offset(0f, consumedY)
        }
        logNested("onPreScroll: No conditions met to consume scroll.")
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        logNested("onPostScroll: consumed.y=${consumed.y}, available.y=${available.y}, source=$source, enabled=$enabled, state.status=${state.status}, state.offset=${state.offset.roundToInt()}")
        if (!enabled || source != NestedScrollSource.UserInput || available.y == 0f) {
            logNested("onPostScroll: Bailing out (enabled=$enabled, source=$source, available.y=${available.y})")
            return Offset.Zero
        }

        if (available.y > 0) {
            logNested("onPostScroll: Pulling DOWN. available.y=${available.y}")
            if (state.status == RefreshStatus.Refreshing) {
                val currentOffset = state.offset
                val targetOffset = (currentOffset + available.y).coerceAtMost(state.refreshingOffsetPx)
                val consumedDelta = targetOffset - currentOffset
                logNested("onPostScroll (Refreshing): currentOffset=$currentOffset, targetOffset=$targetOffset, consumedDelta=$consumedDelta")

                if (abs(consumedDelta) > EPSILON) {
                    logNested("onPostScroll (Refreshing): Consuming $consumedDelta. Launching dispatchScrollDelta.")
                    state.scope.launch { state.dispatchScrollDelta(consumedDelta) }
                }
                return Offset(0f, consumedDelta)
            } else {
                logNested("onPostScroll (Not Refreshing): available.y=${available.y}. Launching dragByRawDelta.")
                state.scope.launch { state.dragByRawDelta(available.y) }
                return Offset(0f, available.y)
            }
        }
        logNested("onPostScroll: Not pulling down or no conditions met.")
        return Offset.Zero
    }


    override suspend fun onPreFling(available: Velocity): Velocity {
        logNested("onPreFling: available.y=${available.y.roundToInt()}, enabled=$enabled, state.status=${state.status}, state.offset=${state.offset.roundToInt()}, state.hasVisibleOffset=${state.hasVisibleOffset}")
        if (!enabled) {
            logNested("onPreFling: Bailing out (disabled)")
            return Velocity.Zero
        }

        if (state.status == RefreshStatus.Refreshing) {
            logNested("onPreFling: Flinging during refresh. Velocity: ${available.y.roundToInt()}")
            state.scope.launch { state.flingDuringRefresh(available.y) }
            return available
        }

        if (state.hasVisibleOffset) {
            logNested("onPreFling: Has visible offset (${state.offset.roundToInt()}). Releasing with velocity: ${available.y.roundToInt()}")
            state.release(onRefresh, available.y, immediateReset)
            return available
        }
        logNested("onPreFling: No conditions met to handle pre-fling.")
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        logNested("onPostFling: consumed.y=${consumed.y.roundToInt()}, available.y=${available.y.roundToInt()}, enabled=$enabled, state.status=${state.status}, state.offset=${state.offset.roundToInt()}, state.hasVisibleOffset=${state.hasVisibleOffset}")

        if (!enabled) {
            logNested("onPostFling: Bailing out (disabled)")
            return Velocity.Zero
        }
        if (state.hasVisibleOffset && available.y != 0f) {
            logNested("onPostFling: Has visible offset and remaining fling velocity. Releasing. Velocity: ${available.y.roundToInt()}")
            state.release(onRefresh, available.y, immediateReset)
            return available
        }
        logNested("onPostFling: No conditions met to handle post-fling.")
        return Velocity.Zero
    }
}

/**
 * 默认刷新指示器
 */
@Composable
fun DefaultRefreshIndicator(state: PullRefreshState) {
    val status = state.status
    val progress = state.progress

    Crossfade(
        targetState = status,
        animationSpec = tween(durationMillis = 150),
        label = "RefreshIndicatorCrossfade"
    ) { currentStatus ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(state.refreshThreshold)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val indicatorSize = 28.dp
            val strokeWidth = 2.5.dp

            if (DEBUG_LOG && (state.offset > 0.01f || state.status.isRefreshingOrDone())) {
                Log.d(
                    TAG,
                    "[Indicator] Rendering for status: $currentStatus, progress: $progress, offset: ${state.offset.roundToInt()}"
                )
            }

            when (currentStatus) {
                RefreshStatus.Idle -> {
                    if (state.offset > EPSILON && !state.status.isRefreshingOrDone()) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(indicatorSize),
                            strokeWidth = strokeWidth,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                RefreshStatus.Dragging -> {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(indicatorSize),
                        strokeWidth = strokeWidth,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = (progress * 0.5f + 0.5f).coerceIn(0.5f, 1f)
                        )
                    )
                }

                RefreshStatus.Triggered -> {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(indicatorSize),
                        strokeWidth = strokeWidth,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                RefreshStatus.Refreshing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(indicatorSize),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                RefreshStatus.Done -> {
                    if (state.offset > EPSILON) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(indicatorSize),
                            strokeWidth = strokeWidth,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = progress)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 下拉刷新布局组件
 */
@Composable
fun RefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    immediateReset: Boolean = false, // 控制是否立即重置
    state: PullRefreshState = rememberPullRefreshState(),
    indicator: @Composable (PullRefreshState) -> Unit = { DefaultRefreshIndicator(it) },
    content: @Composable () -> Unit
) {
    // 记录上一次的刷新状态，用于检测状态变化
    val previousIsRefreshing = remember { mutableStateOf(isRefreshing) }

    LaunchedEffect(isRefreshing, immediateReset) {
        state.logDebug("RefreshLayout: LaunchedEffect for isRefreshing=$isRefreshing, immediateReset=$immediateReset. Current state.status=${state.status}")

        // 当isRefreshing从true变为false时，即使immediateReset=true也需要处理
        if (previousIsRefreshing.value && !isRefreshing) {
            state.logDebug("RefreshLayout: isRefreshing changed from true to false, ensuring proper animation")
            state.syncExternalRefreshing(false)
        }
        // 当immediateReset=false或isRefreshing=true时，正常同步状态
        else if (!immediateReset || isRefreshing) {
            state.syncExternalRefreshing(isRefreshing)
        }

        // 更新前一个状态记录
        previousIsRefreshing.value = isRefreshing
    }

    val nestedScrollConnection = remember(state, onRefresh, enabled, immediateReset) {
        state.logDebug("RefreshLayout: Creating/Re-remembering PullRefreshNestedScrollConnection. enabled=$enabled, immediateReset=$immediateReset")
        PullRefreshNestedScrollConnection(state, onRefresh, enabled, immediateReset)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(connection = nestedScrollConnection)
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val currentOffset = state.offset
                    val indicatorHeightPx = state.refreshThresholdPx.coerceAtLeast(1f)
                    alpha = if (state.status.isRefreshingOrDone()) 1f
                    else (currentOffset / indicatorHeightPx).coerceIn(0f, 1f)
                    translationY = currentOffset - indicatorHeightPx
                }
        ) {
            if (state.offset > 0.01f || state.status.isRefreshingOrDone()) {
                if (DEBUG_LOG) state.logDebug("[IndicatorBox] Composing indicator. Offset: ${state.offset.roundToInt()}, Status: ${state.status}")
                indicator(state)
            } else {
                if (DEBUG_LOG) state.logDebug("[IndicatorBox] Indicator NOT composed. Offset: ${state.offset.roundToInt()}, Status: ${state.status}")
            }
        }

        Box(
            modifier = Modifier.graphicsLayer {
                translationY = state.offset
            }
        ) {
            content()
        }
    }
}
