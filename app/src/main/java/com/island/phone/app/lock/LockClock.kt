package com.island.phone.app.lock

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
private fun rememberMinuteNow(): State<LocalDateTime> {
    val now = remember { mutableStateOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            if (current != now.value) now.value = current
            delay(1000)
        }
    }
    return now
}

@Composable
fun LockClock(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    val context = LocalContext.current
    val now by rememberMinuteNow()
    val is24 = DateFormat.is24HourFormat(context)
    val timeText = remember(now, is24) {
        now.format(DateTimeFormatter.ofPattern(if (is24) "H:mm" else "h:mm"))
    }
    val dateText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            text = dateText,
            style = TextStyle(
                color = colors.secondaryLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = timeText,
            style = TextStyle(
                color = colors.label,
                fontSize = 116.sp,
                lineHeight = 130.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
