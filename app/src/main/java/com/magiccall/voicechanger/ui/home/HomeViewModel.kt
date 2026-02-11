package com.magiccall.voicechanger.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.magiccall.voicechanger.model.CreditManager
import com.magiccall.voicechanger.model.VoicePreset

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val creditManager = CreditManager(application)

    private val _presets = MutableLiveData<List<VoicePreset>>()
    val presets: LiveData<List<VoicePreset>> = _presets

    private val _selectedPreset = MutableLiveData<VoicePreset?>()
    val selectedPreset: LiveData<VoicePreset?> = _selectedPreset

    private val _isActive = MutableLiveData(false)
    val isActive: LiveData<Boolean> = _isActive

    private val _amplitude = MutableLiveData(0f)
    val amplitude: LiveData<Float> = _amplitude

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    init {
        _presets.value = VoicePreset.getAll()
    }

    fun selectPreset(preset: VoicePreset) {
        if (_selectedPreset.value?.id == preset.id) {
            _selectedPreset.value = null
            return
        }
        _selectedPreset.value = preset
    }

    fun setActive(active: Boolean) {
        _isActive.value = active
    }

    fun updateAmplitude(value: Float) {
        _amplitude.value = value
    }

    fun tryActivateWithCredits(): Boolean {
        val preset = _selectedPreset.value ?: return false
        if (!creditManager.canAfford(preset.creditCost)) {
            _toastMessage.value = "Not enough credits! Need ${preset.creditCost} credits."
            return false
        }
        creditManager.spend(preset.creditCost)
        return true
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
