package com.digitalvault.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.security.ClipboardHelper
import com.digitalvault.security.PasswordGenerator
import com.digitalvault.security.PasswordStrength
import com.digitalvault.ui.S
import com.digitalvault.ui.StrengthBar
import kotlin.math.roundToInt

@Composable
fun GeneratorScreen(onUse: (String) -> Unit) {
    val ctx = LocalContext.current
    var length by remember { mutableIntStateOf(16) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var seed by remember { mutableIntStateOf(0) }

    val password = remember(length, upper, lower, digits, symbols, seed) {
        PasswordGenerator.generate(length, upper, lower, digits, symbols)
    }
    val active = listOf(upper, lower, digits, symbols).count { it }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(S.genTitle, style = MaterialTheme.typography.headlineSmall)

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(password, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
            StrengthBar(PasswordStrength.evaluate(password))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { seed++ }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.regenerate)
            }
            OutlinedButton(
                onClick = {
                    ClipboardHelper.copy(ctx, password)
                    Toast.makeText(ctx, S.copied, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.copy)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("${S.genLength}: $length")
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.roundToInt() },
                valueRange = 8f..32f,
                steps = 23
            )
        }

        // Aakhri chalti hui qism band nahi ho sakti
        ToggleRow(S.genUpper, upper) { if (it || active > 1) upper = it }
        ToggleRow(S.genLower, lower) { if (it || active > 1) lower = it }
        ToggleRow(S.genDigits, digits) { if (it || active > 1) digits = it }
        ToggleRow(S.genSymbols, symbols) { if (it || active > 1) symbols = it }

        Button(onClick = { onUse(password) }, modifier = Modifier.fillMaxWidth()) {
            Text(S.useForAccount)
        }
        Spacer(Modifier.size(80.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
