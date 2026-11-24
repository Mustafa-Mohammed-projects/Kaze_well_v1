package com.kazewell.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * How the firewall works (no root needed):
 *
 * Android lets a VPN service choose exactly which apps are routed into its tunnel. We route every
 * installed app that is NOT on the allowed list into a tunnel and simply never forward the packets
 * anywhere, so those apps have no connectivity. Allowed apps are not added to the tunnel, so they
 * keep using the normal network untouched (full speed, no proxying).
 */
class FirewallVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.kazewell.app.START"
        const val ACTION_RELOAD = "com.kazewell.app.RELOAD"
        const val ACTION_STOP = "com.kazewell.app.STOP"

        val running = MutableStateFlow(false)
        val dropped = MutableStateFlow(0L)

        fun start(context: Context) = send(context, ACTION_START)
        fun reload(context: Context) = send(context, ACTION_RELOAD)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            try {
                context.startService(Intent(context, FirewallVpnService::class.java).setAction(action))
            } catch (e: Exception) {
                // Background-start restrictions: nothing sensible to do, the user can reopen the app.
            }
        }
    }

    private val lock = Any()
    private var tunnel: ParcelFileDescriptor? = null
    private var reader: Thread? = null
    private var receiverRegistered = false
    private val counter = AtomicLong(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // A brand-new app was installed: rebuild the rules so it starts out blocked.
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            if (running.value) applyRules()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        // A null intent means Android restarted us after killing the process.
        if (!Prefs(this).enabled) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START || !running.value) {
            counter.set(0)
            dropped.value = 0L
        }
        if (!applyRules()) {
            Prefs(this).enabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun applyRules(): Boolean {
        return synchronized(lock) {
            val allowed = Prefs(this).allowed
            val blocked = AppScanner.packageNames(this).filter { it !in allowed }

            val builder = Builder()
                .setSession("kaze_well")
                .setMtu(1500)
                .addAddress("10.77.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd77:7a6b:6500::2", 128)
                .addRoute("::", 0)
                .setBlocking(true)

            var added = 0
            for (pkg in blocked) {
                try {
                    builder.addAllowedApplication(pkg)
                    added++
                } catch (e: Exception) {
                    // package vanished between the scan and now
                }
            }
            if (added == 0) {
                // With no entries Android would route EVERY app into the tunnel, so route only ourselves.
                try {
                    builder.addAllowedApplication(packageName)
                } catch (e: Exception) {
                }
            }

            val newTunnel = try {
                builder.establish()
            } catch (e: Exception) {
                null
            }
            if (newTunnel == null) {
                running.value = false
                return@synchronized false
            }

            val oldTunnel = tunnel
            val oldReader = reader
            tunnel = newTunnel
            reader = Thread({ drain(newTunnel) }, "kaze_well-drain").also { it.start() }
            try {
                oldTunnel?.close()
            } catch (e: IOException) {
            }
            oldReader?.interrupt()

            running.value = true
            registerPackageReceiver()
            true
        }
    }

    /** Reads and discards every packet: this is what "blocking" means here. */
    private fun drain(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)
        var lastPublish = 0L
        try {
            while (!Thread.currentThread().isInterrupted) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) {
                    val total = counter.incrementAndGet()
                    val now = System.currentTimeMillis()
                    if (now - lastPublish > 400) {
                        lastPublish = now
                        dropped.value = total
                    }
                }
            }
        } catch (e: IOException) {
            // tunnel closed
        } finally {
            dropped.value = counter.get()
        }
    }

    private fun registerPackageReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        filter.addDataScheme("package")
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun shutdown() {
        synchronized(lock) {
            if (receiverRegistered) {
                try {
                    unregisterReceiver(packageReceiver)
                } catch (e: Exception) {
                }
                receiverRegistered = false
            }
            try {
                tunnel?.close()
            } catch (e: IOException) {
            }
            tunnel = null
            reader?.interrupt()
            reader = null
            running.value = false
        }
    }

    /** The user revoked VPN access from Android settings (or another VPN took over). */
    override fun onRevoke() {
        Prefs(this).enabled = false
        shutdown()
        super.onRevoke()
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
