package com.aurafarmers.hetu.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurafarmers.hetu.ai.LLMService
import com.aurafarmers.hetu.ai.STTService
import com.aurafarmers.hetu.data.local.preferences.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Services from ViewModel (injected singletons)
    val llmService = viewModel.llmService
    val sttService = viewModel.sttService
    
    // LLM Model status state
    var modelAvailable by remember { mutableStateOf(llmService.isModelAvailable()) }
    var modelLoaded by remember { mutableStateOf(llmService.isLoaded()) }
    var modelError by remember { mutableStateOf(llmService.getLoadError()) }
    var isLoading by remember { mutableStateOf(false) }
    var copyProgress by remember { mutableStateOf(0) }
    
    // STT/Vosk status state
    var sttAvailable by remember { mutableStateOf(sttService.isModelAvailable()) }
    var sttLoaded by remember { mutableStateOf(sttService.isLoaded()) }
    var sttModelName by remember { mutableStateOf(sttService.getModelName()) }
    var sttError by remember { mutableStateOf(sttService.getError()) }
    var sttLoading by remember { mutableStateOf(false) }
    
    // File picker for model selection
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                copyProgress = 0
                try {
                    llmService.copyModelFromUri(uri) { progress ->
                        copyProgress = progress
                    }
                    modelAvailable = llmService.isModelAvailable()
                    if (modelAvailable) {
                        llmService.loadModel()
                        modelLoaded = llmService.isLoaded()
                        modelError = llmService.getLoadError()
                    }
                } catch (e: Exception) {
                    modelError = e.message
                }
                isLoading = false
            }
        }
    }
    
    // Folder picker for Vosk model
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                sttLoading = true
                sttError = null
                sttService.copyModelFromTreeUri(uri)
                sttAvailable = sttService.isModelAvailable()
                sttLoaded = sttService.isLoaded()
                sttError = sttService.getError()
                sttModelName = sttService.getModelName()
                sttLoading = false
            }
        }
    }
    
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
            // ... (rest of code)

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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // AI Model Section
            Text(
                "AI Model",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (modelLoaded) MaterialTheme.colorScheme.primaryContainer 
                       else if (modelAvailable) MaterialTheme.colorScheme.secondaryContainer
                       else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (modelLoaded) Icons.Filled.CheckCircle
                            else if (modelAvailable) Icons.Outlined.Memory
                            else Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = if (modelLoaded) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    modelLoaded -> "Model Ready"
                                    modelAvailable -> "Model Available"
                                    else -> "No Model Selected"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when {
                                    modelLoaded -> llmService.getModelName() ?: "Model loaded"
                                    modelAvailable -> "Tap Load to activate"
                                    else -> "Select a .gguf model file"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (modelError != null && !modelLoaded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            modelError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { copyProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Copying model... $copyProgress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.FileOpen, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (modelAvailable) "Change" else "Select")
                        }
                        
                        if (modelAvailable && !modelLoaded) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        llmService.loadModel()
                                        modelLoaded = llmService.isLoaded()
                                        modelError = llmService.getLoadError()
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Model")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Voice Input Section (Vosk)
            Text(
                "Voice Input",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (sttLoaded) MaterialTheme.colorScheme.primaryContainer 
                       else if (sttAvailable) MaterialTheme.colorScheme.tertiaryContainer
                       else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (sttLoaded) Icons.Filled.Mic
                            else if (sttAvailable) Icons.Outlined.MicNone
                            else Icons.Outlined.MicOff,
                            contentDescription = null,
                            tint = if (sttLoaded) MaterialTheme.colorScheme.primary
                                   else if (sttAvailable) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    sttLoaded -> "Voice Ready"
                                    sttAvailable -> "Model Found"
                                    else -> "No Model Found"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when {
                                    sttLoaded -> "Using ${sttService.getModelName()}"
                                    sttAvailable -> "Tap to load vosk model"
                                    else -> "Download vosk-model-small-en-us"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (sttError != null && !sttLoaded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            sttError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Select Folder Button
                        OutlinedButton(
                            onClick = { folderPicker.launch(null) },
                            enabled = !sttLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select Folder")
                        }
                        
                        // Check/Reload Button
                        OutlinedButton(
                            onClick = { 
                                scope.launch {
                                    sttLoading = true
                                    sttError = null
                                    sttService.loadModel() // Try auto-load first
                                    sttAvailable = sttService.isModelAvailable()
                                    sttLoaded = sttService.isLoaded()
                                    sttError = sttService.getError()
                                    sttModelName = sttService.getModelName()
                                    sttLoading = false
                                }
                            },
                            enabled = !sttLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                            // Spacer(modifier = Modifier.width(4.dp)) // Save space
                            Text("Reload")
                        }
                    }
                    
                    if (sttLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Copying/Loading model...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Tools
            Text(
                "Developer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { viewModel.generateMockData() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Build, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Mock Data (30 Days)")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { viewModel.deleteAllMockData() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Data")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
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
