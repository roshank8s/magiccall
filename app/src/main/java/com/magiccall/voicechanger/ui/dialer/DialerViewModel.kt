package com.magiccall.voicechanger.ui.dialer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.magiccall.voicechanger.model.CreditManager
import com.magiccall.voicechanger.model.VoicePreset

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    val creditManager = CreditManager(application)

    private val _phoneNumber = MutableLiveData("")
    val phoneNumber: LiveData<String> = _phoneNumber

    private val _selectedPreset = MutableLiveData<VoicePreset?>()
    val selectedPreset: LiveData<VoicePreset?> = _selectedPreset

    fun appendDigit(digit: String) {
        val current = _phoneNumber.value ?: ""
        if (current.length < 15) {
            _phoneNumber.value = current + digit
        }
    }

    fun deleteLastDigit() {
        val current = _phoneNumber.value ?: ""
        if (current.isNotEmpty()) {
            _phoneNumber.value = current.dropLast(1)
        }
    }

    fun clearNumber() {
        _phoneNumber.value = ""
    }

    fun selectPreset(preset: VoicePreset) {
        _selectedPreset.value = if (_selectedPreset.value?.id == preset.id) null else preset
    }

    fun trySpendCredits(): Boolean {
        val preset = _selectedPreset.value ?: return false
        return creditManager.spend(preset.creditCost)
    }
}
