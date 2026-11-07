package com.digitalvault.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.digitalvault.DigitalVaultApp
import com.digitalvault.data.VaultRepository
import com.digitalvault.ui.AuthViewModel
import com.digitalvault.ui.S
import kotlinx.coroutines.launch

private enum class Tab(val label: () -> String, val icon: ImageVector) {
    Home({ S.home }, Icons.Filled.Home),
    Passwords({ S.passwordsTab }, Icons.Filled.VpnKey),
    Generator({ S.generatorTab }, Icons.Filled.Autorenew),
    Notes({ S.notesTab }, Icons.Filled.Description),
    Settings({ S.settingsTab }, Icons.Filled.Settings)
}

@Composable
fun MainScreen(repo: VaultRepository, vm: AuthViewModel, activity: FragmentActivity) {
    val scope = rememberCoroutineScope()
    val settings = (LocalContext.current.applicationContext as DigitalVaultApp).settings

    // Data sirf is screen ke andar collect hota hai: lock par screen hatti to decrypt shuda data bhi chala jata hai
    val accountsFlow = remember { repo.observeAccounts() }
    val categoriesFlow = remember { repo.observeCategories() }
    val notesFlow = remember { repo.observeNotes() }
    val accounts by accountsFlow.collectAsState(initial = emptyList())
    val categories by categoriesFlow.collectAsState(initial = emptyList())
    val notes by notesFlow.collectAsState(initial = emptyList())

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableLongStateOf(FILTER_ALL) }
    var detailId by rememberSaveable { mutableLongStateOf(0L) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableLongStateOf(0L) }
    var noteEditing by rememberSaveable { mutableStateOf(false) }
    var noteId by rememberSaveable { mutableLongStateOf(0L) }
    // Generator se aaya password: jaan-boojh kar saveable nahi
    var prefill by remember { mutableStateOf("") }

    val detail = accounts.firstOrNull { it.id == detailId }
    val currentNote = notes.firstOrNull { it.id == noteId }

    when {
        editing -> {
            BackHandler { editing = false; prefill = "" }
            AddEditScreen(
                initial = accounts.firstOrNull { it.id == editId },
                categories = categories,
                onSave = { acc, exp -> repo.saveAccount(acc, exp) },
                onClose = { editing = false; prefill = "" },
                prefillPassword = prefill
            )
        }

        noteEditing -> {
            BackHandler { noteEditing = false }
            NoteEditScreen(
                initial = currentNote,
                onSave = { n -> repo.saveNote(n) },
                onDelete = if (currentNote != null) {
                    {
                        val id = currentNote.id
                        noteEditing = false
                        noteId = 0L
                        scope.launch { repo.deleteNote(id) }
                    }
                } else null,
                onClose = { noteEditing = false }
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
                                icon = { Icon(t.icon, contentDescription = t.label()) },
                                label = { Text(t.label(), fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tabIndex <= 1) {
                        FloatingActionButton(onClick = { editId = 0L; editing = true }) {
                            Icon(Icons.Filled.Add, contentDescription = S.quickAdd)
                        }
                    } else if (tabIndex == 3) {
                        FloatingActionButton(onClick = { noteId = 0L; noteEditing = true }) {
                            Icon(Icons.Filled.Add, contentDescription = S.addNote)
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (tabIndex) {
                        0 -> DashboardScreen(
                            accounts = accounts,
                            categories = categories,
                            onLock = { vm.lockNow() },
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

                        2 -> GeneratorScreen(onUse = { pw -> prefill = pw; editId = 0L; editing = true })

                        3 -> NotesScreen(notes = notes, onOpen = { noteId = it; noteEditing = true })

                        else -> SettingsScreen(vm = vm, repo = repo, settings = settings, activity = activity)
                    }
                }
            }
        }
    }
}
