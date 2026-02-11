package com.magiccall.voicechanger.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

/**
 * Core audio engine that captures microphone input, applies voice effects
 * in real-time, and plays back the processed audio.
 *
 * Two modes:
 *   - Lab mode  (callMode=false): Uses MIC source + USAGE_MEDIA — for Voice Lab preview
 *   - Call mode (callMode=true):  Uses VOICE_COMMUNICATION source + USAGE_VOICE_COMMUNICATION
 *     This is required during real phone calls because the telephony system locks
 *     the standard MIC source. VOICE_COMMUNICATION shares the mic with the call
 *     and routes output through the communication audio path.
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
    private var audioManager: AudioManager? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL

    var currentEffect: VoiceEffect? = null
    var isRunning: Boolean = false
        private set

    /** When true, uses VOICE_COMMUNICATION path for real call audio */
    var callMode: Boolean = false

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
            audioManager = context.getSystemService(AudioManager::class.java)

            if (callMode) {
                setupCallMode()
            }

            // --- Microphone input ---
            val audioSource = if (callMode) {
                // VOICE_COMMUNICATION works alongside the telephony system during calls
                // and includes AEC + NS which we need
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            }

            audioRecord = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                CHANNEL_IN,
                AUDIO_FORMAT,
                bufferSize
            )

            // --- Speaker/earpiece output ---
            val usage = if (callMode) {
                AudioAttributes.USAGE_VOICE_COMMUNICATION
            } else {
                AudioAttributes.USAGE_MEDIA
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(usage)
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

    /**
     * Configure audio system for in-call voice processing.
     * MODE_IN_COMMUNICATION tells Android we're handling voice communication audio,
     * which allows VOICE_COMMUNICATION source to work during an active cellular call.
     */
    private fun setupCallMode() {
        audioManager?.let { am ->
            previousAudioMode = am.mode
            am.mode = AudioManager.MODE_IN_COMMUNICATION

            // Request audio focus for voice communication
            @Suppress("deprecation")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )

            // Enable speakerphone so processed audio is audible and
            // gets picked up by the mic → sent to the other party
            am.isSpeakerphoneOn = true
        }
    }

    private fun restoreAudioMode() {
        audioManager?.let { am ->
            am.mode = previousAudioMode
            @Suppress("deprecation")
            am.abandonAudioFocus(null)
        }
    }

    private suspend fun processAudioLoop() {
        val readBuffer = ShortArray(bufferSize / 2)

        while (isRunning && coroutineContext.isActive) {
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

            // Write processed audio to output
            audioTrack?.write(processed, 0, processed.size)
        }
    }

    fun stop() {
        isRunning = false
        processingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { /* ignored */ }

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) { /* ignored */ }

        audioRecord = null
        audioTrack = null
        currentEffect?.reset()

        if (callMode) {
            restoreAudioMode()
        }
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
