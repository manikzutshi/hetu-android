package com.aurafarmers.hetu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.preferences.HetuPreferences
import com.aurafarmers.hetu.data.local.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: HetuPreferences,
    private val trackRepository: com.aurafarmers.hetu.data.repository.TrackRepository,
    private val feedPostDao: com.aurafarmers.hetu.data.local.dao.FeedPostDao,
    private val insightDao: com.aurafarmers.hetu.data.local.dao.InsightDao,
    private val messageDao: com.aurafarmers.hetu.data.local.dao.MessageDao,
    // Inject AI services to expose state/actions if needed
    val llmService: com.aurafarmers.hetu.ai.LLMService,
    val sttService: com.aurafarmers.hetu.ai.STTService
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
    val notificationFrequency: StateFlow<String> = preferences.notificationFrequency
    val notificationPersonality: StateFlow<String> = preferences.notificationPersonality

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setNotificationFrequency(frequency: String) {
        viewModelScope.launch {
            preferences.setNotificationFrequency(frequency)
        }
    }

    fun setNotificationPersonality(personality: String) {
        viewModelScope.launch {
            preferences.setNotificationPersonality(personality)
        }
    }
    
    fun deleteAllMockData() {
        viewModelScope.launch {
            trackRepository.deleteAllActions()
            trackRepository.deleteAllOutcomes()
            feedPostDao.deleteAll()
            insightDao.deleteAll()
            messageDao.deleteAll()
        }
    }
    
    fun generateMockData() {
        viewModelScope.launch {
            val random = Random(System.currentTimeMillis())
            
            // Rich action pool with correlatable patterns
            val actionPool = listOf(
                "🥗 Food" to listOf(
                    "Ate a healthy salad for lunch",
                    "Had oatmeal and fruit for breakfast",
                    "Cooked a nice dinner at home",
                    "Had pizza and beer (cheat day!)",
                    "Made a green smoothie",
                    "Skipped breakfast, just had coffee",
                    "Had a heavy pasta dinner"
                ),
                "🏃 Exercise" to listOf(
                    "Went for a morning 5km run",
                    "Did 30 minutes of yoga",
                    "Hit the gym for strength training",
                    "Took a long walk in the park",
                    "Played basketball with friends",
                    "Skipped workout today",
                    "Did a quick home workout"
                ),
                "😴 Sleep" to listOf(
                    "Slept a solid 8 hours",
                    "Only got 5 hours of sleep",
                    "Took a 20-minute power nap",
                    "Went to bed early at 10pm",
                    "Had trouble falling asleep",
                    "Woke up multiple times",
                    "Slept in late today"
                ),
                "🧘 Wellness" to listOf(
                    "Meditated for 15 minutes",
                    "Did deep breathing exercises",
                    "Journaled my thoughts",
                    "Read a book for an hour",
                    "Took mental health break",
                    "Practiced gratitude",
                    "Did nothing - rest day"
                ),
                "☕ Habits" to listOf(
                    "Had 3 cups of coffee",
                    "Drank 2L of water",
                    "No caffeine today",
                    "Had a glass of wine",
                    "Ate late at night",
                    "Took all my vitamins",
                    "Spent time in nature"
                ),
                "💼 Work" to listOf(
                    "Worked from home",
                    "Long day at the office",
                    "Had important meetings",
                    "Deadline pressure today",
                    "Easy work day",
                    "Took a mental health day",
                    "Started new project"
                )
            )
            
            // Outcome pool with clear positive/negative signals
            val outcomePool = listOf(
                "⚡ Energy" to listOf(
                    "Feeling super energetic!" to true,
                    "Sluggish and tired all day" to false,
                    "Moderate energy levels" to true,
                    "Completely exhausted" to false,
                    "Peak performance mode" to true,
                    "Afternoon energy crash" to false
                ),
                "😊 Mood" to listOf(
                    "Happy and content today" to true,
                    "Feeling quite anxious" to false,
                    "Neutral, nothing special" to true,
                    "Really excited about life!" to true,
                    "Feeling down and sad" to false,
                    "Irritable and frustrated" to false
                ),
                "😌 Stress" to listOf(
                    "Relaxed and peaceful" to true,
                    "Bit stressed about work" to false,
                    "Very stressed and overwhelmed" to false,
                    "Calm and focused" to true,
                    "Anxious about tomorrow" to false,
                    "Tension headache from stress" to false
                ),
                "🎯 Focus" to listOf(
                    "Super focused and productive" to true,
                    "Couldn't concentrate at all" to false,
                    "Good focus in the morning" to true,
                    "Brain fog all day" to false,
                    "In the zone, crushed my tasks" to true,
                    "Scattered attention" to false
                ),
                "💪 Physical" to listOf(
                    "Muscles sore but feel good" to true,
                    "Feeling strong and healthy" to true,
                    "Had a headache" to false,
                    "Well rested and recovered" to true,
                    "Body feels heavy and tired" to false,
                    "Back pain today" to false
                )
            )
            
            // Journal conversation templates - realistic daily entries (messy, human)
            val journalTemplates = listOf(
                // Morning entries
                listOf(
                    "Ugh, so tired. Did NOT sleep well.",
                    "Ok, today is the day. Gonna crush it.",
                    "Weird dreams last night. Feeling kinda off.",
                    "Slept in. Oops. Late start but feeling good.",
                    "Morning workout done. I feel like a god."
                ),
                // Work/Productivity entries
                listOf(
                    "Work was absolute chaos today. My boss is driving me nuts.",
                    "Finally finished that project!!!! So happy.",
                    "Can't focus at all today. Just staring at the screen.",
                    "Productive day, got a lot done. Feels good.",
                    "I hate mondays. Why is everything so hard today?",
                    "Great team meeting. Excited about the new features."
                ),
                // Exercise/Health entries
                listOf(
                    "Run was tough but glad I did it.",
                    "Skipped gym again... I'm lazy.",
                    "Yoga class was amazing. Namaste.",
                    "Everything hurts from yesterday. Ouch.",
                    "Need to stop eating junk food. Feeling gross."
                ),
                // Emotional check-ins
                listOf(
                    "Anxious about the deadline. Heart is racing.",
                    "Had lunch with Sarah, was really nice to catch up.",
                    "Feeling kinda lonely tbh.",
                    "Life is good right now. Grateful.",
                    "Money stress is real. Need to save more.",
                    "Just feeling blah. Not sad, just... blah."
                ),
                // Evening reflections
                listOf(
                    "Decent day. Nothing special.",
                    "So exhausted. Bed time immediately.",
                    "Nice quiet evening reading. needed this.",
                    "Dinner with fam was chaotic but fun.",
                    "Week is almost over. Thank god."
                ),
                // Sleep/Rest entries
                listOf(
                    "Brain won't shut up. Can't sleep.",
                    "Finally fixing my sleep schedule.",
                    "Accidentally napped for 3 hours. RIP my sleep tonight.",
                    "Actually woke up rested for once!",
                    "Phone away. sleep time."
                )
            )
            
            // AI response templates
            val aiResponses = listOf(
                "I hear you. It sounds like you're going through a lot. Take it one step at a time.",
                "That's wonderful! Celebrating small wins is important.",
                "It's okay to have off days. Tomorrow is a fresh start.",
                "Taking care of your wellbeing is a priority. Good for you!",
                "I notice you've been mentioning stress a lot lately. Want to talk about it?",
                "Your consistency with exercise is inspiring! Keep it up.",
                "Rest is productive too. Don't be too hard on yourself."
            )

            val today = java.time.LocalDate.now()
            
            // Generate last 14 days (reduced from 30)
            for (i in 0..14) {
                val date = today.minusDays(i.toLong())
                val morningTime = date.atTime(random.nextInt(7, 10), random.nextInt(0, 59))
                val eveningTime = date.atTime(random.nextInt(18, 22), random.nextInt(0, 59))
                val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                
                val morningTimestamp = morningTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                val eveningTimestamp = eveningTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

                // 2-4 Actions per day with realistic timing
                val numActions = random.nextInt(2, 5)
                repeat(numActions) { idx ->
                    val (cat, descList) = actionPool.random(random)
                    val actionTime = if (idx < 2) morningTimestamp else eveningTimestamp
                    trackRepository.insertAction(
                        com.aurafarmers.hetu.data.local.entity.ActionEntity(
                            description = descList.random(random),
                            category = cat,
                            date = dateStr,
                            timestamp = actionTime + (idx * 3600000L) + random.nextLong(1000, 300000)
                        )
                    )
                }

                // 1-2 Outcomes per day
                repeat(random.nextInt(1, 3)) { idx ->
                    val (catOut, outcomes) = outcomePool.random(random)
                    val (desc, _) = outcomes.random(random)
                    trackRepository.insertOutcome(
                        com.aurafarmers.hetu.data.local.entity.OutcomeEntity(
                            description = desc,
                            category = catOut,
                            date = dateStr,
                            timestamp = eveningTimestamp + (idx * 1800000L) + random.nextLong(1000, 60000)
                        )
                    )
                }


                // Occasional Feed Post (~25% chance)
                if (random.nextFloat() < 0.25f) {
                    val feedCategories = listOf("Nature", "Life", "Food", "Work", "Travel", "Fitness", "Friends")
                    val feedCaptions = listOf(
                        "Beautiful moment captured",
                        "Grateful for today",
                        "New adventures await",
                        "Self-care day",
                        "Making memories",
                        "Small wins matter",
                        "Appreciating the little things",
                        "Progress, not perfection"
                    )
                    feedPostDao.insert(
                        com.aurafarmers.hetu.data.local.entity.FeedPostEntity(
                            mediaUri = "",
                            caption = feedCaptions.random(random),
                            location = listOf("Home", "Office", "Park", "Gym", "Cafe", "Trail", "Beach").random(random),
                            timestamp = eveningTimestamp + random.nextLong(60000, 180000),
                            mediaType = "image",
                            category = feedCategories.random(random)
                        )
                    )
                }
            }
        }
    }
}
