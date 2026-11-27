package com.island.phone.app.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.notice.LockNotice
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LockNoticeList(notices: List<LockNotice>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notices.take(3).forEach { notice ->
            key(notice.id) { LockNoticeCard(notice) }
        }
    }
}

@Composable
private fun LockNoticeCard(notice: LockNotice) {
    val colors = IslandTheme.colors
    val timeText = remember(notice.timeMillis) {
        Instant.ofEpochMilli(notice.timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("H:mm"))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.groupedCard.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(
                text = notice.appName,
                style = TextStyle(color = colors.secondaryLabel, fontSize = 13.sp),
            )
            BasicText(
                text = timeText,
                style = TextStyle(color = colors.secondaryLabel, fontSize = 13.sp),
            )
        }
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = notice.title,
            style = TextStyle(
                color = colors.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (notice.body.isNotEmpty()) {
            BasicText(
                text = notice.body,
                style = TextStyle(color = colors.label, fontSize = 15.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
