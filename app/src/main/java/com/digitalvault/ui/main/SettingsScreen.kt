package com.digitalvault.ui.main

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.digitalvault.data.BackupManager
import com.digitalvault.data.CsvManager
import com.digitalvault.data.VaultRepository
import com.digitalvault.data.VaultSnapshot
import com.digitalvault.security.Biometric
import com.digitalvault.security.ClipboardHelper
import com.digitalvault.security.KeyVault
import com.digitalvault.security.PasswordStrength
import com.digitalvault.settings.AppSettings
import com.digitalvault.ui.AuthViewModel
import com.digitalvault.ui.PasswordField
import com.digitalvault.ui.PinField
import com.digitalvault.ui.S
import com.digitalvault.ui.StrengthBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Dlg { None, ChangePin, ChangeMaster, AutoLock, BackupPw, RestorePw, CsvWarn, NewRecovery }

@Composable
fun SettingsScreen(
    vm: AuthViewModel,
    repo: VaultRepository,
    settings: AppSettings,
    activity: FragmentActivity
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dlg by remember { mutableStateOf(Dlg.None) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPw by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pw = pendingPw
        pendingPw = ""
        if (uri != null && pw.isNotEmpty()) scope.launch { info = doBackup(ctx, repo, uri, pw) }
    }
    val restorePick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { pendingUri = uri; dlg = Dlg.RestorePw }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { info = doCsvExport(ctx, repo, uri) }
    }
    val csvImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { info = doCsvImport(ctx, repo, uri) }
    }

    fun startFingerprintEnroll() {
        val cipher = if (Biometric.isAvailable(ctx)) vm.enrollCipher() else null
        if (cipher == null) {
            info = S.fingerprintUnavailable
        } else {
            Biometric.prompt(
                activity, cipher, S.fingerprintEnrollTitle, S.cancel,
                onSuccess = { vm.biometricEnrolled(it) },
                onError = { info = it }
            )
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(S.settingsTab, style = MaterialTheme.typography.headlineSmall)

        Label(S.appearance)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_DARK,
                onClick = { settings.updateTheme(AppSettings.THEME_DARK) }, label = { Text(S.themeDark) }
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_LIGHT,
                onClick = { settings.updateTheme(AppSettings.THEME_LIGHT) }, label = { Text(S.themeLight) }
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_SYSTEM,
                onClick = { settings.updateTheme(AppSettings.THEME_SYSTEM) }, label = { Text(S.themeSystem) }
            )
        }

        Label(S.language)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.language == 0,
                onClick = { settings.updateLanguage(0); S.lang = 0 }, label = { Text(S.langEnglish) }
            )
            FilterChip(
                selected = settings.language == 1,
                onClick = { settings.updateLanguage(1); S.lang = 1 }, label = { Text(S.langUrdu) }
            )
        }

        Label(S.security)
        SettingRow(S.changePin) { dlg = Dlg.ChangePin }
        SettingRow(S.changeMaster) { dlg = Dlg.ChangeMaster }
        SwitchRow(S.fingerprintUnlock, vm.biometricEnabled) { on ->
            if (on) startFingerprintEnroll() else vm.disableBiometric()
        }
        SettingRow(S.autoLock, autoLockLabel(settings.autoLockSeconds)) { dlg = Dlg.AutoLock }
        SwitchRow(S.blockScreens, settings.blockScreenshots) { settings.updateBlockScreenshots(it) }
        SettingRow(S.newRecoveryKey) { dlg = Dlg.NewRecovery }

        Label(S.backupSection)
        SettingRow(S.createBackup) { dlg = Dlg.BackupPw }
        SettingRow(S.restoreBackup) { vm.suppressLockOnce(); restorePick.launch(arrayOf("*/*")) }
        SettingRow(S.exportCsv) { dlg = Dlg.CsvWarn }
        SettingRow(S.importCsv) { vm.suppressLockOnce(); csvImport.launch(arrayOf("*/*")) }
        Text(S.driveSoon, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Label(S.appInfo)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("${S.appName}  ·  ${S.version} $version")
            Text(S.infoOffline, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(S.infoEncryption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = { vm.lockNow() }, modifier = Modifier.fillMaxWidth()) { Text(S.lockNow) }
        Spacer(Modifier.height(16.dp))
    }

    when (dlg) {
        Dlg.ChangePin -> ChangePinDialog(
            vm, onDone = { dlg = Dlg.None; info = S.pinChanged }, onDismiss = { dlg = Dlg.None }
        )
        Dlg.ChangeMaster -> ChangeMasterDialog(
            vm, onDone = { dlg = Dlg.None; info = S.masterChanged }, onDismiss = { dlg = Dlg.None }
        )
        Dlg.AutoLock -> AutoLockDialog(
            current = settings.autoLockSeconds,
            onPick = { settings.updateAutoLock(it); dlg = Dlg.None },
            onDismiss = { dlg = Dlg.None }
        )
        Dlg.BackupPw -> BackupPwDialog(
            onConfirm = { pw ->
                pendingPw = pw
                dlg = Dlg.None
                vm.suppressLockOnce()
                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                createBackup.launch("digital-vault-$stamp.dvbackup")
            },
            onDismiss = { dlg = Dlg.None }
        )
        Dlg.RestorePw -> RestorePwDialog(
            onSubmit = { pw -> doRestore(ctx, repo, pendingUri, pw) },
            onResult = { msg -> dlg = Dlg.None; info = msg },
            onDismiss = { dlg = Dlg.None }
        )
        Dlg.CsvWarn -> CsvWarnDialog(
            vm,
            onConfirmed = {
                dlg = Dlg.None
                vm.suppressLockOnce()
                csvExport.launch("digital-vault-export.csv")
            },
            onDismiss = { dlg = Dlg.None }
        )
        Dlg.NewRecovery -> NewRecoveryDialog(vm, onDismiss = { dlg = Dlg.None })
        Dlg.None -> Unit
    }

    info?.let { msg ->
        AlertDialog(
            onDismissRequest = { info = null },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { info = null }) { Text(S.ok) } }
        )
    }
}

