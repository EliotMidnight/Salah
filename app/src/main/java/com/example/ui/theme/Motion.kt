package com.example.ui.theme

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

/**
 * Shared Material 3 Expressive motion language for SALAH.
 *
 * - Emphasized easing for screen-level and container motion.
 * - Springs for playful but subtle interactive feedback.
 * - One place to keep durations consistent across screens.
 */
val ExpressiveEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val ExpressiveDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
val ExpressiveAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

object ExpressiveMotion {
    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 500

    fun <T> tweenFast(): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = SHORT, easing = ExpressiveDecelerate)

    fun <T> tweenEmphasized(duration: Int = MEDIUM): androidx.compose.animation.core.AnimationSpec<T> =
        tween(durationMillis = duration, easing = ExpressiveEmphasized)

    fun <T> springBouncy(): androidx.compose.animation.core.AnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    fun <T> springSnappy(): androidx.compose.animation.core.AnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

/** Standard expand/collapse for conditional rows, banners and inline panels. */
fun expressiveExpand(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT)) +
        expandVertically(animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized))

fun expressiveCollapse(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT)) +
        shrinkVertically(animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized))

/** Screen-to-screen transitions for the top-level NavHost. */
fun expressiveEnterForward(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveDecelerate)) +
        slideInHorizontally(
            initialOffsetX = { (it * 0.12f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized)
        )

fun expressiveExitForward(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT, easing = ExpressiveAccelerate)) +
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.08f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized)
        )

fun expressiveEnterBack(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveDecelerate)) +
        slideInHorizontally(
            initialOffsetX = { -(it * 0.12f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized)
        )

fun expressiveExitBack(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT, easing = ExpressiveAccelerate)) +
        slideOutHorizontally(
            targetOffsetX = { (it * 0.08f).toInt() },
            animationSpec = tween(durationMillis = ExpressiveMotion.MEDIUM, easing = ExpressiveEmphasized)
        )
