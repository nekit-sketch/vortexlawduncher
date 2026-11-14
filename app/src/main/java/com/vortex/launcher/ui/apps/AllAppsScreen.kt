package com.vortex.launcher.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.apps.AppInfo
import com.vortex.launcher.ui.components.AppIconTile

@Composable
fun AllAppsScreen(apps: List<AppInfo>, onLaunch: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF05070F))))
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        Text("Все приложения", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(apps, key = { it.pkg }) { app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIconTile(app = app, size = 64.dp, focusScale = 1.18f, onClick = { onLaunch(app.pkg) })
                    Spacer(Modifier.height(8.dp))
                    Text(app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
