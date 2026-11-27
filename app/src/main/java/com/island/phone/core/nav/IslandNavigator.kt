package com.island.phone.core.nav

import androidx.compose.runtime.mutableStateListOf

class IslandNavigator(start: Route) {
    val stack = mutableStateListOf(start)

    val canPop: Boolean get() = stack.size > 1

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (canPop) stack.removeAt(stack.lastIndex)
    }
}
