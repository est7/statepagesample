package com.vone.statepage.lib.pulltorefresh

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import org.jetbrains.compose.resources.DrawableResource
import statepagesample.composeapp.generated.resources.Res

/**
 * Description: Animation loader for WebP animation using Glide.
 */
@Composable
fun AnimationImageLoader(
    modifier: Modifier = Modifier, resId: DrawableResource
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(Res.getUri("drawable/webp_loading_colours.webp")).build(),
        contentDescription = "动画 WebP",
        contentScale = ContentScale.Crop,
        modifier = Modifier.width(200.dp).height(280.dp),
    )
}

data class AnimatorSpec(
    val resId: DrawableResource, val width: Int, val height: Int
)
