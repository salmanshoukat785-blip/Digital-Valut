package com.digitalvault.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.Account
import com.digitalvault.data.CategoryEntity
import com.digitalvault.data.VaultRepository
import com.digitalvault.ui.S

@Composable
fun DashboardScreen(
    accounts: List<Account>,
    categories: List<CategoryEntity>,
    onLock: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAdd: () -> Unit,
    onGenerator: () -> Unit,
    onNotes: () -> Unit
) {
    val health = remember(accounts) { VaultRepository.health(accounts) }
    val attention = remember(health) {
        (health.weak + health.reused + health.expired + health.expiringSoon).toSet().size
    }
    val counts = remember(accounts) { accounts.groupingBy { it.categoryId }.eachCount() }
    val catById = remember(categories) { categories.associateBy { it.id } }
    val added = remember(accounts) { VaultRepository.recentlyAdded(accounts, 3) }
    val used = remember(accounts) { VaultRepository.recentlyUsed(accounts, 3) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(S.welcome, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(S.appName, style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.Lock, contentDescription = S.lockNow, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(S.totalPasswords, accounts.size.toString(), MaterialTheme.colorScheme.onSurface)
            StatCard(S.needAttention, attention.toString(), if (attention > 0) Amber else MaterialTheme.colorScheme.onSurface)
        }
        val parts = buildList {
            if (health.weak.isNotEmpty()) add("${health.weak.size} ${S.weak}")
            if (health.reused.isNotEmpty()) add("${health.reused.size} ${S.reused}")
            if (health.expired.isNotEmpty()) add("${health.expired.size} ${S.expired}")
            if (health.expiringSoon.isNotEmpty()) add("${health.expiringSoon.size} ${S.changeSoon}")
        }
        if (parts.isNotEmpty()) {
            Text(parts.joinToString(" · "), color = Amber, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.Add, S.quickAdd, onAdd)
            QuickAction(Icons.Filled.Autorenew, S.generatorTab, onGenerator)
            QuickAction(Icons.Filled.Description, S.notesTab, onNotes)
        }

        SectionTitle(S.categories)
        categories.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { c ->
                    CategoryCard(c, counts[c.id] ?: 0, { onOpenCategory(c.id) })
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (added.isNotEmpty()) {
            SectionTitle(S.recentlyAdded)
            added.forEach { a ->
                AccountRow(a, catById[a.categoryId], statusOf(a, health), { onOpenAccount(a.id) })
            }
        }
        if (used.isNotEmpty()) {
            SectionTitle(S.recentlyUsed)
            used.forEach { a ->
                AccountRow(a, catById[a.categoryId], statusOf(a, health), { onOpenAccount(a.id) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
}

@Composable
private fun RowScope.StatCard(label: String, value: String, valueColor: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 28.sp)
    }
}

@Composable
private fun RowScope.QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun RowScope.CategoryCard(c: CategoryEntity, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(categoryColor(c)),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(c.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(categoryLabel(c), fontSize = 13.sp, maxLines = 1)
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
