package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DaylineSuccess
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Swipe physics and behavior constants.
 */
object SwipeTimelineDefaults {
    val ActionThreshold: Dp = 80.dp
    val MaxResistanceDistance: Dp = 60.dp
    val CornerRadius: Dp = 16.dp
    val DirectionLockSlop: Dp = 12.dp

    // Spring without overshoot (bouncy = 0) to avoid card flying into negative/opposite side
    val ResetSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Explicit direction of horizontal swipe gestures.
 */
enum class TimelineSwipeDirection {
    NONE,
    RIGHT, // Concluir / Reabrir
    LEFT   // Adiar 30m
}

/**
 * Reusable swipe container for timeline cards.
 *
 * Strict Architecture Principles:
 * 1. Complete separation of Gesture Visual State, Domain Event State, and Domain Actions.
 * 2. Visual state resets to neutral (0f, NONE, no action) upon completion or cancellation.
 * 3. Direction is strictly locked once horizontal movement exceeds DirectionLockSlop.
 * 4. Only the active direction's action layer is rendered; never flash or leak the opposite action.
 * 5. Exactly one domain action per physical gesture, guarded by single-action execution flags.
 * 6. Supports `isLocked` (e.g., during asynchronous database persistence) to prevent race conditions.
 */
@Composable
fun SwipeableTimelineItem(
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    isLocked: Boolean = false,
    enabledRight: Boolean = true,
    enabledLeft: Boolean = true,
    onSwipeRight: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    thresholdDp: Dp = SwipeTimelineDefaults.ActionThreshold,
    maxResistanceDp: Dp = SwipeTimelineDefaults.MaxResistanceDistance,
    shape: Shape = RoundedCornerShape(SwipeTimelineDefaults.CornerRadius),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = with(density) { thresholdDp.toPx() }
    val maxResistancePx = with(density) { maxResistanceDp.toPx() }
    val directionLockPx = with(density) { SwipeTimelineDefaults.DirectionLockSlop.toPx() }

    // Visual translation state (isolated purely for UI rendering)
    val offsetX = remember { Animatable(0f) }

    // Locked direction for the ongoing gesture
    var lockedDirection by remember { mutableStateOf(TimelineSwipeDirection.NONE) }
    // Whether a gesture is currently held down by the user
    var isGestureActive by remember { mutableStateOf(false) }

    // Remember updated callbacks and flags to avoid capturing stale closures
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentEnabledRight by rememberUpdatedState(enabledRight)
    val currentEnabledLeft by rememberUpdatedState(enabledLeft)
    val currentIsLocked by rememberUpdatedState(isLocked)

    // Reset visual state immediately if the item becomes locked (e.g., persistence starts)
    LaunchedEffect(isLocked) {
        if (isLocked && (offsetX.value != 0f || lockedDirection != TimelineSwipeDirection.NONE)) {
            offsetX.snapTo(0f)
            lockedDirection = TimelineSwipeDirection.NONE
            isGestureActive = false
        }
    }

    val currentOffset = offsetX.value
    val isThresholdReached = abs(currentOffset) >= thresholdPx

    Box(
        modifier = modifier
            .clip(shape)
            .pointerInput(thresholdPx, maxResistancePx, directionLockPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // If currently locked (e.g. async save in flight), ignore gesture
                    if (currentIsLocked) {
                        return@awaitEachGesture
                    }

                    var totalDx = 0f
                    var totalDy = 0f
                    var isSwipingHorizontal = false
                    var currentLockedDir = TimelineSwipeDirection.NONE
                    var actionTriggered = false
                    var hasFiredHaptic = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.isConsumed) {
                            break
                        }

                        val posChange = change.positionChange()
                        val dx = posChange.x
                        val dy = posChange.y

                        if (!isSwipingHorizontal) {
                            totalDx += dx
                            totalDy += dy
                            val touchSlop = viewConfiguration.touchSlop

                            // Vertical scroll takes precedence over horizontal gesture to preserve smooth list scrolling
                            if (abs(totalDy) > abs(totalDx) && abs(totalDy) > touchSlop) {
                                break
                            } else if (abs(totalDx) > abs(totalDy) && abs(totalDx) > directionLockPx) {
                                if (totalDx > 0 && currentEnabledRight) {
                                    isSwipingHorizontal = true
                                    currentLockedDir = TimelineSwipeDirection.RIGHT
                                    lockedDirection = TimelineSwipeDirection.RIGHT
                                    isGestureActive = true
                                    change.consume()
                                } else if (totalDx < 0 && currentEnabledLeft) {
                                    isSwipingHorizontal = true
                                    currentLockedDir = TimelineSwipeDirection.LEFT
                                    lockedDirection = TimelineSwipeDirection.LEFT
                                    isGestureActive = true
                                    change.consume()
                                } else {
                                    // Swipe attempted in a disabled direction
                                    break
                                }
                            }
                        } else {
                            // Active horizontal swipe in progress
                            change.consume()
                            totalDx += dx

                            // Strict direction lock: do not allow gesture to cross back into opposite side
                            val clampedRawOffset = when (currentLockedDir) {
                                TimelineSwipeDirection.RIGHT -> totalDx.coerceAtLeast(0f)
                                TimelineSwipeDirection.LEFT -> totalDx.coerceAtMost(0f)
                                TimelineSwipeDirection.NONE -> 0f
                            }

                            // Progressive resistance (rubber-band) once threshold is passed
                            val absVal = abs(clampedRawOffset)
                            val sgn = clampedRawOffset.sign
                            val effectiveOffset = if (absVal <= thresholdPx) {
                                clampedRawOffset
                            } else {
                                val excess = absVal - thresholdPx
                                val dampedExcess = (excess * maxResistancePx) / (excess + maxResistancePx)
                                sgn * (thresholdPx + dampedExcess)
                            }

                            // One-shot haptic feedback when crossing threshold
                            val reachedThresholdNow = abs(effectiveOffset) >= thresholdPx
                            if (reachedThresholdNow && !hasFiredHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasFiredHaptic = true
                            } else if (!reachedThresholdNow && hasFiredHaptic) {
                                hasFiredHaptic = false
                            }

                            coroutineScope.launch {
                                offsetX.snapTo(effectiveOffset)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Gesture ended (finger released or gesture cancelled)
                    if (isSwipingHorizontal) {
                        val finalOffset = offsetX.value
                        val reached = abs(finalOffset) >= thresholdPx

                        if (reached && !actionTriggered && !currentIsLocked) {
                            actionTriggered = true
                            if (currentLockedDir == TimelineSwipeDirection.RIGHT && currentEnabledRight) {
                                currentOnSwipeRight()
                            } else if (currentLockedDir == TimelineSwipeDirection.LEFT && currentEnabledLeft) {
                                currentOnSwipeLeft()
                            }
                        }

                        // Animate back to 0 with zero overshoot to prevent opposite-action flashing
                        coroutineScope.launch {
                            try {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = SwipeTimelineDefaults.ResetSpringSpec
                                )
                            } finally {
                                // Full reset of visual gesture state
                                offsetX.snapTo(0f)
                                lockedDirection = TimelineSwipeDirection.NONE
                                isGestureActive = false
                            }
                        }
                    } else {
                        // Gesture cancelled before horizontal lock
                        coroutineScope.launch {
                            offsetX.snapTo(0f)
                            lockedDirection = TimelineSwipeDirection.NONE
                            isGestureActive = false
                        }
                    }
                }
            }
    ) {
        // --- CAMADA TRASEIRA FIXA ---
        // Renderizada estritamente com base na direção bloqueada (lockedDirection).
        // Se não houver direção bloqueada ou deslocamento, nada é desenhado.
        val currentAbs = abs(currentOffset)
        val progress = (currentAbs / thresholdPx).coerceIn(0f, 1f)

        if (lockedDirection != TimelineSwipeDirection.NONE && currentAbs > 1f) {
            when (lockedDirection) {
                TimelineSwipeDirection.RIGHT -> {
                    // Right swipe action: Concluir / Reabrir
                    val alpha = (0.45f + 0.55f * progress).coerceIn(0f, 1f)
                    val bgColor = DaylineSuccess.copy(alpha = alpha)
                    val iconScale = if (isThresholdReached) 1.15f else (0.85f + 0.15f * progress)
                    val iconAlpha = (0.5f + 0.5f * progress).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(shape)
                            .background(bgColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.graphicsLayer {
                                this.alpha = iconAlpha
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.Refresh else Icons.Default.Check,
                                contentDescription = if (isCompleted) "Reabrir" else "Concluir",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) "REABRIR" else "CONCLUIR",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isThresholdReached) FontWeight.ExtraBold else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                TimelineSwipeDirection.LEFT -> {
                    // Left swipe action: Adiar 30 min
                    val alpha = (0.45f + 0.55f * progress).coerceIn(0f, 1f)
                    val bgColor = Color(0xFFD97706).copy(alpha = alpha)
                    val iconScale = if (isThresholdReached) 1.15f else (0.85f + 0.15f * progress)
                    val iconAlpha = (0.5f + 0.5f * progress).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(shape)
                            .background(bgColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.graphicsLayer {
                                this.alpha = iconAlpha
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        ) {
                            Text(
                                text = "ADIAR 30 MIN",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isThresholdReached) FontWeight.ExtraBold else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Adiar 30 minutos",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                TimelineSwipeDirection.NONE -> {
                    // Nothing rendered
                }
            }
        }

        // --- CAMADA FRONTAL MÓVEL ---
        // O card original se desloca horizontalmente com o dedo
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = currentOffset
            }
        ) {
            content()
        }
    }
}
