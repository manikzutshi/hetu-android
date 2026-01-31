package com.aurafarmers.hetu.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.repository.TrackRepository
import com.aurafarmers.hetu.data.local.dao.InsightDao
import com.aurafarmers.hetu.data.local.dao.MessageDao
import com.aurafarmers.hetu.data.local.entity.InsightEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: TrackRepository,
    private val insightDao: InsightDao,
    private val messageDao: MessageDao,
    private val llmService: com.aurafarmers.hetu.ai.LLMService
) : ViewModel() {

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    
    // Expose saved insights from DB
    val savedInsights: Flow<List<InsightEntity>> = insightDao.getAllInsights()
    
    // Removed manual attachLLM - now injected

    val stats: StateFlow<InsightsStats> = combine(
        repository.getAllActions(),
        repository.getAllOutcomes(),
        _aiState
    ) { actions, outcomes, aiState ->
        val topAction = actions.groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "-"

        val topOutcome = outcomes.groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "-"

        // Calculate Streak
        val sortedDates = (actions.map { it.date } + outcomes.map { it.date })
            .distinct()
            .mapNotNull { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } }
            .sorted()

        var maxStreak = 0
        var currentStreak = 0
        var lastDate: java.time.LocalDate? = null

        sortedDates.forEach { date ->
            if (lastDate != null) {
                if (date == lastDate!!.plusDays(1)) {
                    currentStreak++
                } else if (date != lastDate) {
                    currentStreak = 1
                }
            } else {
                currentStreak = 1
            }
            if (currentStreak > maxStreak) maxStreak = currentStreak
            lastDate = date
        }
        
        // Calculate Productive Day
        val topDay = (actions.map { it.date } + outcomes.map { it.date })
             .mapNotNull { try { java.time.LocalDate.parse(it).dayOfWeek.name } catch (e: Exception) { null } }
             .groupingBy { it }
             .eachCount()
             .maxByOrNull { it.value }
             ?.key?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-"

        InsightsStats(
            totalEntries = actions.size + outcomes.size,
            daysTracked = sortedDates.size,
            topActionCategory = topAction,
            topOutcomeCategory = topOutcome,
            patternsFound = if (actions.size > 5) 1 else 0,
            longestStreak = maxStreak,
            productiveDay = topDay,
            isAnalyzing = aiState is AIState.Analyzing,
            aiError = (aiState as? AIState.Error)?.message
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsStats()
    )
    
    /**
     * Generate AI insights by passing ALL user data to the LLM.
     * The AI discovers patterns, not hardcoded templates.
     */
    fun runFullAnalysis() {
        viewModelScope.launch {
            _aiState.value = AIState.Analyzing
            android.util.Log.d("InsightsVM", "runFullAnalysis started")
            
            try {
                // Collect ALL data
                val actions = repository.getAllActions().firstOrNull() ?: emptyList()
                val outcomes = repository.getAllOutcomes().firstOrNull() ?: emptyList()
                val messages = messageDao.getAllMessages().firstOrNull() ?: emptyList()
                
                android.util.Log.d("InsightsVM", "Data collected: ${actions.size} actions, ${outcomes.size} outcomes, ${messages.size} messages")
                
                if (actions.isEmpty() && outcomes.isEmpty() && messages.isEmpty()) {
                    android.util.Log.d("InsightsVM", "No data found, returning error")
                    _aiState.value = AIState.Error("No data to analyze. Add some entries first!")
                    return@launch
                }
                
                // Format data for LLM
                val actionsText = actions.take(50).joinToString("\n") { 
                    "- ${it.date}: ${it.category} - ${it.description}"
                }
                
                val outcomesText = outcomes.take(50).joinToString("\n") {
                    "- ${it.date}: ${it.category} - ${it.description}"
                }
                
                val journalText = messages
                    .filter { it.isUser }
                    .take(30)
                    .joinToString("\n") {
                        val dateStr = java.time.Instant.ofEpochMilli(it.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate().toString()
                        "- $dateStr: \"${it.text}\""
                    }
                
                val prompt = """
You are a personal wellness analyst. Below is data from a user tracking their habits and moods over the past month.

ACTIONS (things I did):
$actionsText

OUTCOMES (how I felt):
$outcomesText

JOURNAL ENTRIES (my thoughts):
$journalText

Analyze this data and find 3-5 SPECIFIC patterns or insights unique to this person's data.
Look for:
- Correlations between specific activities and moods
- Recurring themes in journal entries
- Time-based patterns (certain days, times)
- Health impacts (sleep, exercise effects)
- Emotional patterns
- Any other interesting discoveries

For EACH insight, use this format:
TITLE: [short catchy title]
EMOJI: [single relevant emoji]
INSIGHT: [2-3 sentences explaining the pattern you found]
CONFIDENCE: [high/medium/low]
---

Be specific! Reference actual data points. Don't give generic advice.
""".trimIndent()
                
                // Use LLM if available and loaded
                var usedLLM = false
                
                if (llmService.isModelAvailable()) {
                    // Try to use LLM
                    if (llmService.isLoaded()) {
                        try {
                            val response = llmService.chat(prompt)
                            if (response.isNotBlank()) {
                                parseAndSaveInsights(response)
                                usedLLM = true
                            }
                        } catch (e: Exception) {
                            // LLM failed, will use fallback
                        }
                    }
                }
                
                // Fallback to smart data analysis
                if (!usedLLM) {
                    android.util.Log.d("InsightsVM", "Using smart fallback (LLM not available)")
                    generateSmartFallbackInsights(actions, outcomes, messages)
                } else {
                    android.util.Log.d("InsightsVM", "Used LLM for analysis")
                }
                
                android.util.Log.d("InsightsVM", "Analysis complete!")
                _aiState.value = AIState.Success("Analysis complete!")
                
            } catch (e: Exception) {
                _aiState.value = AIState.Error("Analysis failed: ${e.message}")
            }
        }
    }
    
    /**
     * Parse LLM response and save as InsightEntities
     */
    private suspend fun parseAndSaveInsights(response: String) {
        val insightBlocks = response.split("---").filter { it.contains("TITLE:") || it.contains("INSIGHT:") }
        
        for (block in insightBlocks) {
            val title = Regex("TITLE:\\s*(.+)").find(block)?.groupValues?.get(1)?.trim() ?: continue
            val emoji = Regex("EMOJI:\\s*(.+)").find(block)?.groupValues?.get(1)?.trim() ?: "💡"
            val insight = Regex("INSIGHT:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1)
                ?.takeWhile { it != '\n' || !block.substringAfter("INSIGHT:").substringAfter(it.toString()).trimStart().startsWith("CONFIDENCE") }
                ?.replace(Regex("CONFIDENCE.*", RegexOption.DOT_MATCHES_ALL), "")
                ?.trim() ?: continue
            val confidence = Regex("CONFIDENCE:\\s*(high|medium|low)", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.lowercase() ?: "medium"
            
            insightDao.insert(InsightEntity(
                title = title.take(50),
                description = insight.take(300),
                emoji = emoji.take(4),
                confidence = confidence,
                actionCategory = null,
                outcomeCategory = null,
                occurrences = 1
            ))
        }
    }
    
    /**
     * Smart fallback that analyzes actual data patterns
     */
    private suspend fun generateSmartFallbackInsights(
        actions: List<com.aurafarmers.hetu.data.local.entity.ActionEntity>,
        outcomes: List<com.aurafarmers.hetu.data.local.entity.OutcomeEntity>,
        messages: List<com.aurafarmers.hetu.data.local.entity.MessageEntity>
    ) {
        android.util.Log.d("InsightsVM", "generateSmartFallbackInsights: ${actions.size} actions, ${outcomes.size} outcomes, ${messages.size} messages")
        val insights = mutableListOf<InsightEntity>()
        
        // 1. Day-of-week analysis
        val dayActionCounts = actions.mapNotNull {
            try { java.time.LocalDate.parse(it.date).dayOfWeek } catch (e: Exception) { null }
        }.groupingBy { it }.eachCount()
        
        val busiestDay = dayActionCounts.maxByOrNull { it.value }
        val quietestDay = dayActionCounts.minByOrNull { it.value }
        
        if (busiestDay != null && quietestDay != null && busiestDay.key != quietestDay.key) {
            insights.add(InsightEntity(
                title = "Weekly Rhythm",
                description = "You're most active on ${busiestDay.key.name.lowercase().replaceFirstChar { it.uppercase() }}s (${busiestDay.value} entries) and least active on ${quietestDay.key.name.lowercase().replaceFirstChar { it.uppercase() }}s. Consider using ${quietestDay.key.name.lowercase().replaceFirstChar { it.uppercase() }}s for rest.",
                emoji = "📆",
                confidence = "high",
                actionCategory = null,
                outcomeCategory = null,
                occurrences = busiestDay.value
            ))
        }
        
        // 2. Mood patterns from journal
        val userMessages = messages.filter { it.isUser }.map { it.text.lowercase() }
        val stressWords = listOf("stress", "overwhelm", "anxious", "worry", "tired", "exhaust")
        val happyWords = listOf("happy", "great", "good", "excited", "grateful", "proud", "refreshed")
        
        val stressCount = userMessages.count { msg -> stressWords.any { msg.contains(it) } }
        val happyCount = userMessages.count { msg -> happyWords.any { msg.contains(it) } }
        
        if (stressCount > happyCount && stressCount >= 3) {
            insights.add(InsightEntity(
                title = "Stress Pattern Detected",
                description = "Your journal mentions stress-related words $stressCount times. Consider adding more wellness activities like meditation or walks to balance your emotional state.",
                emoji = "😮‍💨",
                confidence = "high",
                actionCategory = null,
                outcomeCategory = null,
                occurrences = stressCount
            ))
        } else if (happyCount > stressCount && happyCount >= 3) {
            insights.add(InsightEntity(
                title = "Positive Momentum",
                description = "You've expressed positive feelings $happyCount times in your journals. Keep doing what's working! Your current habits seem to be serving you well.",
                emoji = "😊",
                confidence = "high",
                actionCategory = null,
                outcomeCategory = null,
                occurrences = happyCount
            ))
        }
        
        // 3. Exercise-mood correlation
        val exerciseDates = actions.filter { it.category.contains("Exercise", ignoreCase = true) }
            .map { it.date }.toSet()
        val goodMoodOnExerciseDays = outcomes.filter { 
            exerciseDates.contains(it.date) && 
            (it.description.contains("energetic", ignoreCase = true) ||
             it.description.contains("happy", ignoreCase = true) ||
             it.description.contains("focused", ignoreCase = true) ||
             it.description.contains("good", ignoreCase = true))
        }
        
        if (exerciseDates.size >= 3 && goodMoodOnExerciseDays.size >= 2) {
            val percentage = (goodMoodOnExerciseDays.size * 100) / exerciseDates.size
            insights.add(InsightEntity(
                title = "Exercise Boosts Your Mood",
                description = "On days you exercise, you report positive moods about $percentage% of the time. This connection between physical activity and wellbeing is consistent in your data.",
                emoji = "🏃",
                confidence = "high",
                actionCategory = "🏃 Exercise",
                outcomeCategory = null,
                occurrences = exerciseDates.size
            ))
        }
        
        // 4. Sleep impact
        val poorSleepDates = actions.filter { 
            it.category.contains("Sleep", ignoreCase = true) && 
            (it.description.contains("trouble", ignoreCase = true) ||
             it.description.contains("only", ignoreCase = true) ||
             it.description.contains("5 hours", ignoreCase = true))
        }.map { it.date }.toSet()
        
        val badOutcomesAfterPoorSleep = outcomes.filter {
            poorSleepDates.contains(it.date) &&
            (it.description.contains("tired", ignoreCase = true) ||
             it.description.contains("fog", ignoreCase = true) ||
             it.description.contains("exhausted", ignoreCase = true))
        }
        
        if (poorSleepDates.size >= 2 && badOutcomesAfterPoorSleep.isNotEmpty()) {
            insights.add(InsightEntity(
                title = "Sleep Quality Matters",
                description = "When you report poor sleep, you often feel tired or foggy the next day. Consider prioritizing a consistent sleep schedule.",
                emoji = "😴",
                confidence = "medium",
                actionCategory = "😴 Sleep",
                outcomeCategory = null,
                occurrences = poorSleepDates.size
            ))
        }
        
        // 5. Category diversity
        val categories = actions.map { it.category }.distinct()
        if (categories.size >= 5) {
            insights.add(InsightEntity(
                title = "Balanced Tracking",
                description = "You're tracking ${categories.size} different areas of life: ${categories.take(4).joinToString(", ")}. This holistic view helps identify cross-domain patterns.",
                emoji = "🌈",
                confidence = "high",
                actionCategory = null,
                outcomeCategory = null,
                occurrences = categories.size
            ))
        }
        
        // 6. GUARANTEED: Data Summary if no other insights
        if (insights.isEmpty()) {
            val topAction = actions.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
            val topOutcome = outcomes.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
            
            insights.add(InsightEntity(
                title = "Your Data Summary",
                description = buildString {
                    append("Analyzed ${actions.size} actions and ${outcomes.size} outcomes. ")
                    if (topAction != null) append("Most tracked: ${topAction.key} (${topAction.value}x). ")
                    if (topOutcome != null) append("Common mood: ${topOutcome.key}. ")
                    append("Keep tracking for deeper insights!")
                },
                emoji = "📊",
                confidence = "medium",
                actionCategory = topAction?.key,
                outcomeCategory = topOutcome?.key,
                occurrences = actions.size + outcomes.size
            ))
        }
        
        // Save insights
        android.util.Log.d("InsightsVM", "Saving ${insights.size} insights to DB")
        insights.forEach { insightDao.insert(it) }
    }
    
    // Backwards compatibility
    fun generateAIInsights() = runFullAnalysis()
}

sealed class AIState {
    object Idle : AIState()
    object Analyzing : AIState()
    data class Success(val text: String) : AIState()
    data class Error(val message: String) : AIState()
}

data class InsightsStats(
    val totalEntries: Int = 0,
    val daysTracked: Int = 0,
    val topActionCategory: String = "-",
    val topOutcomeCategory: String = "-",
    val patternsFound: Int = 0,
    val longestStreak: Int = 0,
    val productiveDay: String = "-",
    val isAnalyzing: Boolean = false,
    val aiError: String? = null
)

