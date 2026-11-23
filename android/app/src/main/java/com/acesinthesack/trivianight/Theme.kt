package com.acesinthesack.trivianight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

object Palette {
    val bg = Color(0xFF0E1124)
    val bg2 = Color(0xFF151A36)
    val surface = Color(0xFF1A1F40)
    val line = Color(0xFF2B315C)
    val text = Color(0xFFF5F1E8)
    val muted = Color(0xFFA6A9C8)
    val dim = Color(0xFF6F739A)
    val amber = Color(0xFFFFB547)
    val amberInk = Color(0xFF2A1A00)
    val teal = Color(0xFF5FD4C4)
    val tealInk = Color(0xFF062824)
    val red = Color(0xFFFF6B6B)
    val disabledFill = Color(0xFF3A3F66)
    val disabledInk = Color(0xFF7D81A8)
}

/** Text style helper. Sizes are in points on iOS; we use the same numbers as sp. */
fun ts(size: Float, weight: FontWeight = FontWeight.Normal, color: Color = Palette.text): TextStyle =
    TextStyle(fontSize = size.sp, fontWeight = weight, color = color, lineHeight = (size * 1.18f).sp)

/** Scales type with the screen so it reads well on a phone and on a TV via casting. Same formula as iOS. */
fun screenScale(widthDp: Float, heightDp: Float): Float {
    val base = minOf(widthDp / 1200f, heightDp / 800f)
    return base.coerceIn(0.42f, 1.4f)
}

@Composable
fun Background(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Palette.bg)
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF24295A), Color.Transparent),
                        center = Offset(size.width * 0.85f, -size.height * 0.1f), radius = 900.dp.toPx(),
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF2A1F45), Color.Transparent),
                        center = Offset(-size.width * 0.1f, size.height * 1.1f), radius = 700.dp.toPx(),
                    )
                )
            }
    ) { content() }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    size: Float = 20f,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.amber, contentColor = Palette.amberInk,
            disabledContainerColor = Palette.disabledFill, disabledContentColor = Palette.disabledInk,
        ),
        contentPadding = PaddingValues(horizontal = (size * 1.5f).dp, vertical = (size * 0.8f).dp),
        modifier = modifier.scale(if (pressed) 0.97f else 1f),
    ) {
        Text(text, style = ts(size, FontWeight.Black, if (enabled) Palette.amberInk else Palette.disabledInk), maxLines = 1)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, size: Float = 16f, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Palette.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.muted),
        contentPadding = PaddingValues(horizontal = (size * 1.2f).dp, vertical = (size * 0.7f).dp),
        modifier = modifier,
    ) {
        Text(text, style = ts(size, FontWeight.SemiBold, Palette.muted), maxLines = 1)
    }
}

@Composable
fun Pill(text: String, size: Float = 16f) {
    Text(
        text,
        style = ts(size, FontWeight.Medium),
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Palette.line, CircleShape)
            .padding(horizontal = size.dp, vertical = (size * 0.55f).dp),
    )
}

/** Scattered confetti, drawn once per appearance. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val seed = remember { Random.nextLong() }
    val colors = listOf(Palette.amber, Palette.teal, Palette.red, Color(0xFFA78BFA), Palette.text)
    Canvas(modifier.fillMaxSize()) {
        val r = Random(seed)
        for (i in 0 until 70) {
            val x = r.nextFloat() * size.width
            val y = r.nextFloat() * size.height
            val s = (6 + r.nextFloat() * 12).dp.toPx()
            val color = colors[i % colors.size].copy(alpha = (0.3f + r.nextFloat() * 0.5f) * 0.5f)
            if (i % 3 == 0) {
                drawCircle(color, radius = s / 3f, center = Offset(x, y))
            } else {
                rotate(r.nextFloat() * 360f, pivot = Offset(x, y)) {
                    drawRoundRect(color, topLeft = Offset(x, y), size = Size(s, s * 0.45f), cornerRadius = CornerRadius(2.dp.toPx()))
                }
            }
        }
    }
}

private val categoryIcons = mapOf(
    "History" to R.drawable.cat_history,
    "Science" to R.drawable.cat_science,
    "Television" to R.drawable.cat_television,
    "Pop Culture" to R.drawable.cat_pop_culture,
    "Movies & Entertainment" to R.drawable.cat_movies_entertainment,
    "Music" to R.drawable.cat_music,
    "Geography" to R.drawable.cat_geography,
    "Sports" to R.drawable.cat_sports,
    "Literature" to R.drawable.cat_literature,
    "Art & Architecture" to R.drawable.cat_art_architecture,
    "Food & Drink" to R.drawable.cat_food_drink,
    "Animals & Nature" to R.drawable.cat_animals_nature,
    "Technology" to R.drawable.cat_technology,
    "Mythology" to R.drawable.cat_mythology,
    "Words & Language" to R.drawable.cat_words_language,
)

/** Custom line icon for a category, tinted. */
@Composable
fun CategoryIcon(name: String, size: Dp = 20.dp, tint: Color = Palette.amber) {
    val res = categoryIcons[name] ?: return
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

/** "Trivia Night" plus the BIG SCREEN tag, as on the app icon. */
@Composable
fun Wordmark(size: Float = 44f) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((size * 0.3f).dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Trivia Night: Big Screen" },
    ) {
        Text("Trivia Night", style = ts(size, FontWeight.Bold).copy(letterSpacing = (-size * 0.02f).sp))
        Text(
            "BIG SCREEN",
            style = ts(size * 0.3f, FontWeight.Bold, Palette.bg).copy(letterSpacing = (size * 0.03f).sp),
            modifier = Modifier
                .clip(RoundedCornerShape((size * 0.16f).dp))
                .background(Palette.amber)
                .padding(horizontal = (size * 0.24f).dp, vertical = (size * 0.12f).dp),
        )
    }
}

/** "by The Oregon Critter & Travel Co." with the company logo. */
@Composable
fun CompanyCredit(size: Float = 16f) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((size * 0.75f).dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Image(
            painterResource(R.drawable.critter_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size((size * 3.2f).dp)
                .clip(RoundedCornerShape((size * 0.7f).dp)),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Palette.muted)) { append("by ") }
                withStyle(SpanStyle(color = Palette.text, fontWeight = FontWeight.SemiBold)) {
                    append("The Oregon Critter & Travel Co.")
                }
            },
            style = ts(size),
        )
    }
}

