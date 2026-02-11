package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect
import kotlin.math.PI
import kotlin.math.sin

/**
 * Robot voice effect using ring modulation.
 * Multiplies the audio signal by a sine wave carrier frequency
 * to produce a metallic, robotic sound.
 */
class RobotVoiceEffect(
    private val carrierFrequency: Float = 150f
) : VoiceEffect {

    override val name: String = "Robot"
    private var phase: Double = 0.0

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        val phaseIncrement = 2.0 * PI * carrierFrequency / sampleRate

        for (i in input.indices) {
            val modulator = sin(phase).toFloat()
            val sample = (input[i] * modulator).toInt()
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
