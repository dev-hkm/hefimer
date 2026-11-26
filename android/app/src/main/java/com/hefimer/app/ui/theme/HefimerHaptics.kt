package com.hefimer.app.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Refined Apple Taptic-inspired haptic engine for Hefimer.
 *
 * Uses Android API 30+ hardware VibrationEffect primitives (`PRIMITIVE_LOW_TICK`, `PRIMITIVE_TICK`,
 * `PRIMITIVE_CLICK`) at whisper-soft calibrated amplitudes (0.16f..0.46f) so every interaction
 * feels crystalline, delicate, and never harsh or buzzy. Falls back cleanly on API 26-29.
 */
object HefimerHaptics {
    var enabled by mutableStateOf(true)

    @Volatile
    private var cachedVibrator: Vibrator? = null

    private fun vibrator(context: Context): Vibrator? {
        cachedVibrator?.let { return it }
        val vib = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
        cachedVibrator = vib
        return vib
    }

    private fun canVibrate(context: Context): Vibrator? {
        if (!enabled) return null
        val vib = vibrator(context) ?: return null
        return if (runCatching { vib.hasVibrator() }.getOrDefault(false)) vib else null
    }

    private fun playPrimitive(
        context: Context,
        primitiveId: Int,
        scale: Float,
        fallbackMs: Long,
        fallbackAmplitude: Int,
        view: View? = null,
    ) {
        val vib = canVibrate(context) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(primitiveId)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(primitiveId, scale.coerceIn(0.05f, 1f))
                    .compose()
                vib.vibrate(effect)
                return
            }
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                return
            }
            vib.vibrate(VibrationEffect.createOneShot(fallbackMs, fallbackAmplitude.coerceIn(1, 255)))
        }
    }

    private fun playComposition(
        context: Context,
        primitives: List<Triple<Int, Float, Int>>,
        fallbackTimings: LongArray,
        fallbackAmplitudes: IntArray,
    ) {
        val vib = canVibrate(context) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val ids = primitives.map { it.first }.toIntArray()
                if (vib.areAllPrimitivesSupported(*ids)) {
                    val comp = VibrationEffect.startComposition()
                    primitives.forEach { (id, scale, delayMs) ->
                        comp.addPrimitive(id, scale.coerceIn(0.05f, 1f), delayMs.coerceAtLeast(0))
                    }
                    vib.vibrate(comp.compose())
                    return
                }
            }
            vib.vibrate(VibrationEffect.createWaveform(fallbackTimings, fallbackAmplitudes, -1))
        }
    }

    /** Ultra-gentle glass-kiss contact on press down or logo tap. */
    fun microTouch(context: Context, view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.16f, 6L, 28, view)
        } else {
            playPrimitive(context, 0, 0.16f, 6L, 28, view)
        }
    }

    /** Feather-light detent tick when sliding across dock tabs or segmented controls. */
    fun scrubTick(context: Context, view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.22f, 7L, 38, view)
        } else {
            playPrimitive(context, 0, 0.22f, 7L, 38, view)
        }
    }

    /** Crisp whisper-soft tap on buttons, cards, PIN digits, and navigation actions. */
    fun tick(context: Context, view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_TICK, 0.28f, 9L, 46, view)
        } else {
            playPrimitive(context, 0, 0.28f, 9L, 46, view)
        }
    }

    /** Subtle edge-peel / sheet drag threshold tick. */
    fun gestureEdge(context: Context, view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.20f, 7L, 34, view)
        } else {
            playPrimitive(context, 0, 0.20f, 7L, 34, view)
        }
    }

    /** Distinct mechanical toggle: rising double-click ON, soft falling click OFF. */
    fun toggle(context: Context, checked: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checked) {
                playComposition(
                    context = context,
                    primitives = listOf(
                        Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.24f, 0),
                        Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.40f, 18),
                    ),
                    fallbackTimings = longArrayOf(0L, 7L, 18L, 10L),
                    fallbackAmplitudes = intArrayOf(0, 42, 0, 72),
                )
            } else {
                playComposition(
                    context = context,
                    primitives = listOf(
                        Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.30f, 0),
                        Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.16f, 16),
                    ),
                    fallbackTimings = longArrayOf(0L, 8L, 16L, 6L),
                    fallbackAmplitudes = intArrayOf(0, 56, 0, 28),
                )
            }
        } else {
            tick(context)
        }
    }

    /** Signature 3-blade Hefimer launch orbit cadence synced with the intro logo sweep. */
    fun launchOrbit(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playComposition(
                context = context,
                primitives = listOf(
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.16f, 0),
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.24f, 115),
                    Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.36f, 130),
                ),
                fallbackTimings = longArrayOf(0L, 6L, 115L, 8L, 130L, 10L),
                fallbackAmplitudes = intArrayOf(0, 30, 0, 45, 0, 68),
            )
        } else {
            playComposition(
                context = context,
                primitives = emptyList(),
                fallbackTimings = longArrayOf(0L, 7L, 115L, 9L, 130L, 11L),
                fallbackAmplitudes = intArrayOf(0, 32, 0, 48, 0, 68),
            )
        }
    }

    /** Soft velvety settle breath when intro dissolves into the workspace. */
    fun launchSettle(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.18f, 7L, 32)
        } else {
            playPrimitive(context, 0, 0.18f, 7L, 32)
        }
    }

    /**
     * Signature 5-digit Drop code rhythmic cadence:
     * Ascending whisper-soft pulse on each digit tile landing (0..3 LOW_TICK -> 4th final TICK).
     */
    fun codeDigitReveal(context: Context, index: Int) {
        val amp = (0.20f + index.coerceIn(0, 4) * 0.055f).coerceAtMost(0.44f)
        val fallbackAmp = (36 + index.coerceIn(0, 4) * 10).coerceAtMost(78)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val prim = if (index >= 4) {
                VibrationEffect.Composition.PRIMITIVE_TICK
            } else {
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            }
            playPrimitive(context, prim, amp, 9L, fallbackAmp)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_TICK, amp, 9L, fallbackAmp)
        } else {
            playPrimitive(context, 0, amp, 9L, fallbackAmp)
        }
    }

    /** Clean crystalline confirmation for copy-to-clipboard actions. */
    fun copyConfirm(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            playPrimitive(context, VibrationEffect.Composition.PRIMITIVE_TICK, 0.36f, 10L, 64)
        } else {
            playPrimitive(context, 0, 0.36f, 10L, 64)
        }
    }

    /** Delicate liquid-droplet pop when reacting in Chat Room. */
    fun reactionPop(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playComposition(
                context = context,
                primitives = listOf(
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.22f, 0),
                    Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.34f, 22),
                ),
                fallbackTimings = longArrayOf(0L, 7L, 22L, 9L),
                fallbackAmplitudes = intArrayOf(0, 38, 0, 60),
            )
        } else {
            tick(context)
        }
    }

    /** Refined two-beat celebratory chime for upload complete, download saved, QR scan, S3 verified. */
    fun success(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playComposition(
                context = context,
                primitives = listOf(
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.26f, 0),
                    Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.44f, 52),
                ),
                fallbackTimings = longArrayOf(0L, 8L, 52L, 12L),
                fallbackAmplitudes = intArrayOf(0, 44, 0, 82),
            )
        } else {
            playComposition(
                context = context,
                primitives = emptyList(),
                fallbackTimings = longArrayOf(0L, 8L, 52L, 12L),
                fallbackAmplitudes = intArrayOf(0, 44, 0, 82),
            )
        }
    }

    /** Polite, non-jarring double-tap notice for errors or invalid input. */
    fun softError(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playComposition(
                context = context,
                primitives = listOf(
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.32f, 0),
                    Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.26f, 74),
                ),
                fallbackTimings = longArrayOf(0L, 10L, 74L, 9L),
                fallbackAmplitudes = intArrayOf(0, 56, 0, 44),
            )
        } else {
            playComposition(
                context = context,
                primitives = emptyList(),
                fallbackTimings = longArrayOf(0L, 10L, 74L, 9L),
                fallbackAmplitudes = intArrayOf(0, 56, 0, 44),
            )
        }
    }
}
