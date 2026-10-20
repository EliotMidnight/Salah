package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Reduced-motion support.
 */
object SalahReduceMotion {

    /** True when the system animation scale is zeroed. Fails safe to "motion on". */
    fun isEnabled(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)

    @Composable
    fun remember(): Boolean {
        val context = LocalContext.current
        return remember(context) { isEnabled(context) }
    }

    val Local: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }
}

/**
 * The motion language: short, calm, spring-driven. One spring family across
 * press / toggle / selection, so every control feels like it belongs to the
 * same system.
 *
 * - Press: fast scale-down to ~0.97, no overshoot.
 * - Release / commit: spring with gentle overshoot (damping ratio high enough
 *   that the bounce is felt, not seen).
 * - Micro-feedback ≤ 250 ms; transitions ≤ 400 ms.
 * - Only transform and opacity animate - never layout, text or colour.
 * - Reduced motion: clean cross-fade fallback, durations collapse to 0.
 */
object Motion {

    const val MICRO = 120
    const val SHORT = 200
    const val MEDIUM = 280
    const val LONG = 400

    /** Mirrors [SalahReduceMotion.Local]; set once per composition from the root. */
    internal var reduced: Boolean = false

    /** Duration in ms, collapsed to 0 when motion is reduced. */
    fun duration(millis: Int): Int = if (reduced) 0 else millis

    /**
     * The one spring family. Medium stiffness, high damping - the overshoot is
     * gentle enough to feel like feedback, not like a toy.
     */
    fun spring(): androidx.compose.animation.core.AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )

    /** A quicker spring for micro-interactions (press, toggle). */
    fun springQuick(): androidx.compose.animation.core.AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )

    /** A slower spring for sheet/menu entrance. */
    fun springEmphasized(): androidx.compose.animation.core.AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    /** Cross-fade fallback when motion is reduced. */
    fun crossFade(): androidx.compose.animation.core.AnimationSpec<Float> =
        tween(durationMillis = duration(SHORT), easing = LinearEasing)

    /**
     * A 0..1 phase for decorative continuous motion.
     */
    @Composable
    fun continuousPhase(
        durationMillis: Int = 8_000,
        initialPhase: Float = 0f
    ): State<Float> {
        if (reduced) return remember(initialPhase) { mutableFloatStateOf(initialPhase) }
        val transition = rememberInfiniteTransition(label = "continuous")
        return transition.animateFloat(
            initialValue = initialPhase,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    }

    /** Whether decorative, endlessly-repeating motion may start at all. */
    val allowContinuous: Boolean get() = !reduced

    fun <T> quick(): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration(MICRO), easing = Decelerate)

    fun <T> standard(): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration(SHORT), easing = Decelerate)

    fun <T> emphasized(): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration(MEDIUM), easing = Decelerate)

    fun settle(): androidx.compose.animation.core.AnimationSpec<Float> =
        if (reduced) {
            tween(durationMillis = 0)
        } else {
            spring()
        }
}

private fun mutableFloatStateOf(value: Float): androidx.compose.runtime.MutableFloatState =
    androidx.compose.runtime.mutableFloatStateOf(value)

/** Decelerate: quick to start, gentle to land. The default for everything. */
val Decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

/** Accelerate: used only for exits, where leaving should feel immediate. */
val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Top-level screen change. A short cross-fade with a small lateral offset. */
fun screenEnterForward(): EnterTransition =
    fadeIn(tween(durationMillis = Motion.duration(Motion.SHORT), easing = Decelerate)) +
        slideInHorizontally(
            initialOffsetX = { (it * 0.06f).toInt() },
            animationSpec = tween(durationMillis = Motion.duration(Motion.SHORT), easing = Decelerate)
        )

fun screenExitForward(): ExitTransition =
    fadeOut(tween(durationMillis = Motion.duration(Motion.MICRO), easing = Accelerate))

fun screenEnterBack(): EnterTransition =
    fadeIn(tween(durationMillis = Motion.duration(Motion.SHORT), easing = Decelerate)) +
        slideInHorizontally(
            initialOffsetX = { -(it * 0.06f).toInt() },
            animationSpec = tween(durationMillis = Motion.duration(Motion.SHORT), easing = Decelerate)
        )

fun screenExitBack(): ExitTransition =
    fadeOut(tween(durationMillis = Motion.duration(Motion.MICRO), easing = Accelerate)) +
        slideOutHorizontally(
            targetOffsetX = { (it * 0.06f).toInt() },
            animationSpec = tween(durationMillis = Motion.duration(Motion.MICRO), easing = Decelerate)
        )

/**
 * Press feedback: scale down to [pressScale] on press, spring back on release.
 * Only transform animates - never layout, text or colour.
 *
 * Usage:
 *   var pressed by remember { mutableStateOf(false) }
 *   val scale by animateFloatAsState(
 *       targetValue = if (pressed) Motion.pressScale else 1f,
 *       animationSpec = Motion.springQuick()
 *   )
 *   Modifier.scale(scale).pointerInput(Unit) {
 *       detectTapGestures(onPress = { pressed = true }, onRelease = { pressed = false })
 *   }
 */
val pressScale = 0.97f
