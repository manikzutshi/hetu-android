@file:OptIn(ExperimentalMaterial3Api::class)

package com.aurafarmers.hetu.ui.screens.journal

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aurafarmers.hetu.ui.theme.HetuColors
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.aurafarmers.hetu.ai.LLMService
import com.aurafarmers.hetu.ai.STTService
import com.aurafarmers.hetu.ai.AudioRecorder
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode

@Composable
fun JournalScreen(
    navController: NavController,
    viewModel: JournalViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // Manual Instantiation of Services to avoid Hilt EntryPoint complexity in Composable
    // Services are now provided by ViewModel (singleton instances)
    val llmService = viewModel.llmService
    val sttService = viewModel.sttService
    val ttsService = viewModel.ttsService
    val vadService = viewModel.vadService
    val audioRecorder = viewModel.audioRecorder

    LaunchedEffect(Unit) {
        // Initialize services
        // Initialize services - ensure singletons are ready
        launch { vadService.configure() }
        launch { sttService.loadModel() }
        launch { ttsService.loadVoice() }
        launch { if (!llmService.isLoaded()) llmService.loadModel() }
        
        // Loop management
        if (audioRecorder.hasPermission()) {
            viewModel.startWakeWordLoop()
        }
    }
    
    // State
    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Messages from DB
    val dbMessages by viewModel.messages.collectAsState(initial = emptyList())
    
    // Convert DB entities to ChatMessage for UI
    val messages = remember(dbMessages) {
        if (dbMessages.isEmpty()) {
            listOf(
                ChatMessage(
                    id = "welcome",
                    content = "Hi, I'm Hetu. I'm here to listen. How are you feeling today?",
                    isUser = false,
                    timestamp = java.time.LocalDateTime.now()
                )
            )
        } else {
            dbMessages.map { 
                ChatMessage(
                    id = it.id.toString(),
                    content = it.text,
                    isUser = it.isUser,
                    timestamp = java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(it.timestamp), 
                        java.time.ZoneId.systemDefault()
                    )
                )
            }
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
             viewModel.startWakeWordLoop()
        } else {
            Toast.makeText(context, "Microphone permission needed for voice chat", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Model file picker for selecting GGUF model
    var isModelAvailable by remember { mutableStateOf(llmService.isModelAvailable()) }
    var isCopyingModel by remember { mutableStateOf(false) }
    
    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCopyingModel = true
            scope.launch {
                val success = llmService.copyModelFromUri(uri)
                if (success) {
                    isModelAvailable = true
                    Toast.makeText(context, "Model copied successfully! Loading...", Toast.LENGTH_SHORT).show()
                    llmService.loadModel()
                } else {
                    Toast.makeText(context, "Failed to copy model file", Toast.LENGTH_SHORT).show()
                }
                isCopyingModel = false
            }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank()) return
        
        val userMsg = inputText.trim()
        val currentInput = inputText // Capture for async usage
        inputText = ""
        focusManager.clearFocus()
        
        // Save user message to DB
        viewModel.addUserMessage(userMsg)
        
        isProcessing = true
        
        scope.launch {
            try {
                // Generate AI response
                val response = llmService.chat(userMsg)
                
                // Save AI response to DB
                viewModel.addAiMessage(response)
            } catch (e: Exception) {
                viewModel.addAiMessage("Sorry, I'm having trouble thinking right now. (${e.message})")
            } finally {
                isProcessing = false
            }
        }
    }
    
    fun toggleRecording() {
        if (isRecording) {
            // Stop recording
            audioRecorder.stopRecording()
            isRecording = false
        } else {
            // Start recording
            if (!audioRecorder.hasPermission()) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            
            isRecording = true
            scope.launch {
                try {
                    // This suspends until max duration or stopRecording() is called
                    val audioData = audioRecorder.recordAudio()
                    
                    if (audioData.isNotEmpty()) {
                        isProcessing = true
                        val transcription = sttService.transcribe(audioData)
                        if (transcription.isSuccess) {
                            inputText = transcription.text
                        } else {
                            Toast.makeText(context, "Could not hear you clearly", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    isProcessing = false
                    isRecording = false
                }
            }
        }
    }

    // Status
    val currentStatus by viewModel.currentStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Hetu Journal")
                        // Show loaded model name
                        val modelName = llmService.getModelName()?.take(20) ?: "No Model"
                        Text(
                            text = if (modelName != "No Model") "$currentStatus • $modelName" else currentStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentStatus.contains("Listening")) MaterialTheme.colorScheme.primary else HetuColors.Sage
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = HetuColors.Terracotta
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { toggleRecording() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.tertiary, 
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isRecording) "Listening..." else "Type your thoughts...") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() })
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { sendMessage() },
                    enabled = inputText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, 
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send, 
                        contentDescription = "Send", 
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Show model selection button when model is not available
                if (!isModelAvailable && !isCopyingModel) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = HetuColors.Terracotta.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "AI Model Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HetuColors.DarkBrown
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Select your Llama GGUF model file to enable AI chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HetuColors.Taupe
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { modelFilePicker.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = HetuColors.Sage)
                                ) {
                                    Text("Select Model File")
                                }
                            }
                        }
                    }
                }
                
                if (isCopyingModel) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = HetuColors.Sage.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "📥 Copying model file...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HetuColors.DarkBrown
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "This takes 1-3 minutes. Please wait and don't close the app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HetuColors.Taupe
                                )
                            }
                        }
                    }
                }
                
                items(messages) { message ->
                    MessageBubble(message)
                }
                
                if (isProcessing && !isRecording) {
                    item {
                        ThinkingBubble()
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    // User: Sage (Tertiary), AI: Surface/Secondary
    val backgroundColor = if (isUser) 
        MaterialTheme.colorScheme.tertiary 
    else 
        MaterialTheme.colorScheme.surfaceVariant
        
    val textColor = if (isUser) 
        MaterialTheme.colorScheme.onTertiary 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant
        
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
        
        Text(
            text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun ThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use a simple Box with animated alpha instead of CircularProgressIndicator
        // to completely bypass the crashing AnimationSpec in Material3 library if it persists
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(HetuColors.Sage.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(HetuColors.Sage.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(HetuColors.Sage.copy(alpha = alpha), CircleShape)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        Text("Hetu is thinking...", fontSize = 12.sp, color = Color.Gray)
    }
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: java.time.LocalDateTime
)
