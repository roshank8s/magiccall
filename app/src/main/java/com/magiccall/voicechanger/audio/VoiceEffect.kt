package com.magiccall.voicechanger.audio

interface VoiceEffect {
    val name: String
    fun process(input: ShortArray, sampleRate: Int): ShortArray
    fun reset()
}
