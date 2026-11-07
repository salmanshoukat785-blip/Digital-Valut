package com.digitalvault.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.SecureNote
import com.digitalvault.ui.S
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(notes: List<SecureNote>, onOpen: (Long) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            S.notesTitle, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    S.noNotes, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { n ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpen(n.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n.title.ifBlank { S.untitled }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val preview = n.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
                            if (preview.isNotEmpty()) {
                                Text(
                                    preview, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                formatDate(n.updatedAt), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditScreen(
    initial: SecureNote?,
    onSave: suspend (SecureNote) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val key = initial?.id ?: 0L
    var title by remember(key) { mutableStateOf(initial?.title ?: "") }
    var body by remember(key) { mutableStateOf(initial?.body ?: "") }
    var saving by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = S.close) }
            Text(
                if (initial == null) S.addNote else S.editNote,
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)
            )
            if (onDelete != null) {
                IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, contentDescription = S.delete) }
            }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it }, label = { Text(S.noteTitleLabel) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body, onValueChange = { body = it }, label = { Text(S.noteBodyLabel) },
            modifier = Modifier.fillMaxWidth(), minLines = 10
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                saving = true
                scope.launch {
                    onSave(SecureNote(id = initial?.id ?: 0L, title = title.trim(), body = body))
                    onClose()
                }
            },
            enabled = (title.isNotBlank() || body.isNotBlank()) && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp) else Text(S.save)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(S.confirmDeleteNoteTitle) },
            text = { Text(S.confirmDeleteBody) },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text(S.delete) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(S.cancel) } }
        )
    }
}
