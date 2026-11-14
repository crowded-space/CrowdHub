package app.crowdhub.feature.profiles

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.connector.ffscouter.FfConsentDialog
import app.crowdhub.connector.ffscouter.FfDefaults
import app.crowdhub.connector.ffscouter.FfKeyInfo
import app.crowdhub.connector.ffscouter.FfRepository
import app.crowdhub.core.browser.SessionMonitor
import app.crowdhub.core.browser.WebProfileManager
import app.crowdhub.core.browser.WebSessionState
import app.crowdhub.core.db.ProviderAccountDao
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.profile.CredentialValidationResult
import app.crowdhub.core.profile.KeySaveOutcome
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.SecureScreenEffect
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.vault.SecretVault
import app.crowdhub.core.vault.VaultResult
import app.crowdhub.provider.ValidationResult
import app.crowdhub.provider.torn.TornProvider
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    accountId: String,
    accountDao: ProviderAccountDao,
    profileRepository: ProfileRepository,
    tornProvider: TornProvider,
    secretVault: SecretVault,
    webProfileManager: WebProfileManager,
    sessionMonitor: SessionMonitor,
    settingsStore: SettingsStore,
    ffRepository: FfRepository,
    clock: Clock,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    SecureScreenEffect()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val account by accountDao.observeById(accountId).collectAsStateWithLifecycle(null)
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(
        app.crowdhub.core.settings.AppPreferences()
    )
    val webSessionState by sessionMonitor.state.collectAsStateWithLifecycle()
    val ffEnabled by ffRepository.observeEnabled(accountId).collectAsStateWithLifecycle(false)

    var maskedKey by remember { mutableStateOf("Loading...") }
    var hasWebCreds by remember { mutableStateOf(false) }
    var ffKeyInfo by remember {
        mutableStateOf(
            FfKeyInfo(
                hasCustomKey = false,
                maskedCustomKey = "Not set",
                hasTornFallback = false,
                maskedTornKey = "Not set",
                isEnabled = false,
                consentAtMs = null
            )
        )
    }

    var isTestingTorn by remember { mutableStateOf(false) }
    var tornTestMessage by remember { mutableStateOf<String?>(null) }
    var tornTestIsError by remember { mutableStateOf(false) }

    var showReplaceDialog by remember { mutableStateOf(false) }
    var isSavingTornKey by remember { mutableStateOf(false) }
    var newKeyInput by remember { mutableStateOf("") }
    var replaceError by remember { mutableStateOf<String?>(null) }
    var pendingDifferentPlayer by remember { mutableStateOf<KeySaveOutcome.DifferentPlayerWarning?>(null) }

    var showWebLoginDialog by remember { mutableStateOf(false) }
    var webUserInput by remember { mutableStateOf("") }
    var webPassInput by remember { mutableStateOf("") }

    var showFfConsent by remember { mutableStateOf(false) }
    var showFfKeyDialog by remember { mutableStateOf(false) }
    var ffKeyInput by remember { mutableStateOf("") }
    var ffKeyDialogError by remember { mutableStateOf<String?>(null) }
    var isSavingFfKey by remember { mutableStateOf(false) }
    var isTestingFf by remember { mutableStateOf(false) }
    var ffStatusMessage by remember { mutableStateOf<String?>(null) }
    var ffIsError by remember { mutableStateOf(false) }
    var showDisableFfConfirm by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    suspend fun reloadVaultSnapshot() {
        maskedKey = SecretVault.maskSecret(secretVault.get(SecretVault.apiKeyAlias(accountId)))
        hasWebCreds = secretVault.has(SecretVault.webUserAlias(accountId)) &&
            secretVault.has(SecretVault.webPassAlias(accountId))
        ffKeyInfo = ffRepository.getKeyInfo(accountId)
    }

    LaunchedEffect(accountId, account?.lastValidatedAt, ffEnabled) {
        reloadVaultSnapshot()
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Account & API Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val acct = account
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountSummaryCard(
                account = acct,
                ffKeyInfo = ffKeyInfo,
                ffHasError = ffIsError
            )

            TornApiManagerCard(
                account = acct,
                maskedKey = maskedKey,
                isTesting = isTestingTorn,
                testMessage = tornTestMessage,
                testIsError = tornTestIsError,
                clock = clock,
                onReplaceKey = {
                    newKeyInput = ""
                    replaceError = null
                    showReplaceDialog = true
                },
                onTestKey = {
                    scope.launch {
                        isTestingTorn = true
                        tornTestMessage = null
                        val outcome = profileRepository.reverifyAccountKey(accountId) { id, secret ->
                            when (val res = tornProvider.validateCredential(id, secret)) {
                                is ValidationResult.Valid -> CredentialValidationResult(
                                    res.externalId, res.displayName, res.accessLevel, null
                                )
                                is ValidationResult.Invalid -> CredentialValidationResult(
                                    null, null, null, res.error
                                )
                            }
                        }
                        when (outcome) {
                            is KeySaveOutcome.Saved -> {
                                tornTestIsError = false
                                tornTestMessage = "Torn API key verified (${outcome.account.accessLevel ?: "Limited"})."
                            }
                            is KeySaveOutcome.Error -> {
                                tornTestIsError = true
                                tornTestMessage = outcome.message
                            }
                            is KeySaveOutcome.DifferentPlayerWarning -> Unit
                        }
                        reloadVaultSnapshot()
                        isTestingTorn = false
                    }
                },
                onRemoveKey = {
                    scope.launch {
                        profileRepository.removeKeyOnly(accountId)
                        tornTestIsError = false
                        tornTestMessage = "Torn API key removed."
                        reloadVaultSnapshot()
                    }
                },
                onOpenTornApiPage = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TornUrls.API_KEY_PREFS)))
                    }
                }
            )

            FfScouterApiManagerCard(
                ffKeyInfo = ffKeyInfo,
                isTesting = isTestingFf,
                statusMessage = ffStatusMessage,
                isError = ffIsError,
                onSetOrReplaceKey = {
                    ffKeyInput = ""
                    ffKeyDialogError = null
                    showFfKeyDialog = true
                },
                onTestConnection = {
                    scope.launch {
                        isTestingFf = true
                        ffStatusMessage = null
                        val ownId = account?.externalId?.toLongOrNull()
                        when (val res = ffRepository.testConnection(accountId, ownId)) {
                            is ApiResult.Success -> {
                                ffIsError = false
                                ffStatusMessage = "FFScouter connection verified."
                            }
                            is ApiResult.Failure -> {
                                ffIsError = true
                                ffStatusMessage = res.error.message
                            }
                        }
                        reloadVaultSnapshot()
                        isTestingFf = false
                    }
                },
                onRemoveCustomKey = {
                    scope.launch {
                        ffRepository.removeCustomKey(accountId)
                        ffIsError = false
                        ffStatusMessage = if (ffKeyInfo.isEnabled && ffKeyInfo.hasTornFallback) {
                            "Custom FFScouter key removed; now using Torn API key."
                        } else {
                            "Custom FFScouter key removed."
                        }
                        reloadVaultSnapshot()
                    }
                },
                onEnable = { showFfConsent = true },
                onDisable = { showDisableFfConfirm = true },
                onOpenSite = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FfDefaults.SITE_URL)))
                    }
                }
            )

            CrowCard {
                Text(
                    text = "Web session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(4.dp))
                val sessionLabel = when (webSessionState) {
                    WebSessionState.LOGGED_IN -> "Logged in"
                    WebSessionState.LOGGED_OUT -> "Logged out"
                    WebSessionState.UNKNOWN -> "Unknown (open Browser to check)"
                }
                Text(
                    text = "Status: $sessionLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrowColors.TextMuted
                )
                if (!webProfileManager.isMultiProfileSupported()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Session isolation not supported on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.AccentD
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto re-login (opt-in)", color = CrowColors.Text)
                        Text(
                            text = if (hasWebCreds) "Saved encrypted credentials" else "No credentials saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.TextMuted
                        )
                    }
                    Switch(
                        checked = prefs.autoLoginEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showWebLoginDialog = true
                            } else {
                                scope.launch { settingsStore.setAutoLogin(false, false) }
                            }
                        }
                    )
                }
                if (prefs.autoLoginEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-submit login form", color = CrowColors.Text)
                        Switch(
                            checked = prefs.autoLoginAutoSubmit,
                            onCheckedChange = { submit ->
                                scope.launch { settingsStore.setAutoLogin(true, submit) }
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showWebLoginDialog = true }) {
                            Text("Update credentials")
                        }
                        if (hasWebCreds) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        secretVault.delete(SecretVault.webUserAlias(accountId))
                                        secretVault.delete(SecretVault.webPassAlias(accountId))
                                        settingsStore.setAutoLogin(false, false)
                                        hasWebCreds = false
                                    }
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            CrowCard(borderColor = CrowColors.Danger.copy(alpha = 0.45f)) {
                Text(
                    text = "Danger zone",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrowColors.Danger
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrowColors.Danger, contentColor = CrowColors.CrowBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete account")
                }
            }
        }
    }

    if (showReplaceDialog) {
        TornKeyReplaceDialog(
            keyInput = newKeyInput,
            error = replaceError,
            isBusy = isSavingTornKey,
            onKeyChange = {
                newKeyInput = it
                replaceError = null
            },
            onOpenTornApiPage = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TornUrls.API_KEY_PREFS)))
                }
            },
            onConfirm = {
                scope.launch {
                    isSavingTornKey = true
                    val outcome = profileRepository.replaceAccountKey(
                        accountId = accountId,
                        rawKey = newKeyInput,
                        confirmDifferentPlayerReset = false
                    ) { id, secret ->
                        when (val res = tornProvider.validateCredential(id, secret)) {
                            is ValidationResult.Valid -> CredentialValidationResult(
                                res.externalId, res.displayName, res.accessLevel, null
                            )
                            is ValidationResult.Invalid -> CredentialValidationResult(
                                null, null, null, res.error
                            )
                        }
                    }
                    when (outcome) {
                        is KeySaveOutcome.Saved -> {
                            newKeyInput = ""
                            tornTestIsError = false
                            tornTestMessage = "Torn API key updated and verified."
                            showReplaceDialog = false
                            reloadVaultSnapshot()
                        }
                        is KeySaveOutcome.DifferentPlayerWarning -> {
                            pendingDifferentPlayer = outcome
                            showReplaceDialog = false
                        }
                        is KeySaveOutcome.Error -> replaceError = outcome.message
                    }
                    isSavingTornKey = false
                }
            },
            onDismiss = { showReplaceDialog = false }
        )
    }

    pendingDifferentPlayer?.let { warn ->
        AlertDialog(
            onDismissRequest = { pendingDifferentPlayer = null },
            title = { Text("Different Player ID Detected") },
            text = {
                Text(
                    "The new key belongs to ${warn.newDisplayName} (${warn.newExternalId}) instead of player ${warn.existingExternalId}. Reset data for this account to continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val outcome = profileRepository.replaceAccountKey(
                                accountId = accountId,
                                rawKey = newKeyInput,
                                confirmDifferentPlayerReset = true
                            ) { id, secret ->
                                when (val res = tornProvider.validateCredential(id, secret)) {
                                    is ValidationResult.Valid -> CredentialValidationResult(
                                        res.externalId, res.displayName, res.accessLevel, null
                                    )
                                    is ValidationResult.Invalid -> CredentialValidationResult(
                                        null, null, null, res.error
                                    )
                                }
                            }
                            if (outcome is KeySaveOutcome.Saved) {
                                tornTestIsError = false
                                tornTestMessage = "Torn API key updated and account reset."
                            }
                            newKeyInput = ""
                            pendingDifferentPlayer = null
                            reloadVaultSnapshot()
                        }
                    }
                ) { Text("Reset data for this account") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDifferentPlayer = null }) { Text("Cancel") }
            }
        )
    }

    if (showFfKeyDialog) {
        FfApiKeyDialog(
            hasExistingCustomKey = ffKeyInfo.hasCustomKey,
            isAlreadyEnabled = ffKeyInfo.isEnabled,
            keyInput = ffKeyInput,
            error = ffKeyDialogError,
            isBusy = isSavingFfKey,
            onKeyChange = {
                ffKeyInput = it
                ffKeyDialogError = null
            },
            onOpenSite = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FfDefaults.SITE_URL)))
                }
            },
            onConfirm = {
                scope.launch {
                    isSavingFfKey = true
                    ffKeyDialogError = null
                    val ownId = account?.externalId?.toLongOrNull()
                    when (val res = ffRepository.saveCustomKeyAndEnable(accountId, ownId, ffKeyInput)) {
                        is ApiResult.Success -> {
                            ffKeyInput = ""
                            ffIsError = false
                            ffStatusMessage = "Custom FFScouter API key verified and saved."
                            showFfKeyDialog = false
                            reloadVaultSnapshot()
                        }
                        is ApiResult.Failure -> {
                            ffKeyDialogError = res.error.message
                        }
                    }
                    isSavingFfKey = false
                }
            },
            onDismiss = { showFfKeyDialog = false }
        )
    }

    if (showWebLoginDialog) {
        AlertDialog(
            onDismissRequest = { showWebLoginDialog = false },
            title = { Text("Opt-in Auto Re-login") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Credentials are stored encrypted on this device only and are only typed into the torn.com login form.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Use your Torn username & password. Accounts created with Google/Apple still have a Torn password (check your first email or use Torn's password recovery).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                    OutlinedTextField(
                        value = webUserInput,
                        onValueChange = { webUserInput = it },
                        label = { Text("Torn username / email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = webPassInput,
                        onValueChange = { webPassInput = it },
                        label = { Text("Torn password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val uRes = secretVault.put(SecretVault.webUserAlias(accountId), webUserInput.trim())
                            val pRes = secretVault.put(SecretVault.webPassAlias(accountId), webPassInput)
                            if (uRes is VaultResult.Ok && pRes is VaultResult.Ok) {
                                settingsStore.setAutoLogin(true, prefs.autoLoginAutoSubmit)
                                hasWebCreds = true
                            }
                            webUserInput = ""
                            webPassInput = ""
                            showWebLoginDialog = false
                        }
                    }
                ) { Text("Save encrypted") }
            },
            dismissButton = {
                TextButton(onClick = { showWebLoginDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFfConsent) {
        FfConsentDialog(
            hasExistingCustomKey = ffKeyInfo.hasCustomKey,
            onOpenSite = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FfDefaults.SITE_URL)))
                }
            },
            onConfirm = { optionalCustomKey ->
                showFfConsent = false
                scope.launch {
                    isTestingFf = true
                    val ownId = account?.externalId?.toLongOrNull()
                    when (val res = ffRepository.enableWithConsent(accountId, ownId, optionalCustomKey)) {
                        is ApiResult.Success -> {
                            ffIsError = false
                            ffStatusMessage = if (optionalCustomKey != null) {
                                "FFScouter enabled with your custom API key."
                            } else {
                                "FFScouter verified and enabled."
                            }
                        }
                        is ApiResult.Failure -> {
                            ffIsError = true
                            ffStatusMessage = res.error.message
                        }
                    }
                    reloadVaultSnapshot()
                    isTestingFf = false
                }
            },
            onDismiss = { showFfConsent = false }
        )
    }

    if (showDisableFfConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableFfConfirm = false },
            title = { Text("Disable FFScouter?") },
            text = { Text("Disabling deletes cached FFScouter estimates and hides FF/BS columns.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            ffRepository.disable(accountId)
                            ffStatusMessage = null
                            ffIsError = false
                            showDisableFfConfirm = false
                            reloadVaultSnapshot()
                        }
                    }
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableFfConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = { Text("Deletes the encrypted API keys, web session, userscripts, alerts, and cached data for this account.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            profileRepository.deleteAccount(accountId)
                            showDeleteConfirm = false
                            onAccountDeleted()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
