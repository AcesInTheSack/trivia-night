package com.acesinthesack.trivianight

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

data class Question(
    val id: String,          // permanent ID from questions.json, e.g. "q1a2b3c4d5e6f"
    val category: String,
    val difficulty: Int,     // 1..10
    val text: String,
    val answer: String,
)

data class Category(val name: String, val questions: List<Question>)

class QuestionBank(val categories: List<Category>) {

    fun category(named: String): Category? = categories.firstOrNull { it.name == named }

    val totalQuestions: Int get() = categories.sumOf { it.questions.size }

    @Serializable
    private data class FileQ(val id: String? = null, val d: Int, val q: String, val a: String)

    @Serializable
    private data class FileCat(val name: String, val icon: String = "", val questions: List<FileQ>)

    @Serializable
    private data class FileRoot(val version: Int = 1, val categories: List<FileCat>)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): QuestionBank {
            val root = json.decodeFromString(FileRoot.serializer(), text)
            return QuestionBank(root.categories.map { cat ->
                Category(cat.name, cat.questions.mapIndexed { i, q ->
                    Question(q.id ?: "${cat.name}|$i", cat.name, q.d, q.q, q.a)
                })
            })
        }
    }
}

data class GameSettings(
    val selected: Set<String> = emptySet(),
    val questionCount: Int = 20,
    val difficulty: Int = 5,
    val advanced: Boolean = false,
    val categoryDifficulty: Map<String, Int> = emptyMap(),
    val timerSeconds: Int = 30,
) {
    fun difficultyFor(category: String): Int =
        if (advanced) categoryDifficulty[category] ?: difficulty else difficulty

    companion object {
        fun describe(d: Int): String = when {
            d <= 2 -> "Easy: most people will know it"
            d <= 4 -> "Casual: a little thinking required"
            d <= 6 -> "Medium: a good mix for a typical crowd"
            d <= 8 -> "Hard: rewards people who read and remember"
            else -> "Expert: for serious trivia buffs"
        }
    }
}

object GameBuilder {
    /**
     * Picks questions for a game. Same rules as the iOS app:
     * - Spreads the count evenly across the selected categories.
     * - Within a category, prefers questions not in [avoiding], then the closest difficulty.
     * - Interleaves categories so the same one rarely appears twice in a row.
     */
    fun build(
        bank: QuestionBank,
        settings: GameSettings,
        avoiding: Set<String> = emptySet(),
        rng: Random = Random.Default,
    ): List<Question> {
        val cats = bank.categories.filter { it.name in settings.selected }.shuffled(rng)
        if (cats.isEmpty()) return emptyList()

        val available = cats.sumOf { it.questions.size }
        var remaining = settings.questionCount.coerceIn(0, available)
        val perCategory = cats.associate { it.name to 0 }.toMutableMap()
        var i = 0
        while (remaining > 0) {
            val cat = cats[i % cats.size]
            if (perCategory.getValue(cat.name) < cat.questions.size) {
                perCategory[cat.name] = perCategory.getValue(cat.name) + 1
                remaining--
            }
            i++
        }

        val picks = mutableMapOf<String, ArrayDeque<Question>>()
        for (cat in cats) {
            val target = settings.difficultyFor(cat.name)
            val ranked = cat.questions.shuffled(rng).withIndex().sortedWith(
                compareBy<IndexedValue<Question>>(
                    { it.value.id in avoiding },
                    { abs(it.value.difficulty - target) },
                    { it.index },
                )
            ).map { it.value }
            picks[cat.name] = ArrayDeque(ranked.take(perCategory.getValue(cat.name)).shuffled(rng))
        }

        val out = mutableListOf<Question>()
        var added = true
        while (added) {
            added = false
            for (cat in cats) {
                val list = picks.getValue(cat.name)
                if (list.isNotEmpty()) {
                    out += list.removeFirst()
                    added = true
                }
            }
        }
        return out
    }
}

/**
 * Remembers every question that has been shown on screen, across games and app launches,
 * so the app doesn't repeat a question until it has used up the unplayed ones.
 * Stored as a sorted JSON array of IDs, the same format the iOS app uses.
 */
class PlayHistory(private val file: File?) {
    private val listSerializer = ListSerializer(String.serializer())

    var played: Set<String> = load()
        private set

    private fun load(): Set<String> = try {
        if (file != null && file.exists()) Json.decodeFromString(listSerializer, file.readText()).toSet()
        else emptySet()
    } catch (e: Exception) {
        emptySet()
    }

    operator fun contains(id: String) = id in played

    fun markPlayed(id: String) {
        if (id in played) return
        played = played + id
        save()
    }

    fun reset() {
        played = emptySet()
        save()
    }

    private fun save() {
        val f = file ?: return
        try {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(Json.encodeToString(listSerializer, played.sorted()))
            tmp.renameTo(f)
        } catch (_: Exception) {
        }
    }
}
