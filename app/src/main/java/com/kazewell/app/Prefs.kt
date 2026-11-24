package com.kazewell.app

import android.content.Context

/** Tiny persistent store: firewall on/off, start-on-boot, and the set of apps allowed online. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("kaze_well", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(value) {
            sp.edit().putBoolean("enabled", value).apply()
        }

    var autoStart: Boolean
        get() = sp.getBoolean("autostart", true)
        set(value) {
            sp.edit().putBoolean("autostart", value).apply()
        }

    /** Package names that may use the internet while the firewall is running. */
    var allowed: Set<String>
        get() = sp.getStringSet("allowed", null)?.toSet() ?: emptySet()
        set(value) {
            sp.edit().putStringSet("allowed", HashSet(value)).apply()
        }
}
