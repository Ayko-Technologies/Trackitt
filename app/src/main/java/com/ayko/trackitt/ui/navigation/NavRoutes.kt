package com.ayko.trackitt.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object Add : NavRoute("add")
    data object Settings : NavRoute("settings")
    data object Search : NavRoute("search")
    data object Detail : NavRoute("detail/{transactionId}") {
        fun withId(id: Long) = "detail/$id"
    }
}
