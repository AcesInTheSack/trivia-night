package com.acesinthesack.trivianight

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class MainActivity : ComponentActivity() {

    private val model: AppModel by lazy {
        val text = assets.open("questions.json").bufferedReader().use { it.readText() }
        AppModel(QuestionBank.parse(text), PlayHistory(File(filesDir, "played-questions.json")))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            TriviaApp(model) { screen -> onScreenChanged(screen) }
        }
    }

    /** During a game: keep the screen awake and hide the system bars, so the TV shows only the game. */
    private fun onScreenChanged(screen: Screen) {
        val playing = screen != Screen.Setup
        if (playing) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (playing) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun TriviaApp(model: AppModel, onScreenChanged: (Screen) -> Unit = {}) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(model.screen) {
        onScreenChanged(model.screen)
        runCatching { focus.requestFocus() }
    }

    // Back: during a game it steps to the previous question; elsewhere it returns to setup.
    BackHandler(enabled = model.screen != Screen.Setup) {
        when (model.screen) {
            Screen.Game -> if (model.isFirst) model.endGame() else model.previous()
            else -> model.endGame()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Palette.amber, background = Palette.bg, surface = Palette.surface)) {
        Background {
            Crossfade(
                model.screen,
                animationSpec = tween(250),
                label = "screen",
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focus)
                    .focusable()
                    // Keyboard, remote, or game controller: arrows move, space/enter/center reveals.
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown || model.screen != Screen.Game) return@onPreviewKeyEvent false
                        when (e.key) {
                            Key.DirectionRight, Key.PageDown -> { model.next(); true }
                            Key.DirectionLeft, Key.PageUp -> { model.previous(); true }
                            Key.Spacebar, Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> { model.reveal(); true }
                            else -> false
                        }
                    },
            ) { screen ->
                when (screen) {
                    Screen.Setup -> SetupScreen(model)
                    Screen.Splash -> SplashScreen(model)
                    Screen.Game -> GameScreen(model)
                    Screen.End -> EndScreen(model)
                }
            }
        }
    }
}
