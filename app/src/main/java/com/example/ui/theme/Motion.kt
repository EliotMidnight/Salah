package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Reduced-motion support.
 *
 * Compose has no first-class "the user asked for less motion" flag, but Android
 * does: the accessibility setting *Remove animations* (and the developer
 * "Animator duration scale = 0" option) is exposed as
 * `Settings.Global.ANIMATOR_DURATION_SCALE`, which is 0 when animations are off.
 *
 * We read that once per composition, publish it through [Local], and mirror it
 * into [ExpressiveMotion.reduced] so the spec helpers here - which have no
 * composable context - can collapse to an instant change.
 *
 * This matters most for the living sky: the twinkling starfield, drifting clouds
 * and sun corona run continuously, which is exactly the kind of large-area,
 * slow-moving pattern that triggers vestibular symptoms. With motion reduced the
 * sky renders the same scene, statically.
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

    /** Read once and remembered for the current composition. */
    @Composable
    fun remember(): Boolean {
        val context = LocalContext.current
        return remember(context) { isEnabled(context) }
    }

    val Local: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }
}

/**
 * Shared Material 3 Expressive motion language for SALAH.
 *
 * - Emphasized easing for screen-level and container motion.
 * - Springs for playful but subtle interactive feedback.
 * - One place to keep durations consistent across screens.
 * - Honours the system reduced-motion setting (see [NourReduceMotion]-equivalent
 *   [SalahReduceMotion] below).
 */
val ExpressiveEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val ExpressiveDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
val ExpressiveAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

object ExpressiveMotion {
    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 500

    /**
     * Mirrors [SalahReduceMotion.Local] so these non-composable helpers can still
     * honour the user's reduced-motion preference. Set once per composition from
     * the app root.
     */
    internal var reduced: Boolean = false

    /** Duration in ms, collapsed to 0 when motion is reduced. */
    fun duration(millis: Int): Int = if (reduced) 0 else millis

    /** False when decorative, endlessly-repeating motion must not start. */
    val allowContinuous: Boolean get() = !reduced

    fun <T> tweenFast(): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration(SHORT), easing = ExpressiveDecelerate)

    fun <T> tweenEmphasized(duration: Int = MEDIUM): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration(duration), easing = ExpressiveEmphasized)

    fun <T> springBouncy(): androidx.compose.animation.core.AnimationSpec<T> =
        if (reduced) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        }

    fun <T> springSnappy(): androidx.compose.animation.core.AnimationSpec<T> =
        if (reduced) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        }
}

/** Standard expand/collapse for conditional rows, banners and inline panels. */
fun expressiveExpand(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.SHORT))) +
        expandVertically(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized))

fun expressiveCollapse(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.SHORT))) +
        shrinkVertically(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized))

/** Sheet/dialog content entrance used by settings and trust sheets. */
fun expressiveFadeIn(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.SHORT), easing = ExpressiveDecelerate))

/** Screen-to-screen transitions for the top-level NavHost. */
fun expressiveEnterForward(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveDecelerate)) +
        slideInHorizontally(
            initialOffsetX = { (it * 0.12f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized)
        )

fun expressiveExitForward(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.SHORT), easing = ExpressiveAccelerate)) +
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.08f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized)
        )

fun expressiveEnterBack(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveDecelerate)) +
        slideInHorizontally(
            initialOffsetX = { -(it * 0.12f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized)
        )

fun expressiveExitBack(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.SHORT), easing = ExpressiveAccelerate)) +
        slideOutHorizontally(
            targetOffsetX = { (it * 0.08f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.duration(ExpressiveMotion.MEDIUM), easing = ExpressiveEmphasized)
        )