// ---------------------------------------------------------------- rows

@Composable
private fun Label(text: String) {
    Text(
        text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun autoLockLabel(seconds: Int): String = when (seconds) {
    0 -> S.alImmediate
    30 -> S.al30
    60 -> S.al1m
    else -> S.al5m
}

// ---------------------------------------------------------------- dialogs

@Composable
private fun FormDialog(
    title: String,
    confirmLabel: String,
    enabled: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled && !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(S.cancel) } }
    )
}

@Composable
private fun ChangePinDialog(vm: AuthViewModel, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var master by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ok = master.isNotEmpty() && KeyVault.isPinFormatOk(pin.toCharArray()) && pin == pin2
    FormDialog(
        title = S.changePin, confirmLabel = S.save, enabled = ok, busy = busy, error = error,
        onDismiss = onDismiss,
        onConfirm = {
            busy = true
            error = null
            scope.launch {
                val err = vm.changePin(master, pin)
                busy = false
                if (err == null) onDone() else error = err
            }
        }
    ) {
        Text(S.confirmWithMaster, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PasswordField(master, { master = it }, S.currentMaster)
        PinField(pin, { pin = it }, S.newPinLabel)
        PinField(pin2, { pin2 = it }, S.pinConfirm)
        if (pin2.isNotEmpty() && pin != pin2) Text(S.mismatch, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
}

@Composable
private fun ChangeMasterDialog(vm: AuthViewModel, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ok = old.isNotEmpty() && KeyVault.isMasterPasswordAcceptable(new.toCharArray()) && new == new2
    FormDialog(
        title = S.changeMaster, confirmLabel = S.change, enabled = ok, busy = busy, error = error,
        onDismiss = onDismiss,
        onConfirm = {
            busy = true
            error = null
            scope.launch {
                val err = vm.changeMaster(old, new)
                busy = false
                if (err == null) onDone() else error = err
            }
        }
    ) {
        PasswordField(old, { old = it }, S.currentMaster)
        PasswordField(new, { new = it }, S.newMasterLabel)
        if (new.isNotEmpty()) StrengthBar(PasswordStrength.evaluate(new))
        PasswordField(new2, { new2 = it }, S.confirmLabel, ImeAction.Done)
        if (new2.isNotEmpty() && new != new2) Text(S.mismatch, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
}

@Composable
private fun AutoLockDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(S.autoLockTitle) },
        text = {
            Column {
                listOf(0, 30, 60, 300).forEach { sec ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(sec) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == sec, onClick = { onPick(sec) })
                        Text(autoLockLabel(sec))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(S.cancel) } }
    )
}

@Composable
private fun BackupPwDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val ok = pw.length >= 8 && pw == pw2
    FormDialog(
        title = S.backupPwTitle, confirmLabel = S.create, enabled = ok, busy = false, error = null,
        onDismiss = onDismiss, onConfirm = { onConfirm(pw) }
    ) {
        Text(S.backupPwHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PasswordField(pw, { pw = it }, S.backupPwLabel)
        if (pw.isNotEmpty()) StrengthBar(PasswordStrength.evaluate(pw))
        PasswordField(pw2, { pw2 = it }, S.confirmLabel, ImeAction.Done)
        if (pw2.isNotEmpty() && pw != pw2) Text(S.mismatch, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
}

@Composable
private fun RestorePwDialog(
    onSubmit: suspend (String) -> Pair<Boolean, String>,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = S.restorePwTitle, confirmLabel = S.restore, enabled = pw.isNotEmpty(), busy = busy, error = error,
        onDismiss = onDismiss,
        onConfirm = {
            busy = true
            error = null
            scope.launch {
                val (ok, msg) = onSubmit(pw)
                busy = false
                if (ok) onResult(msg) else error = msg
            }
        }
    ) {
        PasswordField(pw, { pw = it }, S.backupPwLabel, ImeAction.Done)
    }
}

@Composable
private fun CsvWarnDialog(vm: AuthViewModel, onConfirmed: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var master by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = S.csvWarnTitle, confirmLabel = S.export, enabled = master.isNotEmpty(), busy = busy, error = error,
        onDismiss = onDismiss,
        onConfirm = {
            busy = true
            error = null
            scope.launch {
                val err = vm.reauth(master)
                busy = false
                if (err == null) onConfirmed() else error = err
            }
        }
    ) {
        Text(S.csvWarnBody, fontSize = 13.sp)
        Text(S.confirmWithMaster, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PasswordField(master, { master = it }, S.masterLabel, ImeAction.Done)
    }
}

@Composable
private fun NewRecoveryDialog(vm: AuthViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var master by remember { mutableStateOf("") }
    var key by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val shown = key
    if (shown == null) {
        FormDialog(
            title = S.newRecoveryKey, confirmLabel = S.continueLabel, enabled = master.isNotEmpty(),
            busy = busy, error = error, onDismiss = onDismiss,
            onConfirm = {
                busy = true
                error = null
                scope.launch {
                    val r = vm.newRecoveryKey(master)
                    busy = false
                    if (r.error == null) key = r.value else error = r.error
                }
            }
        ) {
            Text(S.newRecoveryWarn, fontSize = 13.sp)
            Text(S.confirmWithMaster, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            PasswordField(master, { master = it }, S.masterLabel, ImeAction.Done)
        }
    } else {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(S.recoveryTitle) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(S.recoveryHint, fontSize = 13.sp)
                    shown.split("-").chunked(4).forEach { row ->
                        Text(row.joinToString("  "), fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                    }
                    OutlinedButton(onClick = { ClipboardHelper.copy(ctx, shown) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(S.copyRecovery)
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(S.done) } }
        )
    }
}

// ---------------------------------------------------------------- backup / CSV kaam

private suspend fun doBackup(ctx: Context, repo: VaultRepository, uri: Uri, pw: String): String =
    withContext(Dispatchers.IO) {
        try {
            val bytes = BackupManager.encrypt(repo.snapshot(), pw.toCharArray())
            val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: return@withContext S.backupFailed
            out.use { it.write(bytes) }
            S.backupDone
        } catch (e: Exception) {
            S.backupFailed
        }
    }

/** (kamyab?, paigham) */
private suspend fun doRestore(ctx: Context, repo: VaultRepository, uri: Uri?, pw: String): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
        if (uri == null) return@withContext false to S.backupFailed
        try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext false to S.backupFailed
            if (bytes.size > 50_000_000) return@withContext false to S.restoreWrong
            val snap = BackupManager.decrypt(bytes, pw.toCharArray()) ?: return@withContext false to S.restoreWrong
            val r = repo.importSnapshot(snap)
            true to S.restoreDone(r.accountsAdded, r.accountsSkipped, r.notesAdded)
        } catch (e: Exception) {
            false to S.backupFailed
        }
    }

private suspend fun doCsvExport(ctx: Context, repo: VaultRepository, uri: Uri): String =
    withContext(Dispatchers.IO) {
        try {
            val snap = repo.snapshot()
            val names = snap.categories.associate { it.id to it.name }
            val text = CsvManager.export(snap.accounts) { names[it] ?: "Other" }
            val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: return@withContext S.backupFailed
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            S.csvExported
        } catch (e: Exception) {
            S.backupFailed
        }
    }

private suspend fun doCsvImport(ctx: Context, repo: VaultRepository, uri: Uri): String =
    withContext(Dispatchers.IO) {
        try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext S.csvBad
            if (bytes.size > 20_000_000) return@withContext S.csvBad
            val cats = repo.categoryList()
            val otherId = cats.firstOrNull { it.slug == "other" }?.id ?: cats.firstOrNull()?.id ?: 0L
            val parsed = CsvManager.toAccounts(CsvManager.parse(String(bytes, Charsets.UTF_8))) { name ->
                cats.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id ?: otherId
            } ?: return@withContext S.csvBad
            val r = repo.importSnapshot(VaultSnapshot(emptyList(), parsed.accounts, emptyList()))
            S.csvImported(r.accountsAdded, r.accountsSkipped + parsed.skippedRows)
        } catch (e: Exception) {
            S.csvBad
        }
    }
