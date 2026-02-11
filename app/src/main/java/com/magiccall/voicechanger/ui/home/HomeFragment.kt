package com.magiccall.voicechanger.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.magiccall.voicechanger.MainActivity
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.databinding.FragmentHomeBinding
import com.magiccall.voicechanger.service.VoiceChangerService
import com.magiccall.voicechanger.ui.effects.EffectsAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var effectsAdapter: EffectsAdapter

    private var voiceService: VoiceChangerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as VoiceChangerService.LocalBinder
            voiceService = localBinder.getService()
            serviceBound = true
            voiceService?.setAmplitudeCallback { amplitude ->
                viewModel.updateAmplitude(amplitude)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            serviceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupRecyclerView()
        setupControls()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        effectsAdapter = EffectsAdapter { preset ->
            viewModel.selectPreset(preset)
        }
        binding.effectsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = effectsAdapter
        }
    }

    private fun setupControls() {
        binding.btnStartStop.setOnClickListener {
            if (viewModel.isActive.value == true) {
                stopVoiceChanger()
            } else {
                startVoiceChanger()
            }
        }

        binding.btnBuyCredits.setOnClickListener {
            showCreditStore()
        }
    }

    private fun observeViewModel() {
        viewModel.presets.observe(viewLifecycleOwner) { presets ->
            effectsAdapter.submitList(presets)
        }

        viewModel.selectedPreset.observe(viewLifecycleOwner) { preset ->
            effectsAdapter.setSelectedId(preset?.id)
            binding.selectedEffectName.text = preset?.displayName ?: "No effect selected"
            binding.selectedEffectDesc.text = preset?.description ?: "Choose a voice effect below"
            binding.btnStartStop.isEnabled = preset != null || viewModel.isActive.value == true

            // Update effect on the running service
            if (serviceBound && viewModel.isActive.value == true) {
                voiceService?.setEffect(preset?.effectFactory?.invoke())
            }
        }

        viewModel.isActive.observe(viewLifecycleOwner) { active ->
            updateUI(active)
        }

        viewModel.creditManager.credits.observe(viewLifecycleOwner) { credits ->
            binding.creditsText.text = "$credits"
        }

        viewModel.amplitude.observe(viewLifecycleOwner) { amp ->
            binding.amplitudeBar.progress = (amp * 100).toInt()
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun startVoiceChanger() {
        val activity = requireActivity() as MainActivity
        if (!activity.hasAudioPermission()) {
            Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.selectedPreset.value == null) {
            Toast.makeText(requireContext(), "Select a voice effect first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!viewModel.tryActivateWithCredits()) return

        val intent = Intent(requireContext(), VoiceChangerService::class.java).apply {
            action = VoiceChangerService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), intent)

        // Bind to service
        requireContext().bindService(
            Intent(requireContext(), VoiceChangerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        // Set effect after a small delay to ensure service is bound
        binding.root.postDelayed({
            voiceService?.setEffect(viewModel.selectedPreset.value?.effectFactory?.invoke())
        }, 200)

        viewModel.setActive(true)
    }

    private fun stopVoiceChanger() {
        val intent = Intent(requireContext(), VoiceChangerService::class.java).apply {
            action = VoiceChangerService.ACTION_STOP
        }
        requireContext().startService(intent)

        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }

        viewModel.setActive(false)
    }

    private fun updateUI(active: Boolean) {
        if (active) {
            binding.btnStartStop.text = "STOP"
            binding.btnStartStop.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.stop_button)
            )
            binding.statusText.text = "Voice Changer Active"
            binding.statusDot.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.status_active)
            )
            binding.amplitudeBar.visibility = View.VISIBLE
        } else {
            binding.btnStartStop.text = "START"
            binding.btnStartStop.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.start_button)
            )
            binding.statusText.text = "Ready"
            binding.statusDot.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.status_inactive)
            )
            binding.amplitudeBar.visibility = View.INVISIBLE
            binding.btnStartStop.isEnabled = viewModel.selectedPreset.value != null
        }
    }

    private fun showCreditStore() {
        val packages = viewModel.creditManager.getAvailablePackages()
        val names = packages.map { "${it.description} - ${it.credits} credits (${it.displayPrice})" }
            .toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Buy Credits")
            .setItems(names) { _, which ->
                val pkg = packages[which]
                viewModel.creditManager.simulatePurchase(pkg.id)
                Toast.makeText(
                    requireContext(),
                    "Added ${pkg.credits} credits!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }
        _binding = null
    }
}
