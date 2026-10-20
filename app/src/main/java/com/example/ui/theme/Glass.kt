package com.example.ui.theme

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The chrome material.
 *
 * Glass is chrome only: the floating dock uses it; page content, cards, rows,
 * fields and sheets stay opaque. That is why this token set has no
 * content-surface variants - a call site cannot accidentally make a card glass.
 *
 * The stack is backdrop blur + saturation + a tint body + a specular edge +
 * grain + a soft shadow. The blurred backdrop is captured by
 * [com.example.ui.components.GlassChromeScaffold]; `docs/glass-chrome.md` records
 * the layer anatomy and the platform fallbacks.
 *
 * [tintAlpha] is deliberately higher than the iOS 26 reference range. A 10-18%
 * white film over a black backdrop leaves dark labels at roughly 2:1, and blur
 * cannot recover contrast that the tint never had. 64% is the lowest round value
 * that keeps `onSurface` text above 4.5:1 over either a white or a black
 * backdrop (`GlassContrastTest`) while still letting blurred shapes read
 * through.
 */
object Glass {

    /** Build-time switch. `false` ships the opaque chrome with the same layout. */
    const val ENABLED = true

    val blurRegular = 24.dp
    const val saturation = 1.6f
    const val tintAlpha = 0.64f

    val edgeWidth = 1.dp
    val edgeWidthHighContrast = 2.dp
    val edgeLight = Color.White.copy(alpha = 0.70f)
    val edgeDark = Color.Black.copy(alpha = 0.22f)

    /** Static 3.5% grain; it breaks banding and never animates. */
    const val grainAlpha = 0.035f
    const val grainTile = 128

    val shadowElevation = 20.dp
    const val shadowAlpha = 0.16f

    /** A 1.5% magnification of the captured slice approximates lensing. */
    const val lensScale = 1.015f

    val dockHeight = 64.dp
    val dockMaxWidth = 420.dp
    val dockSideGap = 20.dp
    val dockBottomGap = 12.dp
    val dockHorizontalPadding = 8.dp
    val indicatorWidth = 56.dp
    val indicatorHeight = 32.dp
    val indicatorGap = 4.dp
    val iconSize = 22.dp

    /**
     * At or above this font scale the dock drops labels and keeps the names in
     * semantics, rather than ellipsising every destination.
     */
    const val labelsAtFontScaleBelow = 1.4f

    /** True when this build and platform can render the material at all. */
    val capable: Boolean
        get() = glassEnabled(
            buildEnabled = ENABLED,
            sdkInt = Build.VERSION.SDK_INT,
            reduceTransparency = false,
            highContrast = false
        )
}

/**
 * The single gate. A glass surface may only be drawn when the build allows it,
 * the platform can blur, and neither accessibility override has asked for the
 * opaque path.
 */
internal fun glassEnabled(
    buildEnabled: Boolean,
    sdkInt: Int,
    reduceTransparency: Boolean,
    highContrast: Boolean
): Boolean = buildEnabled &&
    sdkInt >= Build.VERSION_CODES.S &&
    !reduceTransparency &&
    !highContrast

/**
 * Whether this composition may use glass. It defaults to `false`, so a preview
 * or a test that has not opted in gets the opaque path; the app root is the one
 * place that supplies the runtime answer. No glass call site decides this.
 */
val LocalGlassEnabled = staticCompositionLocalOf { false }

/**
 * Android's "high contrast text" setting. When it is on, the material is
 * replaced by an opaque surface with a stronger border; [GlassSurface] owns that
 * fallback so the dock does not have to remember it.
 */
val LocalHighContrast = staticCompositionLocalOf { false }
