package com.aurafarmers.hetu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurafarmers.hetu.data.local.preferences.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val notifFrequency by viewModel.notificationFrequency.collectAsState()
    val notifPersonality by viewModel.notificationPersonality.collectAsState()
    
    var themeExpanded by remember { mutableStateOf(false) }
    var freqExpanded by remember { mutableStateOf(false) }
    var personaExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Appearance
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsRow(
                icon = Icons.Outlined.DarkMode,
                title = "Theme",
                subtitle = when(themeMode) {
                    ThemeMode.LIGHT -> "Light Mode"
                    ThemeMode.DARK -> "Dark Mode"
                    ThemeMode.SYSTEM -> "System Default"
                },
                trailing = {
                    Box {
                        IconButton(onClick = { themeExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Select Theme")
                        }
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Default") },
                                onClick = { 
                                    viewModel.setThemeMode(ThemeMode.SYSTEM)
                                    themeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Light Mode") },
                                onClick = { 
                                    viewModel.setThemeMode(ThemeMode.LIGHT)
                                    themeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark Mode") },
                                onClick = { 
                                    viewModel.setThemeMode(ThemeMode.DARK)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Notifications
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = "Frequency",
                subtitle = notifFrequency,
                trailing = {
                    Box {
                        IconButton(onClick = { freqExpanded = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Frequency")
                        }
                        DropdownMenu(
                            expanded = freqExpanded,
                            onDismissRequest = { freqExpanded = false }
                        ) {
                            listOf("Daily", "Every 2 Days", "Weekly").forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq) },
                                    onClick = {
                                        viewModel.setNotificationFrequency(freq)
                                        freqExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = "Personality",
                subtitle = notifPersonality,
                trailing = {
                    Box {
                        IconButton(onClick = { personaExpanded = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Personality")
                        }
                        DropdownMenu(
                            expanded = personaExpanded,
                            onDismissRequest = { personaExpanded = false }
                        ) {
                            listOf("Friendly", "Direct", "Philosophical", "Warm").forEach { persona ->
                                DropdownMenuItem(
                                    text = { Text(persona) },
                                    onClick = {
                                        viewModel.setNotificationPersonality(persona)
                                        personaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
            
            // Privacy Section
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Privacy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "100% Offline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Your data never leaves this device. No cloud, no sync, no tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About
             Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "हेतु",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Hetu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Version 1.1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
