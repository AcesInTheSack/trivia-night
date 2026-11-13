import Foundation

struct Question: Identifiable, Hashable {
    let id: String          // "<category>|<index>"
    let category: String
    let difficulty: Int     // 1...10
    let text: String
    let answer: String
}

struct Category: Identifiable, Hashable {
    var id: String { name }
    let name: String
    let icon: String
    let questions: [Question]
}

struct QuestionBank {
    let categories: [Category]

    init(categories: [Category]) {
        self.categories = categories
    }

    private struct File: Decodable {
        struct Cat: Decodable { let name: String; let icon: String; let questions: [Q] }
        struct Q: Decodable { let d: Int; let q: String; let a: String }
        let categories: [Cat]
    }

    init(data: Data) throws {
        let file = try JSONDecoder().decode(File.self, from: data)
        categories = file.categories.map { cat in
            Category(
                name: cat.name,
                icon: cat.icon,
                questions: cat.questions.enumerated().map { i, q in
                    Question(id: "\(cat.name)|\(i)", category: cat.name, difficulty: q.d, text: q.q, answer: q.a)
                }
            )
        }
    }

    static func loadBundled(_ bundle: Bundle = .main) -> QuestionBank {
        guard let url = bundle.url(forResource: "questions", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let bank = try? QuestionBank(data: data) else {
            return QuestionBank(categories: [])
        }
        return bank
    }

    func category(named name: String) -> Category? {
        categories.first { $0.name == name }
    }
}

struct GameSettings: Equatable {
    var selected: Set<String> = []
    var questionCount: Int = 20
    var difficulty: Int = 5
    var advanced: Bool = false
    var categoryDifficulty: [String: Int] = [:]
    var timerSeconds: Int = 30

    static let timerRange = 10...120
    static let difficultyRange = 1...10

    func difficulty(for category: String) -> Int {
        advanced ? (categoryDifficulty[category] ?? difficulty) : difficulty
    }

    static func describe(_ d: Int) -> String {
        switch d {
        case ...2: return "Easy: most people will know it"
        case 3...4: return "Casual: a little thinking required"
        case 5...6: return "Medium: a good mix for a typical crowd"
        case 7...8: return "Hard: rewards people who read and remember"
        default: return "Expert: for serious trivia buffs"
        }
    }
}

enum GameBuilder {
    /// Picks questions for a game.
    /// - Spreads the count evenly across the selected categories.
    /// - Within a category, prefers questions not in `avoiding`, then the closest difficulty.
    /// - Interleaves categories so the same one rarely appears twice in a row.
    static func build<R: RandomNumberGenerator>(
        bank: QuestionBank,
        settings: GameSettings,
        avoiding used: Set<String> = [],
        rng: inout R
    ) -> [Question] {
        let cats = bank.categories.filter { settings.selected.contains($0.name) }.shuffled(using: &rng)
        guard !cats.isEmpty else { return [] }

        let available = cats.reduce(0) { $0 + $1.questions.count }
        var remaining = max(0, min(settings.questionCount, available))
        var perCategory: [String: Int] = Dictionary(uniqueKeysWithValues: cats.map { ($0.name, 0) })
        var i = 0
        while remaining > 0 {
            let cat = cats[i % cats.count]
            if perCategory[cat.name, default: 0] < cat.questions.count {
                perCategory[cat.name, default: 0] += 1
                remaining -= 1
            }
            i += 1
        }

        var picks: [String: [Question]] = [:]
        for cat in cats {
            let target = settings.difficulty(for: cat.name)
            let ranked = cat.questions.shuffled(using: &rng).enumerated().sorted { a, b in
                let ua = used.contains(a.element.id), ub = used.contains(b.element.id)
                if ua != ub { return !ua }
                let da = abs(a.element.difficulty - target), db = abs(b.element.difficulty - target)
                if da != db { return da < db }
                return a.offset < b.offset
            }.map(\.element)
            picks[cat.name] = Array(ranked.prefix(perCategory[cat.name, default: 0])).shuffled(using: &rng)
        }

        var out: [Question] = []
        var added = true
        while added {
            added = false
            for cat in cats {
                if var list = picks[cat.name], !list.isEmpty {
                    out.append(list.removeFirst())
                    picks[cat.name] = list
                    added = true
                }
            }
        }
        return out
    }
}
