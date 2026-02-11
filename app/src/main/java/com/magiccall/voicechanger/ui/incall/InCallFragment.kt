package com.magiccall.voicechanger.ui.incall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.magiccall.voicechanger.MainActivity
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.databinding.FragmentIncallBinding
import com.magiccall.voicechanger.model.CallManager
import com.magiccall.voicechanger.model.VoicePreset

class InCallFragment : Fragment() {

    private var _binding: FragmentIncallBinding? = null
    private val binding get() = _binding!!

    private lateinit var callManager: CallManager
    private var isMuted = false
    private var isSpeaker = true // default on for demo mode

    companion object {
        private const val ARG_PHONE_NUMBER = "phone_number"
        private const val ARG_PRESET_ID = "preset_id"

        fun newInstance(phoneNumber: String, presetId: String): InCallFragment {
            return InCallFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHONE_NUMBER, phoneNumber)
                    putString(ARG_PRESET_ID, presetId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val phoneNumber = arguments?.getString(ARG_PHONE_NUMBER) ?: ""
        val presetId = arguments?.getString(ARG_PRESET_ID) ?: ""
        val preset = VoicePreset.getAll().find { it.id == presetId }

        callManager = CallManager(requireContext())

        binding.callerNumber.text = formatPhoneDisplay(phoneNumber)
        binding.callerName.text = "Voice: ${preset?.displayName ?: "None"}"

        setupControls()
        setupEffectSwitcher(presetId)
        observeCallState()

        // Check audio permission and start the call
        val activity = requireActivity() as MainActivity
        if (activity.hasAudioPermission()) {
            callManager.startCall(phoneNumber, preset?.effectFactory?.invoke())
        }
    }

    private fun setupControls() {
        binding.btnEndCall.setOnClickListener {
            callManager.endCall()
            parentFragmentManager.popBackStack()
        }

        binding.btnMute.setOnClickListener {
            isMuted = callManager.toggleMute()
            binding.btnMuteIcon.setImageResource(
                if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
            )
            binding.btnMuteLabel.text = if (isMuted) "Unmute" else "Mute"
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = !isSpeaker
            binding.btnSpeakerIcon.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSpeaker) R.color.primary else R.color.text_secondary
                )
            )
            binding.btnSpeakerLabel.text = if (isSpeaker) "Speaker On" else "Speaker Off"
        }
    }

    private fun setupEffectSwitcher(currentPresetId: String) {
        val presets = VoicePreset.getAll()

        presets.forEach { preset ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = preset.displayName
                isCheckable = true
                isChecked = preset.id == currentPresetId
                setChipBackgroundColorResource(R.color.effect_normal)
                setTextColor(resources.getColor(R.color.text_primary, null))
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    callManager.switchEffect(preset.effectFactory())
                    binding.callerName.text = "Voice: ${preset.displayName}"
                }
            }
            binding.effectSwitchGroup.addView(chip)
        }
    }

    private fun observeCallState() {
        callManager.callState.observe(viewLifecycleOwner) { state ->
            binding.callStatus.text = when (state) {
                CallManager.CallState.IDLE -> "Ready"
                CallManager.CallState.DIALING -> "Calling..."
                CallManager.CallState.ACTIVE -> "Connected"
                CallManager.CallState.ENDED -> "Call Ended"
            }

            // Show/hide the amplitude bar
            binding.callAmplitudeBar.visibility =
                if (state == CallManager.CallState.ACTIVE) View.VISIBLE else View.INVISIBLE

            // Pulse animation on dialing
            if (state == CallManager.CallState.DIALING) {
                binding.callerAvatar.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500)
                    .withEndAction {
                        binding.callerAvatar.animate().scaleX(1f).scaleY(1f).setDuration(500).start()
                    }.start()
            }
        }

        callManager.callDuration.observe(viewLifecycleOwner) { seconds ->
            val min = seconds / 60
            val sec = seconds % 60
            binding.callTimer.text = String.format("%02d:%02d", min, sec)
        }

        callManager.amplitude.observe(viewLifecycleOwner) { amp ->
            binding.callAmplitudeBar.progress = (amp * 100).toInt()
        }
    }

    private fun formatPhoneDisplay(number: String): String {
        return when {
            number.length <= 3 -> number
            number.length <= 6 -> "${number.substring(0, 3)}-${number.substring(3)}"
            number.length <= 10 -> "(${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            else -> number
        }
    }

    override fun onDestroyView() {
        callManager.release()
        _binding = null
        super.onDestroyView()
    }
}
