package com.j.m3play.ui.utils

import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import com.j.m3play.ui.screens.Screens

fun NavController.backToMain() {
    while (!Screens.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}