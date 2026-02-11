package com.magiccall.voicechanger.ui.dialer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.databinding.FragmentDialerBinding
import com.magiccall.voicechanger.model.VoicePreset
import com.magiccall.voicechanger.ui.incall.InCallFragment

class DialerFragment : Fragment() {

    private var _binding: FragmentDialerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DialerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DialerViewModel::class.java]

        setupDialpad()
        setupEffectChips()
        observeViewModel()
    }

    private fun setupDialpad() {
        val digitButtons = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnStar to "*", binding.btnHash to "#"
        )

        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                viewModel.appendDigit(digit)
            }
        }

        binding.btnDelete.setOnClickListener {
            viewModel.deleteLastDigit()
        }

        binding.btnDelete.setOnLongClickListener {
            viewModel.clearNumber()
            true
        }

        binding.btnCall.setOnClickListener {
            val number = viewModel.phoneNumber.value ?: ""
            if (number.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.selectedPreset.value == null) {
                Toast.makeText(requireContext(), "Select a voice effect first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check credits
            if (!viewModel.trySpendCredits()) {
                Toast.makeText(requireContext(), "Not enough credits!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to in-call screen
            val preset = viewModel.selectedPreset.value!!
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, InCallFragment.newInstance(number, preset.id))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupEffectChips() {
        val presets = VoicePreset.getAll()
        val chipGroup = binding.effectChipGroup

        presets.forEach { preset ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = "${preset.displayName} (${preset.creditCost}cr)"
                isCheckable = true
                setChipBackgroundColorResource(R.color.effect_normal)
                setTextColor(resources.getColor(R.color.text_primary, null))
                tag = preset.id
            }
            chip.setOnClickListener {
                viewModel.selectPreset(preset)
            }
            chipGroup.addView(chip)
        }
    }

    private fun observeViewModel() {
        viewModel.phoneNumber.observe(viewLifecycleOwner) { number ->
            binding.phoneNumberDisplay.text = if (number.isNullOrEmpty()) "Enter number" else formatPhoneNumber(number)
        }

        viewModel.selectedPreset.observe(viewLifecycleOwner) { preset ->
            binding.selectedVoiceText.text = if (preset != null) {
                "Voice: ${preset.displayName}"
            } else {
                "Select a voice effect"
            }
        }

        viewModel.creditManager.credits.observe(viewLifecycleOwner) { credits ->
            binding.dialerCreditsText.text = "$credits"
        }
    }

    private fun formatPhoneNumber(number: String): String {
        return when {
            number.length <= 3 -> number
            number.length <= 6 -> "${number.substring(0, 3)}-${number.substring(3)}"
            number.length <= 10 -> "${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
            else -> number
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
