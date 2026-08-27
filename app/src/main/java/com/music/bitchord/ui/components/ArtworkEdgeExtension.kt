package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * How much of the artwork's top edge is stretched to fill the strip.
 *
 * Thin, and thinner than it first was. Only the *vertical* detail of the slice
 * is thrown away by the stretch — every row keeps its full width, so a cover
 * that is black on one side and lit on the other extends as both however thin
 * the slice gets. What thickness costs is the match at the bottom of the strip,
 * where the smear has to meet the picture's own first row: the thicker the
 * slice, the further from that row the smear ends, and the more there is for
 * [EDGE_BLEND] to hide. It was 4dp, which was enough to show.
 */
private val EDGE_SAMPLE = 1.5.dp

/**
 * How far the strip runs past the inset, over the artwork proper, dissolving as
 * it goes.
 *
 * The smear and the picture cannot meet exactly. The strip's last row is the
 * artwork at [EDGE_SAMPLE] down, the picture's first row is the artwork at
 * zero, and on a cover with any vertical structure in that gap — a horizon, a
 * gradient, the edge of a sleeve — the mismatch lands as a hard line straight
 * across the screen. Shrinking [EDGE_SAMPLE] narrows the gap but never closes
 * it.
 *
 * So the two are crossfaded instead of butted together: the strip is drawn over
 * the picture and its alpha ramps to nothing across this distance, which turns
 * the join from an edge into a blend. The picture loses nothing — what covers
 * its first few dp is a smear of those same dp.
 */
private val EDGE_BLEND = 14.dp

/**
 * The artwork's top edge, smeared upward to fill the status bar's strip.
 *
 * Both the player's banner and a detail page's header now begin below the
 * status bar inset, so the system's clock cannot land on top of the picture.
 * That leaves the strip above them showing whatever the page paints underneath
 * — the player's mesh backdrop, a detail page's wash — which is a colour with
 * no relation to the artwork and reads as a hard seam straight across the top
 * of the screen.
 *
 * Filling it with the artwork's own top edge closes the seam without giving the
 * inset back: the picture still starts below it, but the colour above it is the
 * colour the picture starts with.
 *
 * A stretch rather than a second crop of the top band, which is the distinction
 * that makes it work — a band would repeat content the artwork below is already
 * showing, and the repeat is more obvious than the seam it replaced.
 *
 * Draw it *after* the artwork it extends: the last [EDGE_BLEND] of it is a
 * crossfade onto that artwork, and a crossfade only works from on top.
 */
@Composable
fun ArtworkEdgeExtension(
    model: Any?,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    if (height <= 0.dp) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + EDGE_BLEND)
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
                        startY = size.height - EDGE_BLEND.toPx(),
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
            // artwork — the one part of it with nothing to do with the edge
            // being continued.
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Anchored to the top, so what grows is the edge rather
                    // than the middle of the picture. The strip shows the
                    // artwork's top band; pulling that band by this much leaves
                    // only its first [EDGE_SAMPLE] on screen, over the whole
                    // strip.
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    val sample = EDGE_SAMPLE.toPx()
                    scaleY = if (sample > 0f) {
                        (size.height / sample).coerceAtLeast(1f)
                    } else {
                        1f
                    }
                },
        )
    }
}
