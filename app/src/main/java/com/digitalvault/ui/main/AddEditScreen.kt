package com.digitalvault.ui.main

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalvault.data.Account
import com.digitalvault.data.CategoryEntity
import com.digitalvault.data.VaultRepository
import com.digitalvault.security.ClipboardHelper
import com.digitalvault.security.PasswordGenerator
import com.digitalvault.security.PasswordStrength
import com.digitalvault.ui.S
import com.digitalvault.ui.StrengthBar
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AddEditScreen(
    initial: Account?,
    categories: List<CategoryEntity>,
    onSave: suspend (Account, Long?) -> Unit,
    onClose: () -> Unit,
    prefillPassword: String = ""
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val key = initial?.id ?: 0L

    var title by remember(key) { mutableStateOf(initial?.title ?: "") }
    var username by remember(key) { mutableStateOf(initial?.username ?: "") }
    var email by remember(key) { mutableStateOf(initial?.email ?: "") }
    var password by remember(key) { mutableStateOf(initial?.password ?: prefillPassword) }
    var url by remember(key) { mutableStateOf(initial?.url ?: "") }
    var notes by remember(key) { mutableStateOf(initial?.notes ?: "") }
    var categoryId by remember(key) {
        mutableLongStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var customExpiry by remember(key) { mutableStateOf<Long?>(null) }
    var showPw by remember { mutableStateOf(prefillPassword.isNotEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val selectedCategory = categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull()
    val canSave = title.isNotBlank() && password.isNotEmpty() && !saving

    val ce = customExpiry
    val pwChanged = initial != null && initial.password != password
    val expiryText = when {
        ce != null -> formatDate(ce)
        initial != null && !pwChanged -> formatDate(initial.expiryAt) + " " + S.unchanged
        else -> S.expiryDefault
    }

    fun pickDate() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = customExpiry ?: initial?.expiryAt
                ?: (System.currentTimeMillis() + VaultRepository.DEFAULT_EXPIRY_MS)
        }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                customExpiry = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = S.close) }
            Text(
                if (initial == null) S.addTitle else S.editTitle,
                style = MaterialTheme.typography.titleLarge
            )
        }

        OutlinedTextField(
            value = title, onValueChange = { title = it }, label = { Text(S.fTitle) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username, onValueChange = { username = it }, label = { Text(S.fUsername) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text(S.fEmail) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text(S.fPassword) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(
                            if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPw) S.hide else S.show
                        )
                    }
                    IconButton(onClick = { password = PasswordGenerator.generate(16); showPw = true }) {
                        Icon(Icons.Filled.Autorenew, contentDescription = S.generate, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        if (password.isNotEmpty()) {
                            ClipboardHelper.copy(ctx, password)
                            Toast.makeText(ctx, S.copied, Toast.LENGTH_SHORT).show()
                        }
                    }) { Icon(Icons.Filled.ContentCopy, contentDescription = S.copy) }
                }
            }
        )
        if (password.isNotEmpty()) StrengthBar(PasswordStrength.evaluate(password))

        OutlinedTextField(
            value = url, onValueChange = { url = it }, label = { Text(S.fUrl) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
        )

        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "", onValueChange = {}, readOnly = true,
                label = { Text(S.fCategory) }, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
            )
            Box(Modifier.matchParentSize().clickable { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                categories.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name) },
                        onClick = { categoryId = c.id; menuOpen = false },
                        leadingIcon = {
                            Icon(categoryIcon(c.icon), contentDescription = null, tint = categoryColor(c))
                        }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(S.fExpiry, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(expiryText)
            }
            TextButton(onClick = { pickDate() }) { Text(S.expiryChange) }
            if (ce != null) TextButton(onClick = { customExpiry = null }) { Text(S.expiryReset) }
        }

        OutlinedTextField(
            value = notes, onValueChange = { notes = it }, label = { Text(S.fNotes) },
            modifier = Modifier.fillMaxWidth(), minLines = 3
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                saving = true
                val cid = selectedCategory?.id ?: 0L
                scope.launch {
                    onSave(
                        Account(
                            id = initial?.id ?: 0L, title = title.trim(), username = username.trim(),
                            email = email.trim(), password = password, url = url.trim(), notes = notes,
                            categoryId = cid, isFavorite = initial?.isFavorite ?: false
                        ),
                        customExpiry
                    )
                    onClose()
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(S.save)
        }
        Spacer(Modifier.height(24.dp))
    }
}
