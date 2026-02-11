package com.magiccall.voicechanger.service

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.magiccall.voicechanger.model.ActiveCallManager
import com.magiccall.voicechanger.ui.incall.InCallActivity

/**
 * InCallService — Android calls this when a phone call starts/ends.
 * Requires the app to be set as the default dialer/phone app.
 *
 * When a call is added, we store it in ActiveCallManager and launch
 * our custom InCallActivity to show the call UI with voice effects.
 */
class MagicInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        ActiveCallManager.initAudioEngine(this)
        ActiveCallManager.setActiveCall(call)

        // Launch our in-call UI
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        ActiveCallManager.removeCall(call)
    }
}
