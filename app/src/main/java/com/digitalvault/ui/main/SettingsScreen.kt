package com.digitalvault.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.settings.AppSettings
import com.digitalvault.ui.S

@Composable
fun SettingsScreen(settings: AppSettings, onLock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(S.settingsTab, style = MaterialTheme.typography.headlineSmall)

        Text(S.appearance, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_DARK,
                onClick = { settings.updateTheme(AppSettings.THEME_DARK) },
                label = { Text(S.themeDark) }
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_LIGHT,
                onClick = { settings.updateTheme(AppSettings.THEME_LIGHT) },
                label = { Text(S.themeLight) }
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_SYSTEM,
                onClick = { settings.updateTheme(AppSettings.THEME_SYSTEM) },
                label = { Text(S.themeSystem) }
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onLock, modifier = Modifier.fillMaxWidth()) { Text(S.lockNow) }
        Text(S.moreSettings, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
