package com.kazewell.app

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val pkg: String, val label: String, val isSystem: Boolean)

object AppScanner {
    /** Every installed app that declares the INTERNET permission (the only ones worth blocking). */
    @Suppress("DEPRECATION")
    fun internetApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val packages: List<PackageInfo> = try {
            pm.getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET), 0)
        } catch (e: Exception) {
            emptyList()
        }
        val result = ArrayList<AppEntry>(packages.size)
        for (info in packages) {
            val name = info.packageName ?: continue
            if (name == context.packageName) continue
            val ai = info.applicationInfo ?: continue
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            val label = try {
                ai.loadLabel(pm).toString()
            } catch (e: Exception) {
                name
            }
            result.add(AppEntry(name, label, isSystem))
        }
        return result.sortedBy { it.label.lowercase() }
    }

    fun packageNames(context: Context): List<String> = internetApps(context).map { it.pkg }
}

object IconCache {
    private val cache = LruCache<String, ImageBitmap>(320)

    fun peek(pkg: String): ImageBitmap? = cache.get(pkg)

    fun load(context: Context, pkg: String): ImageBitmap? {
        cache.get(pkg)?.let { return it }
        return try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            val bitmap = drawable.toBitmap(96, 96).asImageBitmap()
            cache.put(pkg, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
