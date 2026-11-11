package com.ayko.trackitt.ui.navigation

sealed class NavRoute(val route: String) {
    data object Onboarding : NavRoute("onboarding")
    data object Home : NavRoute("home")
    data object Add : NavRoute("add")
    data object Settings : NavRoute("settings")
    data object Search : NavRoute("search")
    data object Budget : NavRoute("budget")
    data object Insights : NavRoute("insights/{year}/{month}") {
        fun withArgs(year: Int, month: Int) = "insights/$year/$month"
    }
    data object Recap : NavRoute("recap/{year}") {
        fun withYear(year: Int) = "recap/$year"
    }
    data object SmsSources : NavRoute("settings/sms-sources")
    data object MerchantDictionary : NavRoute("settings/merchant-dictionary")
    data object Detail : NavRoute("detail/{transactionId}") {
        fun withId(id: Long) = "detail/$id"
    }
}
