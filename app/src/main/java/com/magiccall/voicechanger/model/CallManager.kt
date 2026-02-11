package com.magiccall.voicechanger.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.magiccall.voicechanger.audio.AudioEngine
import com.magiccall.voicechanger.audio.VoiceEffect

/**
 * Manages call state and audio routing for voice-changed calls.
 *
 * Current mode: LOCAL DEMO — captures mic, applies effect, plays through speaker.
 * Future mode: VoIP — send processed audio to a SIP/VoIP server for real calls.
 *
 * To add real calling later, replace startCall() internals with:
 *   1. SIP registration (e.g., oduwa, twilio, pjsip)
 *   2. Route AudioEngine output to VoIP stream instead of AudioTrack
 */
class CallManager(context: Context) {

    enum class CallState {
        IDLE,       // No active call
        DIALING,    // Connecting (simulated delay)
        ACTIVE,     // Call in progress with voice effect
        ENDED       // Call ended
    }

    private val audioEngine = AudioEngine(context)

    private val _callState = MutableLiveData(CallState.IDLE)
    val callState: LiveData<CallState> = _callState

    private val _callDuration = MutableLiveData(0)
    val callDuration: LiveData<Int> = _callDuration

    private val _currentNumber = MutableLiveData<String?>(null)
    val currentNumber: LiveData<String?> = _currentNumber

    private val _amplitude = MutableLiveData(0f)
    val amplitude: LiveData<Float> = _amplitude

    private var durationThread: Thread? = null
    private var activeEffect: VoiceEffect? = null

    /**
     * Start a voice-changed call.
     * In demo mode: starts audio engine with effect, simulates dialing.
     * With VoIP: would initiate a real SIP call here.
     */
    fun startCall(phoneNumber: String, effect: VoiceEffect?): Boolean {
        if (_callState.value != CallState.IDLE && _callState.value != CallState.ENDED) {
            return false
        }

        _currentNumber.postValue(phoneNumber)
        _callState.postValue(CallState.DIALING)
        activeEffect = effect

        // Simulate dialing delay, then connect
        Thread {
            Thread.sleep(2000) // simulate ring/connect time

            audioEngine.setEffect(effect)
            audioEngine.onAmplitudeUpdate = { amp ->
                _amplitude.postValue(amp)
            }
            val started = audioEngine.start()

            if (started) {
                _callState.postValue(CallState.ACTIVE)
                startDurationTimer()
            } else {
                _callState.postValue(CallState.ENDED)
            }
        }.start()

        return true
    }

    fun switchEffect(effect: VoiceEffect?) {
        activeEffect = effect
        audioEngine.setEffect(effect)
    }

    fun endCall() {
        audioEngine.stop()
        durationThread?.interrupt()
        durationThread = null
        _callState.postValue(CallState.ENDED)
        _callDuration.postValue(0)
        _currentNumber.postValue(null)

        // Reset to idle after a brief pause
        Thread {
            Thread.sleep(500)
            _callState.postValue(CallState.IDLE)
        }.start()
    }

    fun toggleMute(): Boolean {
        // In a real VoIP implementation, mute the outgoing stream
        // For demo mode, we stop/start the audio engine
        return if (audioEngine.isRunning) {
            audioEngine.stop()
            true // now muted
        } else {
            audioEngine.setEffect(activeEffect)
            audioEngine.start()
            false // now unmuted
        }
    }

    private fun startDurationTimer() {
        durationThread = Thread {
            var seconds = 0
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(1000)
                    seconds++
                    _callDuration.postValue(seconds)
                }
            } catch (e: InterruptedException) {
                // Timer stopped
            }
        }.apply { start() }
    }

    fun release() {
        endCall()
        audioEngine.release()
    }
}
