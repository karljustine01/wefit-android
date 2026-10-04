package com.wefit.app

import androidx.compose.runtime.remember
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wefit.app.data.local.ThemeMode
import com.wefit.app.data.local.ThemePreferenceManager
import com.wefit.app.ui.navigation.WeFitNavGraph
import com.wefit.app.ui.theme.WeFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeManager = remember { ThemePreferenceManager(applicationContext) }
            val storedMode by themeManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()

            val useDark = when (storedMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            WeFitTheme(darkTheme = useDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeFitNavGraph()
                }
            }
        }
    }
}