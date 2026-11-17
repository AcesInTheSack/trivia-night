import SwiftUI

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }
}

enum Theme {
    static let bg = Color(hex: 0x0E1124)
    static let bg2 = Color(hex: 0x151A36)
    static let surface = Color(hex: 0x1A1F40)
    static let line = Color(hex: 0x2B315C)
    static let text = Color(hex: 0xF5F1E8)
    static let muted = Color(hex: 0xA6A9C8)
    static let dim = Color(hex: 0x6F739A)
    static let amber = Color(hex: 0xFFB547)
    static let amberInk = Color(hex: 0x2A1A00)
    static let teal = Color(hex: 0x5FD4C4)
    static let tealInk = Color(hex: 0x062824)
    static let red = Color(hex: 0xFF6B6B)

    static func display(_ size: CGFloat, _ weight: Font.Weight = .heavy) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }

    static func body(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .default)
    }
}

struct BackgroundView: View {
    var body: some View {
        ZStack {
            Theme.bg
            RadialGradient(colors: [Color(hex: 0x24295A), .clear], center: UnitPoint(x: 0.85, y: -0.1), startRadius: 0, endRadius: 900)
            RadialGradient(colors: [Color(hex: 0x2A1F45), .clear], center: UnitPoint(x: -0.1, y: 1.1), startRadius: 0, endRadius: 700)
        }
        .ignoresSafeArea()
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var fill: Color = Theme.amber
    var ink: Color = Theme.amberInk
    var size: CGFloat = 20
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.display(size, .heavy))
            .foregroundStyle(isEnabled ? ink : Color(hex: 0x7D81A8))
            .padding(.horizontal, size * 1.5)
            .padding(.vertical, size * 0.8)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isEnabled ? fill : Color(hex: 0x3A3F66))
            )
            .shadow(color: isEnabled ? fill.opacity(0.25) : .clear, radius: 16, y: 6)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct GhostButtonStyle: ButtonStyle {
    var size: CGFloat = 16
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.body(size, .semibold))
            .foregroundStyle(configuration.isPressed ? Theme.text : Theme.muted)
            .padding(.horizontal, size * 1.2)
            .padding(.vertical, size * 0.7)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Theme.line, lineWidth: 1.5)
            )
            .contentShape(Rectangle())
    }
}

struct Pill: View {
    let text: String
    var size: CGFloat = 16
    var body: some View {
        Text(text)
            .font(Theme.body(size, .medium))
            .foregroundStyle(Theme.text)
            .padding(.horizontal, size)
            .padding(.vertical, size * 0.55)
            .background(Capsule().fill(Color.white.opacity(0.04)))
            .overlay(Capsule().stroke(Theme.line, lineWidth: 1))
    }
}

/// Scattered confetti drawn once per appearance.
struct ConfettiView: View {
    @State private var seed = UInt64.random(in: 1...UInt64.max)

    var body: some View {
        Canvas { ctx, size in
            var rng = SeededRNG(seed: seed)
            let colors: [Color] = [Theme.amber, Theme.teal, Theme.red, Color(hex: 0xA78BFA), Theme.text]
            for i in 0..<70 {
                let x = Double.random(in: 0...size.width, using: &rng)
                let y = Double.random(in: 0...size.height, using: &rng)
                let s = Double.random(in: 6...18, using: &rng)
                let color = colors[i % colors.count].opacity(Double.random(in: 0.3...0.8, using: &rng))
                if i % 3 == 0 {
                    ctx.fill(Path(ellipseIn: CGRect(x: x, y: y, width: s / 1.5, height: s / 1.5)), with: .color(color))
                } else {
                    var c = ctx
                    c.translateBy(x: x, y: y)
                    c.rotate(by: .degrees(Double.random(in: 0...360, using: &rng)))
                    c.fill(Path(roundedRect: CGRect(x: 0, y: 0, width: s, height: s * 0.45), cornerRadius: 2), with: .color(color))
                }
            }
        }
        .opacity(0.5)
        .allowsHitTesting(false)
        .ignoresSafeArea()
    }
}

struct SeededRNG: RandomNumberGenerator {
    private var state: UInt64
    init(seed: UInt64) { state = seed == 0 ? 0x9E3779B97F4A7C15 : seed }
    mutating func next() -> UInt64 {
        state &+= 0x9E3779B97F4A7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58476D1CE4E5B9
        z = (z ^ (z >> 27)) &* 0x94D049BB133111EB
        return z ^ (z >> 31)
    }
}

extension Binding where Value == Int {
    /// Bridges an Int binding to the Double that Slider needs.
    var asDouble: Binding<Double> {
        Binding<Double>(get: { Double(wrappedValue) }, set: { wrappedValue = Int($0.rounded()) })
    }
}

/// Custom line icon for a category, from the asset catalog (cat-<slug>), tinted by foregroundStyle.
struct CategoryIcon: View {
    let name: String
    var size: CGFloat = 20

    static func assetName(_ category: String) -> String {
        let slug = category.lowercased()
            .map { $0.isLetter || $0.isNumber ? String($0) : "-" }
            .joined()
            .split(separator: "-", omittingEmptySubsequences: true)
            .joined(separator: "-")
        return "cat-" + slug
    }

    var body: some View {
        Image(Self.assetName(name))
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// “Trivia Night” plus the BIG SCREEN tag, as on the app icon.
struct Wordmark: View {
    var size: CGFloat = 44
    var body: some View {
        HStack(alignment: .center, spacing: size * 0.3) {
            Text("Trivia Night")
                .font(.system(size: size, weight: .bold))
                .foregroundStyle(Theme.text)
                .tracking(-size * 0.02)
            Text("BIG SCREEN")
                .font(.system(size: size * 0.3, weight: .bold))
                .tracking(size * 0.03)
                .foregroundStyle(Theme.bg)
                .padding(.horizontal, size * 0.24)
                .padding(.vertical, size * 0.12)
                .background(RoundedRectangle(cornerRadius: size * 0.16).fill(Theme.amber))
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Trivia Night: Big Screen")
    }
}

/// “by The Oregon Critter & Travel Co.” with the company logo.
struct CompanyCredit: View {
    var size: CGFloat = 16
    var body: some View {
        HStack(spacing: size * 0.75) {
            Image("critter-logo")
                .resizable()
                .scaledToFill()
                .frame(width: size * 3.2, height: size * 3.2)
                .clipShape(RoundedRectangle(cornerRadius: size * 0.7, style: .continuous))
                .accessibilityHidden(true)
            (Text("by ").foregroundStyle(Theme.muted)
             + Text("The Oregon Critter & Travel Co.").foregroundStyle(Theme.text).fontWeight(.semibold))
                .font(Theme.body(size))
        }
        .accessibilityElement(children: .combine)
    }
}
