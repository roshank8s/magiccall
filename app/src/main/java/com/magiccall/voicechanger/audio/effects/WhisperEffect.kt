package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect
import kotlin.math.abs
import kotlin.random.Random

/**
 * Whisper effect - replaces voiced components with noise-modulated whisper.
 * Creates an ASMR-like whispering voice.
 */
class WhisperEffect(
    private val noiseLevel: Float = 0.6f
) : VoiceEffect {

    override val name: String = "Whisper"

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)

        for (i in input.indices) {
            val sample = input[i].toFloat()
            val envelope = abs(sample) / Short.MAX_VALUE
            val noise = (Random.nextFloat() * 2f - 1f) * Short.MAX_VALUE
            val whisper = noise * envelope * noiseLevel + sample * (1f - noiseLevel) * 0.3f
            output[i] = whisper.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    override fun reset() {}
}
