import SwiftUI

// MARK: - Splash

struct SplashView: View {
    let model: AppModel

    var body: some View {
        GeometryReader { geo in
            let s = scale(geo.size)
            ZStack(alignment: .topLeading) {
                ConfettiView()
                VStack(spacing: 0) {
                    Spacer()
                    Text("WELCOME, EVERYBODY")
                        .font(Theme.body(18 * s, .bold))
                        .tracking(5 * s)
                        .foregroundStyle(Theme.amber)
                        .padding(.bottom, 18 * s)
                    (Text("It’s ").foregroundStyle(Theme.text) + Text("Trivia").foregroundStyle(Theme.amber) + Text("\nNight.").foregroundStyle(Theme.text))
                        .font(Theme.display(130 * s, .heavy))
                        .multilineTextAlignment(.center)
                        .lineSpacing(-10 * s)
                        .minimumScaleFactor(0.5)
                    Text("Grab your team, find a pen, and get ready. Answers show up only when the host reveals them.")
                        .font(Theme.body(26 * s))
                        .foregroundStyle(Theme.muted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 900 * s)
                        .padding(.top, 26 * s)
                    HStack(spacing: 12) {
                        Pill(text: "\(model.game.count) questions", size: 17 * s)
                        Pill(text: categorySummary, size: 17 * s)
                        Pill(text: "\(model.settings.timerSeconds)-second timer", size: 17 * s)
                    }
                    .padding(.vertical, 36 * s)
                    Button("Start Match") { model.startMatch() }
                        .buttonStyle(PrimaryButtonStyle(size: 28 * s))
                        .keyboardShortcut(.return, modifiers: [])
                    Spacer()
                    CompanyCredit(size: max(13, 18 * s))
                        .padding(.bottom, 8 * s)
                }
                .padding(32)
                .frame(maxWidth: .infinity)

                Button("← Setup") { model.endGame() }
                    .buttonStyle(GhostButtonStyle(size: 15))
                    .padding(20)
            }
        }
    }

    private var categorySummary: String {
        let names = model.selectedCategories.map(\.name)
        if names.count <= 2 { return names.joined(separator: ", ") }
        return "\(names.prefix(2).joined(separator: ", ")) + \(names.count - 2) more"
    }
}

/// Scales type with the screen so it reads well on a phone and on a TV via mirroring.
func scale(_ size: CGSize) -> CGFloat {
    let base = min(size.width / 1200, size.height / 800)
    return min(max(base, 0.42), 1.4)
}

// MARK: - Question slide

struct GameView: View {
    let model: AppModel
    @State private var confirmEnd = false

    var body: some View {
        GeometryReader { geo in
            let s = scale(geo.size)
            let compact = geo.size.width < 600
            VStack(spacing: 0) {
                topBar(s: s, compact: compact)
                ProgressBar(value: Double(model.index + 1) / Double(max(model.game.count, 1)))
                    .padding(.top, 14 * s)
                stage(s: s, compact: compact)
                navBar(s: s, compact: compact)
            }
            .padding(.horizontal, max(16, 36 * s))
            .padding(.vertical, max(12, 28 * s))
        }
        .alert("End this game?", isPresented: $confirmEnd) {
            Button("Keep playing", role: .cancel) {}
            Button("End game", role: .destructive) { model.endGame() }
        } message: {
            Text("You’ll go back to setup. The current set of questions will be lost.")
        }
        .background(keyboardShortcuts)
    }

    @ViewBuilder
    private func topBar(s: CGFloat, compact: Bool) -> some View {
        if let q = model.current {
            HStack(alignment: .center, spacing: 14) {
                (Text("Question \(model.index + 1) ").foregroundStyle(Theme.text)
                 + Text("of \(model.game.count)").foregroundStyle(Theme.muted))
                    .font(Theme.display(max(18, 28 * s), .bold))
                    .fixedSize()
                if !compact { chips(q, s: s) }
                Spacer(minLength: 8)
                Button("End game") { confirmEnd = true }
                    .buttonStyle(GhostButtonStyle(size: max(13, 15 * s)))
                TimerRing(start: model.slideStartedAt, seconds: model.settings.timerSeconds)
                    .frame(width: max(64, 116 * s), height: max(64, 116 * s))
            }
            if compact {
                HStack(spacing: 8) { chips(q, s: s); Spacer() }
                    .padding(.top, 10)
            }
        }
    }

