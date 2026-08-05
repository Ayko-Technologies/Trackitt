package com.ayko.trackitt.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayko.trackitt.ui.screen.add.AddScreen
import com.ayko.trackitt.ui.screen.budget.BudgetScreen
import com.ayko.trackitt.ui.screen.detail.DetailScreen
import com.ayko.trackitt.ui.screen.home.HomeScreen
import com.ayko.trackitt.ui.screen.insights.InsightsScreen
import com.ayko.trackitt.ui.screen.onboarding.OnboardingScreen
import com.ayko.trackitt.ui.screen.search.SearchScreen
import com.ayko.trackitt.ui.screen.settings.SettingsScreen
import com.ayko.trackitt.ui.screen.settings.SmsSourcesScreen

@Composable
fun TrackittNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val onboardingDone = remember {
        context.getSharedPreferences("trackitt", 0).getBoolean("onboarding_completed", false)
    }
    val startDestination = if (onboardingDone) NavRoute.Home.route else NavRoute.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(NavRoute.Home.route) {
                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(
            NavRoute.Home.route,
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
        ) {
            HomeScreen(
                onAddTap = { navController.navigate(NavRoute.Add.route) },
                onSettingsTap = { navController.navigate(NavRoute.Settings.route) },
                onSearchTap = { navController.navigate(NavRoute.Search.route) },
                onBudgetTap = { navController.navigate(NavRoute.Budget.route) },
                onInsightsTap = { year, month ->
                    navController.navigate(NavRoute.Insights.withArgs(year, month))
                },
                onTransactionTap = { id ->
                    navController.navigate(NavRoute.Detail.withId(id))
                },
            )
        }

        composable(
            NavRoute.Add.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) {
            AddScreen(
                onClose = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
            )
        }

        composable(
            NavRoute.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSmsSources = { navController.navigate(NavRoute.SmsSources.route) },
            )
        }

        composable(
            NavRoute.Search.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onTransactionTap = { id ->
                    navController.navigate(NavRoute.Detail.withId(id))
                },
            )
        }

        composable(
            NavRoute.Budget.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) {
            BudgetScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoute.Insights.route,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType },
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            InsightsScreen(
                year = year,
                month = month,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoute.SmsSources.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
        ) {
            SmsSourcesScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoute.Detail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
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
