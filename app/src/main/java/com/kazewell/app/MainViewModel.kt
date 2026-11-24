package com.kazewell.app

import android.app.Application
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Filter { ALL, USER, SYSTEM, ALLOWED, BLOCKED }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val loading = MutableStateFlow(true)
    val allowed = MutableStateFlow(prefs.allowed)
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(Filter.ALL)
    val autoStart = MutableStateFlow(prefs.autoStart)

    val running: StateFlow<Boolean> = FirewallVpnService.running
    val dropped: StateFlow<Long> = FirewallVpnService.dropped

    val visible: StateFlow<List<AppEntry>> =
        combine(apps, allowed, query, filter) { list, allow, q, f ->
            val needle = q.trim().lowercase()
            list.filter { app ->
                val matchesText = needle.isEmpty() ||
                    app.label.lowercase().contains(needle) ||
                    app.pkg.lowercase().contains(needle)
                val matchesFilter = when (f) {
                    Filter.ALL -> true
                    Filter.USER -> !app.isSystem
                    Filter.SYSTEM -> app.isSystem
                    Filter.ALLOWED -> app.pkg in allow
                    Filter.BLOCKED -> app.pkg !in allow
                }
                matchesText && matchesFilter
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var reloadJob: Job? = null

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = AppScanner.internetApps(getApplication())
            apps.value = list
            loading.value = false
        }
    }

    /** Called when the screen opens: resume the firewall if the user left it on. */
    fun ensureRunning() {
        val ctx = getApplication<Application>()
        if (prefs.enabled && !running.value && VpnService.prepare(ctx) == null) {
            FirewallVpnService.start(ctx)
        }
        refreshApps()
    }

    fun startFirewall() {
        prefs.enabled = true
        FirewallVpnService.start(getApplication())
    }

    fun stopFirewall() {
        prefs.enabled = false
        FirewallVpnService.stop(getApplication())
    }

    fun setAutoStart(on: Boolean) {
        prefs.autoStart = on
        autoStart.value = on
    }

    fun setAllowed(pkg: String, allow: Boolean) {
        commit(if (allow) allowed.value + pkg else allowed.value - pkg)
    }

    fun allowAll(list: List<AppEntry>) = commit(allowed.value + list.map { it.pkg })

    fun blockAll(list: List<AppEntry>) = commit(allowed.value - list.map { it.pkg }.toSet())

    private fun commit(newSet: Set<String>) {
        allowed.value = newSet
        prefs.allowed = newSet
        if (running.value) {
            // Debounced: quick successive toggles trigger a single tunnel rebuild.
            reloadJob?.cancel()
            reloadJob = viewModelScope.launch {
                delay(400)
                FirewallVpnService.reload(getApplication())
            }
        }
    }
}
