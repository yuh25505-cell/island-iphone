package com.island.phone.net

import okio.BufferedSource

internal class SseEvent(val name: String?, val data: String)

internal class SseReader(private val source: BufferedSource) {

    fun next(): SseEvent? {
        var eventName: String? = null
        val data = StringBuilder()
        var hasData = false
        while (true) {
            val line = source.readUtf8Line()
                ?: return if (hasData) SseEvent(eventName, data.toString()) else null
            if (line.isEmpty()) {
                if (hasData) return SseEvent(eventName, data.toString())
                eventName = null
                continue
            }
            if (line.startsWith(":")) continue
            val index = line.indexOf(':')
            val field = if (index < 0) line else line.substring(0, index)
            var value = if (index < 0) "" else line.substring(index + 1)
            if (value.startsWith(" ")) value = value.substring(1)
            when (field) {
                "event" -> eventName = value
                "data" -> {
                    if (hasData) data.append('\n')
                    data.append(value)
                    hasData = true
                }
            }
        }
    }
}
