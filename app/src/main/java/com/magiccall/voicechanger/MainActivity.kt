package com.magiccall.voicechanger

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.magiccall.voicechanger.databinding.ActivityMainBinding
import com.magiccall.voicechanger.ui.dialer.DialerFragment
import com.magiccall.voicechanger.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val dialerFragment = DialerFragment()
    private var activeFragment: Fragment = homeFragment

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (!audioGranted) {
            Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (isDefaultDialer()) {
            Toast.makeText(this, "MagicCall is now your default dialer!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()

        if (savedInstanceState == null) {
            setupBottomNavigation()

            // If opened via ACTION_DIAL, switch to dialer tab
            if (intent?.action == Intent.ACTION_DIAL) {
                binding.bottomNavigation.selectedItemId = R.id.nav_dialer
            }
        }

        // Prompt to set as default dialer if not already
        if (!isDefaultDialer()) {
            promptDefaultDialer()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle new ACTION_DIAL intents
        if (intent?.action == Intent.ACTION_DIAL) {
            binding.bottomNavigation.selectedItemId = R.id.nav_dialer
        }
    }

    private fun setupBottomNavigation() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, dialerFragment, "dialer").hide(dialerFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_dialer -> {
                    switchFragment(dialerFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment) {
        if (target == activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    /**
     * Prompt user to set MagicCall as the default phone/dialer app.
     * This is REQUIRED for InCallService to work — without it, Android
     * won't route call events to our MagicInCallService.
     */
    private fun promptDefaultDialer() {
        AlertDialog.Builder(this)
            .setTitle("Set as Default Dialer")
            .setMessage(
                "MagicCall needs to be your default phone app to apply voice effects during real calls.\n\n" +
                "This lets MagicCall show its own call screen with voice controls when you make or receive calls."
            )
            .setPositiveButton("Set Default") { _, _ ->
                requestDefaultDialer()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    @Suppress("deprecation")
    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName
                )
            }
            defaultDialerLauncher.launch(intent)
        }
    }

    private fun isDefaultDialer(): Boolean {
        val telecomManager = getSystemService(TelecomManager::class.java)
        return telecomManager.defaultDialerPackage == packageName
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
