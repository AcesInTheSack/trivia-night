package com.acesinthesack.trivianight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/** Lays out content with the screen scale factor, inside the safe area. */
@Composable
private fun Scaled(content: @Composable (s: Float, compact: Boolean) -> Unit) {
    ScaledWithSize { s, compact, _ -> content(s, compact) }
}

/** Same as [Scaled], and also says whether the screen is short and wide (a phone held sideways). */
@Composable
private fun ScaledWithSize(content: @Composable (s: Float, compact: Boolean, shortWide: Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        content(
            screenScale(maxWidth.value, maxHeight.value),
            maxWidth.value < 600f,
            maxHeight.value < 520f && maxWidth.value > maxHeight.value,
        )
    }
}

// Splash

@Composable
fun SplashScreen(model: AppModel) {
    Box(Modifier.fillMaxSize()) {
        Confetti()
        ScaledWithSize { s, _, shortWide ->
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = if (shortWide) 12.dp else 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text("WELCOME, EVERYBODY", style = ts(18 * s, FontWeight.Bold, Palette.amber).copy(letterSpacing = (5 * s).sp),
                        modifier = Modifier.padding(bottom = (18 * s).dp))
                    Text(
                        buildAnnotatedString {
                            append("It’s ")
                            withStyle(SpanStyle(color = Palette.amber)) { append("Trivia") }
                            append(if (shortWide) " Night." else "\nNight.")
                        },
                        style = ts(maxOf(48f, 130 * s), FontWeight.Black).copy(lineHeight = (maxOf(48f, 130 * s) * 1.0f).sp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Grab your team, find a pen, and get ready. Answers show up only when the host reveals them.",
                        style = ts(maxOf(15f, 26 * s), color = Palette.muted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = (900 * s).dp).padding(top = (26 * s).dp),
                    )
                    FlowPills(model, s, if (shortWide) 16f else 36 * s)
                    PrimaryButton("Start Match", onClick = { model.startMatch() }, size = maxOf(20f, 28 * s))
                    Spacer(Modifier.weight(1f))
                    CompanyCredit(maxOf(13f, 18 * s))
                }
                GhostButton("← Setup", onClick = { model.endGame() }, size = 15f, modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun FlowPills(model: AppModel, s: Float, vPad: Float) {
    val names = model.selectedCategories.map { it.name }
    val cats = if (names.size <= 2) names.joinToString(", ") else "${names.take(2).joinToString(", ")} + ${names.size - 2} more"
    val size = maxOf(12f, 17 * s)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = vPad.dp),
    ) {
        Pill("${model.game.size} questions", size)
        Pill(cats, size)
        Pill("${model.settings.timerSeconds}-second timer", size)
    }
}

// Question slide

@Composable
fun GameScreen(model: AppModel) {
    var confirmEnd by remember { mutableStateOf(false) }
    Scaled { s, compact ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = maxOf(16f, 36 * s).dp, vertical = maxOf(12f, 28 * s).dp)
        ) {
            TopBar(model, s, compact) { confirmEnd = true }
            ProgressBar((model.index + 1f) / maxOf(model.game.size, 1), Modifier.padding(top = (14 * s).dp))
            Stage(model, s, compact, Modifier.weight(1f))
            NavBar(model, s, compact)
        }
    }
    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this game?") },
            text = { Text("You’ll go back to setup. The current set of questions will be lost.") },
            confirmButton = {
                TextButton(onClick = { confirmEnd = false; model.endGame() }) { Text("End game", color = Palette.red) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep playing", color = Palette.text) } },
            containerColor = Palette.surface,
            titleContentColor = Palette.text,
            textContentColor = Palette.muted,
        )
    }
}

