package com.digitalvault.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.VaultRepository
import com.digitalvault.ui.S
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Home(S.home, Icons.Filled.Home),
    Passwords(S.passwordsTab, Icons.Filled.VpnKey),
    Generator(S.generatorTab, Icons.Filled.Autorenew),
    Notes(S.notesTab, Icons.Filled.Description),
    Settings(S.settingsTab, Icons.Filled.Settings)
}

@Composable
fun MainScreen(repo: VaultRepository, onLock: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Data sirf is screen ke andar collect hota hai: lock par screen hatti to decrypt shuda data bhi chala jata hai
    val accountsFlow = remember { repo.observeAccounts() }
    val categoriesFlow = remember { repo.observeCategories() }
    val accounts by accountsFlow.collectAsState(initial = emptyList())
    val categories by categoriesFlow.collectAsState(initial = emptyList())

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableLongStateOf(FILTER_ALL) }
    var detailId by rememberSaveable { mutableLongStateOf(0L) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableLongStateOf(0L) }

    val detail = accounts.firstOrNull { it.id == detailId }

    when {
        editing -> {
            BackHandler { editing = false }
            AddEditScreen(
                initial = accounts.firstOrNull { it.id == editId },
                categories = categories,
                onSave = { acc, exp -> repo.saveAccount(acc, exp) },
                onClose = { editing = false }
            )
        }

        detail != null -> {
            BackHandler { detailId = 0L }
            AccountDetailScreen(
                account = detail,
                category = categories.firstOrNull { it.id == detail.categoryId },
                onBack = { detailId = 0L },
                onEdit = { editId = detail.id; editing = true },
                onDelete = {
                    val id = detail.id
                    detailId = 0L
                    scope.launch { repo.deleteAccount(id) }
                },
                onToggleFavorite = { scope.launch { repo.setFavorite(detail.id, !detail.isFavorite) } },
                onUsed = { scope.launch { repo.markUsed(detail.id) } }
            )
        }

        else -> {
            BackHandler(enabled = tabIndex != 0) { tabIndex = 0 }
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tabIndex == i,
                                onClick = { tabIndex = i },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tabIndex <= 1) {
                        FloatingActionButton(onClick = { editId = 0L; editing = true }) {
                            Icon(Icons.Filled.Add, contentDescription = S.quickAdd)
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (tabIndex) {
                        0 -> DashboardScreen(
                            accounts = accounts,
                            categories = categories,
                            onLock = onLock,
                            onOpenCategory = { filter = it; tabIndex = 1 },
                            onOpenAccount = { detailId = it },
                            onAdd = { editId = 0L; editing = true },
                            onGenerator = { tabIndex = 2 },
                            onNotes = { tabIndex = 3 }
                        )

                        1 -> PasswordsScreen(
                            accounts = accounts,
                            categories = categories,
                            filter = filter,
                            onFilter = { filter = it },
                            onOpen = { detailId = it },
                            onToggleFav = { a -> scope.launch { repo.setFavorite(a.id, !a.isFavorite) } }
                        )

                        2 -> ComingSoon(S.generatorTab, 5)
                        3 -> ComingSoon(S.notesTab, 5)
                        else -> ComingSoon(S.settingsTab, 7, onLock)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String, part: Int, onLock: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(S.comingSoon(part), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onLock != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onLock) { Text(S.lockNow) }
        }
    }
}
