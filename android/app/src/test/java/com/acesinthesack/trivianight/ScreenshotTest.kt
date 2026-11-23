package com.acesinthesack.trivianight

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Renders every screen at Play Store screenshot sizes.
 * Run: ./gradlew recordRoborazziDebug   (images land in app/build/screenshots/<device>/)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenshotBase(private val device: String) {
    @get:Rule val compose = createComposeRule()

    private val bank = QuestionBank.parse(File("../../TriviaNight/Resources/questions.json").readText())

    private fun shoot(demo: String, file: String) {
        val model = AppModel(bank, PlayHistory(null))
        model.applyDemo(demo)
        if (demo == "setup") {
            // Setup looks best with the default handful of categories picked.
            model.clearAll()
            listOf("History", "Science", "Pop Culture", "Movies & Entertainment", "Music", "Geography").forEach(model::toggle)
            model.setQuestionCount(20)
        }
        compose.mainClock.autoAdvance = false
        compose.setContent { TriviaApp(model) }
        compose.mainClock.advanceTimeBy(1500)
        compose.onRoot().captureRoboImage("build/screenshots/$device/$file.png")
    }

    @Test fun setup() = shoot("setup", "1-setup")
    @Test fun splash() = shoot("splash", "2-splash")
    @Test fun question() = shoot("game", "3-question")
    @Test fun answer() = shoot("answer", "4-answer")
    @Test fun end() = shoot("end", "5-end")
}

/** Phone, portrait, 1215 x 2160. */
@Config(qualifiers = "w405dp-h720dp-port-xxhdpi", sdk = [35])
class PhonePortraitScreenshots : ScreenshotBase("phone-portrait")

/** Phone, landscape (how it's held while casting), 2160 x 1215. */
@Config(qualifiers = "w720dp-h405dp-land-xxhdpi", sdk = [35])
class PhoneLandscapeScreenshots : ScreenshotBase("phone-landscape")

/** 7-inch tablet, landscape, 1920 x 1200. */
@Config(qualifiers = "w960dp-h600dp-land-xhdpi", sdk = [35])
class Tablet7Screenshots : ScreenshotBase("tablet-7")

/** 10-inch tablet, landscape, 2560 x 1600. */
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi", sdk = [35])
class Tablet10Screenshots : ScreenshotBase("tablet-10")


/** The 1024 x 500 feature graphic Google Play shows at the top of the listing. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1024dp-h500dp-land-mdpi", sdk = [35])
class FeatureGraphic {
    @get:Rule val compose = createComposeRule()

    @Test fun featureGraphic() {
        compose.setContent {
            Background {
                androidx.compose.foundation.layout.Row(
                    androidx.compose.ui.Modifier.fillMaxSize().padding(horizontal = 64.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(48.dp),
                ) {
                    androidx.compose.foundation.Image(
                        android.graphics.BitmapFactory.decodeFile("../play/icon-512.png").let {
                            androidx.compose.ui.graphics.painter.BitmapPainter(
                                it.asImageBitmap()
                            )
                        },
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(260.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(56.dp)),
                    )
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                    ) {
                        Wordmark(58f)
                        androidx.compose.material3.Text(
                            "Host trivia night on the big screen. 3,300 questions, 15 categories, no repeats.",
                            style = ts(26f, color = Palette.muted),
                        )
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/feature-graphic.png")
    }
}
