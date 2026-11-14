package com.vortex.launcher.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

enum class NetState { WIFI, OTHER, NONE }

private fun readNet(ctx: Context): NetState {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return NetState.NONE
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetState.NONE
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetState.WIFI
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetState.OTHER
        else -> NetState.NONE
    }
}

@Composable
fun rememberNetState(): State<NetState> {
    val ctx = LocalContext.current.applicationContext
    return produceState(initialValue = readNet(ctx)) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = readNet(ctx) }
            override fun onLost(network: Network) { value = readNet(ctx) }
            override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) { value = readNet(ctx) }
        }
        cm.registerDefaultNetworkCallback(cb)
        awaitDispose { cm.unregisterNetworkCallback(cb) }
    }
}
