package com.aurafarmers.hetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aurafarmers.hetu.ui.navigation.HetuNavGraph
import com.aurafarmers.hetu.ui.theme.HetuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferences: com.aurafarmers.hetu.data.local.preferences.HetuPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = com.aurafarmers.hetu.data.local.preferences.ThemeMode.SYSTEM)
            
            HetuTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.aurafarmers.hetu.ui.screens.MainScreen()
                }
            }
        }
    }
}
