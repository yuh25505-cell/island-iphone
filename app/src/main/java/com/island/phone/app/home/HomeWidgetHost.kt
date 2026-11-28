package com.island.phone.app.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.island.phone.app.home.widget.DateWidget
import com.island.phone.app.home.widget.PhotoWidget
import com.island.phone.app.home.widget.QuoteWidget
import com.island.phone.app.home.widget.WeekWidget
import com.island.phone.app.home.widget.WordWidget
import com.island.phone.data.settings.HomeWidgetTextStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal enum class EditTarget { DATE, QUOTE, WEEK, PHOTO }

@Composable
internal fun HomeWidgetBody(
    item: HomeWidgetItem,
    store: HomeWidgetTextStore,
    editing: Boolean,
    onEdit: (EditTarget) -> Unit,
) {
    val copy = item.copy
    when (item.kind) {
        HomeWidgetKind.DATE -> {
            val lines by remember(copy) { store.dateLinesOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.dateLinesOf(copy).first() } },
            )
            DateWidget(lines = lines, onClick = { if (!editing) onEdit(EditTarget.DATE) })
        }
        HomeWidgetKind.QUOTE -> {
            val lines by remember(copy) { store.quoteLinesOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.quoteLinesOf(copy).first() } },
            )
            QuoteWidget(lines = lines, onClick = { if (!editing) onEdit(EditTarget.QUOTE) })
        }
        HomeWidgetKind.WORD -> WordWidget()
        HomeWidgetKind.WEEK -> {
            val texts by remember(copy) { store.weekTextsOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.weekTextsOf(copy).first() } },
            )
            val avatar by remember(copy) { store.weekAvatarOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.weekAvatarOf(copy).first() } },
            )
            WeekWidget(texts = texts, avatar = avatar, onClick = { if (!editing) onEdit(EditTarget.WEEK) })
        }
        HomeWidgetKind.PHOTOS -> {
            val sources by remember(copy) { store.photoSourcesOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.photoSourcesOf(copy).first() } },
            )
            val captions by remember(copy) { store.photoCaptionsOf(copy) }.collectAsStateWithLifecycle(
                initialValue = remember(copy) { runBlocking { store.photoCaptionsOf(copy).first() } },
            )
            PhotoWidget(sources = sources, captions = captions, onClick = { if (!editing) onEdit(EditTarget.PHOTO) })
        }
    }
}
