package com.example.childtrackerapp.schedule.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.childtrackerapp.schedule.ui.add.AddScheduleScreen
import com.example.childtrackerapp.schedule.ui.daily.DailyScreen
import com.example.childtrackerapp.schedule.ui.edit.EditScheduleScreen
import com.example.childtrackerapp.schedule.ui.weekly.WeeklyScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Destinations.Daily.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Daily Screen
        composable(Destinations.Daily.route) {
            DailyScreen(
                onNavigateToAdd = {
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    navController.navigate(Destinations.AddSchedule.createRoute(today))
                },
                onNavigateToEdit = { scheduleId ->
                    navController.navigate(Destinations.EditSchedule.createRoute(scheduleId))
                },
                onNavigateToWeekly = {
                    navController.navigate(Destinations.Weekly.route)
                }
            )
        }

        // Weekly Screen
        composable(Destinations.Weekly.route) {
            WeeklyScreen(
                onNavigateToDaily = { date ->
                    // Navigate back to daily screen with selected date
                    navController.popBackStack()
                },
                onNavigateToAdd = { date ->
                    navController.navigate(Destinations.AddSchedule.createRoute(date))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Schedule Screen
        composable(
            route = Destinations.AddSchedule.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            AddScheduleScreen(
                selectedDate = date,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Schedule Screen
        composable(
            route = Destinations.EditSchedule.route,
            arguments = listOf(
                navArgument("scheduleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
            EditScheduleScreen(
                scheduleId = scheduleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}