    @ViewBuilder
    private func chips(_ q: Question, s: CGFloat) -> some View {
        HStack(spacing: 6 * max(s, 0.7)) {
            CategoryIcon(name: q.category, size: max(13, 18 * s))
            Text(q.category)
        }
            .font(Theme.body(max(12, 17 * s), .semibold))
            .foregroundStyle(Theme.amber)
            .padding(.horizontal, 14 * max(s, 0.7)).padding(.vertical, 6 * max(s, 0.7))
            .background(Capsule().fill(Theme.amber.opacity(0.12)))
            .overlay(Capsule().stroke(Theme.amber.opacity(0.35)))
            .lineLimit(1)
            .fixedSize()
        Text("Difficulty \(q.difficulty)")
            .font(Theme.body(max(12, 17 * s), .semibold))
            .foregroundStyle(Theme.muted)
            .padding(.horizontal, 14 * max(s, 0.7)).padding(.vertical, 6 * max(s, 0.7))
            .overlay(Capsule().stroke(Theme.line))
            .lineLimit(1)
            .fixedSize()
    }

    private func stage(s: CGFloat, compact: Bool) -> some View {
        VStack(spacing: max(24, 56 * s)) {
            Text(model.current?.text ?? "")
                .font(Theme.display(compact ? 30 : 76 * s, .bold))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.5)
                .frame(maxWidth: 1500 * s)
                .id("q-\(model.index)")
                .transition(.opacity)

            ZStack {
                if model.revealed {
                    VStack(spacing: 8) {
                        Text("ANSWER")
                            .font(Theme.body(max(12, 18 * s), .bold))
                            .tracking(4)
                            .foregroundStyle(Theme.teal)
                        Text(model.current?.answer ?? "")
                            .font(Theme.display(compact ? 32 : 72 * s, .heavy))
                            .foregroundStyle(Theme.teal)
                            .multilineTextAlignment(.center)
                            .minimumScaleFactor(0.5)
                    }
                    .transition(.scale(scale: 0.9).combined(with: .opacity))
                } else {
                    Text("ANSWER HIDDEN")
                        .font(Theme.body(max(14, 22 * s), .semibold))
                        .tracking(3)
                        .foregroundStyle(Theme.dim)
                        .padding(.horizontal, max(24, 44 * s))
                        .padding(.vertical, max(14, 24 * s))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(Theme.line, style: StrokeStyle(lineWidth: 2, dash: [7, 6]))
                        )
                }
            }
            .frame(minHeight: max(80, 150 * s))
            .animation(.spring(response: 0.35, dampingFraction: 0.7), value: model.revealed)
        }
        .padding(.horizontal, compact ? 4 : 60 * s)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func navBar(s: CGFloat, compact: Bool) -> some View {
        let fs = max(16, 22 * s)
        return HStack(spacing: max(10, 20 * s)) {
            Button { model.previous() } label: {
                Label(compact ? "" : "Previous", systemImage: "chevron.left")
                    .labelStyle(NavLabelStyle(compact: compact))
            }
            .buttonStyle(NavButtonStyle(size: fs))
            .disabled(model.isFirst)
            .accessibilityLabel("Previous question")

            Button(model.revealed ? "Answer shown" : "Show Answer") { model.reveal() }
                .buttonStyle(ShowButtonStyle(size: max(18, 28 * s), shown: model.revealed))
                .disabled(model.revealed)

            Button { model.next() } label: {
                Label(compact ? "" : (model.isLast ? "Finish" : "Next"), systemImage: "chevron.right")
                    .labelStyle(NavLabelStyle(compact: compact, iconTrailing: true))
            }
            .buttonStyle(NavButtonStyle(size: fs))
            .accessibilityLabel(model.isLast ? "Finish" : "Next question")
        }
    }

    /// Hardware keyboard support for an iPad host: arrows move, space reveals.
    private var keyboardShortcuts: some View {
        ZStack {
            Button("") { model.next() }.keyboardShortcut(.rightArrow, modifiers: [])
            Button("") { model.previous() }.keyboardShortcut(.leftArrow, modifiers: [])
            Button("") { model.reveal() }.keyboardShortcut(.space, modifiers: [])
        }
        .opacity(0)
        .accessibilityHidden(true)
    }
}

