package com.magiccall.voicechanger.audio.effects

import com.magiccall.voicechanger.audio.VoiceEffect

/**
 * Simple reverb effect using multiple comb filters (Schroeder reverb).
 * Creates a sense of space and room acoustics.
 */
class ReverbEffect(
    private val roomSize: Float = 0.6f,
    private val wetMix: Float = 0.3f
) : VoiceEffect {

    override val name: String = "Reverb"

    private val combDelays = intArrayOf(1116, 1188, 1277, 1356)
    private val combBuffers = Array(combDelays.size) { FloatArray(combDelays[it]) }
    private val combIndices = IntArray(combDelays.size)

    override fun process(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)

        for (i in input.indices) {
            val dry = input[i].toFloat() / Short.MAX_VALUE

            var wet = 0f
            for (c in combDelays.indices) {
                val buf = combBuffers[c]
                val idx = combIndices[c]

                val delayed = buf[idx]
                val newVal = dry + delayed * roomSize
                buf[idx] = newVal

                combIndices[c] = (idx + 1) % buf.size
                wet += delayed
            }

            wet /= combDelays.size

            val mixed = (dry * (1f - wetMix) + wet * wetMix) * Short.MAX_VALUE
            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    override fun reset() {
        for (i in combBuffers.indices) {
            combBuffers[i].fill(0f)
            combIndices[i] = 0
        }
    }
}
