package com.brandonwallace.bastiontd

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.brandonwallace.bastiontd.audio.SoundEngine
import com.brandonwallace.bastiontd.game.GameEngine
import com.brandonwallace.bastiontd.gl.GameView
import com.brandonwallace.bastiontd.ui.BastionHudOverlay
import com.brandonwallace.bastiontd.ui.BastionTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var soundEngine: SoundEngine
    private lateinit var gameEngine: GameEngine
    private var gameViewRef: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        soundEngine = SoundEngine()
        gameEngine = GameEngine(soundEngine)

        val prefs = getSharedPreferences(GameEngine.PREFS_NAME, MODE_PRIVATE)
        gameEngine.load(prefs)

        setContent {
            BastionTheme {
                var uiSnap by remember { mutableStateOf(gameEngine.captureUiSnapshot()) }

                // Continuous UI telemetry sync + periodic auto-save
                LaunchedEffect(Unit) {
                    var ticks = 0
                    while (true) {
                        delay(80L)
                        uiSnap = gameEngine.captureUiSnapshot()
                        ticks++
                        if (ticks % 250 == 0) {
                            gameEngine.save(prefs)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            GameView(
                                context = ctx,
                                engine = gameEngine,
                                onTapCallback = {
                                    uiSnap = gameEngine.captureUiSnapshot()
                                }
                            ).also { gv ->
                                gameViewRef = gv
                                gv.onResume()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    BastionHudOverlay(
                        snap = uiSnap,
                        engine = gameEngine,
                        onAction = {
                            uiSnap = gameEngine.captureUiSnapshot()
                        }
                    )
                }

                DisposableEffect(Unit) {
                    onDispose {
                        gameEngine.save(prefs)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        soundEngine.resume()
        soundEngine.setAmbientPlaying(true)
        gameViewRef?.onResume()
    }

    override fun onPause() {
        val prefs = getSharedPreferences(GameEngine.PREFS_NAME, MODE_PRIVATE)
        gameEngine.save(prefs)
        soundEngine.setAmbientPlaying(false)
        soundEngine.pause()
        gameViewRef?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences(GameEngine.PREFS_NAME, MODE_PRIVATE)
        gameEngine.save(prefs)
        soundEngine.release()
        super.onDestroy()
    }
}
