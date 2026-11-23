package com.acesinthesack.trivianight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

enum class Screen { Setup, Splash, Game, End }

/** App state and game flow. Mirrors AppModel.swift so both apps behave the same. */
class AppModel(
    val bank: QuestionBank,
    val history: PlayHistory,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val rng: Random = Random.Default,
) {
    var settings by mutableStateOf(GameSettings())
        private set
    var screen by mutableStateOf(Screen.Setup)
        private set

    var game by mutableStateOf<List<Question>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var revealed by mutableStateOf(false)
        private set
    var slideStartedAt by mutableLongStateOf(clock())
        private set
    private var usedLastGame: Set<String> = emptySet()

    /** Bumped when history changes so the counts on the setup screen refresh. */
    var historyVersion by mutableIntStateOf(0)
        private set

    init {
        val defaults = listOf("History", "Science", "Pop Culture", "Movies & Entertainment")
        settings = settings.copy(selected = defaults.filter { bank.category(it) != null }.toSet())
    }

    // Setup

    /** Direct settings edit, for tests. */
    internal fun updateSettings(transform: (GameSettings) -> GameSettings) {
        settings = transform(settings)
    }

    val selectedCategories: List<Category>
        get() = bank.categories.filter { it.name in settings.selected }

    val maxQuestions: Int get() = maxOf(1, selectedCategories.sumOf { it.questions.size })

    val canStart: Boolean get() = settings.selected.isNotEmpty()

    fun toggle(category: String) {
        val s = settings.selected
        settings = settings.copy(selected = if (category in s) s - category else s + category)
        clampCount()
    }

    fun selectAll() {
        settings = settings.copy(selected = bank.categories.map { it.name }.toSet())
        clampCount()
    }

    fun clearAll() {
        settings = settings.copy(selected = emptySet())
        clampCount()
    }

    fun setQuestionCount(n: Int) {
        settings = settings.copy(questionCount = n)
        clampCount()
    }

    private fun clampCount() {
        settings = settings.copy(questionCount = settings.questionCount.coerceIn(1, maxQuestions))
    }

    fun setDifficulty(d: Int) {
        settings = settings.copy(difficulty = d.coerceIn(1, 10))
    }

    fun setAdvanced(on: Boolean) {
        settings = settings.copy(advanced = on)
    }

    fun categoryDifficulty(name: String): Int = settings.categoryDifficulty[name] ?: settings.difficulty

    fun setCategoryDifficulty(name: String, value: Int) {
        settings = settings.copy(categoryDifficulty = settings.categoryDifficulty + (name to value.coerceIn(1, 10)))
    }

    fun setTimer(seconds: Int) {
        settings = settings.copy(timerSeconds = seconds.coerceIn(10, 120))
    }

    // Game flow

    fun startGame() {
        // Skip anything ever shown before, plus last game's picks (in case some weren't reached).
        val avoid = history.played + usedLastGame
        game = GameBuilder.build(bank, settings, avoid, rng)
        usedLastGame = game.map { it.id }.toSet()
        index = 0
        revealed = false
        screen = Screen.Splash
    }

    fun startMatch() {
        if (game.isEmpty()) return
        index = 0
        presentSlide()
        screen = Screen.Game
    }

    val current: Question? get() = game.getOrNull(index)
    val isFirst: Boolean get() = index == 0
    val isLast: Boolean get() = index == game.size - 1

    /** Every time a slide appears the answer starts hidden and the timer restarts. */
    private fun presentSlide() {
        revealed = false
        slideStartedAt = clock()
        current?.let {
            history.markPlayed(it.id)
            historyVersion++
        }
    }

    // Play history

    val totalQuestions: Int get() = bank.totalQuestions

    /** Unplayed questions in the selected categories. */
    val unplayedInSelection: Int
        get() {
            historyVersion // read so Compose recomposes when history changes
            return selectedCategories.sumOf { cat -> cat.questions.count { it.id !in history } }
        }

    val playedCount: Int
        get() {
            historyVersion
            return history.played.size
        }

    fun resetHistory() {
        history.reset()
        usedLastGame = emptySet()
        historyVersion++
    }

    fun reveal() {
        if (screen != Screen.Game) return
        revealed = true
    }

    fun next() {
        if (screen != Screen.Game) return
        if (index + 1 >= game.size) {
            screen = Screen.End
        } else {
            index++
            presentSlide()
        }
    }

    fun previous() {
        if (screen != Screen.Game || index <= 0) return
        index--
        presentSlide()
    }

    fun reviewLast() {
        if (game.isEmpty()) return
        index = game.size - 1
        presentSlide()
        screen = Screen.Game
    }

    fun endGame() {
        screen = Screen.Setup
    }

    // Screenshot / demo support (used by the screenshot tests)

    fun applyDemo(name: String) {
        resetHistory()
        selectAll()
        setQuestionCount(20)
        startGame()
        when (name) {
            "splash" -> {}
            "game" -> startMatch()
            "answer" -> { startMatch(); reveal() }
            "end" -> { startMatch(); screen = Screen.End }
            else -> screen = Screen.Setup
        }
    }
}
