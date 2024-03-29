package com.shirosoftware.sealprogrammingmobile.ui.navigation

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.shirosoftware.sealprogrammingmobile.ui.screens.camera.CameraScreen
import com.shirosoftware.sealprogrammingmobile.ui.screens.info.InfoScreen
import com.shirosoftware.sealprogrammingmobile.ui.screens.main.MainScreen
import com.shirosoftware.sealprogrammingmobile.ui.screens.settings.SettingsScreen

@ExperimentalAnimationApi
@Composable
fun AppNavHost(navController: NavHostController, sharedViewModel: SharedViewModel) {
    val context = LocalContext.current
    val detectionResult by sharedViewModel.result.collectAsState()

    AnimatedNavHost(
        navController = navController,
        startDestination = "main",
    ) {
        composable("main") {
            MainScreen(
                hiltViewModel(),
                detectionResult = detectionResult,
                onClickCamera = {
                    navController.navigate("camera")
                },
                onClickInfo = {
                    navController.navigate("info")
                }
            )
        }
        composable("info") {
            InfoScreen(
                onBack = {
                    navController.popBackStack()
                },
                onShowLicense = {
                    context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                hiltViewModel(),
                onBack = {
                    navController.popBackStack()
                })
        }
        composable("camera") {
            CameraScreen(
                hiltViewModel(),
                onCompleted = { result ->
                    sharedViewModel.updateResult(result)
                    navController.popBackStack()
                },
                onClickSettings = {
                    navController.navigate("settings")
                },
                onBack = {
                    navController.popBackStack()
                })
        }
    }
}
