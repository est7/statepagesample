package com.vone.statepage.lib.pulltorefresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Description: Default indicator for pull to refresh.
 *              It is using Lottie animation.
 */
@Composable
fun VoneRefreshIndicator(
    pullRefreshState: PullRefreshState,
    modifier: Modifier = Modifier,
    animatorSpec: AnimatorSpec
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(pullRefreshState.refreshThreshold)
            .padding(top = 16.dp)
    ) {
        AnimationImageLoader(
            modifier = Modifier
                .height(animatorSpec.height.dp)
                .width(animatorSpec.width.dp),
            resId = animatorSpec.resId
        )
    }
}
