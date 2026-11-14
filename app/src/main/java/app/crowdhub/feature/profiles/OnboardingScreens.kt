package app.crowdhub.feature.profiles

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.crowdhub.core.alerts.PollWorker
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.profile.CredentialValidationResult
import app.crowdhub.core.profile.KeySaveOutcome
import app.crowdhub.core.profile.OnboardingStep
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.CrowdWordmark
import app.crowdhub.core.ui.components.SecureScreenEffect
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.ValidationResult
import app.crowdhub.provider.torn.TornProvider
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    profileRepository: ProfileRepository,
    tornProvider: TornProvider,
    settingsStore: SettingsStore,
    scheduler: Scheduler,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var step by remember { mutableStateOf<OnboardingStep?>(null) }
    var activeProfileId by remember { mutableStateOf<String?>(null) }
    var profileName by remember { mutableStateOf("Main") }
    var apiKeyInput by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notifGranted by remember { mutableStateOf(scheduler.hasNotificationPermission()) }
    var exactAlarmGranted by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    LaunchedEffect(Unit) {
        val resumeStep = runCatching { profileRepository.determineStartStep() }
            .getOrDefault(OnboardingStep.WELCOME)
        if (resumeStep == OnboardingStep.DONE) {
            onFinished()
            return@LaunchedEffect
        }
        val existingProfile = runCatching { profileRepository.getActiveProfile() }.getOrNull()
        if (existingProfile != null) {
            activeProfileId = existingProfile.id
            profileName = existingProfile.name
        }
        step = resumeStep
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                null -> {
                    CircularProgressIndicator(color = CrowColors.Sheen)
                }
                OnboardingStep.WELCOME -> {
                    CrowdWordmark()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Watch the flock.",
                        color = CrowColors.TextMuted,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(28.dp))
                    CrowCard {
                        Text(
                            text = "Privacy-first Torn companion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CrowColors.Text
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Local-only storage, Android Keystore encryption for your API key, and compliance-first monitoring.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrowColors.TextMuted
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { step = OnboardingStep.CREATE_PROFILE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_get_started")
                    ) {
                        Text("Get started")
                    }
                }
                OnboardingStep.CREATE_PROFILE -> {
                    CrowCard {
                        Text(
                            text = "Step 1 of 3 · Create Profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrowColors.SheenSoft
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Name your profile container",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrowColors.Text
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_profile_name_input")
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val profile = profileRepository.createOrActivateProfile(profileName)
                                    activeProfileId = profile.id
                                    errorMessage = null
                                    step = OnboardingStep.ADD_ACCOUNT
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_profile_next")
                        ) {
                            Text("Continue")
                        }
                    }
                }
                OnboardingStep.ADD_ACCOUNT -> {
                    SecureScreenEffect()
                    CrowCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Key, contentDescription = null, tint = CrowColors.AccentD)
                            Text(
                                text = "Step 2 of 3 · Add Torn Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrowColors.SheenSoft
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Use a Limited-access API key. You can revoke it anytime in Torn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrowColors.Text
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(TornUrls.API_KEY_PREFS))
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Open Torn API page")
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                errorMessage = null
                            },
                            label = { Text("Torn Limited API key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !isValidating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_api_key_input")
                        )
                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                color = CrowColors.Danger,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isValidating = true
                                    errorMessage = null
                                    val profileId = activeProfileId
                                        ?: profileRepository.createOrActivateProfile(profileName).id.also {
                                            activeProfileId = it
                                        }
                                    val outcome = profileRepository.createValidatedAccount(
                                        profileId = profileId,
                                        providerId = TornProvider.PROVIDER_ID,
                                        rawKey = apiKeyInput
                                    ) { acctId, secret ->
                                        when (val res = tornProvider.validateCredential(acctId, secret)) {
                                            is ValidationResult.Valid -> CredentialValidationResult(
                                                externalId = res.externalId,
                                                displayName = res.displayName,
                                                accessLevel = res.accessLevel,
                                                error = null
                                            )
                                            is ValidationResult.Invalid -> CredentialValidationResult(
                                                externalId = null,
                                                displayName = null,
                                                accessLevel = null,
                                                error = res.error
                                            )
                                        }
                                    }
                                    isValidating = false
                                    when (outcome) {
                                        is KeySaveOutcome.Saved -> {
                                            apiKeyInput = ""
                                            step = OnboardingStep.PERMISSIONS
                                        }
                                        is KeySaveOutcome.Error -> errorMessage = outcome.message
                                        is KeySaveOutcome.DifferentPlayerWarning -> {}
                                    }
                                }
                            },
                            enabled = !isValidating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_validate_button")
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    color = CrowColors.Text,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Validate")
                            }
                        }
                    }
                }
                OnboardingStep.PERMISSIONS -> {
                    CrowCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = CrowColors.Ok)
                            Text(
                                text = "Step 3 of 3 · Alert Permissions",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrowColors.SheenSoft
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Grant notification and exact alarm access so bar, cooldown, travel, and stock timers fire on time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrowColors.TextMuted
                        )
                        Spacer(Modifier.height(12.dp))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                enabled = !notifGranted,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (notifGranted) "Notifications granted" else "Allow notifications")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        )
                                    }
                                    exactAlarmGranted = scheduler.canScheduleExactAlarms()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (exactAlarmGranted) "Exact alarms granted"
                                    else "Allow exact alarms (system settings)"
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        settingsStore.setOnboardingCompleted(true)
                                        PollWorker.enqueue(context)
                                        onFinished()
                                    }
                                },
                                modifier = Modifier.testTag("onboarding_skip_permissions")
                            ) {
                                Text("Skip")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        settingsStore.setOnboardingCompleted(true)
                                        PollWorker.enqueue(context)
                                        onFinished()
                                    }
                                },
                                modifier = Modifier.testTag("onboarding_finish_button")
                            ) {
                                Text("Finish")
                            }
                        }
                    }
                }
                OnboardingStep.DONE -> {}
            }
        }
    }
}
