package com.vortex.launcher.ui.components

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

fun openSystem(ctx: Context, action: String) {
    runCatching { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openSearch(ctx: Context) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { ctx.startActivity(Intent(Intent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

/** «Поставить по умолчанию»: RoleManager (HOME) с фолбэком на системные настройки. */
@Composable
fun rememberDefaultLauncherRequester(): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    return {
        val rm = ctx.getSystemService(RoleManager::class.java)
        val viaRole = runCatching {
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                true
            } else false
        }.getOrDefault(false)
        if (!viaRole) {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                .onFailure { openSystem(ctx, Settings.ACTION_SETTINGS) }
        }
    }
}
