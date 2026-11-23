package com.acesinthesack.trivianight

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import kotlin.math.roundToInt

private fun fmt(n: Int): String = NumberFormat.getIntegerInstance().format(n)

@Composable
fun SetupScreen(model: AppModel) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Wordmark(44f)
                    Text("Set up tonight’s game, then cast this screen to the TV.", style = ts(17f, color = Palette.muted))
                }
                StepCard(1, "Pick your categories",
                    "Choose as many as you like. Questions rotate through the categories you pick.",
                    trailing = { CategoryTools(model) }) { Categories(model) }
                StepCard(2, "How many questions?",
                    "Questions you’ve already played are saved for last, so nothing repeats until the unplayed ones run out.") {
                    CountControl(model)
                }
                StepCard(3, "How challenging?",
                    "Slide to set the difficulty for the whole game, or open Advanced to set it per category.") {
                    DifficultyControl(model)
                }
                StepCard(4, "Timer for each question",
                    "The countdown starts when each question appears. Nothing happens at zero; it’s just a guide.") {
                    TimerControl(model)
                }
            }
        }
        LaunchBar(model)
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    hint: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.035f), Color.White.copy(alpha = 0.01f))))
            .border(1.dp, Palette.line, shape)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.amber), contentAlignment = Alignment.Center) {
                Text("$number", style = ts(17f, FontWeight.Black, Palette.amberInk))
            }
            Text(title, style = ts(24f, FontWeight.Bold), modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Text(hint, style = ts(15f, color = Palette.muted), modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        content()
    }
}

@Composable
private fun SmallLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, style = ts(14f, FontWeight.Medium, Palette.muted)) }
}

@Composable
private fun CategoryTools(model: AppModel) {
    Row {
        SmallLink("Select all") { model.selectAll() }
        SmallLink("Clear") { model.clearAll() }
    }
}

@Composable
private fun Categories(model: AppModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = maxOf(1, ((maxWidth.value + 10f) / 210f).toInt())
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                model.bank.categories.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { cat -> CategoryTile(model, cat.name, Modifier.weight(1f)) }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        val n = model.settings.selected.size
        Text(
            if (n == 0) "Pick at least one category to start." else "$n of ${model.bank.categories.size} selected",
            style = ts(14f, color = Palette.muted),
        )
    }
}

@Composable
private fun CategoryTile(model: AppModel, name: String, modifier: Modifier) {
    val on = name in model.settings.selected
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(if (on) Palette.amber.copy(alpha = 0.10f) else Palette.bg2)
            .border(1.5.dp, if (on) Palette.amber else Palette.line, shape)
            .semantics { selected = on }
            .clickable(role = Role.Checkbox) { model.toggle(name) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryIcon(name, 22.dp)
        Text(name, style = ts(15.5f, FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (on) Palette.amber else Color.Transparent)
                .border(1.5.dp, if (on) Palette.amber else Color(0xFF4A5089), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Text("✓", style = ts(12f, FontWeight.Black, Palette.amberInk))
        }
    }
}

@Composable
private fun amberSliderColors() = SliderDefaults.colors(
    thumbColor = Palette.amber,
    activeTrackColor = Palette.amber,
    inactiveTrackColor = Palette.line,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
    disabledThumbColor = Palette.dim,
    disabledActiveTrackColor = Palette.dim,
    disabledInactiveTrackColor = Palette.line,
)

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = ts(22f, FontWeight.Bold)) }
}

@Composable
private fun CountControl(model: AppModel) {
    val s = model.settings
    val n = s.selected.size
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.bg2)
                    .border(1.dp, Palette.line, RoundedCornerShape(14.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StepperButton("−") { model.setQuestionCount(s.questionCount - 1) }
                Text("${s.questionCount}", style = ts(32f, FontWeight.Black), textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                StepperButton("+") { model.setQuestionCount(s.questionCount + 1) }
            }
            Text(
                if (n == 0) "Pick categories first" else "Up to ${fmt(model.maxQuestions)} with $n ${if (n == 1) "category" else "categories"}",
                style = ts(14f, color = Palette.muted),
            )
        }
        if (model.maxQuestions > 1) {
            Slider(
                value = s.questionCount.toFloat(),
                onValueChange = { model.setQuestionCount(it.roundToInt()) },
                valueRange = 1f..model.maxQuestions.toFloat(),
                colors = amberSliderColors(),
            )
        }
        HistoryRow(model)
    }
}

