package com.digitalvault.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.Account
import com.digitalvault.data.CategoryEntity
import com.digitalvault.data.PasswordHealth
import java.text.DateFormat
import java.util.Date

val Amber = Color(0xFFF5B14C)
val Red = Color(0xFFFF7A7A)

fun categoryIcon(key: String): ImageVector = when (key) {
    "group" -> Icons.Filled.Group
    "mail" -> Icons.Filled.Mail
    "bank" -> Icons.Filled.AccountBalance
    "language" -> Icons.Filled.Language
    "work" -> Icons.Filled.Work
    "person" -> Icons.Filled.Person
    "wifi" -> Icons.Filled.Wifi
    "more" -> Icons.Filled.MoreHoriz
    else -> Icons.Filled.Folder
}

fun categoryColor(c: CategoryEntity?): Color = if (c == null) Color(0xFF757575) else Color(c.color)

fun formatDate(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

fun statusOf(a: Account, h: PasswordHealth): Pair<String, Color>? = when {
    a.id in h.expired -> "Expired" to Red
    a.id in h.expiringSoon -> "Change soon" to Amber
    a.id in h.weak -> "Weak" to Red
    a.id in h.reused -> "Reused" to Amber
    else -> null
}

@Composable
fun Avatar(title: String, color: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AccountRow(
    account: Account,
    category: CategoryEntity?,
    status: Pair<String, Color>?,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(account.title, categoryColor(category))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(account.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = account.username.ifBlank { account.email }
            if (sub.isNotBlank()) {
                Text(
                    sub, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (status != null) Text(status.first, color = status.second, fontSize = 11.sp)
        }
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (account.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (account.isFavorite) Amber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
