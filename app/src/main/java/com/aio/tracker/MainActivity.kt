package com.aio.tracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.ui.AioRoot
import com.aio.tracker.ui.theme.AioTheme
import com.aio.tracker.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    private val pendingDestination = mutableStateOf<String?>(null)
    @Volatile private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !ready }
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingDestination.value = intent?.getStringExtra(EXTRA_DESTINATION)

        val container = applicationContext.container
        setContent {
            val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null as AppSettings?)
            val settings = settingsState
            if (settings != null) {
            LaunchedEffect(Unit) { ready = true }
            val dark = isAppInDarkTheme(settings.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            AioTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                AioRoot(
                    settings = settings,
                    pendingDestination = pendingDestination.value,
                    onDestinationConsumed = { pendingDestination.value = null },
                )
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let { pendingDestination.value = it }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        private val LIGHT_SCRIM = Color.argb(0x00, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x00, 0x1B, 0x1B, 0x1B)
    }
}
