package com.island.phone.core.nav

sealed interface Route {
    data object Home : Route
}
