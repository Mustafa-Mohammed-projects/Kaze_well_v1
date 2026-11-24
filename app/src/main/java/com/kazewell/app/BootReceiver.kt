package com.kazewell.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Re-enables the firewall after a reboot or app update, if the user left it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        if (!prefs.enabled || !prefs.autoStart) return
        // Only start silently if the user already granted VPN permission earlier.
        if (VpnService.prepare(context) == null) {
            FirewallVpnService.start(context)
        }
    }
}
