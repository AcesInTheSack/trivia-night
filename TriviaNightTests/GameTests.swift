import XCTest
@testable import TriviaNight

final class GameTests: XCTestCase {
    private var bank: QuestionBank!

    override func setUp() {
        bank = QuestionBank.loadBundled(Bundle(for: AppModel.self))
    }

    func testBankHas15CategoriesOf20() {
        XCTAssertEqual(bank.categories.count, 15)
        for cat in bank.categories {
            XCTAssertEqual(cat.questions.count, 20, cat.name)
            XCTAssertTrue(cat.questions.allSatisfy { (1...10).contains($0.difficulty) }, cat.name)
        }
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
        s.questionCount = 500
        var rng = SeededRNG(seed: 1)
        XCTAssertEqual(GameBuilder.build(bank: bank, settings: s, rng: &rng).count, 20)
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
        let model = AppModel(bank: bank)
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
        let model = AppModel(bank: bank)
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
        let model = AppModel(bank: bank)
        model.selectAll()
        model.setQuestionCount(200)
        XCTAssertEqual(model.settings.questionCount, 200)
        model.settings.selected = ["Music"]
        model.toggle("History")   // now History + Music → max 40
        XCTAssertEqual(model.settings.questionCount, 40)
    }
}
