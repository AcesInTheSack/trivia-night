package com.acesinthesack.trivianight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Port of TriviaNightTests/GameTests.swift, so both apps are held to the same rules. */
class GameTest {
    private val bank: QuestionBank = QuestionBank.parse(File("../../TriviaNight/Resources/questions.json").readText())

    /** The bank trimmed to [n] questions per category, for tests that need to exhaust a category. */
    private fun smallBank(n: Int = 20) = QuestionBank(bank.categories.map { Category(it.name, it.questions.take(n)) })

    private fun model(b: QuestionBank = bank) = AppModel(b, PlayHistory(null))

    @Test fun bankHas15BalancedCategories() {
        assertEquals(15, bank.categories.size)
        val size = bank.categories.first().questions.size
        assertTrue(size >= 100)
        for (cat in bank.categories) {
            assertEquals(cat.name, size, cat.questions.size)
            assertEquals("${cat.name} covers every difficulty", (1..10).toSet(), cat.questions.map { it.difficulty }.toSet())
            assertTrue(cat.name, cat.questions.all { it.text.isNotEmpty() && it.answer.isNotEmpty() })
        }
    }

    @Test fun everyQuestionHasAUniquePermanentId() {
        val ids = bank.categories.flatMap { c -> c.questions.map { it.id } }
        assertEquals("IDs must be unique", ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("q") && it.length == 13 })
    }

    @Test fun historyPersistsAcrossLaunches() {
        val f = File.createTempFile("played", ".json").apply { delete() }
        try {
            val first = PlayHistory(f)
            first.markPlayed("qabc")
            first.markPlayed("qdef")
            val reloaded = PlayHistory(f)
            assertEquals(setOf("qabc", "qdef"), reloaded.played)
            assertEquals("""["qabc","qdef"]""", f.readText())
            reloaded.reset()
            assertTrue(PlayHistory(f).played.isEmpty())
        } finally {
            f.delete()
        }
    }

    @Test fun shownQuestionsAreNeverRepeatedUntilTheBankRunsOut() {
        val m = model(smallBank(20))
        m.updateSettings { it.copy(selected = setOf("History", "Music"), questionCount = 10) } // 40 questions
        val seen = mutableSetOf<String>()
        repeat(4) {
            m.startGame()
            m.startMatch()
            repeat(10) { seen += m.current!!.id; m.next() }
        }
        assertEquals("four games used every question exactly once", 40, seen.size)
        assertEquals(0, m.unplayedInSelection)
        m.startGame() // bank exhausted: repeats allowed
        assertEquals(10, m.game.size)
        m.resetHistory()
        assertEquals(40, m.unplayedInSelection)
    }

    @Test fun onlyQuestionsActuallyShownCountAsPlayed() {
        val m = model()
        m.setQuestionCount(10)
        m.startGame()
        assertEquals("building a game doesn't mark anything", 0, m.playedCount)
        m.startMatch(); m.next(); m.next()
        assertEquals(3, m.playedCount)
    }

    @Test fun buildRespectsCountAndCategories() {
        val s = GameSettings(selected = setOf("History", "Music", "Sports"), questionCount = 25)
        val game = GameBuilder.build(bank, s, rng = Random(42))
        assertEquals(25, game.size)
        assertEquals("no duplicates", 25, game.map { it.id }.toSet().size)
        assertTrue(game.all { it.category in s.selected })
    }

    @Test fun countIsCappedByAvailableQuestions() {
        val s = GameSettings(selected = setOf("History"), questionCount = 5000)
        assertEquals(bank.category("History")!!.questions.size, GameBuilder.build(bank, s, rng = Random(1)).size)
    }

    @Test fun difficultyTargetsClosestQuestions() {
        val s = GameSettings(selected = setOf("Science"), questionCount = 4, difficulty = 10)
        assertTrue(GameBuilder.build(bank, s, rng = Random(7)).all { it.difficulty >= 9 })
    }

    @Test fun advancedPerCategoryDifficulty() {
        val s = GameSettings(
            selected = setOf("Science", "History"), questionCount = 4, advanced = true,
            categoryDifficulty = mapOf("Science" to 1, "History" to 10),
        )
        val game = GameBuilder.build(bank, s, rng = Random(3))
        assertTrue(game.filter { it.category == "Science" }.all { it.difficulty <= 2 })
        assertTrue(game.filter { it.category == "History" }.all { it.difficulty >= 9 })
    }

    @Test fun categoriesAlternate() {
        val s = GameSettings(selected = setOf("Science", "History"), questionCount = 10)
        val game = GameBuilder.build(bank, s, rng = Random(9))
        for (i in 1 until game.size) assertTrue(game[i].category != game[i - 1].category)
    }

    @Test fun avoidsPreviousGameQuestions() {
        val s = GameSettings(selected = setOf("Music"), questionCount = 10)
        val rng = Random(5)
        val first = GameBuilder.build(bank, s, rng = rng)
        val second = GameBuilder.build(bank, s, first.map { it.id }.toSet(), rng)
        assertTrue(first.map { it.id }.intersect(second.map { it.id }.toSet()).isEmpty())
    }

    @Test fun answerIsHiddenOnEveryNavigation() {
        val m = model()
        m.startGame(); m.startMatch()
        assertFalse(m.revealed)
        m.reveal()
        assertTrue(m.revealed)
        m.next()
        assertFalse("moving forward hides the answer", m.revealed)
        m.previous()
        assertFalse("moving back hides the answer, even if revealed before", m.revealed)
    }

    @Test fun flowReachesEndAndCanReview() {
        val m = model()
        m.setQuestionCount(3)
        m.startGame()
        assertEquals(Screen.Splash, m.screen)
        m.startMatch()
        assertEquals(Screen.Game, m.screen)
        m.previous()
        assertEquals("can't go before the first question", 0, m.index)
        m.next(); m.next(); m.next()
        assertEquals(Screen.End, m.screen)
        m.reviewLast()
        assertEquals(Screen.Game, m.screen)
        assertEquals(2, m.index)
        assertFalse(m.revealed)
    }

    @Test fun countClampsWhenCategoriesChange() {
        val m = model(smallBank(20))
        m.selectAll()
        m.setQuestionCount(200)
        assertEquals(200, m.settings.questionCount)
        m.updateSettings { it.copy(selected = setOf("Music")) }
        m.toggle("History") // now History + Music -> max 40
        assertEquals(40, m.settings.questionCount)
    }

    @Test fun everyCategoryHasAnIcon() {
        val drawables = File("src/main/res/drawable").list()!!.toSet()
        for (cat in bank.categories) {
            val slug = cat.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            assertTrue("icon for ${cat.name}", "cat_$slug.xml" in drawables)
        }
        assertTrue(File("src/main/res/drawable-nodpi/critter_logo.png").exists())
    }
}
