package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect

/**
 * Echo/delay effect using a circular delay buffer.
 * Creates repeating delayed copies of the audio signal.
 */
class EchoEffect(
    private val delayMs: Int = 250,
    private val decay: Float = 0.5f,
    private val mix: Float = 0.4f
) : VoiceEffect {

    override val name: String = "Echo"
    private var delayBuffer: FloatArray? = null
    private var writePos: Int = 0

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        val delaySamples = (sampleRate * delayMs / 1000)
        if (delayBuffer == null || delayBuffer!!.size != delaySamples) {
            delayBuffer = FloatArray(delaySamples)
            writePos = 0
        }

        val buffer = delayBuffer!!
        val output = ShortArray(input.size)

        for (i in input.indices) {
            val dry = input[i].toFloat()
            val delayed = buffer[writePos]

            val wet = dry + delayed * decay
            buffer[writePos] = wet

            writePos = (writePos + 1) % buffer.size

            val mixed = dry * (1f - mix) + delayed * mix
            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    override fun reset() {
        delayBuffer = null
        writePos = 0
    }
}
