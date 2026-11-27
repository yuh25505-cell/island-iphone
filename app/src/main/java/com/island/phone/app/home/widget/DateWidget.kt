package com.island.phone.app.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
private fun rememberToday(): State<LocalDate> {
    val today = remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            val now = LocalDate.now()
            if (now != today.value) today.value = now
        }
    }
    return today
}

@Composable
fun DateWidget(
    lines: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IslandTheme.colors
    val today by rememberToday()
    val day = today.dayOfMonth.toString()
    val month = today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))

    Row(
        modifier
            .fillMaxSize()
            .widgetTap(onClick)
            .background(colors.gray6, RoundedCornerShape(34.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(124.dp),
            ) {
                BasicText(
                    text = day,
                    modifier = Modifier.align(Alignment.TopCenter),
                    style = TextStyle(
                        color = colors.label.copy(alpha = 0.38f),
                        fontSize = 92.sp,
                        lineHeight = 100.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
                BasicText(
                    text = month,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 62.dp),
                    style = TextStyle(
                        color = colors.label,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = weekday,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 96.dp),
                    style = TextStyle(
                        color = colors.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
        }
        Box(
            Modifier
                .width(1.5.dp)
                .height(64.dp)
                .background(colors.label),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            lines.forEach { line ->
                WidgetText(
                    text = line,
                    size = 12f,
                    lineHeight = 19f,
                    color = colors.label,
                )
            }
        }
    }
}
