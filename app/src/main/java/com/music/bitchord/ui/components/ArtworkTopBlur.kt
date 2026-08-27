package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * How hard the band is blurred.
 *
 * Hard enough that nothing in it is legible, which is the whole mechanism: what
 * this draws is the artwork's own top band, and a *sharp* copy of it sitting
 * directly above the sharp original reads as the picture repeating. Blurred past
 * recognition it reads as light coming off the top of the cover instead, which
 * is what it is meant to be.
 */
private val BLUR_RADIUS = 28.dp

/**
 * Grown before it is blurred, so the blur's clamped edges fall outside the strip.
 *
 * A blur has nothing to sample past the edge of its own layer and fades towards
 * nothing there. At this size that shows as a dark rim down both sides; scaling
 * the blurred layer up pushes the rim off screen. The same trick the mesh
 * backdrop uses, for the same reason.
 */
private const val OVERSCAN = 1.25f

/**
 * How far the band runs past the inset, over the artwork proper, dissolving as
 * it goes.
 *
 * The band and the picture show the same pixels, one blurred and one not, so
 * there is no content mismatch to hide — but there is still a sharpness one,
 * and butted together that lands as a line where the focus changes. Ramping the
 * band's alpha to nothing across this distance turns that change into a
 * gradient. Short, because a long one starts to look like the top of the cover
 * is out of focus rather than lit.
 */
private val BLEND = 16.dp

/**
 * The artwork's top band, blurred, filling the strip behind the status bar.
 *
 * The player's banner and both detail headers begin below the status bar inset,
 * so the system's clock cannot land on a cover. That leaves the strip above them
 * showing whatever the page paints underneath — the player's mesh backdrop, a
 * detail page's wash — a colour with no relation to the artwork, and a seam
 * straight across the top of the screen.
 *
 * This replaces an earlier attempt that stretched the cover's topmost 1.5dp to
 * fill the same strip. The idea was sound and the result was not: a slice that
 * thin is a handful of pixel rows, and pulling them twenty times their height
 * turns any vertical structure near the top of a cover — a horizon, a gradient,
 * the edge of a sleeve — into hard streaks running the width of the screen.
 * Blurring a real band instead has no such failure mode, because there is
 * nothing left in it sharp enough to streak.
 *
 * Draw it *after* the artwork it sits over: the last [BLEND] of it is a
 * crossfade onto that artwork, and a crossfade only works from on top.
 */
@Composable
fun ArtworkTopBlur(
    model: Any?,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    if (height <= 0.dp) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + BLEND)
            .clipToBounds()
            .graphicsLayer {
                // The mask below erases part of what this layer drew, which it
                // can only do in a buffer of its own.
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height - BLEND.toPx(),
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // Crop centres what it keeps unless told otherwise, and a centred
            // crop of a strip this short is a band from the middle of the
            // artwork — the one part of it with nothing to do with the top.
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Anchored to the top: the band has to keep starting at the
                    // cover's first row, so the overscan grows downward only.
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = OVERSCAN
                    scaleY = OVERSCAN
                }
                // Last in the chain, so it is the inner layer and the scale
                // above applies to its result — which is what carries the
                // faded edges off screen rather than magnifying them.
                //
                // RenderEffect, so API 31+. Below that this is a no-op and the
                // band is a sharp copy of the cover's top: visible, but a
                // duplicate rather than the streaks the stretch left, and on
                // devices old enough that the blur behind every bar in this app
                // is already falling back to a scrim.
                .blur(BLUR_RADIUS),
        )
    }
}
