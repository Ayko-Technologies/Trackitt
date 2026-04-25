package com.ayko.trackitt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayko.trackitt.ui.screen.add.AddScreen
import com.ayko.trackitt.ui.screen.detail.DetailScreen
import com.ayko.trackitt.ui.screen.home.HomeScreen
import com.ayko.trackitt.ui.screen.search.SearchScreen
import com.ayko.trackitt.ui.screen.settings.SettingsScreen

@Composable
fun TrackittNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route,
    ) {
        composable(NavRoute.Home.route) {
            HomeScreen(
                onAddTap = { navController.navigate(NavRoute.Add.route) },
                onSettingsTap = { navController.navigate(NavRoute.Settings.route) },
                onSearchTap = { navController.navigate(NavRoute.Search.route) },
                onTransactionTap = { id ->
                    navController.navigate(NavRoute.Detail.withId(id))
                },
            )
        }

        composable(NavRoute.Add.route) {
            AddScreen(
                onClose = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavRoute.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onTransactionTap = { id ->
                    navController.navigate(NavRoute.Detail.withId(id))
                },
            )
        }

        composable(
            NavRoute.Detail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
            DetailScreen(
                transactionId = txId,
                onBack = { navController.popBackStack() },
                onConfirm = { navController.popBackStack() },
            )
        }
    }
}
