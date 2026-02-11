package com.magiccall.voicechanger.ui.incall

import android.media.AudioManager
import android.os.Bundle
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.databinding.ActivityIncallBinding
import com.magiccall.voicechanger.model.ActiveCallManager
import com.magiccall.voicechanger.model.VoicePreset

/**
 * Full-screen in-call activity shown during real phone calls.
 * Launched by MagicInCallService when a call is added.
 *
 * Shows: caller number, call status, timer, amplitude bar,
 * voice effect switcher, mute/speaker/hold controls, end call button.
 */
class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncallBinding
    private var isMuted = false
    private var isSpeaker = false
    private var callSeconds = 0
    private var timerThread: Thread? = null

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityIncallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupControls()
        setupEffectSwitcher()
        observeCallState()
    }

    private fun setupControls() {
        binding.btnEndCall.setOnClickListener {
            ActiveCallManager.hangUp()
        }

        binding.btnMute.setOnClickListener {
            isMuted = ActiveCallManager.toggleMute()
            binding.btnMuteIcon.setImageResource(
                if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
            )
            binding.btnMuteLabel.text = if (isMuted) "Unmute" else "Mute"
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = !isSpeaker
            val audioManager = getSystemService(AudioManager::class.java)
            audioManager.isSpeakerphoneOn = isSpeaker
            updateSpeakerUI()
        }

        binding.btnHold.setOnClickListener {
            val call = ActiveCallManager.activeCall.value ?: return@setOnClickListener
            if (call.state == Call.STATE_HOLDING) {
                ActiveCallManager.unhold()
                binding.btnHoldLabel.text = "Hold"
            } else {
                ActiveCallManager.hold()
                binding.btnHoldLabel.text = "Resume"
            }
        }

        // Answer button (only visible for incoming calls)
        binding.btnAnswer.visibility = View.GONE
        binding.btnAnswer.setOnClickListener {
            ActiveCallManager.answerCall()
            binding.btnAnswer.visibility = View.GONE
        }
    }

    private fun setupEffectSwitcher() {
        val presets = VoicePreset.getAll()
        val currentPresetId = ActiveCallManager.selectedPresetId

        // "Normal" chip — no effect
        val noEffectChip = com.google.android.material.chip.Chip(this).apply {
            text = "Normal"
            isCheckable = true
            isChecked = currentPresetId == null
            setChipBackgroundColorResource(R.color.effect_normal)
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        noEffectChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ActiveCallManager.setEffect(null)
                binding.activeEffectName.text = "Normal Voice"
            }
        }
        binding.effectSwitchGroup.addView(noEffectChip)

        // Voice effect chips
        presets.forEach { preset ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = preset.displayName
                isCheckable = true
                isChecked = preset.id == currentPresetId
                setChipBackgroundColorResource(R.color.effect_normal)
                setTextColor(resources.getColor(R.color.text_primary, null))
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    ActiveCallManager.setEffect(preset.effectFactory())
                    binding.activeEffectName.text = "Voice: ${preset.displayName}"
                }
            }
            binding.effectSwitchGroup.addView(chip)
        }
    }

    private fun observeCallState() {
        ActiveCallManager.callState.observe(this) { state ->
            binding.callStatus.text = stateToString(state)

            when (state) {
                Call.STATE_ACTIVE -> {
                    binding.callAmplitudeBar.visibility = View.VISIBLE
                    binding.btnAnswer.visibility = View.GONE
                    startTimer()
                }
                Call.STATE_DISCONNECTED -> {
                    stopTimer()
                    binding.callAmplitudeBar.visibility = View.INVISIBLE
                    binding.root.postDelayed({ finish() }, 1500)
                }
                Call.STATE_RINGING -> {
                    binding.btnAnswer.visibility = View.VISIBLE
                }
            }
        }

        ActiveCallManager.activeCall.observe(this) { call ->
            if (call == null) {
                finish()
                return@observe
            }
            val number = ActiveCallManager.getCallNumber()
            binding.callerNumber.text = formatPhoneDisplay(number)
        }

        ActiveCallManager.amplitude.observe(this) { amp ->
            binding.callAmplitudeBar.progress = (amp * 100).toInt()
        }

        // AudioEngine auto-enables speaker in call mode — sync UI
        ActiveCallManager.speakerOn.observe(this) { on ->
            isSpeaker = on
            updateSpeakerUI()
        }

        // Set initial effect label
        val presetId = ActiveCallManager.selectedPresetId
        val preset = VoicePreset.getAll().find { it.id == presetId }
        binding.activeEffectName.text = if (preset != null) {
            "Voice: ${preset.displayName}"
        } else {
            "Normal Voice"
        }
    }

    private fun startTimer() {
        stopTimer()
        callSeconds = 0
        timerThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(1000)
                    callSeconds++
                    runOnUiThread {
                        val min = callSeconds / 60
                        val sec = callSeconds % 60
                        binding.callTimer.text = String.format("%02d:%02d", min, sec)
                    }
                }
            } catch (e: InterruptedException) {
                // Timer stopped
            }
        }.apply { start() }
    }

    private fun stopTimer() {
        timerThread?.interrupt()
        timerThread = null
    }

    private fun stateToString(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "Initiating..."
            Call.STATE_DIALING -> "Dialing..."
            Call.STATE_RINGING -> "Incoming Call"
            Call.STATE_HOLDING -> "On Hold"
            Call.STATE_ACTIVE -> "Connected"
            Call.STATE_DISCONNECTED -> "Call Ended"
            Call.STATE_CONNECTING -> "Connecting..."
            else -> "..."
        }
    }

    private fun updateSpeakerUI() {
        binding.btnSpeakerIcon.setColorFilter(
            ContextCompat.getColor(
                this,
                if (isSpeaker) R.color.primary else R.color.text_secondary
            )
        )
        binding.btnSpeakerLabel.text = if (isSpeaker) "Speaker On" else "Speaker Off"
    }

    private fun formatPhoneDisplay(number: String): String {
        return when {
            number.isEmpty() -> "Unknown"
            number.length <= 3 -> number
            number.length <= 6 -> "${number.substring(0, 3)}-${number.substring(3)}"
            number.length <= 10 ->
                "(${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            else -> number
        }
    }

    @Suppress("deprecation")
    override fun onBackPressed() {
        // During active call, minimize instead of closing
        if (ActiveCallManager.callState.value == Call.STATE_ACTIVE) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
