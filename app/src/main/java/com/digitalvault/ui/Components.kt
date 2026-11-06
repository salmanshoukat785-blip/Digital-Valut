package com.digitalvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.security.Strength

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun LogoBadge(size: Int = 72) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction = ImeAction.Next
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

@Composable
fun StrengthBar(strength: Strength) {
    val n = when (strength) { Strength.WEAK -> 1; Strength.MEDIUM -> 2; Strength.STRONG -> 3 }
    val color = when (strength) {
        Strength.WEAK -> Color(0xFFFF7A7A)
        Strength.MEDIUM -> Color(0xFFF5B14C)
        Strength.STRONG -> Color(0xFF6FD08C)
    }
    val label = when (strength) { Strength.WEAK -> "Weak"; Strength.MEDIUM -> "Medium"; Strength.STRONG -> "Strong" }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < n) color else MaterialTheme.colorScheme.outline)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 12.sp)
    }
}

@Composable
fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 18.dp)) {
        repeat(maxOf(4, count)) { i ->
            val base = Modifier.size(14.dp).clip(CircleShape)
            Box(
                if (i < count) base.background(MaterialTheme.colorScheme.primary)
                else base.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, plain: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (plain) Color.Transparent else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun Keypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    showFingerprint: Boolean,
    onFingerprint: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { d ->
                    KeyButton(onClick = { onDigit(d) }) { Text(d.toString(), fontSize = 24.sp) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (showFingerprint) {
                KeyButton(onClick = onFingerprint, plain = true) {
                    Icon(
                        Icons.Filled.Fingerprint, contentDescription = S.fingerprint,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
            KeyButton(onClick = { onDigit('0') }) { Text("0", fontSize = 24.sp) }
            KeyButton(onClick = onBackspace, plain = true) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = S.delete)
            }
        }
    }
}
