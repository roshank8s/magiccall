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
 *   4. AudioEngine runs alongside the call, processing mic → effect → speaker
 *   5. On speakerphone, the other party hears the effect through acoustic coupling
 */
object ActiveCallManager {

    private val _activeCall = MutableLiveData<Call?>(null)
    val activeCall: LiveData<Call?> = _activeCall

    private val _callState = MutableLiveData(Call.STATE_NEW)
    val callState: LiveData<Int> = _callState

    private val _amplitude = MutableLiveData(0f)
    val amplitude: LiveData<Float> = _amplitude

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

    private fun startVoiceEffect() {
        audioEngine?.let { engine ->
            engine.setEffect(currentEffect)
            engine.onAmplitudeUpdate = { amp ->
                _amplitude.postValue(amp)
            }
            engine.start()
        }
    }

    private fun stopVoiceEffect() {
        audioEngine?.stop()
    }

    fun toggleMute(): Boolean {
        val engine = audioEngine ?: return false
        return if (engine.isRunning) {
            engine.stop()
            true // now muted
        } else {
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
