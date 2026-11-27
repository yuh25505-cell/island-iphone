package com.island.phone.data.notice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LockNotice(
    val id: Long,
    val appName: String,
    val title: String,
    val body: String,
    val timeMillis: Long,
)

object LockNoticeStore {
    private val state = MutableStateFlow<List<LockNotice>>(emptyList())
    val notices: StateFlow<List<LockNotice>> = state.asStateFlow()

    fun post(notice: LockNotice) {
        state.update { old -> listOf(notice) + old.filterNot { it.id == notice.id } }
    }

    fun remove(id: Long) {
        state.update { old -> old.filterNot { it.id == id } }
    }

    fun clear() {
        state.value = emptyList()
    }
}
