package com.example.scorereader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scorereader.ui.library.LibraryScreen
import com.example.scorereader.ui.reader.ReaderScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "library"
    ) {
        composable("library") {
            LibraryScreen { score ->
                navController.navigate("reader/${score.filePath}")
            }
        }

        composable(
            route = "reader/{filePath}",
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                }
            )
        ) {
            ReaderScreen()
        }
    }
}