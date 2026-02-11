package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tremor/vibrato amplitude modulation effect.
 * Makes the voice sound shaky or trembling.
 */
class TremorEffect(
    private val rate: Float = 6f,
    private val depth: Float = 0.5f
) : VoiceEffect {

    override val name: String = "Tremor"
    private var phase: Double = 0.0

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        val phaseIncrement = 2.0 * PI * rate / sampleRate

        for (i in input.indices) {
            val modulation = 1.0 - depth * (0.5 + 0.5 * sin(phase))
            val sample = (input[i] * modulation).toInt()
            output[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            phase += phaseIncrement
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }

        return output
    }

    override fun reset() {
        phase = 0.0
    }
}