@Composable
private fun TopBar(model: AppModel, s: Float, compact: Boolean, onEnd: () -> Unit) {
    val q = model.current ?: return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                buildAnnotatedString {
                    append("Question ${model.index + 1} ")
                    withStyle(SpanStyle(color = Palette.muted)) { append("of ${model.game.size}") }
                },
                style = ts(maxOf(18f, 28 * s), FontWeight.Bold),
                maxLines = 1,
            )
            if (!compact) Chips(q, s)
            Spacer(Modifier.weight(1f))
            GhostButton("End game", onClick = onEnd, size = maxOf(13f, 15 * s))
            TimerRing(model.slideStartedAt, model.settings.timerSeconds, Modifier.size(maxOf(64f, 116 * s).dp))
        }
        if (compact) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chips(q, s) }
        }
    }
}

@Composable
private fun Chips(q: Question, s: Float) {
    val k = maxOf(s, 0.7f)
    val fs = maxOf(12f, 17 * s)
    Row(
        Modifier
            .clip(CircleShape)
            .background(Palette.amber.copy(alpha = 0.12f))
            .border(1.dp, Palette.amber.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = (14 * k).dp, vertical = (6 * k).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6 * k).dp),
    ) {
        CategoryIcon(q.category, maxOf(13f, 18 * s).dp)
        Text(q.category, style = ts(fs, FontWeight.SemiBold, Palette.amber), maxLines = 1)
    }
    Text(
        "Difficulty ${q.difficulty}",
        style = ts(fs, FontWeight.SemiBold, Palette.muted),
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, Palette.line, CircleShape)
            .padding(horizontal = (14 * k).dp, vertical = (6 * k).dp),
    )
}

/** Shrinks a font size for long text so questions fit on small screens, like minimumScaleFactor on iOS. */
private fun fitSize(base: Float, length: Int, comfortable: Int): Float =
    if (length <= comfortable) base else maxOf(base * 0.55f, base * comfortable / length)

@Composable
private fun Stage(model: AppModel, s: Float, compact: Boolean, modifier: Modifier) {
    val q = model.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 4.dp else (60 * s).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(maxOf(24f, 56 * s).dp, Alignment.CenterVertically),
    ) {
        val qSize = fitSize(if (compact) 30f else maxOf(30f, 76 * s), q?.text?.length ?: 0, if (compact) 110 else 90)
        AnimatedContent(model.index, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "q") {
            Text(
                model.game.getOrNull(it)?.text ?: "",
                style = ts(qSize, FontWeight.Bold).copy(lineHeight = (qSize * 1.15f).sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = (1500 * s).dp),
            )
        }
        Box(Modifier.heightIn(min = maxOf(80f, 150 * s).dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                model.revealed to model.index,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.9f)) togetherWith
                        fadeOut(tween(100))
                },
                label = "answer",
            ) { (shown, _) ->
                if (shown) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ANSWER", style = ts(maxOf(12f, 18 * s), FontWeight.Bold, Palette.teal).copy(letterSpacing = 4.sp))
                        val a = q?.answer ?: ""
                        val aSize = fitSize(if (compact) 32f else maxOf(32f, 72 * s), a.length, if (compact) 40 else 45)
                        Text(a, style = ts(aSize, FontWeight.Black, Palette.teal), textAlign = TextAlign.Center)
                    }
                } else {
                    val r = 18.dp
                    Text(
                        "ANSWER HIDDEN",
                        style = ts(maxOf(14f, 22 * s), FontWeight.SemiBold, Palette.dim).copy(letterSpacing = 3.sp),
                        modifier = Modifier
                            .drawBehind {
                                drawRoundRect(
                                    Palette.line,
                                    cornerRadius = CornerRadius(r.toPx()),
                                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx()))),
                                )
                            }
                            .padding(horizontal = maxOf(24f, 44 * s).dp, vertical = maxOf(14f, 24 * s).dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit, size: Float, enabled: Boolean, description: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.3f)
            .clip(shape)
            .background(Palette.surface)
            .border(1.5.dp, Palette.line, shape)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = (size * 1.3f).dp, vertical = (size * 0.8f).dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = ts(size, FontWeight.Bold), maxLines = 1) }
}

