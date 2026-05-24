package njs.listentogospel

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import njs.listentogospel.ui.MainScreen
import njs.listentogospel.ui.theme.ListenToGospelTheme
import njs.listentogospel.viewmodel.BiblePlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: BiblePlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
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

    override fun onResume() {
        super.onResume()
        (application as ListenToGospelApp).audioPlayer.reassertPlaybackIfNeeded()
    }

    override fun onStop() {
        playerViewModel.persistFocusedSession()
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            (application as ListenToGospelApp).audioPlayer.stopBecauseAppClosed()
        }
        super.onDestroy()
    }
}
