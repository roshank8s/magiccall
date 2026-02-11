package com.magiccall.voicechanger.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioAttributes
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

/**
 * Core audio engine that captures microphone input, applies voice effects
 * in real-time, and plays back the processed audio.
 */
class AudioEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var processingJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var currentEffect: VoiceEffect? = null
    var isRunning: Boolean = false
        private set

    var onAmplitudeUpdate: ((Float) -> Unit)? = null

    private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT)
    private val bufferSize = maxOf(minBufferSize * 2, 4096)

    fun start(): Boolean {
        if (isRunning) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_IN,
                AUDIO_FORMAT,
                bufferSize
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_OUT)
                .setEncoding(AUDIO_FORMAT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()
            isRunning = true

            processingJob = scope.launch {
                processAudioLoop()
            }

            return true
        } catch (e: Exception) {
            stop()
            return false
        }
    }

    private suspend fun processAudioLoop() {
        val readBuffer = ShortArray(bufferSize / 2)

        while (isRunning && isActive) {
            val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
            if (readCount <= 0) continue

            val inputChunk = readBuffer.copyOf(readCount)

            // Calculate amplitude for UI visualization
            var maxAmplitude = 0f
            for (i in 0 until readCount) {
                val abs = kotlin.math.abs(inputChunk[i].toFloat())
                if (abs > maxAmplitude) maxAmplitude = abs
            }
            val normalizedAmplitude = maxAmplitude / Short.MAX_VALUE
            withContext(Dispatchers.Main) {
                onAmplitudeUpdate?.invoke(normalizedAmplitude)
            }

            // Apply voice effect
            val processed = currentEffect?.process(inputChunk, SAMPLE_RATE) ?: inputChunk

            // Write processed audio to speaker
            audioTrack?.write(processed, 0, processed.size)
        }
    }

    fun stop() {
        isRunning = false
        processingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}

        audioRecord = null
        audioTrack = null
        currentEffect?.reset()
    }

    fun setEffect(effect: VoiceEffect?) {
        currentEffect?.reset()
        currentEffect = effect
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
