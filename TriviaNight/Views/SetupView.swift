import SwiftUI

struct SetupView: View {
    @Bindable var model: AppModel
    @State private var confirmReset = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header
                StepCard(number: 1, title: "Pick your categories",
                         hint: "Choose as many as you like. Questions rotate through the categories you pick.",
                         trailing: AnyView(categoryTools)) {
                    categories
                }
                StepCard(number: 2, title: "How many questions?",
                         hint: "Questions you’ve already played are saved for last, so nothing repeats until the unplayed ones run out.") {
                    countControl
                }
                StepCard(number: 3, title: "How challenging?",
                         hint: "Slide to set the difficulty for the whole game, or open Advanced to set it per category.") {
                    difficultyControl
                }
                StepCard(number: 4, title: "Timer for each question",
                         hint: "The countdown starts when each question appears. Nothing happens at zero; it’s just a guide.") {
                    timerControl
                }
            }
            .frame(maxWidth: 980)
            .padding(.horizontal, 20)
            .padding(.top, 28)
            .padding(.bottom, 24)
            .frame(maxWidth: .infinity)
        }
        .scrollIndicators(.hidden)
        .safeAreaInset(edge: .bottom) { launchBar }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Wordmark(size: 44)
            Text("Set up tonight’s game, then mirror this screen to the TV.")
                .font(Theme.body(17))
                .foregroundStyle(Theme.muted)
        }
        .padding(.bottom, 12)
    }

    // MARK: Step 1

    private var categoryTools: some View {
        HStack(spacing: 4) {
            Button("Select all") { model.selectAll() }
            Button("Clear") { model.clearAll() }
        }
        .font(Theme.body(14, .medium))
        .foregroundStyle(Theme.muted)
        .buttonStyle(.borderless)
    }

    private var categories: some View {
        VStack(alignment: .leading, spacing: 12) {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 200), spacing: 10)], spacing: 10) {
                ForEach(model.bank.categories) { cat in
                    let on = model.settings.selected.contains(cat.name)
                    Button { model.toggle(cat.name) } label: {
                        HStack(spacing: 10) {
                            CategoryIcon(name: cat.name, size: 22)
                                .foregroundStyle(Theme.amber)
                            Text(cat.name)
                                .font(Theme.body(15.5, .semibold))
                                .foregroundStyle(Theme.text)
                                .multilineTextAlignment(.leading)
                                .lineLimit(2)
                                .minimumScaleFactor(0.85)
                            Spacer(minLength: 4)
                            ZStack {
                                RoundedRectangle(cornerRadius: 6).fill(on ? Theme.amber : .clear)
                                RoundedRectangle(cornerRadius: 6).stroke(on ? Theme.amber : Color(hex: 0x4A5089), lineWidth: 1.5)
                                if on { Image(systemName: "checkmark").font(.system(size: 11, weight: .heavy)).foregroundStyle(Theme.amberInk) }
                            }
                            .frame(width: 20, height: 20)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, minHeight: 58, alignment: .leading)
                        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(on ? Theme.amber.opacity(0.10) : Theme.bg2))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(on ? Theme.amber : Theme.line, lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
            }
            Text(model.settings.selected.isEmpty
                 ? "Pick at least one category to start."
                 : "\(model.settings.selected.count) of \(model.bank.categories.count) selected")
                .font(Theme.body(14))
                .foregroundStyle(Theme.muted)
        }
    }

    // MARK: Step 2

    private var countControl: some View {
        let count = Binding<Int>(get: { model.settings.questionCount }, set: { model.setQuestionCount($0) })
        let n = model.settings.selected.count
        return VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 18) {
                HStack(spacing: 6) {
                    stepButton("minus") { model.setQuestionCount(model.settings.questionCount - 1) }
                    Text("\(model.settings.questionCount)")
                        .font(Theme.display(32))
                        .monospacedDigit()
                        .frame(minWidth: 70)
                    stepButton("plus") { model.setQuestionCount(model.settings.questionCount + 1) }
                }
                .padding(6)
                .background(RoundedRectangle(cornerRadius: 14).fill(Theme.bg2))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Theme.line))
                Text(n == 0 ? "Pick categories first" : "Up to \(model.maxQuestions) with \(n) \(n == 1 ? "category" : "categories")")
                    .font(Theme.body(14))
                    .foregroundStyle(Theme.muted)
            }
            if model.maxQuestions > 1 {
                Slider(value: count.asDouble, in: 1...Double(model.maxQuestions), step: 1)
                    .tint(Theme.amber)
            }
            historyRow
        }
    }

    private var historyRow: some View {
        let unplayed = model.unplayedInSelection
        let low = !model.settings.selected.isEmpty && unplayed < model.settings.questionCount
        return HStack(spacing: 12) {
            Image(systemName: low ? "exclamationmark.circle" : "checkmark.circle")
                .foregroundStyle(low ? Theme.amber : Theme.teal)
            Text(model.settings.selected.isEmpty
                 ? "\(model.totalQuestions.formatted()) questions in the bank"
                 : "\(unplayed.formatted()) not played yet in these categories"
                   + (low ? " · some repeats this game" : ""))
                .font(Theme.body(14))
                .foregroundStyle(Theme.muted)
            Spacer(minLength: 8)
            if model.playedCount > 0 {
                Button("Reset played history") { confirmReset = true }
                    .font(Theme.body(14, .medium))
                    .foregroundStyle(Theme.muted)
                    .buttonStyle(.borderless)
            }
        }
        .alert("Reset played history?", isPresented: $confirmReset) {
            Button("Cancel", role: .cancel) {}
            Button("Reset", role: .destructive) { model.resetHistory() }
        } message: {
            Text("The app has shown \(model.playedCount.formatted()) questions so far. Resetting lets all of them come up again.")
        }
    }

    private func stepButton(_ icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 18, weight: .bold))
                .frame(width: 44, height: 44)
                .background(RoundedRectangle(cornerRadius: 10).fill(Theme.surface))
        }
        .buttonStyle(.plain)
        .foregroundStyle(Theme.text)
    }

    // MARK: Step 3

    private var difficultyControl: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(model.settings.advanced ? "—" : "\(model.settings.difficulty)")
                    .font(Theme.display(40))
                    .monospacedDigit()
                Text(model.settings.advanced ? "Set per category below" : GameSettings.describe(model.settings.difficulty))
                    .font(Theme.body(16))
                    .foregroundStyle(Theme.muted)
            }
            Slider(value: $model.settings.difficulty.asDouble, in: 1...10, step: 1)
                .tint(Theme.amber)
                .disabled(model.settings.advanced)
                .opacity(model.settings.advanced ? 0.35 : 1)
            HStack(alignment: .top) {
                endLabel("1 · Easy", "Most people in the room will know it.", .leading)
                Spacer(minLength: 20)
                endLabel("10 · Expert", "Only serious trivia buffs will get it.", .trailing)
            }
            Button {
                withAnimation(.easeOut(duration: 0.2)) { model.settings.advanced.toggle() }
            } label: {
                Label("Advanced: set difficulty per category", systemImage: "slider.horizontal.3")
                    .font(Theme.body(14.5, .semibold))
                    .foregroundStyle(model.settings.advanced ? Theme.amber : Theme.muted)
                    .padding(.horizontal, 14).padding(.vertical, 9)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(model.settings.advanced ? Theme.amber : Theme.line))
            }
            .buttonStyle(.plain)
            .padding(.top, 4)

            if model.settings.advanced {
                VStack(alignment: .leading, spacing: 10) {
                    Divider().overlay(Theme.line)
                    Text("Each category gets its own level. The main slider is turned off while this is open.")
                        .font(Theme.body(14)).foregroundStyle(Theme.muted)
                    if model.selectedCategories.isEmpty {
                        Text("Select categories above to set their difficulty.")
                            .font(Theme.body(14)).foregroundStyle(Theme.dim)
                    }
                    ForEach(model.selectedCategories) { cat in
                        let b = Binding<Int>(get: { model.categoryDifficulty(cat.name) },
                                             set: { model.setCategoryDifficulty(cat.name, $0) })
                        ViewThatFits(in: .horizontal) {
                            HStack(spacing: 16) {
                                Label { Text(cat.name) } icon: { CategoryIcon(name: cat.name, size: 18).foregroundStyle(Theme.amber) }
                                    .font(Theme.body(15, .semibold)).frame(width: 210, alignment: .leading)
                                Slider(value: b.asDouble, in: 1...10, step: 1).tint(Theme.amber)
                                Text("\(b.wrappedValue)").font(Theme.display(20)).monospacedDigit().frame(width: 32, alignment: .trailing)
                            }
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Label { Text(cat.name) } icon: { CategoryIcon(name: cat.name, size: 18).foregroundStyle(Theme.amber) }
                                        .font(Theme.body(15, .semibold))
                                    Spacer()
                                    Text("\(b.wrappedValue)").font(Theme.display(20)).monospacedDigit()
                                }
                                Slider(value: b.asDouble, in: 1...10, step: 1).tint(Theme.amber)
                            }
                        }
                    }
                }
                .transition(.opacity)
            }
        }
    }

    private func endLabel(_ title: String, _ detail: String, _ align: HorizontalAlignment) -> some View {
        VStack(alignment: align, spacing: 2) {
            Text(title).font(Theme.body(13.5, .semibold)).foregroundStyle(Theme.text)
            Text(detail).font(Theme.body(13.5)).foregroundStyle(Theme.muted)
                .multilineTextAlignment(align == .leading ? .leading : .trailing)
        }
    }

    // MARK: Step 4

    private var timerControl: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(model.settings.timerSeconds)").font(Theme.display(40)).monospacedDigit()
                Text("seconds").font(Theme.body(16, .medium)).foregroundStyle(Theme.muted)
            }
            Slider(value: $model.settings.timerSeconds.asDouble, in: 10...120, step: 5)
                .tint(Theme.amber)
            HStack {
                Text("10 sec"); Spacer(); Text("120 sec")
            }
            .font(Theme.body(13.5, .semibold))
        }
    }

    // MARK: Launch bar

    private var launchBar: some View {
        HStack(spacing: 16) {
            Text(summary)
                .font(Theme.body(14.5))
                .foregroundStyle(Theme.muted)
                .lineLimit(2)
            Spacer(minLength: 8)
            Button("Start Game  →") { model.startGame() }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!model.canStart)
                .keyboardShortcut(.return, modifiers: .command)
        }
        .frame(maxWidth: 980)
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(.ultraThinMaterial.opacity(0.9))
        .background(Theme.bg.opacity(0.6))
    }

    private var summary: String {
        let s = model.settings
        guard !s.selected.isEmpty else { return "Choose at least one category" }
        let n = s.selected.count
        let diff = s.advanced ? "custom difficulty" : "difficulty \(s.difficulty)"
        return "\(s.questionCount) questions · \(n) \(n == 1 ? "category" : "categories") · \(diff) · \(s.timerSeconds)s timer"
    }
}

struct StepCard<Content: View>: View {
    let number: Int
    let title: String
    let hint: String
    var trailing: AnyView? = nil
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 14) {
                Text("\(number)")
                    .font(Theme.display(17))
                    .foregroundStyle(Theme.amberInk)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(Theme.amber))
                Text(title)
                    .font(Theme.display(24, .bold))
                    .foregroundStyle(Theme.text)
                Spacer(minLength: 8)
                if let trailing { trailing }
            }
            Text(hint)
                .font(Theme.body(15))
                .foregroundStyle(Theme.muted)
                .padding(.top, 6)
                .padding(.bottom, 18)
            content
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(LinearGradient(colors: [Color.white.opacity(0.035), Color.white.opacity(0.01)], startPoint: .top, endPoint: .bottom))
        )
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(Theme.line))
    }
}
