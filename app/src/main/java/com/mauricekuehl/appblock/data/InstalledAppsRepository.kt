package com.mauricekuehl.appblock.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(
    val label: String,
    val packageName: String,
)

@Suppress("DEPRECATION")
class InstalledAppsRepository(private val context: Context) {
    fun loadLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { info ->
                InstalledApp(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = info.activityInfo.packageName,
                )
            }
            .filterNot { it.packageName == context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun labelFor(packageName: String): String = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}
