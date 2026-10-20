package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import com.example.ui.theme.Glass
import com.example.ui.theme.LocalGlassEnabled
import com.example.ui.theme.LocalHighContrast
import kotlin.math.roundToInt

/**
 * The one remembered capture layer shared by the chrome.
 *
 * The scaffold records the content once and the glass surface draws the slice
 * under itself. Keeping the state separate from the drawing code is what makes
 * the opaque fallback the same layout with no capture at all, rather than a
 * second tree.
 */
@Stable
internal class GlassBackdropState internal constructor(
    internal val layer: GraphicsLayer
) {
    internal var bounds by mutableStateOf<Rect?>(null)
}

@Composable
internal fun rememberGlassBackdropState(): GlassBackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdropState(layer) }
}

internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

/**
 * The app frame that lets content run under the floating dock.
 *
 * The content Box fills the screen so a scrolling screen has something under
 * the dock to blur; the dock is overlaid, not placed in a `bottomBar` slot. If
 * glass is off the same tree is used and the dock paints opaque, so the two
 * paths share geometry, inset handling and touch targets.
 *
 * The captured region is only the dock plus enough padding for the blur kernel,
 * not the whole screen. It is recorded from the content Box, whose origin is the
 * root; the dock's [onGloballyPositioned] supplies the root-space rectangle.
 */
@Composable
internal fun GlassChromeScaffold(
    modifier: Modifier = Modifier,
    dock: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val glassEnabled = LocalGlassEnabled.current
    val backdrop = rememberGlassBackdropState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .captureGlassBackdrop(backdrop, glassEnabled)
        ) {
            content()
        }

        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = Glass.dockSideGap,
                        end = Glass.dockSideGap,
                        bottom = Glass.dockBottomGap
                    )
            ) {
                dock(
                    Modifier.onGloballyPositioned { coordinates ->
                        backdrop.bounds = coordinates.boundsInRoot()
                    }
                )
            }
        }
    }
}

/**
 * Records the content slice under the dock into [state]'s layer.
 *
 * The layer is deliberately the dock rectangle plus two blur radii: a full
 * screen layer would double the fill cost of every animated frame, while a
 * dock-sized texture is enough for the blur kernel to sample past the edge.
 * The content is drawn normally afterwards, so the recording is invisible.
 */
private fun Modifier.captureGlassBackdrop(
    state: GlassBackdropState,
    enabled: Boolean
): Modifier {
    if (!enabled) return this
    return drawWithContent {
        val bounds = state.bounds
        if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
            val pad = Glass.blurRegular.toPx() * 2f
            val width = (bounds.width + pad * 2f).roundToInt().coerceAtLeast(1)
            val height = (bounds.height + pad * 2f).roundToInt().coerceAtLeast(1)
            state.layer.record(size = IntSize(width, height)) {
                translate(
                    left = -(bounds.left - pad),
                    top = -(bounds.top - pad)
                ) {
                    this@drawWithContent.drawContent()
                }
            }
        }
        drawContent()
    }
}

/**
 * One chrome surface: the captured backdrop, the tint body, the specular edge,
 * the grain and the shadow. Every fallback decision lives here:
 *
 * - API below 31, `Glass.ENABLED = false`, Reduce Transparency, or high contrast
 *   text paint an opaque `surface` instead;
 * - high contrast also doubles the border;
 * - if the capture has not happened yet the opaque surface is painted for that
 *   frame rather than showing an unblurred, under-tinted slice.
 *
 * Only one surface may use the shared layer at a time; the app's only glass is
 * the dock. A second glass surface would need its own capture rectangle and
 * layer.
 */
@Composable
internal fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val enabled = LocalGlassEnabled.current
    val highContrast = LocalHighContrast.current
    val backdrop = LocalGlassBackdrop.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val blur = remember(enabled, highContrast, density) {
        if (enabled && !highContrast && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                BlurEffect(
                    radiusX = with(density) { Glass.blurRegular.toPx() },
                    radiusY = with(density) { Glass.blurRegular.toPx() },
                    edgeTreatment = TileMode.Clamp
                )
            }.getOrNull()?.takeIf { it.isSupported() }
        } else {
            null
        }
    }
    val saturation = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(Glass.saturation) }
        )
    }
    val active = enabled && !highContrast && blur != null
    val layer = backdrop?.layer
    SideEffect {
        layer?.renderEffect = if (active) blur else null
        layer?.colorFilter = if (active) saturation else null
    }

    val grain = remember(darkSurface) { makeGrainBitmap(darkSurface) }
    val grainBrush = remember(grain) {
        ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
    }
    val surface = MaterialTheme.colorScheme.surface
    val border = MaterialTheme.colorScheme.outline
    val highContrastBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .shadow(
                elevation = Glass.shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = Glass.shadowAlpha),
                spotColor = Color.Black.copy(alpha = Glass.shadowAlpha)
            )
            .clip(shape)
            .drawBehind {
                val backdropBounds = backdrop?.bounds
                if (active && layer != null && backdropBounds != null) {
                    val pad = Glass.blurRegular.toPx() * 2f
                    // The captured layer starts at the dock rectangle's top-left
                    // minus the blur padding, so it is drawn at -pad in local space.
                    scale(Glass.lensScale, Glass.lensScale, pivot = center) {
                        translate(left = -pad, top = -pad) {
                            drawLayer(layer)
                        }
                    }
                    drawRect(surface.copy(alpha = Glass.tintAlpha))
                    drawRect(brush = grainBrush, alpha = Glass.grainAlpha)
                } else {
                    drawRect(surface)
                }

                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    brush = if (active) {
                        Brush.linearGradient(
                            colors = listOf(Glass.edgeLight, Glass.edgeDark),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    } else {
                        SolidColor(if (highContrast) highContrastBorder else border)
                    },
                    style = Stroke(
                        width = (if (highContrast) Glass.edgeWidthHighContrast
                        else Glass.edgeWidth).toPx()
                    )
                )
            },
        content = content
    )
}

/**
 * Deterministic static grain. A seeded LCG keeps screenshot recording stable;
 * the tile is drawn at [Glass.grainAlpha] so the per-pixel alpha only shapes the
 * texture. Dark surfaces use white grain, light surfaces black.
 */
private fun makeGrainBitmap(dark: Boolean): ImageBitmap {
    val tile = Glass.grainTile
    val pixels = IntArray(tile * tile)
    var seed = 0x9E3779B9.toInt()
    val rgb = if (dark) 0x00FFFFFF else 0x00000000
    for (i in pixels.indices) {
        seed = seed * 1664525 + 1013904223
        val alpha = (seed ushr 24) and 0xFF
        pixels[i] = (alpha shl 24) or rgb
    }
    return Bitmap.createBitmap(pixels, tile, tile, Bitmap.Config.ARGB_8888).asImageBitmap()
}
