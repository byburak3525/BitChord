package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.thumbnailBorder
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * The transport buttons' touch target, and with it the row's height.
 *
 * 48dp because that is the floor Material puts under a touch target and Apple
 * puts at 44pt — it was 40, which is under both. It is the tallest thing in the
 * row, so honouring the floor is what takes the bar from 56dp to 64dp. That is
 * the trade and it is the right way round: a bar is a place to hit a control
 * from a pocket, and eight points of height is a cheaper thing to spend than a
 * missed skip.
 */
private val GLYPH_SLOT = 48.dp

/**
 * The play and skip glyphs themselves.
 *
 * 36 in a 48dp slot, so the glyph carries the bar and still keeps 6dp of margin
 * on every side of its target. Growing it costs no height: the slot is what the
 * row is measured by, and that is unchanged.
 */
private val GLYPH_SIZE = 36.dp

/** The spinner that stands in for the play glyph, kept in proportion to it. */
private val SPINNER_SIZE = 24.dp

/**
 * The gap between the two transport controls.
 *
 * Material asks for at least 8dp between adjacent touch targets, and these had
 * none: two [GLYPH_SLOT] boxes sharing an edge, so the boundary between "pause"
 * and "skip" was a line with nothing either side of it. What space there looked
 * to be was only the margin each glyph keeps inside its own slot, and a thumb
 * lands on a target's edge far more often than it lands on a glyph's.
 *
 * Taken from the title's width rather than the bar's height, so nothing above
 * or below it moves.
 */
private val TRANSPORT_GAP = 8.dp

/**
 * Vertical padding. With the 48dp touch targets — the tallest thing in the row
 * — it sets the bar's height at 56dp and so its pill radius at 28.
 *
 * All of what the 48dp target cost is clawed back now: the bar is the 56 it was
 * before the target grew, but the target is 48 instead of 40 and the glyph 36
 * instead of 28. This is the only height in the row nothing is measured
 * against, which is why it is the only place the trimming could come from.
 *
 * It is also the floor. Below this the row's contents would have to give up
 * height themselves, and the tallest of them is the touch target — the one
 * thing here worth keeping at its size. Shrinking the artwork buys nothing: at
 * 40dp it is already shorter than the slot beside it and sets no part of the
 * bar's height.
 */
private val ROW_PADDING_VERTICAL = 4.dp

/**
 * Horizontal padding, larger than the vertical.
 *
 * A pill's ends are semicircles, so the edge nearest the artwork is not the one
 * beside it but the one curving away above and below it. Padding the ends by
 * the vertical figure would leave the artwork touching that curve.
 *
 * 16 rather than the 12 it was: the artwork sat closer to the left curve than
 * it wanted to. There is room for it — the 40dp artwork is centred in a 48dp
 * row, so it starts 8dp down, and at that depth a 28dp radius has already
 * fallen back to 8.4dp from the edge. Both ends take it, so the skip glyph
 * clears the right curve by the same margin.
 */
private val ROW_PADDING_HORIZONTAL = 16.dp

/**
 * The artwork's corner, on the 8dp every other thumbnail in the app carries.
 *
 * It used to be 7, picked so the bar's corner could sit concentric with it.
 * A pill has no corner to be concentric with — its radius is whatever half the
 * height happens to be — so that constraint is gone and the artwork can go
 * back to matching [SongRow].
 */
private val ART_CORNER = 8.dp

/** Frosted mini player that rides just above the floating tab bar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    // percent rather than a dp figure, so the corner stays exactly half the
    // height if the row's contents ever change it — which is what keeps a pill
    // a pill instead of a rounded rectangle. Same idiom as [FloatingBottomBar]
    // directly below it, so the two shapes are the same family.
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .padding(horizontal = PAGE_GUTTER)
            .clip(shape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin(MaterialTheme.colorScheme.surface))
                },
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_PADDING_HORIZONTAL,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(ART_CORNER))
                    .thumbnailBorder(RoundedCornerShape(ART_CORNER))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isLoading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(SPINNER_SIZE),
                    )
                }
            } else {
                IconButton(onClick = onPlayPause, modifier = Modifier.size(GLYPH_SLOT)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(GLYPH_SIZE),
                    )
                }
            }
            Spacer(Modifier.width(TRANSPORT_GAP))
            IconButton(onClick = onNext, modifier = Modifier.size(GLYPH_SLOT)) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(GLYPH_SIZE),
                )
            }
        }
    }
}
