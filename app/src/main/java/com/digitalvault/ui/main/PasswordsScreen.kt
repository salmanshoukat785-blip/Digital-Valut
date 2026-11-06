package com.digitalvault.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalvault.data.Account
import com.digitalvault.data.CategoryEntity
import com.digitalvault.data.VaultRepository
import com.digitalvault.ui.S

const val FILTER_ALL = -1L
const val FILTER_FAV = -2L

@Composable
fun PasswordsScreen(
    accounts: List<Account>,
    categories: List<CategoryEntity>,
    filter: Long,
    onFilter: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onToggleFav: (Account) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val health = remember(accounts) { VaultRepository.health(accounts) }
    val catById = remember(categories) { categories.associateBy { it.id } }
    val counts = remember(accounts) { accounts.groupingBy { it.categoryId }.eachCount() }
    val favCount = remember(accounts) { accounts.count { it.isFavorite } }
    val visible = remember(accounts, query, filter) {
        VaultRepository.search(accounts, query)
            .filter {
                when (filter) {
                    FILTER_ALL -> true
                    FILTER_FAV -> it.isFavorite
                    else -> it.categoryId == filter
                }
            }
            .sortedWith(compareByDescending<Account> { it.isFavorite }.thenBy { it.title.lowercase() })
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            placeholder = { Text(S.searchHint) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = S.close) }
                }
            },
            singleLine = true
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filter == FILTER_ALL, onClick = { onFilter(FILTER_ALL) },
                    label = { Text("${S.all} ${accounts.size}") }
                )
            }
            item {
                FilterChip(
                    selected = filter == FILTER_FAV, onClick = { onFilter(FILTER_FAV) },
                    label = { Text("${S.favorites} $favCount") },
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Amber) }
                )
            }
            items(categories, key = { it.id }) { c ->
                FilterChip(
                    selected = filter == c.id, onClick = { onFilter(c.id) },
                    label = { Text("${c.name} ${counts[c.id] ?: 0}") },
                    leadingIcon = {
                        Icon(categoryIcon(c.icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = categoryColor(c))
                    }
                )
            }
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (accounts.isEmpty()) S.noPasswords else S.noResults,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible, key = { it.id }) { a ->
                    AccountRow(
                        a, catById[a.categoryId], statusOf(a, health),
                        onClick = { onOpen(a.id) }, onToggleFavorite = { onToggleFav(a) }
                    )
                }
            }
        }
    }
}
