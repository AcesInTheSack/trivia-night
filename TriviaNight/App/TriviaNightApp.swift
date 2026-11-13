import SwiftUI
import UIKit

@main
struct TriviaNightApp: App {
    @State private var model = AppModel()

    init() {
        // CI screenshots: launch with `-uiDemo splash|game|answer|end`.
        if let demo = UserDefaults.standard.string(forKey: "uiDemo") {
            let m = AppModel()
            m.applyDemo(demo)
            _model = State(initialValue: m)
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
                .preferredColorScheme(.dark)
        }
    }
}

struct RootView: View {
    let model: AppModel

    var body: some View {
        ZStack {
            BackgroundView()
            switch model.screen {
            case .setup: SetupView(model: model).transition(.opacity)
            case .splash: SplashView(model: model).transition(.opacity)
            case .game: GameView(model: model).transition(.opacity)
            case .end: EndView(model: model).transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: model.screen)
        .statusBarHidden(model.screen != .setup)
        .persistentSystemOverlays(model.screen == .setup ? .automatic : .hidden)
        .onChange(of: model.screen, initial: true) { _, screen in
            // Keep the screen awake while a game is on the TV.
            UIApplication.shared.isIdleTimerDisabled = screen != .setup
        }
    }
}
