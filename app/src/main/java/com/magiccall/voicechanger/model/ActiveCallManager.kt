package com.magiccall.voicechanger.model

import android.content.Context
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.magiccall.voicechanger.audio.AudioEngine
import com.magiccall.voicechanger.audio.VoiceEffect

/**
 * Singleton that manages the active phone call and the voice effect audio engine.
 *
 * Flow:
 *   1. User dials number → TelecomManager places real SIM call
 *   2. MagicInCallService receives Call object → stored here
 *   3. InCallActivity observes state and controls the call
 *   4. AudioEngine runs in callMode — uses VOICE_COMMUNICATION source
 *      so it can capture mic audio during an active cellular call
 *   5. Processed audio plays through speaker → other party hears it
 */
object ActiveCallManager {

    private val _activeCall = MutableLiveData<Call?>(null)
    val activeCall: LiveData<Call?> = _activeCall

    private val _callState = MutableLiveData(Call.STATE_NEW)
    val callState: LiveData<Int> = _callState

    private val _amplitude = MutableLiveData(0f)
    val amplitude: LiveData<Float> = _amplitude

    /** True when speakerphone is on (managed by InCallActivity) */
    private val _speakerOn = MutableLiveData(false)
    val speakerOn: LiveData<Boolean> = _speakerOn

    private var audioEngine: AudioEngine? = null
    private var currentEffect: VoiceEffect? = null

    // The preset ID selected before placing the call
    var selectedPresetId: String? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            _callState.postValue(state)

            when (state) {
                Call.STATE_ACTIVE -> startVoiceEffect()
                Call.STATE_DISCONNECTED -> {
                    stopVoiceEffect()
                    _activeCall.postValue(null)
                }
            }
        }
    }

    fun setActiveCall(call: Call) {
        _activeCall.postValue(call)
        _callState.postValue(call.state)
        call.registerCallback(callCallback)
    }

    fun removeCall(call: Call) {
        call.unregisterCallback(callCallback)
        stopVoiceEffect()
        _activeCall.postValue(null)
        _callState.postValue(Call.STATE_DISCONNECTED)
    }

    fun answerCall() {
        _activeCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun hangUp() {
        _activeCall.value?.disconnect()
    }

    fun hold() {
        _activeCall.value?.hold()
    }

    fun unhold() {
        _activeCall.value?.unhold()
    }

    fun initAudioEngine(context: Context) {
        if (audioEngine == null) {
            audioEngine = AudioEngine(context.applicationContext)
        }
    }

    fun setEffect(effect: VoiceEffect?) {
        currentEffect = effect
        audioEngine?.setEffect(effect)
    }

    /**
     * Start the audio engine in call mode.
     * Call mode uses VOICE_COMMUNICATION audio source + USAGE_VOICE_COMMUNICATION
     * which allows mic capture during active cellular calls and routes processed
     * audio through the communication audio path.
     */
    private fun startVoiceEffect() {
        audioEngine?.let { engine ->
            // Enable call mode — switches to VOICE_COMMUNICATION path
            engine.callMode = true
            engine.setEffect(currentEffect)
            engine.onAmplitudeUpdate = { amp ->
                _amplitude.postValue(amp)
            }
            engine.start()
            // Speaker is auto-enabled by AudioEngine.setupCallMode()
            _speakerOn.postValue(true)
        }
    }

    private fun stopVoiceEffect() {
        audioEngine?.stop()
        _speakerOn.postValue(false)
    }

    fun toggleMute(): Boolean {
        val engine = audioEngine ?: return false
        return if (engine.isRunning) {
            engine.stop()
            true // now muted
        } else {
            engine.callMode = true
            engine.setEffect(currentEffect)
            engine.start()
            false // unmuted
        }
    }

    fun getCallNumber(): String {
        val call = _activeCall.value ?: return ""
        val handle = call.details?.handle ?: return ""
        return handle.schemeSpecificPart ?: ""
    }

    fun release() {
        stopVoiceEffect()
        audioEngine?.release()
        audioEngine = null
    }
}