@Composable
private fun HistoryRow(model: AppModel) {
    var confirmReset by remember { mutableStateOf(false) }
    val s = model.settings
    val unplayed = model.unplayedInSelection
    val low = s.selected.isNotEmpty() && unplayed < s.questionCount
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (low) "!" else "✓", style = ts(15f, FontWeight.Black, if (low) Palette.amber else Palette.teal))
        Text(
            if (s.selected.isEmpty()) "${fmt(model.totalQuestions)} questions in the bank"
            else "${fmt(unplayed)} not played yet in these categories" + if (low) " · some repeats this game" else "",
            style = ts(14f, color = Palette.muted),
            modifier = Modifier.weight(1f),
        )
        if (model.playedCount > 0) SmallLink("Reset played history") { confirmReset = true }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset played history?") },
            text = { Text("The app has shown ${fmt(model.playedCount)} questions so far. Resetting lets all of them come up again.") },
            confirmButton = {
                TextButton(onClick = { model.resetHistory(); confirmReset = false }) { Text("Reset", color = Palette.red) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = Palette.text) } },
            containerColor = Palette.surface,
            titleContentColor = Palette.text,
            textContentColor = Palette.muted,
        )
    }
}

@Composable
private fun DifficultyControl(model: AppModel) {
    val s = model.settings
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (s.advanced) "—" else "${s.difficulty}", style = ts(40f, FontWeight.Black))
            Text(
                if (s.advanced) "Set per category below" else GameSettings.describe(s.difficulty),
                style = ts(16f, color = Palette.muted),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Slider(
            value = s.difficulty.toFloat(),
            onValueChange = { model.setDifficulty(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            enabled = !s.advanced,
            colors = amberSliderColors(),
            modifier = Modifier.alpha(if (s.advanced) 0.35f else 1f),
        )
        Row(verticalAlignment = Alignment.Top) {
            EndLabel("1 · Easy", "Most people in the room will know it.", TextAlign.Start, Modifier.weight(1f))
            Spacer(Modifier.width(20.dp))
            EndLabel("10 · Expert", "Only serious trivia buffs will get it.", TextAlign.End, Modifier.weight(1f))
        }
        val advColor = if (s.advanced) Palette.amber else Palette.muted
        Text(
            "☰  Advanced: set difficulty per category",
            style = ts(14.5f, FontWeight.SemiBold, advColor),
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, if (s.advanced) Palette.amber else Palette.line, RoundedCornerShape(10.dp))
                .clickable(role = Role.Switch) { model.setAdvanced(!s.advanced) }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
        AnimatedVisibility(s.advanced) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = Palette.line)
                Text("Each category gets its own level. The main slider is turned off while this is open.",
                    style = ts(14f, color = Palette.muted))
                if (model.selectedCategories.isEmpty()) {
                    Text("Select categories above to set their difficulty.", style = ts(14f, color = Palette.dim))
                }
                model.selectedCategories.forEach { cat ->
                    val value = model.categoryDifficulty(cat.name)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryIcon(cat.name, 18.dp)
                            Text(cat.name, style = ts(15f, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            Text("$value", style = ts(20f, FontWeight.Black))
                        }
                        Slider(
                            value = value.toFloat(),
                            onValueChange = { model.setCategoryDifficulty(cat.name, it.roundToInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = amberSliderColors(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EndLabel(title: String, detail: String, align: TextAlign, modifier: Modifier) {
    Column(modifier, horizontalAlignment = if (align == TextAlign.Start) Alignment.Start else Alignment.End) {
        Text(title, style = ts(13.5f, FontWeight.SemiBold), textAlign = align)
        Text(detail, style = ts(13.5f, color = Palette.muted), textAlign = align)
    }
}

@Composable
private fun TimerControl(model: AppModel) {
    val s = model.settings
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${s.timerSeconds}", style = ts(40f, FontWeight.Black))
            Text("seconds", style = ts(16f, FontWeight.Medium, Palette.muted), modifier = Modifier.padding(bottom = 6.dp))
        }
        Slider(
            value = s.timerSeconds.toFloat(),
            onValueChange = { model.setTimer((it / 5f).roundToInt() * 5) },
            valueRange = 10f..120f,
            colors = amberSliderColors(),
        )
        Row {
            Text("10 sec", style = ts(13.5f, FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            Text("120 sec", style = ts(13.5f, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun LaunchBar(model: AppModel) {
    val s = model.settings
    val summary = if (s.selected.isEmpty()) "Choose at least one category" else {
        val n = s.selected.size
        val diff = if (s.advanced) "custom difficulty" else "difficulty ${s.difficulty}"
        "${s.questionCount} questions · $n ${if (n == 1) "category" else "categories"} · $diff · ${s.timerSeconds}s timer"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(Palette.bg.copy(alpha = 0.92f))
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(summary, style = ts(14.5f, color = Palette.muted), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            PrimaryButton("Start Game  →", onClick = { model.startGame() }, enabled = model.canStart)
        }
    }
}
