package com.digitalvault.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.Account
import com.digitalvault.data.CategoryEntity
import com.digitalvault.security.ClipboardHelper
import com.digitalvault.security.PasswordStrength
import com.digitalvault.ui.S
import com.digitalvault.ui.StrengthBar

@Composable
fun AccountDetailScreen(
    account: Account,
    category: CategoryEntity?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUsed: () -> Unit
) {
    val ctx = LocalContext.current
    var showPw by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    fun copy(text: String) {
        ClipboardHelper.copy(ctx, text)
        Toast.makeText(ctx, S.copied, Toast.LENGTH_SHORT).show()
        onUsed()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back) }
            Avatar(account.title, categoryColor(category), size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                account.title, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (account.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = S.favorites,
                    tint = if (account.isFavorite) Amber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = S.edit) }
            IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, contentDescription = S.delete) }
        }

        if (account.username.isNotBlank()) {
            DetailField(S.fUsername, account.username) { CopyButton { copy(account.username) } }
        }
        if (account.email.isNotBlank()) {
            DetailField(S.fEmail, account.email) { CopyButton { copy(account.email) } }
        }
        DetailField(S.fPassword, if (showPw) account.password else "••••••••••••", mono = true) {
            IconButton(onClick = { showPw = !showPw }) {
                Icon(
                    if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPw) S.hide else S.show
                )
            }
            CopyButton { copy(account.password) }
        }
        StrengthBar(PasswordStrength.evaluate(account.password))
        if (account.url.isNotBlank()) DetailField(S.fUrl, account.url)
        DetailField(S.fCategory, category?.name ?: "-")
        DetailField(S.dateAdded, formatDate(account.createdAt))
        DetailField(S.expires, formatDate(account.expiryAt))
        if (account.notes.isNotBlank()) DetailField(S.fNotes, account.notes)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(S.confirmDeleteTitle) },
            text = { Text(S.confirmDeleteBody) },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text(S.delete) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(S.cancel) } }
        )
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.ContentCopy, contentDescription = S.copy) }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    mono: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default)
        }
        actions()
    }
}
