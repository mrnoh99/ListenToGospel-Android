package njs.listentogospel

import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import njs.listentogospel.ui.LocalAppAccessibilityEnabled
import njs.listentogospel.ui.MainScreen
import njs.listentogospel.ui.theme.ListenToGospelTheme
import njs.listentogospel.util.isTalkBackServiceEnabled
import njs.listentogospel.viewmodel.BiblePlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: BiblePlayerViewModel by viewModels()
    private var talkBackWasActive = false

    private val accessibilityStateListener = AccessibilityManager.AccessibilityStateChangeListener {
        onAccessibilityModeChanged()
    }

    private val touchExplorationListener = AccessibilityManager.TouchExplorationStateChangeListener {
        onAccessibilityModeChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        talkBackWasActive = isTalkBackServiceEnabled()
        applyWindowAccessibilityMode(talkBackWasActive)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val accessibilityManager = getSystemService(AccessibilityManager::class.java)
        accessibilityManager?.addAccessibilityStateChangeListener(accessibilityStateListener)
        accessibilityManager?.addTouchExplorationStateChangeListener(touchExplorationListener)

        setContent {
            val useTalkBack = isTalkBackServiceEnabled()
            CompositionLocalProvider(LocalAppAccessibilityEnabled provides useTalkBack) {
                key(useTalkBack) {
                    ListenToGospelTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as ListenToGospelApp).audioPlayer.reassertPlaybackIfNeeded()
        onAccessibilityModeChanged()
    }

    override fun onDestroy() {
        val accessibilityManager = getSystemService(AccessibilityManager::class.java)
        accessibilityManager?.removeAccessibilityStateChangeListener(accessibilityStateListener)
        accessibilityManager?.removeTouchExplorationStateChangeListener(touchExplorationListener)
        if (isFinishing) {
            (application as ListenToGospelApp).audioPlayer.stopBecauseAppClosed()
        }
        super.onDestroy()
    }

    override fun onStop() {
        playerViewModel.persistFocusedSession()
        super.onStop()
    }

    private fun onAccessibilityModeChanged() {
        val talkBackActive = isTalkBackServiceEnabled()
        applyWindowAccessibilityMode(talkBackActive)

        if (talkBackWasActive != talkBackActive) {
            talkBackWasActive = talkBackActive
            clearAccessibilityFocus()
            recreate()
        }
    }

    private fun applyWindowAccessibilityMode(talkBackActive: Boolean) {
        window.decorView.importantForAccessibility = if (talkBackActive) {
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        } else {
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    private fun clearAccessibilityFocus() {
        window.decorView.post {
            window.decorView.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
                null
            )
            window.decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        }
    }
}
