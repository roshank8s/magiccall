package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect
import kotlin.math.roundToInt

/**
 * Real-time pitch shift using linear interpolation resampling.
 * pitchFactor > 1.0 = higher pitch (chipmunk/helium)
 * pitchFactor < 1.0 = lower pitch (deep/monster)
 */
class PitchShiftEffect(
    override val name: String,
    private val pitchFactor: Float
) : VoiceEffect {

    private var residualPhase: Float = 0f

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        if (pitchFactor == 1.0f) return input

        val outputLength = (input.size / pitchFactor).roundToInt()
        val output = ShortArray(outputLength)

        for (i in output.indices) {
            val srcIndex = i * pitchFactor + residualPhase
            val srcIndexInt = srcIndex.toInt()
            val fraction = srcIndex - srcIndexInt

            if (srcIndexInt >= input.size - 1) break

            val sample1 = input[srcIndexInt].toFloat()
            val sample2 = input[minOf(srcIndexInt + 1, input.size - 1)].toFloat()
            val interpolated = sample1 + fraction * (sample2 - sample1)

            output[i] = interpolated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Track the residual phase for seamless buffer transitions
        val totalRead = outputLength * pitchFactor + residualPhase
        residualPhase = totalRead - totalRead.toInt()

        return output
    }

    override fun reset() {
        residualPhase = 0f
    }
}
