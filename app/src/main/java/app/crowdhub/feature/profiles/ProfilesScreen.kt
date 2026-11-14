package app.crowdhub.feature.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.db.ProfileEntity
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.theme.CrowColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    profileRepository: ProfileRepository,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onNeedOnboarding: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profiles by profileRepository.observeProfiles().collectAsStateWithLifecycle(emptyList())
    val activeState by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        app.crowdhub.core.profile.ActiveProfileState(null, null, emptyList())
    )

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ProfileEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ProfileEntity?>(null) }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Profiles (Advanced)") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newProfileName = ""
                    showCreateDialog = true
                },
                containerColor = CrowColors.Sheen,
                contentColor = CrowColors.Text
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Create profile")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Multi-profile containers isolate accounts, API keys, alerts, scripts, and web sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = CrowColors.TextMuted
            )

            if (profiles.isEmpty()) {
                EmptyView(title = "Nothing perched here yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profiles, key = { it.id }) { profile ->
                        CrowCard(
                            borderColor = if (profile.isActive) CrowColors.Sheen else CrowColors.Outline,
                            modifier = Modifier.clickable {
                                scope.launch { profileRepository.switchActiveProfile(profile.id) }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (profile.isActive) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = "Active",
                                            tint = CrowColors.Ok
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CrowColors.Text
                                        )
                                        Text(
                                            text = if (profile.isActive) "Active profile" else "Tap to switch active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrowColors.TextMuted
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            renameTarget = profile
                                            renameInput = profile.name
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Rename profile")
                                    }
                                    IconButton(onClick = { deleteTarget = profile }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete profile",
                                            tint = CrowColors.Danger
                                        )
                                    }
                                }
                            }

                            if (profile.isActive && activeState.primaryAccount != null) {
                                Spacer(Modifier.height(8.dp))
                                val acct = activeState.primaryAccount!!
                                TextButton(onClick = { onOpenAccount(acct.id) }) {
                                    Text("Manage account: ${acct.displayName ?: acct.externalId ?: acct.id}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Profile") },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            profileRepository.createOrActivateProfile(newProfileName)
                            showCreateDialog = false
                            onNeedOnboarding()
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_profile")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Profile") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            profileRepository.renameProfile(target.id, renameInput)
                            renameTarget = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Profile?") },
            text = {
                Text("This permanently wipes all accounts, API keys, web cookies, userscripts, alerts, and cached data in '${target.name}'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            profileRepository.deleteProfile(target.id)
                            val nextStep = profileRepository.determineStartStep()
                            deleteTarget = null
                            if (nextStep != app.crowdhub.core.profile.OnboardingStep.DONE) {
                                onNeedOnboarding()
                            }
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
