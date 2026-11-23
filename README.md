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

## Questions

`TriviaNight/Resources/questions.json` holds the question bank (3,300 questions, 220 per category, built from `questions/original-300.csv` and `questions/raw/<batch>/<Category>.txt` by `scripts/build_pool_csv.py`). Every question has a permanent ID, `q` plus 12 hex characters taken from a hash of the question's text. The app saves the ID of every question it shows on screen (on the device, across games and relaunches) and won't pick that question again until the unplayed ones in the chosen categories run out. Setup shows how many unplayed questions are left and has a button to reset the history.

To load a new pool, put it in a CSV with columns `category, difficulty, question, answer` (difficulty 1-10) and run:

```
python3 scripts/import_questions.py pool.csv            # replace the bank
python3 scripts/import_questions.py more.csv --merge    # add to the bank
```

The script assigns the IDs, drops duplicates (ignoring case, punctuation and spacing), and lists rows it skipped and duplicates whose answers disagree. Because an ID depends only on the question text, re-importing never changes the ID of a question that's already been played.

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

## Android

`android/` holds a native Kotlin / Jetpack Compose version of the app with the same setup flow, game flow, question picker, and no-repeat history. It reads the same `TriviaNight/Resources/questions.json` (copied into the app's assets at build time), so the question bank and IDs stay shared. Package name `com.acesinthesack.trivianight`, min Android 8.0 (API 26), targets Android 16 (API 36).

```
cd android
./gradlew testDebugUnitTest --tests '*GameTest*'   # same rules as TriviaNightTests
./gradlew recordRoborazziDebug                     # Play-size screenshots + feature graphic in app/build/screenshots
./gradlew bundleRelease                            # app/build/outputs/bundle/release/app-release.aab
```

`scripts/android_assets.py` regenerates the Android icons, category drawables, and logo from the iOS asset catalog. Store listing text, Play Console answers, and upload-ready images are in `android/play/`.

Signing: the release bundle is signed with the upload key when `android/keystore.properties` exists (never committed) or when the GitHub secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, and `ANDROID_KEY_PASSWORD` are set. `.github/workflows/android.yml` runs the tests, renders screenshots, and builds the bundle on every change under `android/`.
