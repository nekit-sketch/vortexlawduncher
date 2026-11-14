package com.vortex.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.vortex.launcher.data.settings.SettingsRepository
import com.vortex.launcher.ui.MainViewModel
import com.vortex.launcher.ui.VortexRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    private val vm: MainViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = vm.refreshApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // КРИТИЧНО: восстановить сохранённый skinType (Классика/Гейминг) до первого кадра.
        vm.restore(settingsRepository.readBlocking())

        lifecycleScope.launch { vm.standby.collect { applyDim(it) } }

        ContextCompat.registerReceiver(
            this, packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_EXPORTED
        )

        setContent { VortexRoot(vm) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.goHome() // кнопка Home: назад на главный экран, скин не меняется
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (vm.standby.value) { vm.wake(); return true }
            if (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_SETTINGS) {
                vm.toggleControlCenter(); return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun applyDim(on: Boolean) {
        val lp = window.attributes
        lp.screenBrightness = if (on) 0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onDestroy()
    }
}
