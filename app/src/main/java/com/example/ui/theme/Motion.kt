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
 *
 * Compose has no first-class "the user wants less motion" flag, but Android does:
 * the accessibility setting *Remove animations* (and the developer option
 * *Animator duration scale = 0*) is exposed as
 * [Settings.Global.ANIMATOR_DURATION_SCALE], which is 0 when animations are off.
 *
 * Read once per composition, published through [SalahReduceMotion.Local], and
 * mirrored into [Motion.reduced] so the non-composable helpers below can collapse
 * to an instant change.
 *
 * Continuous motion - the living sky, the syncing spinner - is the part that
 * actually matters here. Large-area, slow-moving patterns are exactly what
 * triggers vestibular symptoms, so [continuousPhase] is what the sky and the
 * spinner should drive: when motion is reduced it returns a fixed value and the
 * animation never starts at all.
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
 * The motion language: short, calm, and communicative.
 *
 * Durations dropped from 200/350/500ms to 120/200/280ms. Motion here exists to
 * show where a thing came from and to confirm an action landed - nothing more.
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
     * A 0..1 phase for decorative continuous motion.
     *
     * Returns a permanently fixed value when motion is reduced, so callers can
     * keep reading a phase unconditionally and the underlying infinite animation
     * is never even created.
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

    fun <T> settle(): androidx.compose.animation.core.AnimationSpec<T> =
        if (reduced) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
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
