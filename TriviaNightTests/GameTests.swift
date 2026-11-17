import XCTest
@testable import TriviaNight

final class GameTests: XCTestCase {
    private var bank: QuestionBank!

    override func setUp() {
        bank = QuestionBank.loadBundled(Bundle(for: AppModel.self))
    }

    /// The bundled bank trimmed to `n` questions per category, for tests that need to exhaust a category.
    private func smallBank(_ n: Int = 20) -> QuestionBank {
        QuestionBank(categories: bank.categories.map {
            Category(name: $0.name, icon: $0.icon, questions: Array($0.questions.prefix(n)))
        })
    }

    func testBankHas15BalancedCategories() {
        XCTAssertEqual(bank.categories.count, 15)
        let size = bank.categories.first?.questions.count ?? 0
        XCTAssertGreaterThanOrEqual(size, 100)
        for cat in bank.categories {
            XCTAssertEqual(cat.questions.count, size, cat.name)
            XCTAssertEqual(Set(cat.questions.map(\.difficulty)), Set(1...10), "\(cat.name) covers every difficulty")
            XCTAssertTrue(cat.questions.allSatisfy { !$0.text.isEmpty && !$0.answer.isEmpty }, cat.name)
        }
    }

    func testEveryQuestionHasAUniquePermanentID() {
        let ids = bank.categories.flatMap { $0.questions.map(\.id) }
        XCTAssertEqual(Set(ids).count, ids.count, "IDs must be unique")
        XCTAssertTrue(ids.allSatisfy { $0.hasPrefix("q") && $0.count == 13 }, "IDs come from questions.json, not array position")
    }

    func testHistoryPersistsAcrossLaunches() throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        defer { try? FileManager.default.removeItem(at: url) }
        let first = PlayHistory(url: url)
        first.markPlayed("qabc")
        first.markPlayed("qdef")
        let reloaded = PlayHistory(url: url)
        XCTAssertEqual(reloaded.played, ["qabc", "qdef"])
        reloaded.reset()
        XCTAssertTrue(PlayHistory(url: url).played.isEmpty)
    }

    func testShownQuestionsAreNeverRepeatedUntilTheBankRunsOut() {
        let model = AppModel(bank: smallBank(20), history: PlayHistory(url: nil))
        model.settings.selected = ["History", "Music"]   // 40 questions
        model.settings.questionCount = 10
        var seen = Set<String>()
        for _ in 0..<4 {                                  // 4 games x 10 = all 40
            model.startGame()
            model.startMatch()
            for _ in 0..<10 { seen.insert(model.current!.id); model.next() }
        }
        XCTAssertEqual(seen.count, 40, "four games used every question exactly once")
        XCTAssertEqual(model.unplayedInSelection, 0)
        model.startGame()                                 // bank exhausted: repeats allowed
        XCTAssertEqual(model.game.count, 10)
        model.resetHistory()
        XCTAssertEqual(model.unplayedInSelection, 40)
    }

    func testOnlyQuestionsActuallyShownCountAsPlayed() {
        let model = AppModel(bank: bank, history: PlayHistory(url: nil))
        model.settings.questionCount = 10
        model.startGame()
        XCTAssertEqual(model.playedCount, 0, "building a game doesn't mark anything")
        model.startMatch(); model.next(); model.next()
        XCTAssertEqual(model.playedCount, 3)
    }

    func testBuildRespectsCountAndCategories() {
        var s = GameSettings()
        s.selected = ["History", "Music", "Sports"]
        s.questionCount = 25
        var rng = SeededRNG(seed: 42)
        let game = GameBuilder.build(bank: bank, settings: s, rng: &rng)
        XCTAssertEqual(game.count, 25)
        XCTAssertEqual(Set(game.map(\.id)).count, 25, "no duplicates")
        XCTAssertTrue(game.allSatisfy { s.selected.contains($0.category) })
    }

    func testCountIsCappedByAvailableQuestions() {
        var s = GameSettings()
        s.selected = ["History"]
        s.questionCount = 5000
        var rng = SeededRNG(seed: 1)
        let available = bank.category(named: "History")!.questions.count
        XCTAssertEqual(GameBuilder.build(bank: bank, settings: s, rng: &rng).count, available)
    }

    func testDifficultyTargetsClosestQuestions() {
        var s = GameSettings()
        s.selected = ["Science"]
        s.questionCount = 4
        s.difficulty = 10
        var rng = SeededRNG(seed: 7)
        let game = GameBuilder.build(bank: bank, settings: s, rng: &rng)
        XCTAssertTrue(game.allSatisfy { $0.difficulty >= 9 })
    }

    func testAdvancedPerCategoryDifficulty() {
        var s = GameSettings()
        s.selected = ["Science", "History"]
        s.questionCount = 4
        s.advanced = true
        s.categoryDifficulty = ["Science": 1, "History": 10]
        var rng = SeededRNG(seed: 3)
        let game = GameBuilder.build(bank: bank, settings: s, rng: &rng)
        XCTAssertTrue(game.filter { $0.category == "Science" }.allSatisfy { $0.difficulty <= 2 })
        XCTAssertTrue(game.filter { $0.category == "History" }.allSatisfy { $0.difficulty >= 9 })
    }

    func testCategoriesAlternate() {
        var s = GameSettings()
        s.selected = ["Science", "History"]
        s.questionCount = 10
        var rng = SeededRNG(seed: 9)
        let game = GameBuilder.build(bank: bank, settings: s, rng: &rng)
        for i in 1..<game.count {
            XCTAssertNotEqual(game[i].category, game[i - 1].category)
        }
    }

    func testAvoidsPreviousGameQuestions() {
        var s = GameSettings()
        s.selected = ["Music"]
        s.questionCount = 10
        var rng = SeededRNG(seed: 5)
        let first = GameBuilder.build(bank: bank, settings: s, rng: &rng)
        let second = GameBuilder.build(bank: bank, settings: s, avoiding: Set(first.map(\.id)), rng: &rng)
        XCTAssertTrue(Set(first.map(\.id)).isDisjoint(with: second.map(\.id)))
    }

    func testAnswerIsHiddenOnEveryNavigation() {
        let model = AppModel(bank: bank, history: PlayHistory(url: nil))
        model.startGame()
        model.startMatch()
        XCTAssertFalse(model.revealed)
        model.reveal()
        XCTAssertTrue(model.revealed)
        model.next()
        XCTAssertFalse(model.revealed, "moving forward hides the answer")
        model.previous()
        XCTAssertFalse(model.revealed, "moving back hides the answer, even if revealed before")
    }

    func testFlowReachesEndAndCanReview() {
        let model = AppModel(bank: bank, history: PlayHistory(url: nil))
        model.settings.questionCount = 3
        model.startGame()
        XCTAssertEqual(model.screen, .splash)
        model.startMatch()
        XCTAssertEqual(model.screen, .game)
        model.previous()
        XCTAssertEqual(model.index, 0, "can't go before the first question")
        model.next(); model.next(); model.next()
        XCTAssertEqual(model.screen, .end)
        model.reviewLast()
        XCTAssertEqual(model.screen, .game)
        XCTAssertEqual(model.index, 2)
        XCTAssertFalse(model.revealed)
    }

    func testCountClampsWhenCategoriesChange() {
        let model = AppModel(bank: smallBank(20), history: PlayHistory(url: nil))
        model.selectAll()
        model.setQuestionCount(200)
        XCTAssertEqual(model.settings.questionCount, 200)
        model.settings.selected = ["Music"]
        model.toggle("History")   // now History + Music → max 40
        XCTAssertEqual(model.settings.questionCount, 40)
    }
}
