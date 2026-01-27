package com.aurafarmers.hetu.ui.screens.track

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackScreen(
    onBack: () -> Unit,
    viewModel: TrackViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var isAction by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expectation by remember { mutableStateOf("") }
    var daysUntilCheck by remember { mutableStateOf(3) }
    
    val actionCategories = listOf("🥗 Food", "😴 Sleep", "🏃 Exercise", "🧘 Wellness", "💊 Supplement", "📚 Other")
    val outcomeCategories = listOf("⚡ Energy", "😊 Mood", "🎯 Focus", "💪 Physical", "😌 Stress", "📈 Other")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Something") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            // ... (rest of UI code, keep UI same) ...
            
            // Toggle: Action vs Outcome
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = isAction,
                    onClick = { isAction = true },
                    label = { Text("What I tried") },
                    leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isAction,
                    onClick = { isAction = false },
                    label = { Text("How I feel") },
                    leadingIcon = { Icon(Icons.Outlined.SentimentSatisfied, null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Category Selection
            Text(
                if (isAction) "Category" else "What aspect?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = if (isAction) actionCategories else outcomeCategories
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isAction) "What did you try?" else "How are you feeling?") },
                placeholder = { 
                    Text(
                        if (isAction) "e.g., Started taking magnesium before bed"
                        else "e.g., Woke up feeling more rested than usual"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            
            if (isAction) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Expectation
                Text(
                    "What do you expect?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "This helps track if your predictions match reality",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = expectation,
                    onValueChange = { expectation = it },
                    placeholder = { Text("e.g., Better sleep quality in 2-3 days") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Check-in reminder
                Text(
                    "Remind me to check in after",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 7).forEach { days ->
                        FilterChip(
                            selected = daysUntilCheck == days,
                            onClick = { daysUntilCheck = days },
                            label = { Text("$days day${if (days > 1) "s" else ""}") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save button
            Button(
                onClick = { 
                    if (isAction) {
                        viewModel.saveAction(description, selectedCategory!!, expectation, daysUntilCheck)
                    } else {
                        viewModel.saveOutcome(description, selectedCategory!!)
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = description.isNotBlank() && selectedCategory != null
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isAction) "Save Action" else "Log Outcome",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