struct NavLabelStyle: LabelStyle {
    var compact: Bool
    var iconTrailing = false
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 10) {
            if iconTrailing && !compact { configuration.title }
            configuration.icon.font(.system(size: 18, weight: .heavy))
            if !iconTrailing && !compact { configuration.title }
        }
    }
}

struct NavButtonStyle: ButtonStyle {
    var size: CGFloat
    @Environment(\.isEnabled) private var isEnabled
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.body(size, .bold))
            .foregroundStyle(Theme.text)
            .padding(.horizontal, size * 1.3)
            .padding(.vertical, size * 0.8)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(configuration.isPressed ? Color(hex: 0x222852) : Theme.surface))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Theme.line, lineWidth: 1.5))
            .opacity(isEnabled ? 1 : 0.3)
    }
}

struct ShowButtonStyle: ButtonStyle {
    var size: CGFloat
    var shown: Bool
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.display(size, .heavy))
            .foregroundStyle(shown ? Theme.teal : Theme.tealInk)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .padding(.horizontal, size * 1.6)
            .padding(.vertical, size * 0.75)
            .frame(minWidth: size * 9)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(shown ? Color.clear : (configuration.isPressed ? Color(hex: 0x78E0D2) : Theme.teal)))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(shown ? Theme.teal.opacity(0.35) : Theme.teal, lineWidth: 1.5))
    }
}

struct ProgressBar: View {
    var value: Double
    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Capsule().fill(Theme.line)
                Capsule().fill(Theme.amber).frame(width: g.size.width * value)
                    .animation(.easeOut(duration: 0.3), value: value)
            }
        }
        .frame(height: 4)
    }
}

struct TimerRing: View {
    let start: Date
    let seconds: Int

    var body: some View {
        TimelineView(.animation(minimumInterval: 0.05)) { ctx in
            let total = Double(seconds)
            let remaining = max(0, total - ctx.date.timeIntervalSince(start))
            let fraction = total > 0 ? remaining / total : 0
            let done = remaining <= 0
            let warn = !done && remaining <= min(10, total * 0.25)
            let color = done ? Theme.red : (warn ? Theme.amber : Theme.teal)
            GeometryReader { g in
                let lw = g.size.width * 0.08
                ZStack {
                    Circle().stroke(Theme.line, lineWidth: lw)
                    Circle()
                        .trim(from: 0, to: fraction)
                        .stroke(color, style: StrokeStyle(lineWidth: lw, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                    Text("\(Int(remaining.rounded(.up)))")
                        .font(Theme.display(g.size.width * 0.38))
                        .monospacedDigit()
                        .foregroundStyle(done ? Theme.red : Theme.text)
                }
                .padding(lw / 2)
            }
        }
        .accessibilityLabel("Time remaining")
    }
}

// MARK: - End

struct EndView: View {
    let model: AppModel

    var body: some View {
        GeometryReader { geo in
            let s = scale(geo.size)
            ZStack {
                ConfettiView()
                VStack(spacing: 0) {
                    Text("THAT’S THE LAST QUESTION")
                        .font(Theme.body(18 * s, .bold))
                        .tracking(5 * s)
                        .foregroundStyle(Theme.amber)
                        .padding(.bottom, 18 * s)
                    (Text("That’s a ").foregroundStyle(Theme.text) + Text("wrap.").foregroundStyle(Theme.amber))
                        .font(Theme.display(120 * s, .heavy))
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.5)
                    Text("Pens down. Time to tally the scores. Thanks for playing!")
                        .font(Theme.body(26 * s))
                        .foregroundStyle(Theme.muted)
                        .multilineTextAlignment(.center)
                        .padding(.top, 24 * s)
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 14) { buttons(s) }
                        VStack(spacing: 12) { buttons(s) }
                    }
                    .padding(.top, 44 * s)
                }
                .padding(32)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    @ViewBuilder
    private func buttons(_ s: CGFloat) -> some View {
        Button("◀  Back to last question") { model.reviewLast() }
            .buttonStyle(GhostButtonStyle(size: max(15, 18 * s)))
        Button("New game setup") { model.endGame() }
            .buttonStyle(GhostButtonStyle(size: max(15, 18 * s)))
        Button("Play again with new questions") { model.startGame() }
            .buttonStyle(PrimaryButtonStyle(size: max(17, 22 * s)))
    }
}
