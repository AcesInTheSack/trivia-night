import Foundation
import Observation

enum Screen: Equatable {
    case setup, splash, game, end
}

@Observable
final class AppModel {
    let bank: QuestionBank
    let history: PlayHistory
    var settings = GameSettings()
    var screen: Screen = .setup

    private(set) var game: [Question] = []
    private(set) var index = 0
    private(set) var revealed = false
    private(set) var slideStartedAt = Date()
    private var usedLastGame: Set<String> = []
    /// Bumped when history changes so views showing counts refresh.
    private(set) var historyVersion = 0

    init(bank: QuestionBank = .loadBundled(), history: PlayHistory = PlayHistory()) {
        self.bank = bank
        self.history = history
        let defaults = ["History", "Science", "Pop Culture", "Movies & Entertainment"]
        settings.selected = Set(defaults.filter { bank.category(named: $0) != nil })
    }

    // MARK: Setup

    var selectedCategories: [Category] {
        bank.categories.filter { settings.selected.contains($0.name) }
    }

    var maxQuestions: Int {
        max(1, selectedCategories.reduce(0) { $0 + $1.questions.count })
    }

    var canStart: Bool { !settings.selected.isEmpty }

    func toggle(_ category: String) {
        if settings.selected.contains(category) {
            settings.selected.remove(category)
        } else {
            settings.selected.insert(category)
        }
        clampCount()
    }

    func selectAll() {
        settings.selected = Set(bank.categories.map(\.name))
        clampCount()
    }

    func clearAll() {
        settings.selected = []
        clampCount()
    }

    func setQuestionCount(_ n: Int) {
        settings.questionCount = n
        clampCount()
    }

    func clampCount() {
        settings.questionCount = min(max(1, settings.questionCount), maxQuestions)
    }

    func categoryDifficulty(_ name: String) -> Int {
        settings.categoryDifficulty[name] ?? settings.difficulty
    }

    func setCategoryDifficulty(_ name: String, _ value: Int) {
        settings.categoryDifficulty[name] = value
    }

    // MARK: Game flow

    func startGame() {
        var rng = SystemRandomNumberGenerator()
        // Skip anything ever shown before, plus last game's picks (in case some weren't reached).
        let avoid = history.played.union(usedLastGame)
        game = GameBuilder.build(bank: bank, settings: settings, avoiding: avoid, rng: &rng)
        usedLastGame = Set(game.map(\.id))
        index = 0
        revealed = false
        screen = .splash
    }

    func startMatch() {
        guard !game.isEmpty else { return }
        index = 0
        presentSlide()
        screen = .game
    }

    var current: Question? { game.indices.contains(index) ? game[index] : nil }
    var isFirst: Bool { index == 0 }
    var isLast: Bool { index == game.count - 1 }

    /// Every time a slide appears the answer starts hidden and the timer restarts.
    private func presentSlide() {
        revealed = false
        slideStartedAt = Date()
        if let q = current {
            history.markPlayed(q.id)
            historyVersion += 1
        }
    }

    // MARK: Play history

    var totalQuestions: Int { bank.categories.reduce(0) { $0 + $1.questions.count } }

    /// Unplayed questions in the selected categories.
    var unplayedInSelection: Int {
        _ = historyVersion
        return selectedCategories.reduce(0) { sum, cat in
            sum + cat.questions.filter { !history.contains($0.id) }.count
        }
    }

    var playedCount: Int {
        _ = historyVersion
        return history.played.count
    }

    func resetHistory() {
        history.reset()
        usedLastGame = []
        historyVersion += 1
    }

    func reveal() {
        guard screen == .game else { return }
        revealed = true
    }

    func next() {
        guard screen == .game else { return }
        if index + 1 >= game.count {
            screen = .end
        } else {
            index += 1
            presentSlide()
        }
    }

    func previous() {
        guard screen == .game, index > 0 else { return }
        index -= 1
        presentSlide()
    }

    func reviewLast() {
        guard !game.isEmpty else { return }
        index = game.count - 1
        presentSlide()
        screen = .game
    }

    func endGame() {
        screen = .setup
    }

    // MARK: Screenshot / demo support (used by CI to capture screens)

    func applyDemo(_ name: String) {
        resetHistory()
        selectAll()
        settings.questionCount = 20
        startGame()
        switch name {
        case "splash": break
        case "game": startMatch()
        case "answer": startMatch(); reveal()
        case "end": startMatch(); screen = .end
        default: screen = .setup
        }
    }
}
