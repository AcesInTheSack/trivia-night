# Trivia Night (working title)

An iPhone and iPad app for running an in-person trivia night. The host mirrors the device to a TV with AirPlay or a cable, picks categories, question count, difficulty, and a timer, then steps through question slides. The answer stays hidden until the host taps Show Answer, every time a slide appears.

Built with SwiftUI, iOS 17+.

## Layout

```
project.yml                 XcodeGen spec (the .xcodeproj is generated, not committed)
TriviaNight/App             app entry point and screen switching
TriviaNight/Models          question bank, game settings, question picker, app state
TriviaNight/Views           setup, splash, question slide, end screen, theme
TriviaNight/Resources       questions.json (15 categories × 20), asset catalog
TriviaNightTests            unit tests for question picking and game flow
.github/workflows/ci.yml    builds, runs tests, and captures simulator screenshots on every push
.github/workflows/testflight.yml   signed build + TestFlight upload (manual)
fastlane/Fastfile           the TestFlight lane
```

## Building

There's no Mac in the loop, so GitHub's macOS runners do the building. Every push to `main` runs CI; screenshots of each screen on an iPad and an iPhone are attached to the run as the `screenshots` artifact.

On a Mac: `brew install xcodegen && xcodegen generate && open TriviaNight.xcodeproj`.

## Getting it onto TestFlight / the App Store

One-time setup in your Apple Developer account:

1. **Team ID.** developer.apple.com → Account → Membership details. Copy the 10-character Team ID.
2. **Bundle ID.** developer.apple.com → Certificates, Identifiers & Profiles → Identifiers → + → App IDs → App. Bundle ID (explicit): `com.acesinthesack.trivianight`. No capabilities needed. (If you'd rather use a different ID, change `PRODUCT_BUNDLE_IDENTIFIER` in `project.yml` and the bundle ID in `ci.yml`.)
3. **App record.** appstoreconnect.apple.com → Apps → + → New App. Platform iOS, name "Trivia Night" (you can rename before release), pick the bundle ID from step 2, SKU anything (e.g. `trivianight`).
4. **API key.** App Store Connect → Users and Access → Integrations → App Store Connect API → Team Keys → +. Name it "GitHub CI", role **Admin** (needed for cloud-managed signing). Download the `.p8` file (you only get one chance), and note the Key ID and the Issuer ID shown above the list.
5. **GitHub secrets.** In this repo: Settings → Secrets and variables → Actions → New repository secret. Add:
   - `APPLE_TEAM_ID`: from step 1
   - `ASC_KEY_ID`: Key ID from step 4
   - `ASC_ISSUER_ID`: Issuer ID from step 4
   - `ASC_KEY_P8_BASE64`: the `.p8` file, base64-encoded. On Windows PowerShell:
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Downloads\AuthKey_XXXXXX.p8")) | Set-Clipboard`
     then paste.

Then: Actions → TestFlight → Run workflow. The build shows up in App Store Connect under TestFlight after Apple finishes processing it (usually 10–30 minutes). Install the TestFlight app on your iPad to try it.

Submitting for review still happens in App Store Connect: screenshots, description, privacy answers (the app collects no data), and age rating.
