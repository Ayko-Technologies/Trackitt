package com.ayko.trackitt.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.ayko.trackitt.ui.screen.recap.RecapScreen
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
            // Home opens transactions from a category list, which closes as the transaction opens and
            // reopens over Home straight away on the way back. Sliding Home either way would move the
            // screen behind the list, so the two swap without animating.
            exitTransition = {
                if (targetState.destination.route == NavRoute.Detail.route) ExitTransition.None
                else slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
            },
            popEnterTransition = {
                if (initialState.destination.route == NavRoute.Detail.route) EnterTransition.None
                else slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200))
            },
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
                onRecapTap = { year -> navController.navigate(NavRoute.Recap.withYear(year)) },
            )
        }

        composable(
            NavRoute.Recap.route,
            arguments = listOf(navArgument("year") { type = NavType.IntType }),
            // A story opens over everything, rising from the bottom like a sheet.
            enterTransition = { slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)) },
            popExitTransition = { slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(200)) },
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            RecapScreen(year = year, onClose = { navController.popBackStack() })
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
                onOpenRecap = { year -> navController.navigate(NavRoute.Recap.withYear(year)) },
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
            enterTransition = {
                if (initialState.destination.route == NavRoute.Home.route) EnterTransition.None
                else slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200))
            },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) },
            popExitTransition = {
                if (targetState.destination.route == NavRoute.Home.route) ExitTransition.None
                else slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
            },
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
