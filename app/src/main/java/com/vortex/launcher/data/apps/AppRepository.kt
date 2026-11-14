package com.vortex.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val pkg: String,
    val label: String,
    val isGame: Boolean,
    val icon: ImageBitmap,
    val banner: ImageBitmap?
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val pm = ctx.packageManager
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    @Suppress("DEPRECATION")
    suspend fun refresh() = withContext(Dispatchers.Default) {
        val intents = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .map { Intent(Intent.ACTION_MAIN).addCategory(it) }
        val resolved = intents.flatMap { pm.queryIntentActivities(it, 0) }
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }

        _apps.value = resolved.mapNotNull { ri ->
            runCatching {
                val ai = ri.activityInfo.applicationInfo
                val banner = runCatching {
                    (ri.activityInfo.loadBanner(pm) ?: ai.loadBanner(pm))?.toBitmap()?.asImageBitmap()
                }.getOrNull()
                AppInfo(
                    pkg = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    isGame = ai.category == ApplicationInfo.CATEGORY_GAME ||
                        (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0,
                    icon = ri.loadIcon(pm).toBitmap(256, 256).asImageBitmap(),
                    banner = banner
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    fun launch(pkg: String) {
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }
}