@Composable
private fun NavBar(model: AppModel, s: Float, compact: Boolean) {
    val fs = maxOf(16f, 22 * s)
    val showSize = maxOf(18f, 28 * s)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(maxOf(10f, 20 * s).dp, Alignment.CenterHorizontally),
    ) {
        NavButton(if (compact) "‹" else "‹  Previous", { model.previous() }, fs, !model.isFirst, "Previous question")
        val shown = model.revealed
        val shape = RoundedCornerShape(16.dp)
        Box(
            Modifier
                .then(if (compact) Modifier.weight(1f) else Modifier.widthIn(min = (showSize * 9).dp))
                .clip(shape)
                .background(if (shown) Color.Transparent else Palette.teal)
                .border(1.5.dp, if (shown) Palette.teal.copy(alpha = 0.35f) else Palette.teal, shape)
                .clickable(enabled = !shown, role = Role.Button) { model.reveal() }
                .padding(horizontal = (showSize * (if (compact) 0.6f else 1.6f)).dp, vertical = (showSize * 0.75f).dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (shown) "Answer shown" else "Show Answer",
                style = ts(showSize, FontWeight.Black, if (shown) Palette.teal else Palette.tealInk),
                maxLines = 1,
            )
        }
        NavButton(
            if (compact) "›" else if (model.isLast) "Finish  ›" else "Next  ›",
            { model.next() }, fs, true, if (model.isLast) "Finish" else "Next question",
        )
    }
}

@Composable
fun ProgressBar(value: Float, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(value, tween(300), label = "progress")
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Palette.line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(v.coerceIn(0f, 1f)).clip(CircleShape).background(Palette.amber))
    }
}

@Composable
fun TimerRing(start: Long, seconds: Int, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start, seconds) {
        while (true) {
            withFrameMillis { now = System.currentTimeMillis() }
            if (now - start > seconds * 1000L + 100) break
        }
    }
    val total = seconds.toFloat()
    val remaining = maxOf(0f, total - (now - start) / 1000f)
    val fraction = if (total > 0) remaining / total else 0f
    val done = remaining <= 0f
    val warn = !done && remaining <= minOf(10f, total * 0.25f)
    val color = if (done) Palette.red else if (warn) Palette.amber else Palette.teal
    BoxWithConstraints(modifier.semantics { contentDescription = "Time remaining" }, contentAlignment = Alignment.Center) {
        val w = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val lw = size.width * 0.08f
            val inset = lw / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - lw, size.height - lw)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Palette.line, 0f, 360f, false, topLeft, arcSize, style = Stroke(lw))
            drawArc(color, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(lw, cap = StrokeCap.Round))
        }
        Text(
            "${ceil(remaining).toInt()}",
            style = ts(w.value * 0.38f, FontWeight.Black, if (done) Palette.red else Palette.text),
        )
    }
}

// End

@Composable
fun EndScreen(model: AppModel) {
    Box(Modifier.fillMaxSize()) {
        Confetti()
        Scaled { s, compact ->
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("THAT’S THE LAST QUESTION", style = ts(maxOf(12f, 18 * s), FontWeight.Bold, Palette.amber).copy(letterSpacing = (5 * s).sp),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = (18 * s).dp))
                Text(
                    buildAnnotatedString {
                        append("That’s a ")
                        withStyle(SpanStyle(color = Palette.amber)) { append("wrap.") }
                    },
                    style = ts(maxOf(44f, 120 * s), FontWeight.Black),
                    textAlign = TextAlign.Center,
                )
                Text("Pens down. Time to tally the scores. Thanks for playing!",
                    style = ts(maxOf(15f, 26 * s), color = Palette.muted), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = (24 * s).dp))
                val bs = maxOf(15f, 18 * s)
                val buttons: @Composable () -> Unit = {
                    GhostButton("◀  Back to last question", onClick = { model.reviewLast() }, size = bs)
                    GhostButton("New game setup", onClick = { model.endGame() }, size = bs)
                    PrimaryButton("Play again with new questions", onClick = { model.startGame() }, size = maxOf(17f, 22 * s))
                }
                Spacer(Modifier.height((44 * s).dp))
                if (compact) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { buttons() }
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) { buttons() }
                }
            }
        }
    }
}
